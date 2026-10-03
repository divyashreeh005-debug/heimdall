package com.heimdall.identity;

import com.heimdall.config.HeimdallProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final HeimdallProperties properties;
    private final SecretKey key;

    public JwtService(HeimdallProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(
                properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String issue(UUID userId, UUID orgId, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.getJwt().getIssuer())
                .subject(userId.toString())
                .claim("org_id", orgId.toString())
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getJwt().getTtlMinutes(), ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
