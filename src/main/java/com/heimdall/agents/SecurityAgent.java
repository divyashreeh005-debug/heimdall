package com.heimdall.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class SecurityAgent extends AbstractLlmAgent {
    public SecurityAgent(OllamaClient ollamaClient, ObjectMapper objectMapper) { super(ollamaClient, objectMapper); }
    @Override public String id() { return "agent.security"; }
    @Override protected String claimClassFocus() { return "security"; }
}
