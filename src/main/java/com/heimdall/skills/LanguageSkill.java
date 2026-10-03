package com.heimdall.skills;

import java.util.List;

/**
 * docs/04-skills-system.md / docs/skills/spec.md: a versioned analysis pack
 * for one language, loaded from docs/skills/language/*.md front matter +
 * body in the real implementation. This skeleton loads a simplified YAML
 * shape from src/main/resources/skills/language/*.yaml (see SkillLoader).
 */
public record LanguageSkill(
        String skillId,
        String version,
        String language,
        List<String> claimClasses,
        String promptTemplate
) { }
