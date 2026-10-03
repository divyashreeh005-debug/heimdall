package com.heimdall.consoleapi;

import com.heimdall.identity.JwtService;
import com.heimdall.identity.Role;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Minimal login for the Phase 1 skeleton. Real auth is GitHub-App-installer
 * driven (org admin installs the App, Console session is bootstrapped from
 * the GitHub OAuth callback) - stubbed here as an email lookup so the
 * Console has something to call end to end.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JdbcTemplate jdbcTemplate;
    private final JwtService jwtService;

    public AuthController(JdbcTemplate jdbcTemplate, JwtService jwtService) {
        this.jdbcTemplate = jdbcTemplate;
        this.jwtService = jwtService;
    }

    public record LoginRequest(String email) { }
    public record LoginResponse(String token) { }

    @PostMapping("/dev-login")
    public LoginResponse devLogin(@RequestBody LoginRequest request) {
        var row = jdbcTemplate.queryForMap("""
            SELECT u.id AS user_id, m.org_id, m.role
            FROM users u JOIN memberships m ON m.user_id = u.id
            WHERE u.email = ?
            """, request.email());
        UUID userId = (UUID) row.get("user_id");
        UUID orgId = (UUID) row.get("org_id");
        Role role = Role.valueOf((String) row.get("role"));
        return new LoginResponse(jwtService.issue(userId, orgId, role));
    }
}
