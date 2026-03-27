package com.mmmail.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtService {

    private final SecretKey secretKey;
    private final long expireMinutes;

    public JwtService(
            @Value("${mmmail.jwt-secret}") String jwtSecret,
            @Value("${mmmail.jwt-expire-minutes:15}") long expireMinutes
    ) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.expireMinutes = expireMinutes;
    }

    public String generateToken(JwtPrincipal principal) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(expireMinutes * 60);

        return Jwts.builder()
                .subject(principal.userId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .claims(Map.of(
                        "email", principal.email(),
                        "role", principal.role(),
                        "tokenVersion", principal.tokenVersion(),
                        "sessionId", principal.sessionId()
                ))
                .signWith(secretKey)
                .compact();
    }

    public JwtPrincipal parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = Long.parseLong(claims.getSubject());
        String email = claims.get("email", String.class);
        String role = claims.get("role", String.class);
        Integer tokenVersion = claims.get("tokenVersion", Integer.class);
        Number sessionIdClaim = claims.get("sessionId", Number.class);
        Long sessionId = sessionIdClaim == null ? null : sessionIdClaim.longValue();

        return new JwtPrincipal(userId, email, role, tokenVersion, sessionId);
    }
}
