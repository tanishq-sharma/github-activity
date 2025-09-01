package com.example.github.rest;

import com.example.github.model.RepositoryActivity;
import com.example.github.service.GitHubApiClient;
import com.example.github.service.GitHubRepositoryActivityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class GitHubActivityController {
    private static final Logger log = LoggerFactory.getLogger(GitHubActivityController.class);

    @GetMapping("/api/activity/{userOrOrg}")
    public ResponseEntity<?> getActivity(
            @PathVariable String userOrOrg,
            @RequestParam(name = "commits", defaultValue = "20") int commits,
            @RequestParam(name = "includeForks", defaultValue = "false") boolean includeForks,
            @RequestHeader(name = "X-GitHub-Token", required = false) String token) {
        try {
            if (commits <= 0) commits = 20;
            if (commits > 100) commits = 100; // API per_page max
            String effectiveToken = token != null ? token : System.getenv("GITHUB_TOKEN");
            GitHubApiClient client = new GitHubApiClient(effectiveToken);
            GitHubRepositoryActivityService service = new GitHubRepositoryActivityService(client);
            List<RepositoryActivity> activities = service.fetchRepositoryActivities(userOrOrg, commits, includeForks);
            return ResponseEntity.ok(activities);
        } catch (IOException e) {
            log.error("IO error fetching activity for {}", userOrOrg, e);
            Throwable cause = e.getCause();
            if (cause instanceof UnknownHostException || cause instanceof ConnectException) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Unable to reach GitHub API (network restriction or connectivity issue): " + cause.getMessage());
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (InterruptedException e) {
            log.error("Interrupted while fetching activity for {}", userOrOrg, e);
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Interrupted");
        } catch (Exception e) {
            log.error("Unexpected error fetching activity for {}", userOrOrg, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error: " + e.getMessage());
        }
    }
}
