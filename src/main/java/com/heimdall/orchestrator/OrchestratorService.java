package com.heimdall.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heimdall.agents.Agent;
import com.heimdall.agents.FindingCandidate;
import com.heimdall.audit.AuditService;
import com.heimdall.common.Fingerprint;
import com.heimdall.common.VerificationStatus;
import com.heimdall.decision.Decision;
import com.heimdall.decision.DecisionService;
import com.heimdall.notification.CommentService;
import com.heimdall.notification.GitHubCheckPublisher;
import com.heimdall.policy.PolicyDocument;
import com.heimdall.policy.PolicyLoader;
import com.heimdall.skills.LanguageSkill;
import com.heimdall.skills.SkillLoader;
import com.heimdall.verdict.RiskSnapshotResult;
import com.heimdall.verdict.VerdictEngine;
import com.heimdall.verdict.VerdictInputs;
import com.heimdall.verification.VerificationGuard;
import com.heimdall.verification.VerificationResult;
import com.heimdall.workspace.AnalysisWorkspace;
import com.heimdall.workspace.AnalysisWorkspaceService;
import com.heimdall.workspace.ChangedFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * The DAG from docs/00-vision.md, wired end to end:
 *
 *   Analyze -> Find -> Verify -> Filter hallucinations -> Impact -> Score
 *   -> Explain -> MERGE | REVIEW | BLOCK
 *
 * One call to runJob() processes exactly one claimed analysis_jobs row.
 * Every stage writes an audit event so a run is fully reconstructable from
 * audit_events alone (docs/09-observability-and-audit.md).
 */
@Service
public class OrchestratorService {

    private final JdbcTemplate jdbcTemplate;
    private final AnalysisWorkspaceService workspaceService;
    private final SkillLoader skillLoader;
    private final List<Agent> agents;
    private final VerificationGuard verificationGuard;
    private final VerdictEngine verdictEngine;
    private final DecisionService decisionService;
    private final PolicyLoader policyLoader;
    private final AuditService auditService;
    private final GitHubCheckPublisher checkPublisher;
    private final CommentService commentService;
    private final ObjectMapper objectMapper;

    public OrchestratorService(JdbcTemplate jdbcTemplate, AnalysisWorkspaceService workspaceService,
                                SkillLoader skillLoader, List<Agent> agents, VerificationGuard verificationGuard,
                                VerdictEngine verdictEngine, DecisionService decisionService, PolicyLoader policyLoader,
                                AuditService auditService, GitHubCheckPublisher checkPublisher,
                                CommentService commentService, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.workspaceService = workspaceService;
        this.skillLoader = skillLoader;
        this.agents = agents;
        this.verificationGuard = verificationGuard;
        this.verdictEngine = verdictEngine;
        this.decisionService = decisionService;
        this.policyLoader = policyLoader;
        this.auditService = auditService;
        this.checkPublisher = checkPublisher;
        this.commentService = commentService;
        this.objectMapper = objectMapper;
    }

