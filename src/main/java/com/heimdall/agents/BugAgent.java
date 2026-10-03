package com.heimdall.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class BugAgent extends AbstractLlmAgent {
    public BugAgent(OllamaClient ollamaClient, ObjectMapper objectMapper) { super(ollamaClient, objectMapper); }
    @Override public String id() { return "agent.bug"; }
    @Override protected String claimClassFocus() { return "bug"; }
}
