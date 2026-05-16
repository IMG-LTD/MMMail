package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginAnomalyV212ContractIntegrationTest {

    private static final String PASSWORD = "Password@123";
    private static final String HOME_IP = "203.0.113.10";
    private static final String VPN_IP = "198.51.100.20";
    private static final String ATTACK_IP = "192.0.2.30";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void geoChangedLoginShouldReturnRiskAndAcknowledgeableSecurityEvent() throws Exception {
        String email = uniqueEmail("geo-risk");
        register(email, HOME_IP);
        login(email, PASSWORD, HOME_IP).andExpect(status().isOk());

        MvcResult riskyLogin = login(email, PASSWORD, VPN_IP)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.risk").value("medium"))
                .andExpect(jsonPath("$.data.secondFactorRequired").value(true))
                .andExpect(jsonPath("$.data.riskReasons[0]").value("geo_change"))
                .andExpect(jsonPath("$.data.securityEventId").isNotEmpty())
                .andReturn();

        JsonNode loginJson = readJson(riskyLogin);
        String token = loginJson.at("/data/accessToken").asText();
        String eventId = loginJson.at("/data/securityEventId").asText();

        mockMvc.perform(get("/api/v1/security/events")
                        .param("type", "LOGIN_GEO_CHANGE")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(eventId))
                .andExpect(jsonPath("$.data[0].risk").value("medium"))
                .andExpect(jsonPath("$.data[0].city").value("Amsterdam"))
                .andExpect(jsonPath("$.data[0].acknowledgedAt").doesNotExist());

        mockMvc.perform(post("/api/v1/security/events/{id}/ack", eventId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.acknowledgedAt").isNotEmpty());
    }

    @Test
    void bruteForceShouldLockAccountAndAllowAdminForceLogout() throws Exception {
        String email = uniqueEmail("brute-risk");
        register(email, HOME_IP);
        login(email, PASSWORD, HOME_IP).andExpect(status().isOk());

        for (int i = 0; i < 5; i++) {
            login(email, "WrongPassword@123", ATTACK_IP)
                    .andExpect(status().isBadRequest());
        }

        login(email, PASSWORD, ATTACK_IP)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", containsString("temporarily locked")));

        String adminToken = login("admin@mmmail.local", PASSWORD, HOME_IP)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = readToken(adminToken);

        MvcResult anomalies = mockMvc.perform(get("/api/v1/admin/security/anomalies")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("BRUTE_FORCE_LOCK"))
                .andExpect(jsonPath("$.data[0].severity").value("HIGH"))
                .andExpect(jsonPath("$.data[0].lockedUntil").isNotEmpty())
                .andReturn();

        String anomalyId = readJson(anomalies).at("/data/0/id").asText();
        mockMvc.perform(post("/api/v1/admin/security/anomalies/{id}/action", anomalyId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"force-logout\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actionStatus").value("FORCE_LOGOUT"));
    }

    private String readToken(String body) throws Exception {
        return objectMapper.readTree(body).at("/data/accessToken").asText();
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + System.nanoTime() + "@mmmail.local";
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void register(String email, String ipAddress) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(remoteAddr(ipAddress))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(email)))
                .andExpect(status().isOk());
    }

    private ResultActions login(String email, String password, String ipAddress) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .with(remoteAddr(ipAddress))
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload(email, password)));
    }

    private RequestPostProcessor remoteAddr(String ipAddress) {
        return request -> {
            request.setRemoteAddr(ipAddress);
            return request;
        };
    }

    private String registerPayload(String email) {
        return """
                {
                  "email": "%s",
                  "password": "%s",
                  "displayName": "V212 Login Security"
                }
                """.formatted(email, PASSWORD);
    }

    private String loginPayload(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }
}
