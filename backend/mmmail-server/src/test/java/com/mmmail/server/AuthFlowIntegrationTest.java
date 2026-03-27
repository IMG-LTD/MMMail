package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerAndLoginShouldWork() throws Exception {
        String email = "new-user-%d@mmmail.local".formatted(System.nanoTime());
        String registerPayload = """
                {
                  "email": "%s",
                  "password": "Password@123",
                  "displayName": "New User"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        String loginPayload = """
                {
                  "email": "%s",
                  "password": "Password@123"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.user.email").value(email));
    }

    @Test
    void sessionListAndRevokeShouldWork() throws Exception {
        String email = "session-user-%d@mmmail.local".formatted(System.nanoTime());
        String registerPayload = """
                {
                  "email": "%s",
                  "password": "Password@123",
                  "displayName": "Session User"
                }
                """.formatted(email);

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        String registerToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .at("/data/accessToken")
                .asText();

        String loginPayload = """
                {
                  "email": "%s",
                  "password": "Password@123"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        MvcResult sessionsResult = mockMvc.perform(get("/api/v1/auth/sessions")
                        .header("Authorization", "Bearer " + registerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").exists())
                .andReturn();

        JsonNode sessionsJson = objectMapper.readTree(sessionsResult.getResponse().getContentAsString());
        assertThat(sessionsJson.at("/data").isArray()).isTrue();
        assertThat(sessionsJson.at("/data").size()).isGreaterThanOrEqualTo(2);

        JsonNode target = null;
        for (JsonNode node : sessionsJson.at("/data")) {
            if (!node.path("current").asBoolean(false)) {
                target = node;
                break;
            }
        }
        assertThat(target).isNotNull();
        String targetId = target.path("id").asText();

        mockMvc.perform(post("/api/v1/auth/sessions/" + targetId + "/revoke")
                        .header("Authorization", "Bearer " + registerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        MvcResult afterResult = mockMvc.perform(get("/api/v1/auth/sessions")
                        .header("Authorization", "Bearer " + registerToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode afterJson = objectMapper.readTree(afterResult.getResponse().getContentAsString());
        assertThat(afterJson.at("/data").size()).isEqualTo(sessionsJson.at("/data").size() - 1);
    }

    @Test
    void refreshShouldRotateTokenAndInvalidateOldRefreshToken() throws Exception {
        String email = "refresh-user-%d@mmmail.local".formatted(System.nanoTime());
        String registerPayload = """
                {
                  "email": "%s",
                  "password": "Password@123",
                  "displayName": "Refresh User"
                }
                """.formatted(email);

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String refreshToken = registerJson.at("/data/refreshToken").asText();
        assertThat(refreshToken).isNotBlank();

        String refreshPayload = """
                {
                  "refreshToken": "%s"
                }
                """.formatted(refreshToken);

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        String rotatedRefreshToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                .at("/data/refreshToken")
                .asText();
        assertThat(rotatedRefreshToken).isNotBlank();
        assertThat(rotatedRefreshToken).isNotEqualTo(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(20004));
    }

    @Test
    void corsPreflightShouldAllowCsrfHeaderForRefresh() throws Exception {
        mockMvc.perform(options("/api/v1/auth/refresh")
                        .header("Origin", "http://127.0.0.1:3001")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-mmmail-csrf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:3001"))
                .andExpect(header().string("Access-Control-Allow-Headers", org.hamcrest.Matchers.containsString("x-mmmail-csrf")));
    }

    @Test
    void refreshWithCookieAndCsrfShouldWork() throws Exception {
        String email = "refresh-cookie-user-%d@mmmail.local".formatted(System.nanoTime());
        String registerPayload = """
                {
                  "email": "%s",
                  "password": "Password@123",
                  "displayName": "Refresh Cookie User"
                }
                """.formatted(email);

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        Cookie refreshCookie = registerResult.getResponse().getCookie("MMMAIL_REFRESH_TOKEN");
        Cookie csrfCookie = registerResult.getResponse().getCookie("MMMAIL_CSRF_TOKEN");
        assertThat(refreshCookie).isNotNull();
        assertThat(csrfCookie).isNotNull();
        assertThat(refreshCookie.getValue()).isNotBlank();
        assertThat(csrfCookie.getValue()).isNotBlank();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .cookie(refreshCookie, csrfCookie)
                        .header("X-MMMAIL-CSRF", csrfCookie.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("MMMAIL_REFRESH_TOKEN")));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .cookie(refreshCookie, csrfCookie)
                        .header("X-MMMAIL-CSRF", "invalid-csrf-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(10003));
    }

    @Test
    void logoutCurrentShouldClearCookiesAndInvalidateAccessToken() throws Exception {
        String email = "logout-user-%d@mmmail.local".formatted(System.nanoTime());
        MvcResult registerResult = register(email, "Logout User");
        JsonNode auth = objectMapper.readTree(registerResult.getResponse().getContentAsString()).path("data");
        String accessToken = auth.path("accessToken").asText();

        MvcResult logoutResult = mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        assertThat(logoutResult.getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(header -> assertThat(header).contains("MMMAIL_REFRESH_TOKEN=").contains("Max-Age=0"))
                .anySatisfy(header -> assertThat(header).contains("MMMAIL_CSRF_TOKEN=").contains("Max-Age=0"));

        mockMvc.perform(get("/api/v1/auth/sessions")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(10002));
    }

    @Test
    void logoutAllShouldInvalidateAllAccessTokens() throws Exception {
        String email = "logout-all-user-%d@mmmail.local".formatted(System.nanoTime());
        MvcResult registerResult = register(email, "Logout All User");
        String registerToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .at("/data/accessToken")
                .asText();

        String loginPayload = """
                {
                  "email": "%s",
                  "password": "Password@123"
                }
                """.formatted(email);
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();
        String loginToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .at("/data/accessToken")
                .asText();

        mockMvc.perform(post("/api/v1/auth/logout-all")
                        .header("Authorization", "Bearer " + registerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/auth/sessions")
                        .header("Authorization", "Bearer " + registerToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(10002));

        mockMvc.perform(get("/api/v1/auth/sessions")
                        .header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(10002));
    }

    private MvcResult register(String email, String displayName) throws Exception {
        String registerPayload = """
                {
                  "email": "%s",
                  "password": "Password@123",
                  "displayName": "%s"
                }
                """.formatted(email, displayName);

        return mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
    }
}
