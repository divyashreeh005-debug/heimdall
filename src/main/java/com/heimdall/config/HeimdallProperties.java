package com.heimdall.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "heimdall")
public class HeimdallProperties {

    private final Jwt jwt = new Jwt();
    private final Github github = new Github();
    private final Ollama ollama = new Ollama();
    private final Worker worker = new Worker();

    public Jwt getJwt() { return jwt; }
    public Github getGithub() { return github; }
    public Ollama getOllama() { return ollama; }
    public Worker getWorker() { return worker; }

    public static class Jwt {
        private String secret;
        private String issuer;
        private int ttlMinutes;
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public int getTtlMinutes() { return ttlMinutes; }
        public void setTtlMinutes(int ttlMinutes) { this.ttlMinutes = ttlMinutes; }
    }

    public static class Github {
        private String appId;
        private String privateKeyPath;
        private String webhookSecret;
        private String apiBaseUrl;
        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }
        public String getPrivateKeyPath() { return privateKeyPath; }
        public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }
        public String getWebhookSecret() { return webhookSecret; }
        public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
        public String getApiBaseUrl() { return apiBaseUrl; }
        public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
    }

    public static class Ollama {
        private String baseUrl;
        private String chatModel;
        private String embedModel;
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getChatModel() { return chatModel; }
        public void setChatModel(String chatModel) { this.chatModel = chatModel; }
        public String getEmbedModel() { return embedModel; }
        public void setEmbedModel(String embedModel) { this.embedModel = embedModel; }
    }

    public static class Worker {
        private long pollIntervalMs = 2000;
        private String workerId;
        public long getPollIntervalMs() { return pollIntervalMs; }
        public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }
        public String getWorkerId() { return workerId; }
        public void setWorkerId(String workerId) { this.workerId = workerId; }
    }
}
