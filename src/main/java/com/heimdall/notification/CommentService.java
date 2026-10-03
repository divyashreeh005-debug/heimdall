package com.heimdall.notification;

import com.heimdall.config.HeimdallProperties;
import com.heimdall.decision.Decision;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Posts the summary comment alongside the Check (docs/12-roadmap.md Phase 1 must-have). */
@Component
public class CommentService {

    private final RestClient restClient;

    public CommentService(HeimdallProperties properties) {
        this.restClient = RestClient.builder().baseUrl(properties.getGithub().getApiBaseUrl()).build();
    }

    public void postSummary(String repoFullName, int prNumber, Decision decision, String installationToken) {
        String body = "## Heimdall verdict: **" + decision.verdict() + "**\n\n" + decision.explanation()
                + "\n\n_Findings, evidence, and audit trail: see the Heimdall Console._";
        restClient.post()
                .uri("/repos/{repo}/issues/{number}/comments", repoFullName, prNumber)
                .header("Authorization", "Bearer " + installationToken)
                .header("Accept", "application/vnd.github+json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("body", body))
                .retrieve()
                .toBodilessEntity();
    }
}
