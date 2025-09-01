package com.example.github.service;

import com.example.github.model.GitHubCommit;
import com.example.github.model.GitHubRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class GitHubApiClient {
    private static final Logger log = LoggerFactory.getLogger(GitHubApiClient.class);
    private static final String BASE_URL = "https://api.github.com";
    private final String token;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GitHubApiClient(String token) {
        this.token = token;
        this.httpClient = HttpClient.newBuilder().build();
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<GitHubRepository> listAllRepositories(String userOrOrg) throws IOException, InterruptedException {
        List<GitHubRepository> all = new ArrayList<>();
        int page = 1;
        while (true) {
            String url = BASE_URL + "/users/" + userOrOrg + "/repos?per_page=100&page=" + page;
            log.debug("Requesting repos page {} for {} -> {}", page, userOrOrg, url);
            HttpRequest request = baseRequest(url).GET().build();
            HttpResponse<String> resp = sendWithRateLimit(request);
            log.debug("Response repos page {} status={} length={}", page, resp.statusCode(), resp.body() == null ? 0 : resp.body().length());
            if (resp.statusCode() == 404) {
                throw new IOException("User or organization not found: " + userOrOrg);
            }
            if (resp.statusCode() >= 400) {
                throw new IOException("Failed to list repositories. Status=" + resp.statusCode() + " body=" + truncate(resp.body()));
            }
            List<GitHubRepository> pageData = objectMapper.readValue(resp.body(), new TypeReference<>(){});
            if (pageData.isEmpty()) break;
            all.addAll(pageData);
            if (pageData.size() < 100) break; // last page
            page++;
        }
        return all;
    }

    public List<GitHubCommit> listRecentCommits(String fullRepoName, int limit) throws IOException, InterruptedException {
        // limit <= 100 for GitHub API per_page maximum
        int perPage = Math.min(limit, 100);
        String url = BASE_URL + "/repos/" + fullRepoName + "/commits?per_page=" + perPage;
        log.debug("Requesting commits {} perPage={} -> {}", fullRepoName, perPage, url);
        HttpRequest request = baseRequest(url).GET().build();
        HttpResponse<String> resp = sendWithRateLimit(request);
        log.debug("Response commits {} status={} length={}", fullRepoName, resp.statusCode(), resp.body() == null ? 0 : resp.body().length());
        if (resp.statusCode() == 404) {
            log.warn("Repo not found when fetching commits: {}", fullRepoName);
            return List.of();
        }
        if (resp.statusCode() >= 400) {
            throw new IOException("Failed to list commits for repo " + fullRepoName + " status=" + resp.statusCode() + " body=" + truncate(resp.body()));
        }
        return objectMapper.readValue(resp.body(), new TypeReference<>(){});
    }

    private String truncate(String body) {
        if (body == null) return null;
        return body.length() > 300 ? body.substring(0,300) + "..." : body;
    }

    private HttpRequest.Builder baseRequest(String url) {
        HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create(url))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "GitHub-Activity-Connector/1.0");
        if (token != null && !token.isBlank()) {
            b.header("Authorization", "Bearer " + token.trim());
        }
        return b;
    }

    private HttpResponse<String> sendWithRateLimit(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 403 && response.headers().firstValue("X-RateLimit-Remaining").orElse("1").equals("0")) {
            long resetEpoch = response.headers().firstValue("X-RateLimit-Reset").map(Long::parseLong).orElse(0L);
            long waitMillis = Math.max(0, (resetEpoch * 1000) - System.currentTimeMillis()) + 1000;
            log.warn("Rate limit hit. Sleeping for {} ms until reset at {}", waitMillis, Instant.ofEpochSecond(resetEpoch));
            TimeUnit.MILLISECONDS.sleep(waitMillis);
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }
        // If still rate limited, caller will see 403.
        return response;
    }
}
