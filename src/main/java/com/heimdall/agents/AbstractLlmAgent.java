package com.heimdall.agents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heimdall.common.Severity;
import com.heimdall.common.UntrustedText;
import com.heimdall.skills.LanguageSkill;
import com.heimdall.workspace.AnalysisWorkspace;
import com.heimdall.workspace.ChangedFile;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared plumbing for the four Phase 1 agents (Bug, Security, Test, Impact).
 * Builds a delimited prompt from the diff, calls Ollama, parses the JSON
 * response into FindingCandidate. Subclasses only supply their claim class
 * and any pre-filtering of which changed files matter to them.
 */
public abstract class AbstractLlmAgent implements Agent {

    protected final OllamaClient ollamaClient;
    protected final ObjectMapper objectMapper;

    protected AbstractLlmAgent(OllamaClient ollamaClient, ObjectMapper objectMapper) {
        this.ollamaClient = ollamaClient;
        this.objectMapper = objectMapper;
    }

    protected abstract String claimClassFocus();

    @Override
    public List<FindingCandidate> analyze(AnalysisWorkspace workspace, LanguageSkill skill) {
        String diffText = buildDiffExcerpt(workspace);
        UntrustedText untrusted = new UntrustedText("diff", diffText);

        String userPrompt = """
            Focus claim class: %s
            Skill: %s@%s

            %s

            Respond with a JSON array of findings, each matching heimdall.finding.v1
            (fields: claim_class, title, claim, severity, confidence, locations).
            """.formatted(claimClassFocus(), skill.skillId(), skill.version(), untrusted.toDelimitedBlock());

        String raw = ollamaClient.generate(skill.promptTemplate(), userPrompt);
        return parseFindings(raw);
    }

    private String buildDiffExcerpt(AnalysisWorkspace workspace) {
        StringBuilder sb = new StringBuilder();
        for (ChangedFile file : workspace.changedFiles()) {
            sb.append("--- ").append(file.status()).append(' ').append(file.path()).append('\n');
            if (!"D".equals(file.status())) {
                try {
                    String content = Files.readString(workspace.root().resolve(file.path()));
                    // Cap per-file excerpt so one huge generated file doesn't blow the context window.
                    sb.append(content, 0, Math.min(content.length(), 4000)).append('\n');
                } catch (IOException ignored) {
                    // Binary or unreadable file: skip content, keep the path in the excerpt.
                }
            }
        }
        return sb.toString();
    }

    private List<FindingCandidate> parseFindings(String rawJson) {
        List<FindingCandidate> out = new ArrayList<>();
        try {
            JsonNode arr = objectMapper.readTree(rawJson);
            JsonNode items = arr.isArray() ? arr : arr.path("findings");
            for (JsonNode item : items) {
                out.add(toCandidate(item));
            }
        } catch (Exception malformedModelOutput) {
            // A malformed or empty model response yields zero candidates rather than
            // failing the run - the agent's output is advisory, never load-bearing on its own.
        }
        return out;
    }

    private FindingCandidate toCandidate(JsonNode item) {
        List<FindingCandidate.Location> locations = new ArrayList<>();
        for (JsonNode loc : item.path("locations")) {
            locations.add(new FindingCandidate.Location(
                    loc.path("path").asText(),
                    loc.path("start_line").asInt(0),
                    loc.has("end_line") ? loc.path("end_line").asInt() : null,
                    loc.path("symbol").asText(null)));
        }
        return new FindingCandidate(
                id(),
                item.path("claim_class").asText(claimClassFocus()),
                item.path("title").asText("Untitled finding"),
                item.path("claim").asText(""),
                parseSeverity(item.path("severity").asText("medium")),
                item.path("confidence").asDouble(0.5),
                locations,
                List.of());
    }

    private Severity parseSeverity(String s) {
        try {
            return Severity.valueOf(s.toLowerCase());
        } catch (Exception e) {
            return Severity.medium;
        }
    }
}
