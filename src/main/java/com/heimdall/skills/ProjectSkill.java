package com.heimdall.skills;

public record ProjectSkill(
        java.util.UUID repositoryId,
        String status, // draft | active | archived
        String summary,
        java.util.List<String> conventions
) { }
