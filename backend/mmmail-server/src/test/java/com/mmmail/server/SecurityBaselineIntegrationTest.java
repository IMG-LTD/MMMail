package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "mmmail.security.rate-limit.login.max-attempts=2",
        "mmmail.security.rate-limit.login.window-seconds=600",
        "mmmail.security.rate-limit.client-errors.max-events=2",
        "mmmail.security.rate-limit.client-errors.window-seconds=600"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityBaselineIntegrationTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void authResponsesShouldExposeSecurityHeadersAndCookieDefaults() throws Exception {
        String email = "security-headers-" + System.nanoTime() + "@mmmail.local";
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(email, PASSWORD, "Security Headers")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Content-Security-Policy", Matchers.containsString("frame-ancestors 'none'")))
                .andExpect(header().string("Permissions-Policy", Matchers.containsString("camera=()")))
                .andReturn();

        List<String> cookieHeaders = result.getResponse().getHeaders("Set-Cookie");
        assertThat(cookieHeaders)
                .anySatisfy(value -> assertThat(value)
                        .contains("MMMAIL_REFRESH_TOKEN=")
                        .contains("HttpOnly")
                        .contains("SameSite=Lax"))
                .anySatisfy(value -> assertThat(value)
                        .contains("MMMAIL_CSRF_TOKEN=")
                        .contains("SameSite=Lax"));
    }

    @Test
    void invalidLoginAttemptsShouldBeRateLimited() throws Exception {
        String email = "security-login-" + System.nanoTime() + "@mmmail.local";
        register(email, "Security Login");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(email, "WrongPassword@123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(20002));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(email, "WrongPassword@123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(20002));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(email, PASSWORD)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(10004))
                .andExpect(jsonPath("$.message").value("Too many login attempts. Try again later."));
    }

    @Test
    void clientErrorReportingShouldRequireAuthAndEnforceAbuseGuard() throws Exception {
        mockMvc.perform(post("/api/v1/system/errors/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clientErrorPayload("client-error-unauth")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(10002));

        String email = "security-errors-" + System.nanoTime() + "@mmmail.local";
        String token = register(email, "Security Errors");

        postClientError(token, "client-error-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        postClientError(token, "client-error-2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        postClientError(token, "client-error-3")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(10004))
                .andExpect(jsonPath("$.message").value("Client error reporting is temporarily rate limited"));
    }

    private String register(String email, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(email, PASSWORD, displayName)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/accessToken").asText();
    }

    private org.springframework.test.web.servlet.ResultActions postClientError(String token, String requestId) throws Exception {
        return mockMvc.perform(post("/api/v1/system/errors/client")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(clientErrorPayload(requestId)));
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String registerPayload(String email, String password, String displayName) {
        return """
                {
                  "email": "%s",
                  "password": "%s",
                  "displayName": "%s"
                }
                """.formatted(email, password, displayName);
    }

    private String loginPayload(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }

    private String clientErrorPayload(String requestId) {
        return """
                {
                  "message": "Unhandled runtime error",
                  "category": "WINDOW_ERROR",
                  "severity": "ERROR",
                  "detail": "ReferenceError: boom",
                  "path": "/settings/system-health",
                  "method": "GET",
                  "requestId": "%s"
                }
                """.formatted(requestId);
    }
}
