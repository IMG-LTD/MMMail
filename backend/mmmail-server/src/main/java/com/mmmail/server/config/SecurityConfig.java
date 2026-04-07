package com.mmmail.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.common.model.Result;
import com.mmmail.server.observability.RequestTracingFilter;
import com.mmmail.server.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfig {

    private static final String API_CONTENT_SECURITY_POLICY = "default-src 'none'; frame-ancestors 'none'; base-uri 'none'";
    private static final String API_PERMISSIONS_POLICY = "camera=(), geolocation=(), microphone=()";

    private final JwtAuthFilter jwtAuthFilter;
    private final RequestTracingFilter requestTracingFilter;
    private final ObjectMapper objectMapper;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            JwtAuthFilter jwtAuthFilter,
            RequestTracingFilter requestTracingFilter,
            ObjectMapper objectMapper,
            @Value("${mmmail.cors-allowed-origins}") String corsAllowedOrigins
    ) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.requestTracingFilter = requestTracingFilter;
        this.objectMapper = objectMapper;
        this.allowedOrigins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .collect(Collectors.toList());
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .httpBasic(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                        .referrerPolicy(policy -> policy.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .addHeaderWriter(this::writeApiSecurityHeaders))
                .exceptionHandling(handlers -> handlers
                        .authenticationEntryPoint(restAuthenticationEntryPoint())
                        .accessDeniedHandler(restAccessDeniedHandler()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()
                        .requestMatchers("/actuator/prometheus", "/actuator/metrics/**")
                        .hasRole("ADMIN")
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/public/mail/**",
                                "/api/v1/public/drive/**",
                                "/api/v1/public/meet/**",
                                "/api/v1/public/pass/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health"
                        )
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public FilterRegistrationBean<RequestTracingFilter> requestTracingFilterRegistration() {
        FilterRegistrationBean<RequestTracingFilter> registration = new FilterRegistrationBean<>(requestTracingFilter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration() {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(jwtAuthFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(buildAllowedOriginPatterns());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "X-MMMAIL-CSRF", "X-MMMAIL-ORG-ID", "X-Drive-Share-Password", "Accept", "Origin", "Cache-Control", "Pragma"));
        config.setExposedHeaders(List.of("Content-Disposition", "X-Preview-Truncated", "Set-Cookie"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private AuthenticationEntryPoint restAuthenticationEntryPoint() {
        return (request, response, exception) -> writeSecurityError(response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
    }

    private AccessDeniedHandler restAccessDeniedHandler() {
        return (request, response, exception) -> writeSecurityError(response, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN);
    }

    private void writeApiSecurityHeaders(HttpServletRequest request, HttpServletResponse response) {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/") && !path.startsWith("/actuator/")) {
            return;
        }
        response.setHeader("Content-Security-Policy", API_CONTENT_SECURITY_POLICY);
        response.setHeader("Permissions-Policy", API_PERMISSIONS_POLICY);
    }

    private void writeSecurityError(HttpServletResponse response, HttpStatus status, ErrorCode errorCode) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Result.failure(errorCode.getCode(), errorCode.getMessage())));
    }

    private List<String> buildAllowedOriginPatterns() {
        List<String> patterns = new ArrayList<>(allowedOrigins);
        patterns.add("http://localhost:*");
        patterns.add("http://127.0.0.1:*");
        return patterns;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
