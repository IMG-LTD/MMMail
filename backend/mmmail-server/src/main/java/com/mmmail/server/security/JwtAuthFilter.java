package com.mmmail.server.security;

import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.mapper.UserSessionMapper;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.entity.UserSession;
import com.mmmail.common.observability.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserAccountMapper userAccountMapper;
    private final UserSessionMapper userSessionMapper;

    public JwtAuthFilter(JwtService jwtService, UserAccountMapper userAccountMapper, UserSessionMapper userSessionMapper) {
        this.jwtService = jwtService;
        this.userAccountMapper = userAccountMapper;
        this.userSessionMapper = userSessionMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                JwtPrincipal principal = jwtService.parseToken(token);
                UserAccount user = userAccountMapper.selectById(principal.userId());
                if (user == null
                        || principal.tokenVersion() == null
                        || !principal.tokenVersion().equals(user.getTokenVersion())
                        || !isSessionActive(principal)) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()))
                        );
                MDC.put(TraceContext.USER_ID_MDC, String.valueOf(principal.userId()));
                MDC.put(TraceContext.SESSION_ID_MDC, String.valueOf(principal.sessionId()));
                MDC.put(TraceContext.ROLE_MDC, principal.role());
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSessionActive(JwtPrincipal principal) {
        if (principal.sessionId() == null) {
            return false;
        }
        UserSession session = userSessionMapper.selectById(principal.sessionId());
        if (session == null || !principal.userId().equals(session.getOwnerId())) {
            return false;
        }
        if (session.getRevoked() != null && session.getRevoked() == 1) {
            return false;
        }
        return session.getExpiresAt() != null && session.getExpiresAt().isAfter(LocalDateTime.now());
    }
}
