package com.mmmail.server.security;

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
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final JwtPrincipalValidator jwtPrincipalValidator;

    public JwtAuthFilter(JwtService jwtService, JwtPrincipalValidator jwtPrincipalValidator) {
        this.jwtService = jwtService;
        this.jwtPrincipalValidator = jwtPrincipalValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                JwtPrincipal principal = jwtService.parseToken(token);
                if (!jwtPrincipalValidator.isActive(principal)) {
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
}
