package com.heimdall.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * docs/02-domain-model.md: "Finding fingerprint":
 *   fingerprint = sha256(org_id + repo_id + claim_class + path_normalized + symbol_or_sink + rule_id)
 * Stable identity across commits so risk evolution and suppressions work.
 */
public final class Fingerprint {

    private Fingerprint() { }

    public static String of(String orgId, String repoId, String claimClass,
                             String pathNormalized, String symbolOrSink, String ruleId) {
        String raw = String.join("|", orgId, repoId, claimClass, pathNormalized,
                nullSafe(symbolOrSink), nullSafe(ruleId));
        return sha256Hex(raw);
    }

    /** Secondary match key for when a sink moves inside the same file. */
    public static String fuzzy(String claimClass, String symbol) {
        return sha256Hex(claimClass + "|" + nullSafe(symbol));
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
