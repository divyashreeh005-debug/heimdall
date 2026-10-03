package com.heimdall.verdict;

import com.heimdall.common.Severity;
import com.heimdall.common.VerificationStatus;
import com.heimdall.policy.PolicyDocument;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure function: (VerdictInputs, PolicyDocument) -> RiskSnapshotResult.
 * docs/adr/0004-deterministic-verdict-engine.md: no LLM call here, no
 * randomness, no wall-clock dependence in the math (inputs_hash makes the
 * snapshot reproducible). Weight table transcribed from
 * docs/06-risk-and-decision.md "Default weights".
 *
 * UNVERIFIED / REJECTED / FALSE_POSITIVE findings contribute 0 to the score
 * (they still affect the Decision step, not the score itself).
 */
public class VerdictEngine {

    public RiskSnapshotResult score(VerdictInputs in, PolicyDocument policy) {
        Map<String, Integer> w = policy.verdict().weights();
        Map<String, Integer> components = new LinkedHashMap<>();

        components.put("bug", scoreBug(in, w));
        components.put("security", scoreSecurity(in, w));
        components.put("testing", scoreTesting(in, w));
        components.put("impact", scoreImpact(in, w));
        components.put("size", scoreSize(in, policy.size()));
        components.put("dependency", in.dependencyHighSeverityHit()
                ? clamp(w.getOrDefault("dependency_high", 15), w.getOrDefault("dependency_cap", 25)) : 0);
        components.put("policy", 0); // hard blocks are evaluated separately in DecisionService, not scored additively

        if (in.secretVerified()) {
            components.merge("security", clamp(w.getOrDefault("secret_verified", 40), 40), Integer::sum);
        }

        int total = components.values().stream().mapToInt(Integer::intValue).sum();
        total = Math.max(0, Math.min(100, total));

        return new RiskSnapshotResult(total, bandOf(total), components,
                policy.verdict().weightVersion(), policy.version(), inputsHash(in, policy));
    }

    private int scoreBug(VerdictInputs in, Map<String, Integer> w) {
        long verifiedHighOrCriticalBugs = in.verifiedFindings().stream()
                .filter(f -> "bug".equals(f.claimClass()) && f.status() == VerificationStatus.VERIFIED)
                .filter(f -> f.severity() == Severity.high || f.severity() == Severity.critical)
                .count();
        if (verifiedHighOrCriticalBugs == 0) return 0;
        int score = w.getOrDefault("high_verified_bug", 25);
        if (verifiedHighOrCriticalBugs > 1) {
            score += (int) (verifiedHighOrCriticalBugs - 1) * w.getOrDefault("additional_verified_bug", 10);
        }
        return clamp(score, w.getOrDefault("bug_cap", 35));
    }

    private int scoreSecurity(VerdictInputs in, Map<String, Integer> w) {
        boolean hasCritical = in.verifiedFindings().stream()
                .anyMatch(f -> "security".equals(f.claimClass()) && f.status() == VerificationStatus.VERIFIED
                        && f.severity() == Severity.critical);
        if (hasCritical) {
            return clamp(w.getOrDefault("critical_verified_security", 40), 40);
        }
        long highCount = in.verifiedFindings().stream()
                .filter(f -> "security".equals(f.claimClass()) && f.status() == VerificationStatus.VERIFIED
                        && f.severity() == Severity.high)
                .count();
        return clamp((int) highCount * w.getOrDefault("high_verified_security", 15),
                w.getOrDefault("high_verified_security_cap", 30));
    }

    private int scoreTesting(VerdictInputs in, Map<String, Integer> w) {
        int score = 0;
        if (in.requiredTestsFailed()) score += w.getOrDefault("failing_required_tests", 20);
        if (in.missingTestsOnRiskyPaths()) {
            score += clamp(w.getOrDefault("missing_tests_risky", 5), w.getOrDefault("missing_tests_cap", 10));
        }
        return score;
    }

    private int scoreImpact(VerdictInputs in, Map<String, Integer> w) {
        return switch (in.impactBand() == null ? "" : in.impactBand()) {
            case "high" -> w.getOrDefault("impact_high", 20);
            case "medium" -> w.getOrDefault("impact_medium", 10);
            default -> 0;
        };
    }

    private int scoreSize(VerdictInputs in, PolicyDocument.Size size) {
        if (in.changedFileCount() > size.largeFiles() || in.changedLineCount() > size.largeLines()) return 10;
        return 0;
    }

    private int clamp(int value, int cap) {
        return Math.min(value, cap);
    }

    private String bandOf(int score) {
        if (score <= 25) return "LOW";
        if (score <= 50) return "MEDIUM";
        if (score <= 75) return "HIGH";
        return "CRITICAL";
    }

    private String inputsHash(VerdictInputs in, PolicyDocument policy) {
        try {
            String raw = in.toString() + "|" + policy.verdict().weightVersion() + "|" + policy.version();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("sha256:");
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
