package com.heimdall.verification;

import com.heimdall.agents.FindingCandidate;
import com.heimdall.common.Severity;
import com.heimdall.common.VerificationStatus;
import com.heimdall.workspace.AnalysisWorkspace;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * docs/adr/0003-tool-first-verification.md and
 * docs/adr/0004-deterministic-verdict-engine.md: an LLM claim never becomes
 * VERIFIED on its own prose. Every candidate is checked against a
 * deterministic tool signal before its status can move off UNVERIFIED.
 *
 * Phase 1 implements two concrete, tool-first checks that map directly to
 * the exit criteria in docs/12-roadmap.md:
 *   1. Raw SQL string concatenation -> pattern-based static check (tool-verifiable).
 *   2. Claims about already-parameterized SQL -> rejected as false positives
 *      when the tool finds no concatenation at the cited location.
 * Everything else Phase 1 does not have a tool for stays UNVERIFIED and is
 * routed to the human verification inbox rather than silently trusted.
 */
@Service
public class VerificationGuard {

    private static final java.util.regex.Pattern SQL_CONCAT =
            java.util.regex.Pattern.compile("(?i)(SELECT|INSERT|UPDATE|DELETE)\\b.*\\+\\s*\\w+");

    public VerificationResult verify(FindingCandidate candidate, AnalysisWorkspace workspace) {
        if (!"security".equals(candidate.claimClass()) || candidate.locations().isEmpty()) {
            return unverified(candidate, "No deterministic tool available for this claim class in Phase 1.");
        }

        FindingCandidate.Location loc = candidate.locations().get(0);
        String snippet = readSnippet(workspace, loc);
        if (snippet == null) {
            return unverified(candidate, "Cited location could not be read from the workspace.");
        }

        boolean concatenationFound = SQL_CONCAT.matcher(snippet).find();
        boolean claimsInjectionRisk = candidate.claim().toLowerCase().contains("sql injection")
                || candidate.claim().toLowerCase().contains("concat");

        if (claimsInjectionRisk && concatenationFound) {
            return verified(candidate, "static_analysis",
                    "Concatenated SQL confirmed at cited location by pattern check.");
        }
        if (claimsInjectionRisk && !concatenationFound) {
            return rejected(candidate,
                    "Claim asserts SQL injection but no string-concatenated SQL was found at the cited location; "
                    + "likely refers to already-parameterized code.");
        }
        return unverified(candidate, "Claim class matched but no concrete evidence pattern applied.");
    }

    private String readSnippet(AnalysisWorkspace workspace, FindingCandidate.Location loc) {
        try {
            var lines = Files.readAllLines(workspace.root().resolve(loc.path()));
            int start = Math.max(0, loc.startLine() - 3);
            int end = Math.min(lines.size(), (loc.endLine() != null ? loc.endLine() : loc.startLine()) + 3);
            return String.join("\n", lines.subList(start, end));
        } catch (Exception e) {
            return null;
        }
    }

    private VerificationResult verified(FindingCandidate c, String toolKind, String reason) {
        List<VerificationResult.EvidenceItem> evidence = new ArrayList<>();
        evidence.add(new VerificationResult.EvidenceItem(toolKind, "sql-concat-pattern", reason, null));
        // Invariant (docs/02-domain-model.md): VERIFIED + severity=critical requires
        // at least one non-LLM evidence kind - satisfied here by the static_analysis item.
        return new VerificationResult(VerificationStatus.VERIFIED, 0.9, reason, List.of("static_analysis"), evidence);
    }

    private VerificationResult rejected(FindingCandidate c, String reason) {
        return new VerificationResult(VerificationStatus.REJECTED, 0.85, reason, List.of("static_analysis"), List.of());
    }

    private VerificationResult unverified(FindingCandidate c, String reason) {
        return new VerificationResult(VerificationStatus.UNVERIFIED, 0.0, reason, List.of(), List.of());
    }
}
