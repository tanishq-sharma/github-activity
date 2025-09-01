package com.example.github.service;

import com.example.github.model.GitHubCommit;
import com.example.github.model.GitHubRepository;
import com.example.github.model.RepositoryActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GitHubRepositoryActivityService {
    private static final Logger log = LoggerFactory.getLogger(GitHubRepositoryActivityService.class);
    private final GitHubApiClient apiClient;

    public GitHubRepositoryActivityService(GitHubApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public List<RepositoryActivity> fetchRepositoryActivities(String userOrOrg, int commitsPerRepo, boolean includeForks) throws IOException, InterruptedException {
        List<GitHubRepository> repos = apiClient.listAllRepositories(userOrOrg);
        if (!includeForks) {
            repos = repos.stream().filter(r -> !r.isFork()).collect(Collectors.toList());
        }
        List<RepositoryActivity> activities = new ArrayList<>();
        for (GitHubRepository repo : repos) {
            try {
                List<GitHubCommit> commits = apiClient.listRecentCommits(repo.getFullName(), commitsPerRepo);
                activities.add(new RepositoryActivity(repo, commits));
            } catch (Exception e) {
                log.warn("Failed to fetch commits for repo {}: {}", repo.getFullName(), e.getMessage());
            }
        }
        return activities;
    }
}

