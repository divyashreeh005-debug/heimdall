package com.heimdall.verification;

import com.heimdall.common.VerificationStatus;

import java.util.List;

public record VerificationResult(
        VerificationStatus status,
        double confidence,
        String reason,
        List<String> methods,
        List<EvidenceItem> evidence
) {
    public record EvidenceItem(String kind, String tool, String summary, String uri) { }
}