    public void runJob(UUID runId) throws Exception {
        RunContext ctx = loadRunContext(runId);
        markRunning(ctx.runId());
        auditService.record(ctx.orgId(), ctx.runId(), "system", "orchestrator", "run.started",
                Map.of("head_sha", ctx.headSha()));

        AnalysisWorkspace workspace = workspaceService.prepare(ctx.cloneUrl(), ctx.baseSha(), ctx.headSha());
        auditService.record(ctx.orgId(), ctx.runId(), "system", "orchestrator", "workspace.ready",
                Map.of("changed_files", workspace.changedFiles().size()));

        LanguageSkill skill = skillLoader.forLanguage(detectDominantLanguage(workspace))
                .orElseGet(() -> skillLoader.forLanguage("java").orElseThrow());

        List<FindingCandidate> candidates = new ArrayList<>();
        for (Agent agent : agents) {
            candidates.addAll(agent.analyze(workspace, skill));
        }
        auditService.record(ctx.orgId(), ctx.runId(), "system", "orchestrator", "findings.proposed",
                Map.of("count", candidates.size()));

        List<VerdictInputs.VerifiedFinding> verified = new ArrayList<>();
        long unverifiedHighOrCritical = 0;
        boolean secretVerified = false;

        for (FindingCandidate candidate : candidates) {
            UUID findingId = persistFinding(ctx, candidate);
            VerificationResult result = verificationGuard.verify(candidate, workspace);
            persistVerification(findingId, result);

            verified.add(new VerdictInputs.VerifiedFinding(candidate.claimClass(), candidate.severity(), result.status()));
            if (result.status() == VerificationStatus.UNVERIFIED
                    && (candidate.severity().name().equals("high") || candidate.severity().name().equals("critical"))) {
                unverifiedHighOrCritical++;
            }
            if (result.status() == VerificationStatus.VERIFIED && "secret".equals(candidate.claimClass())) {
                secretVerified = true;
            }
        }
        auditService.record(ctx.orgId(), ctx.runId(), "system", "verification-guard", "findings.verified",
                Map.of("verified_or_rejected", candidates.size()));

        // Phase 1 skeleton: test execution (sandbox) and richer impact banding are
        // structurally wired but not fully implemented; see docs/12-roadmap.md Phase 1b.
        VerdictInputs inputs = new VerdictInputs(
                verified,
                unverifiedHighOrCritical,
                false, // TODO: wire sandbox test runner result
                impactBandFrom(workspace),
                workspace.changedFiles().size(),
                estimateChangedLines(workspace),
                false,
                false,
                secretVerified
        );

        PolicyDocument policy = policyLoader.activeFor(ctx.orgId());
        RiskSnapshotResult snapshot = verdictEngine.score(inputs, policy);
        persistRiskSnapshot(ctx.runId(), snapshot);

        Set<String> hardBlockTriggers = detectHardBlockTriggers(candidates, inputs);
        Decision decision = decisionService.decide(snapshot, inputs, policy, hardBlockTriggers);
        persistDecision(ctx.runId(), decision);

        auditService.record(ctx.orgId(), ctx.runId(), "system", "verdict-engine", "run.decided",
                Map.of("verdict", decision.verdict().name(), "score", snapshot.score(), "band", snapshot.band()));

        publishToGitHub(ctx, decision);
        markSucceeded(ctx.runId());
    }

    private RunContext loadRunContext(UUID runId) {
        return jdbcTemplate.queryForObject("""
            SELECT ar.id AS run_id, ar.org_id, ar.repository_id, ar.pull_request_id, ar.pr_number,
                   r.full_name, r.provider, ar.head_sha, pr.base_sha
            FROM analysis_runs ar
            JOIN repositories r ON r.id = ar.repository_id
            JOIN pull_requests pr ON pr.id = ar.pull_request_id
            WHERE ar.id = ?
            """, (rs, i) -> new RunContext(
                (UUID) rs.getObject("run_id"), (UUID) rs.getObject("org_id"), (UUID) rs.getObject("repository_id"),
                (UUID) rs.getObject("pull_request_id"), rs.getInt("pr_number"), rs.getString("full_name"),
                "https://github.com/" + rs.getString("full_name") + ".git",
                rs.getString("base_sha"), rs.getString("head_sha")), runId);
    }

    private void markRunning(UUID runId) {
        jdbcTemplate.update("UPDATE analysis_runs SET state = 'running' WHERE id = ?", runId);
    }

    private void markSucceeded(UUID runId) {
        jdbcTemplate.update("UPDATE analysis_runs SET state = 'succeeded', finished_at = now() WHERE id = ?", runId);
    }

    @Transactional
    public void markFailed(UUID runId, String error) {
        jdbcTemplate.update("UPDATE analysis_runs SET state = 'failed', finished_at = now() WHERE id = ?", runId);
    }

