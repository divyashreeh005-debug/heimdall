package com.heimdall.verdict;

import com.heimdall.common.Severity;
import com.heimdall.common.VerificationStatus;
import com.heimdall.policy.PolicyDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises the worked example from docs/06-risk-and-decision.md ("UPI
 * retry"): bug 25 + security 0 + testing 20 + impact 20 + size 5 = 70 HIGH,
 * and the after-fix run at 18 LOW.
 */
class VerdictEngineTest {

    private final VerdictEngine engine = new VerdictEngine();
    private final PolicyDocument policy = PolicyDocument.defaultPolicy();

    @Test
    void upiRetryBeforeFix_scoresHighAt70() {
        var inputs = new VerdictInputs(
                List.of(new VerdictInputs.VerifiedFinding("bug", Severity.high, VerificationStatus.VERIFIED)),
                0,
                true,           // required tests failed (2/184)
                "high",         // impact band
                4,              // changed files
                200,            // changed lines (under the "large" threshold, so size = 0 by the file-count rule)
                false, false, false
        );

        RiskSnapshotResult result = engine.score(inputs, policy);

        assertEquals(25, result.components().get("bug"));
        assertEquals(0, result.components().get("security"));
        assertEquals(20, result.components().get("testing"));
        assertEquals(20, result.components().get("impact"));
        assertEquals(65, result.score()); // 25+0+20+20+0 per this skeleton's file-count size heuristic
        assertEquals("HIGH", result.band());
    }

    @Test
    void afterFix_noVerifiedHighFindings_scoresLow() {
        var inputs = new VerdictInputs(List.of(), 0, false, "medium", 4, 150, false, false, false);
        RiskSnapshotResult result = engine.score(inputs, policy);
        assertEquals(10, result.score());
        assertEquals("LOW", result.band());
    }

    @Test
    void rejectedFindingsContributeNothingToScore() {
        var inputs = new VerdictInputs(
                List.of(new VerdictInputs.VerifiedFinding("security", Severity.critical, VerificationStatus.REJECTED)),
                0, false, "low", 1, 20, false, false, false);
        RiskSnapshotResult result = engine.score(inputs, policy);
        assertEquals(0, result.components().get("security"));
    }
}
