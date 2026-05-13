package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendV21OpsRuntimeBridgeTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void v21OpsShouldBridgeSuiteRuntimeReadModels() throws Exception {
        String token = register("v21-ops-" + System.nanoTime() + "@mmmail.local");
        createPassItem(token, "V21 Ops Runtime Secret");
        seedNotification(token);

        JsonNode projects = v21Get(token, "/api/v2/collaboration/projects", "limit", "20");
        JsonNode tasks = v21Get(token, "/api/v2/collaboration/tasks", "limit", "20");
        JsonNode activity = v21Get(token, "/api/v2/collaboration/activity", "limit", "20");
        JsonNode notifications = v21Get(token, "/api/v2/notifications", "limit", "20");
        JsonNode subscriptions = v21Get(token, "/api/v2/notifications/subscriptions");
        JsonNode commands = v21Get(token, "/api/v2/command-center/commands");

        assertThat(collectFieldValues(projects, "product")).contains("PASS");
        assertThat(collectFieldValues(tasks, "product")).contains("PASS");
        assertThat(collectFieldValues(activity, "product")).contains("PASS");
        assertThat(notifications).isNotEmpty();
        assertThat(subscriptions.path(0).path("id").asText()).isEqualTo("web-push-mail-inbox");
        assertThat(commands).isNotEmpty();

        String commandId = commands.path(0).path("id").asText();
        mockMvc.perform(get("/api/v2/command-center/commands/" + commandId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(commandId));
    }

    @Test
    void v21NotificationsShouldSupportExplicitReadPatchOnly() throws Exception {
        String token = register("v21-ops-notification-" + System.nanoTime() + "@mmmail.local");
        seedNotification(token);

        JsonNode notifications = v21Get(token, "/api/v2/notifications", "limit", "20");
        String notificationId = notifications.path(0).path("id").asText();
        mockMvc.perform(patch("/api/v2/notifications/" + notificationId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "READ"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(notificationId))
                .andExpect(jsonPath("$.data.status").value("READ"))
                .andExpect(jsonPath("$.data.readAt").isNotEmpty());

        mockMvc.perform(patch("/api/v2/notifications/" + notificationId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ARCHIVED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()));
    }

    @Test
    void v21OpsShouldRejectUnsupportedSubscriptionWritesAndGatePremiumRoutes() throws Exception {
        String token = register("v21-ops-gate-" + System.nanoTime() + "@mmmail.local");

        assertUnsupportedPatch(token, "/api/v2/notifications/subscriptions/web-push-mail-inbox");

        assertPremiumPostGate(token, "/api/v2/command-center/runs");
        assertPremiumGate(token, "/api/v2/notifications/rules");
        assertPremiumPostGate(token, "/api/v2/notifications/send");
    }

    private JsonNode v21Get(String token, String path, String... params) throws Exception {
        MockHttpServletRequestBuilder builder = get(path).header("Authorization", "Bearer " + token);
        for (int index = 0; index < params.length; index += 2) {
            builder.param(params[index], params[index + 1]);
        }
        MvcResult result = mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return readJson(result).path("data");
    }

    private void assertUnsupportedPatch(String token, String path) throws Exception {
        mockMvc.perform(patch(path)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()));
    }

    private void assertPremiumGate(String token, String path) throws Exception {
        mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode()));
    }

    private void assertPremiumPostGate(String token, String path) throws Exception {
        mockMvc.perform(post(path)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode()));
    }

    private void createPassItem(String token, String title) throws Exception {
        mockMvc.perform(post("/api/v1/pass/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "itemType": "LOGIN",
                                  "website": "https://ops.example.com",
                                  "username": "ops@example.com",
                                  "secretCiphertext": "Ciphertext#123",
                                  "note": "v2.1 ops runtime bridge seed"
                                }
                                """.formatted(title)))
                .andExpect(status().isOk());
    }

    private void seedNotification(String token) throws Exception {
        mockMvc.perform(post("/api/v1/suite/governance/change-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode": "SECURITY_BASELINE_HARDENING",
                                  "reason": "v2.1 runtime bridge notification seed"
                                }
                                """))
                .andExpect(status().isOk());
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "displayName": "V21 Ops"
                                }
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/accessToken").asText();
    }

    private Set<String> collectFieldValues(JsonNode nodes, String fieldName) {
        Set<String> values = new LinkedHashSet<>();
        for (JsonNode node : nodes) {
            String value = node.path(fieldName).asText();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
