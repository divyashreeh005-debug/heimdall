package com.heimdall.ingestion;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HmacVerifierTest {

    private final HmacVerifier verifier = new HmacVerifier();

    @Test
    void acceptsCorrectSignature() throws Exception {
        String payload = "{\"action\":\"opened\"}";
        String secret = "dev-webhook-secret";
        String signature = "sha256=" + hmacHex(payload, secret);

        assertTrue(verifier.isValid(payload, signature, secret));
    }

    @Test
    void rejectsTamperedPayload() throws Exception {
        String secret = "dev-webhook-secret";
        String signature = "sha256=" + hmacHex("{\"action\":\"opened\"}", secret);

        assertFalse(verifier.isValid("{\"action\":\"closed\"}", signature, secret));
    }

    @Test
    void rejectsMissingSignature() {
        assertFalse(verifier.isValid("{}", null, "secret"));
    }

    private String hmacHex(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : raw) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