    private UUID persistFinding(RunContext ctx, FindingCandidate c) {
        String path = c.locations().isEmpty() ? "" : c.locations().get(0).path();
        String symbol = c.locations().isEmpty() ? null : c.locations().get(0).symbol();
        String fingerprint = Fingerprint.of(ctx.orgId().toString(), ctx.repositoryId().toString(),
                c.claimClass(), path, symbol, c.agentId());
        try {
            String locationsJson = objectMapper.writeValueAsString(c.locations());
            return jdbcTemplate.queryForObject("""
                INSERT INTO findings (run_id, fingerprint, agent_id, claim_class, title, claim, severity, confidence, locations)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb) RETURNING id
                """, UUID.class, ctx.runId(), fingerprint, c.agentId(), c.claimClass(), c.title(), c.claim(),
                    c.severity().name(), c.confidence(), locationsJson);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void persistVerification(UUID findingId, VerificationResult result) {
        try {
            String methodsJson = objectMapper.writeValueAsString(result.methods());
            jdbcTemplate.update("""
                INSERT INTO verifications (finding_id, status, confidence, reason, methods)
                VALUES (?, ?, ?, ?, ?::jsonb)
                """, findingId, result.status().name(), result.confidence(), result.reason(), methodsJson);
            for (VerificationResult.EvidenceItem ev : result.evidence()) {
                jdbcTemplate.update("""
                    INSERT INTO evidence (finding_id, kind, tool, summary, uri)
                    VALUES (?, ?, ?, ?, ?)
                    """, findingId, ev.kind(), ev.tool(), ev.summary(), ev.uri());
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void persistRiskSnapshot(UUID runId, RiskSnapshotResult snapshot) {
        try {
            String breakdownJson = objectMapper.writeValueAsString(snapshot.components());
            jdbcTemplate.update("""
                INSERT INTO risk_snapshots (run_id, score, band, breakdown, inputs_hash, policy_version, weight_version)
                VALUES (?, ?, ?, ?::jsonb, ?, ?, ?)
                """, runId, snapshot.score(), snapshot.band(), breakdownJson, snapshot.inputsHash(),
                    snapshot.policyVersion(), snapshot.weightVersion());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void persistDecision(UUID runId, Decision decision) {
        jdbcTemplate.update("""
            INSERT INTO decisions (run_id, verdict, check_conclusion, explanation)
            VALUES (?, ?, ?, ?)
            """, runId, decision.verdict().name(), decision.checkConclusion(), decision.explanation());
    }

    private void publishToGitHub(RunContext ctx, Decision decision) {
        try {
            // Installation-token exchange is not implemented in this skeleton
            // (see notification.GitHubCheckPublisher javadoc); best-effort only.
            String installationToken = System.getenv().getOrDefault("GITHUB_DEV_TOKEN", "");
            if (!installationToken.isBlank()) {
                checkPublisher.publish(ctx.repoFullName(), ctx.headSha(), decision, installationToken);
                commentService.postSummary(ctx.repoFullName(), ctx.prNumber(), decision, installationToken);
            }
        } catch (Exception notifyFailure) {
            auditService.record(ctx.orgId(), ctx.runId(), "system", "notification", "github.publish.failed",
                    Map.of("error", String.valueOf(notifyFailure.getMessage())));
        }
    }

    private String detectDominantLanguage(AnalysisWorkspace workspace) {
        Map<String, Long> counts = new HashMap<>();
        for (ChangedFile f : workspace.changedFiles()) {
            String ext = f.path().contains(".") ? f.path().substring(f.path().lastIndexOf('.') + 1) : "";
            String lang = switch (ext) {
                case "java" -> "java";
                case "py" -> "python";
                case "ts", "tsx" -> "typescript";
                case "go" -> "go";
                case "sql" -> "sql";
                case "tf" -> "terraform";
                default -> null;
            };
            if (lang != null) counts.merge(lang, 1L, Long::sum);
        }
        return counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("java");
    }

    private String impactBandFrom(AnalysisWorkspace workspace) {
        // Phase 1: shallow heuristic (file count) stands in for the real import-graph
        // impact agent output; docs/12-roadmap.md marks richer impact as Phase 2.
        int n = workspace.changedFiles().size();
        if (n > 15) return "high";
        if (n > 5) return "medium";
        return "low";
    }

    private int estimateChangedLines(AnalysisWorkspace workspace) {
        return workspace.changedFiles().size() * 40; // placeholder until diffstat parsing is wired
    }

    private Set<String> detectHardBlockTriggers(List<FindingCandidate> candidates, VerdictInputs inputs) {
        Set<String> triggers = new HashSet<>();
        if (inputs.requiredTestsFailed()) triggers.add("required_tests_failed");
        for (FindingCandidate c : candidates) {
            if ("security".equals(c.claimClass()) && c.claim().toLowerCase().contains("sql injection")) {
                triggers.add("verified_sqli");
            }
        }
        return triggers;
    }
}
