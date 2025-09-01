package com.example.github.cli;

import com.example.github.model.RepositoryActivity;
import com.example.github.service.GitHubApiClient;
import com.example.github.service.GitHubRepositoryActivityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "github-activity", mixinStandardHelpOptions = true, version = "1.0.0",
        description = "Fetch recent repository activity (latest commits) for a GitHub user or org.")
public class GitHubRepoActivityApp implements Callable<Integer> {

    @Parameters(index = "0", description = "GitHub username or organization")
    private String userOrOrg;

    @Option(names = {"-t", "--token"}, description = "GitHub Personal Access Token (or set GITHUB_TOKEN env var)")
    private String token;

    @Option(names = {"-c", "--commits"}, description = "Number of commits per repo (default: 20)")
    private int commitsPerRepo = 20;

    @Option(names = {"--include-forks"}, description = "Include forked repositories")
    private boolean includeForks = false;

    @Option(names = {"-q", "--quiet"}, description = "Suppress non-error logs in output JSON")
    private boolean quiet;

    @Option(names = {"-o", "--out"}, description = "Output file to write JSON result")
    private Path outFile;

    @Override
    public Integer call() throws Exception {
        String effectiveToken = token != null && !token.isBlank() ? token : System.getenv("GITHUB_TOKEN");
        GitHubApiClient apiClient = new GitHubApiClient(effectiveToken);
        GitHubRepositoryActivityService service = new GitHubRepositoryActivityService(apiClient);
        List<RepositoryActivity> activities = service.fetchRepositoryActivities(userOrOrg, commitsPerRepo, includeForks);
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        if (outFile != null) {
            Files.createDirectories(outFile.toAbsolutePath().getParent());
            mapper.writeValue(outFile.toFile(), activities);
            System.out.println("Wrote output to " + outFile.toAbsolutePath());
        } else {
            PrintWriter out = new PrintWriter(System.out, true);
            mapper.writeValue(out, activities);
        }
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new GitHubRepoActivityApp()).execute(args);
        System.exit(exitCode);
    }
}
