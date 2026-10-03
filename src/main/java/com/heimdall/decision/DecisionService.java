package com.heimdall.decision;

import com.heimdall.policy.PolicyDocument;
import com.heimdall.verdict.RiskSnapshotResult;
import com.heimdall.verdict.VerdictInputs;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * Decision tree transcribed verbatim from docs/06-risk-and-decision.md:
 *
 *   if hard_block_rule: BLOCK
 *   else if score >= 76 or verified_critical_security_or_secret: BLOCK
 *   else if score >= 51 or unverified_high_or_critical >= 1 or tests_failed: REVIEW
 *   else if score >= 26: REVIEW
 *   else: MERGE
 *
 * Unverified high-severity is never auto-BLOCK on LLM suspicion alone, and
 * never silently MERGEd either - it forces REVIEW.
 */
@Service
public class DecisionService {

    public Decision decide(RiskSnapshotResult snapshot, VerdictInputs inputs, PolicyDocument policy, Set<String> hardBlockTriggers) {
        Set<String> triggered = new HashSet<>(hardBlockTriggers);
        triggered.retainAll(policy.hardBlocks());

        boolean verifiedCriticalSecurityOrSecret = snapshot.components().getOrDefault("security", 0) >= 40;

        if (!triggered.isEmpty()) {
            return new Decision(Verdict.BLOCK, "failure",
                    "Hard policy rule(s) triggered: " + String.join(", ", triggered));
        }
        if (snapshot.score() >= 76 || verifiedCriticalSecurityOrSecret) {
            return new Decision(Verdict.BLOCK, "failure",
                    "Risk score " + snapshot.score() + " (" + snapshot.band() + ") exceeds the BLOCK threshold, "
                    + "or a critical verified security/secret finding is present.");
        }
        if (snapshot.score() >= 51 || inputs.unverifiedHighOrCriticalCount() >= 1 || inputs.requiredTestsFailed()) {
            return new Decision(Verdict.REVIEW, "neutral",
                    "Risk score " + snapshot.score() + ", " + inputs.unverifiedHighOrCriticalCount()
                    + " unverified high/critical finding(s), tests failed=" + inputs.requiredTestsFailed() + ".");
        }
        if (snapshot.score() >= 26) {
            return new Decision(Verdict.REVIEW, "neutral", "Risk score " + snapshot.score() + " (" + snapshot.band() + ").");
        }
        return new Decision(Verdict.MERGE, "success", "Risk score " + snapshot.score() + " (" + snapshot.band() + "); no blocking conditions.");
    }
}
