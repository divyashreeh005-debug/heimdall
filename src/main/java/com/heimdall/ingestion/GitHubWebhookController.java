package com.heimdall.ingestion;

import com.heimdall.config.HeimdallProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Entry point of the whole pipeline (docs/01-c4-architecture.md, Level 2:
 * PR -> WH -> Ingress -> API). Verifies HMAC, dedupes on GitHub's
 * X-GitHub-Delivery id, and only then hands off to WebhookIngestionService.
 *
 * PR title/body/commit messages arriving here are untrusted
 * (docs/02-domain-model.md, "Untrusted fields") and are never touched in
 * this controller beyond passing them through as opaque data.
 */
@RestController
@RequestMapping("/webhooks/github")
public class GitHubWebhookController {

    private final HmacVerifier hmacVerifier;
    private final HeimdallProperties properties;
    private final WebhookIngestionService ingestionService;

    public GitHubWebhookController(HmacVerifier hmacVerifier,
                                    HeimdallProperties properties,
                                    WebhookIngestionService ingestionService) {
        this.hmacVerifier = hmacVerifier;
        this.properties = properties;
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public ResponseEntity<String> receive(
            @RequestHeader("X-GitHub-Event") String eventType,
            @RequestHeader("X-GitHub-Delivery") String deliveryId,
            @RequestHeader("X-Hub-Signature-256") String signature,
            @RequestBody String rawPayload) {

        String secret = properties.getGithub().getWebhookSecret();
        if (!hmacVerifier.isValid(rawPayload, signature, secret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("bad signature");
        }

        boolean firstTimeSeen = ingestionService.recordDelivery(deliveryId, eventType);
        if (!firstTimeSeen) {
            // Same delivery redelivered by GitHub: ack without reprocessing.
            return ResponseEntity.ok("duplicate-ignored");
        }

        if ("pull_request".equals(eventType)) {
            ingestionService.handlePullRequestEvent(rawPayload);
        }
        return ResponseEntity.accepted().body("queued");
    }
}
