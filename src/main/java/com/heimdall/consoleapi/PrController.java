package com.heimdall.consoleapi;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Read models backed by the SQL views from V1__init.sql
 * (pr_overview, finding_inbox, risk_evolution, impact_graph, audit_feed) -
 * see docs/02-domain-model.md "Read models for the Console".
 */
@RestController
@RequestMapping("/api/v1")
public class PrController {

    private final JdbcTemplate jdbcTemplate;

    public PrController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/repositories/{repoId}/pull-requests")
    public List<Map<String, Object>> listPrs(@PathVariable UUID repoId) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM pr_overview WHERE repository_id = ? ORDER BY number DESC", repoId);
    }

    @GetMapping("/pull-requests/{prId}")
    public Map<String, Object> prDetail(@PathVariable UUID prId) {
        Map<String, Object> overview = jdbcTemplate.queryForMap(
                "SELECT * FROM pr_overview WHERE pull_request_id = ?", prId);
        UUID runId = (UUID) overview.get("latest_run_id");

        List<Map<String, Object>> findings = jdbcTemplate.queryForList("""
            SELECT f.*, v.status AS verification_status, v.reason AS verification_reason
            FROM findings f LEFT JOIN verifications v ON v.finding_id = f.id
            WHERE f.run_id = ? ORDER BY
              CASE f.severity WHEN 'critical' THEN 0 WHEN 'high' THEN 1 WHEN 'medium' THEN 2 WHEN 'low' THEN 3 ELSE 4 END
            """, runId);

        List<Map<String, Object>> auditTrail = jdbcTemplate.queryForList(
                "SELECT * FROM audit_feed WHERE pull_request_id = ? ORDER BY created_at DESC LIMIT 100", prId);

        return Map.of("overview", overview, "findings", findings, "audit", auditTrail);
    }

    @GetMapping("/pull-requests/{prId}/risk-evolution")
    public List<Map<String, Object>> riskEvolution(@PathVariable UUID prId) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM risk_evolution WHERE pull_request_id = ? ORDER BY created_at", prId);
    }

    @GetMapping("/inbox")
    public List<Map<String, Object>> verificationInbox(@RequestParam UUID orgId) {
        return jdbcTemplate.queryForList("""
            SELECT fi.* FROM finding_inbox fi
            JOIN analysis_runs ar ON ar.id = fi.run_id
            WHERE ar.org_id = ?
            ORDER BY fi.created_at DESC
            """, orgId);
    }
}
