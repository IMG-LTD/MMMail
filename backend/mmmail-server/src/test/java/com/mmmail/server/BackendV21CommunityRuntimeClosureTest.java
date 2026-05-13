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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendV21CommunityRuntimeClosureTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void workspaceShouldExposeAndPatchRealCollaborationTasks() throws Exception {
        String token = register("v21-community-workspace-" + System.nanoTime() + "@mmmail.local", "V21 Workspace");
        String projectId = createCollaborationProject(token, "Runtime Closure", "WORKSPACE");
        String taskId = createCollaborationTask(token, projectId, "Prepare checklist");
        String workspaceTaskId = "collaboration-task-" + taskId;

        JsonNode summary = getData(token, "/api/v2/workspace/summary");
        assertThat(summary.path("productCards").size()).isGreaterThanOrEqualTo(1);
        assertThat(summary.path("systemStatus").asText()).isEqualTo("READY");

        JsonNode activity = getData(token, "/api/v2/workspace/activity");
        assertThat(activity.isArray()).isTrue();

        JsonNode task = findById(getData(token, "/api/v2/workspace/tasks"), workspaceTaskId);
        assertThat(task.path("title").asText()).isEqualTo("Prepare checklist");
        assertThat(task.path("completed").asBoolean()).isFalse();

        patchWorkspaceTask(token, workspaceTaskId);
        JsonNode updatedTask = findById(getData(token, "/api/v2/collaboration/tasks"), taskId);
        assertThat(updatedTask.path("title").asText()).isEqualTo("Done checklist");
        assertThat(updatedTask.path("status").asText()).isEqualTo("DONE");

        mockMvc.perform(patch("/api/v2/workspace/tasks/unsupported-1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()));
    }

    @Test
    void settingsShouldBridgeProfileDevicesSecurityAndNotifications() throws Exception {
        String email = "v21-community-settings-" + System.nanoTime() + "@mmmail.local";
        String firstToken = register(email, "V21 Settings");
        String currentToken = login(email);

        mockMvc.perform(patch("/api/v2/settings/profile")
                        .header("Authorization", "Bearer " + currentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profilePatchJson("V21 Settings Updated")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("V21 Settings Updated"));

        assertSecuritySettings(currentToken);
        assertNotifications(currentToken);
        assertDeviceRevocation(firstToken, currentToken);
    }

    @Test
    void entitlementsShouldReflectCatalogTiers() throws Exception {
        String token = register("v21-community-entitlements-" + System.nanoTime() + "@mmmail.local", "V21 Entitlements");

        JsonNode rows = getData(token, "/api/v2/entitlements");
        assertEntitlement(rows, "GET /api/v2/workspace/summary", "available", null);
        assertEntitlement(rows, "POST /api/v2/command-center/runs", "locked", "premium");
        assertEntitlement(rows, "GET /api/v2/billing/summary", "locked", "hosted");
        assertEntitlement(rows, "GET /api/v2/admin/summary", "locked", "enterprise-governance");
        assertThat(containsKey(rows, "GET /api/v2/share/mail/:token")).isFalse();

        JsonNode matrix = getData(token, "/api/v2/entitlements/matrix");
        assertThat(containsText(matrix.path("community"), "GET /api/v2/workspace/summary")).isTrue();
        assertThat(containsText(matrix.path("premium"), "POST /api/v2/command-center/runs")).isTrue();
        assertThat(containsText(matrix.path("hosted"), "GET /api/v2/billing/summary")).isTrue();
    }

    private void assertSecuritySettings(String token) throws Exception {
        JsonNode security = getData(token, "/api/v2/settings/security");
        assertThat(security.path("mfaEnabled").asBoolean()).isFalse();
        assertThat(security.path("recoveryEmail").isNull()).isTrue();

        mockMvc.perform(patch("/api/v2/settings/security")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mfaEnabled\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()));
    }

    private void assertNotifications(String token) throws Exception {
        JsonNode notifications = getData(token, "/api/v2/settings/notifications");
        assertThat(notifications.path("emailDigest").asBoolean()).isTrue();
        assertThat(notifications.path("productUpdates").asBoolean()).isTrue();

        mockMvc.perform(patch("/api/v2/settings/notifications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailDigest\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()));
    }

    private void assertDeviceRevocation(String firstToken, String currentToken) throws Exception {
        JsonNode devices = getData(currentToken, "/api/v2/settings/devices");
        String firstSessionId = currentSessionId(firstToken);
        String currentSessionId = currentSessionId(currentToken);
        assertThat(containsId(devices, firstSessionId)).isTrue();
        assertThat(containsId(devices, currentSessionId)).isTrue();

        mockMvc.perform(delete("/api/v2/settings/devices/" + firstSessionId)
                        .header("Authorization", "Bearer " + currentToken))
                .andExpect(status().isOk());
        assertThat(containsId(getData(currentToken, "/api/v2/settings/devices"), firstSessionId)).isFalse();

        mockMvc.perform(delete("/api/v2/settings/devices/" + currentSessionId)
                        .header("Authorization", "Bearer " + currentToken))
                .andExpect(status().isBadRequest());
    }

    private String createCollaborationProject(String token, String name, String product) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v2/collaboration/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "product": "%s"
                                }
                                """.formatted(name, product)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private String createCollaborationTask(String token, String projectId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v2/collaboration/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "%s",
                                  "title": "%s"
                                }
                                """.formatted(projectId, title)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private void patchWorkspaceTask(String token, String workspaceTaskId) throws Exception {
        mockMvc.perform(patch("/api/v2/workspace/tasks/" + workspaceTaskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "completed": true,
                                  "title": "Done checklist"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(workspaceTaskId))
                .andExpect(jsonPath("$.data.completed").value(true));
    }

    private JsonNode getData(String token, String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return readJson(result).path("data");
    }

    private String register(String email, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "displayName": "%s"
                                }
                                """.formatted(email, PASSWORD, displayName)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/accessToken").asText();
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/accessToken").asText();
    }

    private String currentSessionId(String token) throws Exception {
        JsonNode devices = getData(token, "/api/v2/settings/devices");
        for (JsonNode device : devices) {
            if (device.path("current").asBoolean()) {
                return device.path("id").asText();
            }
        }
        throw new AssertionError("Current device was not returned");
    }

    private JsonNode findById(JsonNode rows, String id) {
        for (JsonNode row : rows) {
            if (id.equals(row.path("id").asText())) {
                return row;
            }
        }
        throw new AssertionError("Missing row id " + id);
    }

    private void assertEntitlement(JsonNode rows, String key, String state, String requiredPlan) {
        JsonNode row = findByKey(rows, key);
        assertThat(row.path("state").asText()).isEqualTo(state);
        if (requiredPlan == null) {
            assertThat(row.path("requiredPlan").isNull()).isTrue();
            return;
        }
        assertThat(row.path("requiredPlan").asText()).isEqualTo(requiredPlan);
    }

    private JsonNode findByKey(JsonNode rows, String key) {
        for (JsonNode row : rows) {
            if (key.equals(row.path("key").asText())) {
                return row;
            }
        }
        throw new AssertionError("Missing entitlement key " + key);
    }

    private boolean containsKey(JsonNode rows, String key) {
        for (JsonNode row : rows) {
            if (key.equals(row.path("key").asText())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsId(JsonNode rows, String id) {
        for (JsonNode row : rows) {
            if (id.equals(row.path("id").asText())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsText(JsonNode rows, String text) {
        for (JsonNode row : rows) {
            if (text.equals(row.asText())) {
                return true;
            }
        }
        return false;
    }

    private String profilePatchJson(String displayName) {
        return """
                {
                  "displayName": "%s",
                  "signature": "Runtime closure signature",
                  "timezone": "Asia/Shanghai",
                  "preferredLocale": "zh-CN",
                  "mailAddressMode": "PROTON_ADDRESS",
                  "autoSaveSeconds": 30,
                  "undoSendSeconds": 5,
                  "driveVersionRetentionCount": 30,
                  "driveVersionRetentionDays": 365
                }
                """.formatted(displayName);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
