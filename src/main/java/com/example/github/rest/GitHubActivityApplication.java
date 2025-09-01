



package com.example.github.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.github")
public class GitHubActivityApplication {
    public static void main(String[] args) {
        SpringApplication.run(GitHubActivityApplication.class, args);
    }
}

