package com.heimdall.agents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heimdall.config.HeimdallProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin client for Ollama's /api/generate (docs/08-deployment-podman.md:
 * Qwen2.5-Coder 14B + nomic-embed-text, reached over mTLS between pods).
 * Prompt/response text from here is model output, not ground truth — the
 * Verification Guard is what actually proves or rejects a claim.
 */
@Component
public class OllamaClient {

    private final RestClient restClient;
    private final HeimdallProperties properties;
    private final ObjectMapper objectMapper;

    public OllamaClient(HeimdallProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getOllama().getBaseUrl())
                .build();
    }

    public String generate(String systemPrompt, String userPrompt) {
        var body = java.util.Map.of(
                "model", properties.getOllama().getChatModel(),
                "system", systemPrompt,
                "prompt", userPrompt,
                "stream", false,
                "format", "json"
        );
        JsonNode response = restClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        return response == null ? "{}" : response.path("response").asText("{}");
    }

    public float[] embed(String text) {
        var body = java.util.Map.of("model", properties.getOllama().getEmbedModel(), "prompt", text);
        JsonNode response = restClient.post()
                .uri("/api/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        if (response == null) return new float[0];
        JsonNode arr = response.path("embedding");
        float[] out = new float[arr.size()];
        for (int i = 0; i < arr.size(); i++) out[i] = (float) arr.get(i).asDouble();
        return out;
    }
}
