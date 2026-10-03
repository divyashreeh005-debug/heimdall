package com.heimdall.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heimdall.jobs.JobQueueService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Normalizes the GitHub payload and idempotently enqueues an AnalysisRun.
 * Idempotency key: (repository_id, pr_number, head_sha) — a re-delivery or a
 * re-run of the same head is a no-op (docs/02-domain-model.md, AnalysisRun).
 */
@Service
public class WebhookIngestionService {

    private static final java.util.Set<String> RELEVANT_ACTIONS =
            java.util.Set.of("opened", "synchronize", "reopened", "ready_for_review");

    private final JdbcTemplate jdbcTemplate;
    private final JobQueueService jobQueueService;
    private final ObjectMapper objectMapper;

    public WebhookIngestionService(JdbcTemplate jdbcTemplate, JobQueueService jobQueueService, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jobQueueService = jobQueueService;
        this.objectMapper = objectMapper;
    }

    /** @return true if this delivery id had not been seen before. */
    @Transactional
    public boolean recordDelivery(String deliveryId, String eventType) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO webhook_deliveries (delivery_id, event_type) VALUES (?, ?)",
                    deliveryId, eventType);
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            return false;
        }
    }

    @Transactional
    public void handlePullRequestEvent(String rawPayload) {
        JsonNode root = parse(rawPayload);
        String action = root.path("action").asText();
        if (!RELEVANT_ACTIONS.contains(action)) {
            return; // e.g. "labeled", "closed" - not analysis triggers in Phase 1
        }

        JsonNode pr = root.path("pull_request");
        JsonNode repo = root.path("repository");

        UUID repositoryId = upsertRepository(repo);
        UUID pullRequestId = upsertPullRequest(repositoryId, pr);
        String headSha = pr.path("head").path("sha").asText();

        UUID runId = insertAnalysisRunIfAbsent(repositoryId, pullRequestId, pr.path("number").asInt(), headSha, root);
        if (runId != null) {
            jobQueueService.enqueue(runId);
        }
    }

    private UUID upsertRepository(JsonNode repo) {
        // Simplified for the skeleton: assumes org already provisioned via
        // GitHub App installation flow (identity module). Real implementation
        // resolves org_id from the installation id in the payload.
        String nativeId = repo.path("id").asText();
        String fullName = repo.path("full_name").asText();
        return jdbcTemplate.queryForObject("""
            INSERT INTO repositories (org_id, provider, native_id, full_name, default_branch)
            SELECT id, 'github', ?, ?, ?
            FROM organizations LIMIT 1
            ON CONFLICT (org_id, provider, native_id)
            DO UPDATE SET full_name = EXCLUDED.full_name
            RETURNING id
            """, UUID.class, nativeId, fullName, repo.path("default_branch").asText("main"));
    }

    private UUID upsertPullRequest(UUID repositoryId, JsonNode pr) {
        int number = pr.path("number").asInt();
        return jdbcTemplate.queryForObject("""
            INSERT INTO pull_requests (repository_id, number, native_id, title, author_login, base_sha, head_sha, state)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (repository_id, number)
            DO UPDATE SET head_sha = EXCLUDED.head_sha, title = EXCLUDED.title, updated_at = now()
            RETURNING id
            """, UUID.class,
            repositoryId, number, pr.path("id").asText(), pr.path("title").asText(),
            pr.path("user").path("login").asText(), pr.path("base").path("sha").asText(),
            pr.path("head").path("sha").asText(), pr.path("state").asText("open"));
    }

    private UUID insertAnalysisRunIfAbsent(UUID repositoryId, UUID pullRequestId, int prNumber, String headSha, JsonNode root) {
        try {
            return jdbcTemplate.queryForObject("""
                INSERT INTO analysis_runs (org_id, repository_id, pull_request_id, pr_number, head_sha, state)
                SELECT org_id, id, ?, ?, ?, 'queued' FROM repositories WHERE id = ?
                RETURNING id
                """, UUID.class, pullRequestId, prNumber, headSha, repositoryId);
        } catch (DataIntegrityViolationException alreadyQueued) {
            // Same (repository_id, pr_number, head_sha) already has a run: idempotent no-op.
            return null;
        }
    }

    private JsonNode parse(String rawPayload) {
        try {
            return objectMapper.readTree(rawPayload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed webhook payload", e);
        }
    }
}
