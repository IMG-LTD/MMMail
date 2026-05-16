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
    void v21CommandPanelShouldExposeCatalogPinsRecentsAndQuickSearch() throws Exception {
        String token = register("v21-command-panel-" + System.nanoTime() + "@mmmail.local");
        createPassItem(token, "Command Panel Search Seed");

        JsonNode catalog = v21Get(token, "/api/v2/command-center/catalog", "context", "/mail/inbox");
        JsonNode composeCommand = findById(catalog, "mail.compose");

        assertThat(composeCommand.path("title").asText()).isEqualTo("Compose new mail");
        assertThat(composeCommand.at("/action/kind").asText()).isEqualTo("navigate");
        assertThat(composeCommand.at("/action/payload/routePath").asText()).isEqualTo("/mail");

        mockMvc.perform(post("/api/v2/command-center/pin")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "mail.compose",
                                  "pinned": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commandId").value("mail.compose"))
                .andExpect(jsonPath("$.data.pinned").value(true));

        JsonNode recents = v21Get(token, "/api/v2/command-center/recents", "limit", "5");
        assertThat(recents.path(0).path("commandId").asText()).isEqualTo("mail.compose");

        JsonNode pinnedCatalog = v21Get(token, "/api/v2/command-center/catalog", "context", "/mail/inbox");
        assertThat(findById(pinnedCatalog, "mail.compose").path("pinned").asBoolean()).isTrue();

        JsonNode quickSearch = v21Get(token, "/api/v2/command-center/quick-search", "q", "Command Panel", "limit", "5");
        assertThat(quickSearch).isNotEmpty();
        assertThat(collectFieldValues(quickSearch, "sourceType")).contains("content");
    }

    @Test
    void v21CollaborationBoardShouldPersistDragOrdering() throws Exception {
        String token = register("v21-board-" + System.nanoTime() + "@mmmail.local");
        String projectId = createProject(token, "Release Board " + System.nanoTime());
        String firstTaskId = createTask(token, projectId, "First card", "OPEN");
        String secondTaskId = createTask(token, projectId, "Second card", "OPEN");
        String thirdTaskId = createTask(token, projectId, "Third card", "OPEN");

        mockMvc.perform(patch("/api/v2/collaboration/tasks/" + thirdTaskId + "/move")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "columnId": "OPEN",
                                  "beforeTaskId": "%s"
                                }
                                """.formatted(firstTaskId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(thirdTaskId))
                .andExpect(jsonPath("$.data.columnId").value("OPEN"))
                .andExpect(jsonPath("$.data.position").isNotEmpty());

        mockMvc.perform(patch("/api/v2/collaboration/tasks/" + secondTaskId + "/move")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "columnId": "IN_PROGRESS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(secondTaskId))
                .andExpect(jsonPath("$.data.columnId").value("IN_PROGRESS"));

        JsonNode board = v21Get(token, "/api/v2/collaboration/projects/" + projectId + "/board");
        JsonNode openColumn = findColumn(board, "OPEN");
        JsonNode inProgressColumn = findColumn(board, "IN_PROGRESS");

        assertThat(openColumn.path("tasks").path(0).path("id").asText()).isEqualTo(thirdTaskId);
        assertThat(openColumn.path("tasks").path(1).path("id").asText()).isEqualTo(firstTaskId);
        assertThat(inProgressColumn.path("tasks").path(0).path("id").asText()).isEqualTo(secondTaskId);
        assertThat(inProgressColumn.path("tasks").path(0).path("position").asText()).isNotBlank();
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

    private String createProject(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v2/collaboration/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "product": "MAIL",
                                  "status": "ACTIVE"
                                }
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private String createTask(String token, String projectId, String title, String status) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v2/collaboration/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "%s",
                                  "title": "%s",
                                  "status": "%s",
                                  "assigneeEmail": "ops@example.com"
                                }
                                """.formatted(projectId, title, status)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/id").asText();
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

    private JsonNode findById(JsonNode nodes, String id) {
        for (JsonNode node : nodes) {
            if (id.equals(node.path("id").asText())) {
                return node;
            }
        }
        throw new AssertionError("Expected item id not found: " + id);
    }

    private JsonNode findColumn(JsonNode board, String columnId) {
        for (JsonNode column : board.path("columns")) {
            if (columnId.equals(column.path("columnId").asText())) {
                return column;
            }
        }
        throw new AssertionError("Expected board column not found: " + columnId);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
