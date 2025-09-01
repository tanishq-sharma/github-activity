# GitHub Activity Connector

CLI tool to fetch public repositories and recent commits (default last 20) for a GitHub user or organization.

## Features
- Lists all public repositories for a user/org (handles pagination)
- Fetches most recent commits per repository (default 20)
- Rate limit handling with automatic wait & retry
- Outputs structured JSON representing repositories and commits (via CLI or REST API)

## Requirements
- Java 17+
- Maven 3.8+

## Build
```bash
mvn clean package
```
Produces an executable Spring Boot JAR at:
```
target/github-activity-connector-1.0.0.jar
```
(An additional *-jar-with-dependencies.jar* shaded artifact is also produced; you can use either. The plain Boot JAR is executable.)

## Quick Start
1. Build: `mvn clean package`
2. Export a GitHub token (recommended to increase rate limit): `export GITHUB_TOKEN=ghp_yourtoken`
3. Start REST server: `java -jar target/github-activity-connector-1.0.0.jar`
4. Query API: `curl "http://localhost:8080/api/activity/octocat?commits=5" -H "X-GitHub-Token: $GITHUB_TOKEN"`
5. Or run the CLI directly (see below).

---
## REST API Usage
### Run Server
```bash
java -jar target/github-activity-connector-1.0.0.jar
```
Server starts on `http://localhost:8080`.

### Endpoint
`GET /api/activity/{userOrOrg}`

Query Parameters:
- `commits` (int, optional, default 20, max 100) – number of recent commits per repo
- `includeForks` (boolean, optional, default false) – include forked repositories

Headers:
- `X-GitHub-Token: <token>` (optional). If omitted, server will use `GITHUB_TOKEN` environment variable if present.

Response: JSON array (same structure as CLI output) of repository activity objects.

### Example Requests
Basic (unauthenticated – low rate limit):
```bash
curl http://localhost:8080/api/activity/octocat
```
Authenticated:
```bash
curl "http://localhost:8080/api/activity/octocat?commits=10&includeForks=false" \
  -H "X-GitHub-Token: $GITHUB_TOKEN" \
  -o octocat_activity.json
```


### Rate Limiting Notes
- Server pauses automatically and retries once if the core limit is exhausted.
- Provide a token to raise the hourly quota.

---
## CLI Usage
Set your token (recommended):
```bash
export GITHUB_TOKEN=ghp_yourtokenhere
```
Run:
```bash
java -jar target/github-activity-connector-1.0.0-jar-with-dependencies.jar <user-or-org> [options]
```

### Options
- `-t, --token <token>` Provide token explicitly (overrides env var)
- `-c, --commits <n>` Number of commits per repo (default 20, max 100 per GitHub API request)
- `--include-forks` Include forked repositories (default false)
- `-h, --help` Show help
- `-V, --version` Show version

### Example
```bash
java -jar target/github-activity-connector-1.0.0-jar-with-dependencies.jar octocat -c 10 > activity.json
```

### Output Structure (truncated example)
```json
[
  {
    "repository": {
      "name": "Hello-World",
      "fullName": "octocat/Hello-World",
      "htmlUrl": "https://github.com/octocat/Hello-World",
      "description": "My first repository",
      "fork": false,
      "language": "JavaScript"
    },
    "recentCommits": [
      {
        "sha": "abc123...",
        "htmlUrl": "https://github.com/octocat/Hello-World/commit/abc123",
        "commit": {
          "message": "Fix bug",
          "author": {
            "name": "Monalisa Octocat",
            "email": "octocat@github.com",
            "date": "2024-08-16T12:34:56Z"
          }
        }
      }
    ]
  }
]
```

## Error & Rate Limit Handling
- If a 403 with exhausted rate limit is encountered, the client waits until reset (plus 1s) then retries once.
- Non-existent user/org returns a descriptive error.
- Individual repo commit fetch failures are logged and skipped.

