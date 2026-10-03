package com.heimdall.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ImpactAgent extends AbstractLlmAgent {
    public ImpactAgent(OllamaClient ollamaClient, ObjectMapper objectMapper) { super(ollamaClient, objectMapper); }
    @Override public String id() { return "agent.impact"; }
    @Override protected String claimClassFocus() { return "impact"; }
}
