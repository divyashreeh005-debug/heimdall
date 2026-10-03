package com.heimdall.policy;

import java.util.List;
import java.util.Map;

/**
 * Java shape of docs/policies/example-policy.yaml (schema heimdall.policy.v1).
 * Loaded per org; org admins PUT a new version via the Console (Phase 1b editor).
 */
public record PolicyDocument(
        String version,
        boolean analyzeDrafts,
        Verdict verdict,
        Size size,
        List<String> hardBlocks,
        ProtectedPaths protectedPaths,
        boolean testsRequired
) {
    public record Verdict(String weightVersion, boolean reviewFailsCheck, Map<String, Integer> weights) { }
    public record Size(int largeFiles, int largeLines, int mediumFiles, int mediumLines) { }
    public record ProtectedPaths(String action, List<String> globs) { }

    public static PolicyDocument defaultPolicy() {
        Map<String, Integer> w = Map.ofEntries(
                Map.entry("critical_verified_security", 40),
                Map.entry("high_verified_security", 15),
                Map.entry("high_verified_security_cap", 30),
                Map.entry("high_verified_bug", 25),
                Map.entry("additional_verified_bug", 10),
                Map.entry("bug_cap", 35),
                Map.entry("failing_required_tests", 20),
                Map.entry("impact_high", 20),
                Map.entry("impact_medium", 10),
                Map.entry("large_pr", 10),
                Map.entry("missing_tests_risky", 5),
                Map.entry("missing_tests_cap", 10),
                Map.entry("dependency_high", 15),
                Map.entry("dependency_cap", 25),
                Map.entry("secret_verified", 40));
        return new PolicyDocument(
                "1",
                false,
                new Verdict("verdict.weights.v1", false, w),
                new Size(40, 1200, 15, 400),
                List.of("verified_secret_non_test", "verified_sqli", "verified_command_injection",
                        "verified_rce", "verified_insecure_deserialization", "required_tests_failed"),
                new ProtectedPaths("block", List.of(".github/workflows/**", "deploy/prod/**", "**/*credentials*")),
                true);
    }
}
