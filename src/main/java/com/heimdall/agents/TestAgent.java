package com.heimdall.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class TestAgent extends AbstractLlmAgent {
    public TestAgent(OllamaClient ollamaClient, ObjectMapper objectMapper) { super(ollamaClient, objectMapper); }
    @Override public String id() { return "agent.test"; }
    @Override protected String claimClassFocus() { return "testing"; }
}
