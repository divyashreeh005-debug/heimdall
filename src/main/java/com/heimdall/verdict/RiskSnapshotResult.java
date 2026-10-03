package com.heimdall.verdict;

import java.util.Map;

public record RiskSnapshotResult(
        int score,
        String band,
        Map<String, Integer> components,
        String weightVersion,
        String policyVersion,
        String inputsHash
) { }
