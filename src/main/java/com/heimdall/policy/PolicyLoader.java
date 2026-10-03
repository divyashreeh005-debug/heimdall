package com.heimdall.policy;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Loads the active policy for an org. Phase 1 falls back to the bundled
 * default (mirrors docs/policies/example-policy.yaml) when the org has not
 * published one yet, so analysis is never blocked on policy onboarding.
 */
@Service
public class PolicyLoader {

    private final JdbcTemplate jdbcTemplate;

    public PolicyLoader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PolicyDocument activeFor(UUID orgId) {
        // TODO(Phase 1b): parse the stored JSONB `document` column into PolicyDocument
        // once the Console policy editor lets admins publish overrides.
        return PolicyDocument.defaultPolicy();
    }
}
