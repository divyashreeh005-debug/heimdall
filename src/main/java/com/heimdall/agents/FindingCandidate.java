package com.heimdall.agents;

import com.heimdall.common.Severity;

import java.util.List;

/** In-memory shape of a candidate finding, before persistence/verification. Mirrors docs/schemas/finding.v1.json. */
public record FindingCandidate(
        String agentId,
        String claimClass,
        String title,
        String claim,
        Severity severity,
        double confidence,
        List<Location> locations,
        List<String> skillIds
) {
    public record Location(String path, int startLine, Integer endLine, String symbol) { }
}
