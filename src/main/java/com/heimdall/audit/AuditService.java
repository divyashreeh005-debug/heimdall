package com.heimdall.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Append-only (docs/02-domain-model.md: "no updates, no deletes in the
 * POC"). Payload is JSONB and must never carry raw secrets or full file
 * blobs - callers pass summaries/fingerprints only.
 */
@Service
public class AuditService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AuditService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void record(UUID orgId, UUID runId, String actorType, String actorId, String eventType, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            jdbcTemplate.update("""
                INSERT INTO audit_events (org_id, run_id, actor_type, actor_id, event_type, payload)
                VALUES (?, ?, ?, ?, ?, ?::jsonb)
                """, orgId, runId, actorType, actorId, eventType, json);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize audit payload", e);
        }
    }
}
