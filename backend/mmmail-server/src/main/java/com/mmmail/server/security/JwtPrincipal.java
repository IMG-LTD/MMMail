package com.mmmail.server.security;

public record JwtPrincipal(Long userId, String email, String role, Integer tokenVersion, Long sessionId) {
}
