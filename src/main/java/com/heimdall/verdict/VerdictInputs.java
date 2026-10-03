package com.heimdall.verdict;

import com.heimdall.common.Severity;
import com.heimdall.common.VerificationStatus;

import java.util.List;

/** Everything the pure-function Verdict Engine needs; assembled by the orchestrator from run data. */
public record VerdictInputs(
        List<VerifiedFinding> verifiedFindings,
        long unverifiedHighOrCriticalCount,
        boolean requiredTestsFailed,
        String impactBand,       // "high" | "medium" | "low"
        int changedFileCount,
        int changedLineCount,
        boolean missingTestsOnRiskyPaths,
        boolean dependencyHighSeverityHit,
        boolean secretVerified
) {
    public record VerifiedFinding(String claimClass, Severity severity, VerificationStatus status) { }
}
