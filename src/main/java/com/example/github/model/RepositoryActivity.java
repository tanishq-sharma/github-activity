package com.example.github.model;

import java.util.List;

public class RepositoryActivity {
    private GitHubRepository repository;
    private List<GitHubCommit> recentCommits;

    public RepositoryActivity() {}

    public RepositoryActivity(GitHubRepository repository, List<GitHubCommit> recentCommits) {
        this.repository = repository;
        this.recentCommits = recentCommits;
    }

    public GitHubRepository getRepository() { return repository; }
    public void setRepository(GitHubRepository repository) { this.repository = repository; }
    public List<GitHubCommit> getRecentCommits() { return recentCommits; }
    public void setRecentCommits(List<GitHubCommit> recentCommits) { this.recentCommits = recentCommits; }
}

