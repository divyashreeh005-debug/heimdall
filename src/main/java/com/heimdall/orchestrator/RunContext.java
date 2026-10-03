package com.heimdall.orchestrator;

import java.util.UUID;

record RunContext(
        UUID runId,
        UUID orgId,
        UUID repositoryId,
        UUID pullRequestId,
        int prNumber,
        String repoFullName,
        String cloneUrl,
        String baseSha,
        String headSha
) { }
