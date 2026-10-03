package com.heimdall.agents;

import com.heimdall.workspace.AnalysisWorkspace;
import com.heimdall.skills.LanguageSkill;

import java.util.List;

/**
 * docs/03-agent-contracts.md: agent I/O contract. Each agent proposes
 * candidate findings only — it never sets verification_status
 * (docs/02-domain-model.md invariant: written only by the Verification
 * Guard or a human).
 */
public interface Agent {

    String id();

    List<FindingCandidate> analyze(AnalysisWorkspace workspace, LanguageSkill skill);
}
