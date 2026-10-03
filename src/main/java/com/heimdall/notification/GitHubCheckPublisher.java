package com.heimdall.notification;

import com.heimdall.config.HeimdallProperties;
import com.heimdall.decision.Decision;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Publishes the `heimdall/verdict` Check (docs/12-roadmap.md Phase 1
 * must-have; docs/06-risk-and-decision.md check_conclusion mapping:
 * MERGE -> success, REVIEW -> neutral, BLOCK -> failure).
 *
 * Auth against GitHub uses a GitHub App installation token in the real
 * deployment (JWT signed with the App's private key, exchanged for an
 * installation token). That token-exchange step is out of scope for this
 * skeleton; installationToken is passed in already-resolved.
 */
@Component
public class GitHubCheckPublisher {

    private final RestClient restClient;

    public GitHubCheckPublisher(HeimdallProperties properties) {
        this.restClient = RestClient.builder().baseUrl(properties.getGithub().getApiBaseUrl()).build();
    }

    public void publish(String repoFullName, String headSha, Decision decision, String installationToken) {
        var body = java.util.Map.of(
                "name", "heimdall/verdict",
                "head_sha", headSha,
                "status", "completed",
                "conclusion", decision.checkConclusion(),
                "output", java.util.Map.of(
                        "title", "Heimdall: " + decision.verdict(),
                        "summary", decision.explanation()));

        restClient.post()
                .uri("/repos/{repo}/check-runs", repoFullName)
                .header("Authorization", "Bearer " + installationToken)
                .header("Accept", "application/vnd.github+json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
