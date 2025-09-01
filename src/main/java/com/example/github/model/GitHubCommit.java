package com.example.github.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubCommit {
    private String sha;
    @JsonProperty("html_url")
    private String htmlUrl;
    private Commit commit;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Commit {
        private String message;
        private GitHubCommitAuthor author;

        public Commit() {}

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public GitHubCommitAuthor getAuthor() { return author; }
        public void setAuthor(GitHubCommitAuthor author) { this.author = author; }
    }

    public GitHubCommit() {}

    public String getSha() { return sha; }
    public void setSha(String sha) { this.sha = sha; }
    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }
    public Commit getCommit() { return commit; }
    public void setCommit(Commit commit) { this.commit = commit; }
}

