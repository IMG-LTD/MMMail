package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * v2.1 End-to-end user interaction simulation test.
 * Simulates a complete user journey through the MMMail platform.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class V21UserInteractionE2eTest {

    private static final String TEST_EMAIL = "e2e-user-" + System.nanoTime() + "@mmmail.local";
    private static final String TEST_PASSWORD = "SecurePass@2026";
    private static final String TEST_DISPLAY_NAME = "E2E Test User";
    private static final int HEALTH_STATUS_UP = 200;
    private static final int HEALTH_STATUS_DOWN = 503;
    private static String accessToken;
    private static String refreshToken;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // ==================== 1. AUTH FLOW ====================

    @Test @Order(1)
    void userRegistration() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s", "displayName": "%s"}
                                """.formatted(TEST_EMAIL, TEST_PASSWORD, TEST_DISPLAY_NAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode data = readData(result);
        accessToken = data.get("accessToken").asText();
        refreshToken = data.get("refreshToken").asText();
        System.out.println("[E2E] Registration successful, token obtained");
    }

    @Test @Order(2)
    void userLogin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v2/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(TEST_EMAIL, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        JsonNode data = readData(result);
        accessToken = data.get("accessToken").asText();
        refreshToken = data.get("refreshToken").asText();
        System.out.println("[E2E] Login successful via v2 API");
    }

    @Test @Order(3)
    void tokenRefresh() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v2/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        JsonNode data = readData(result);
        accessToken = data.get("accessToken").asText();
        refreshToken = data.get("refreshToken").asText();
        System.out.println("[E2E] Token refresh successful");
    }

    @Test @Order(4)
    void listSessions() throws Exception {
        mockMvc.perform(get("/api/v2/auth/sessions")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
        System.out.println("[E2E] Session list retrieved");
    }

    // ==================== 2. MAIL MODULE ====================

    @Test @Order(10)
    void mailListMessages() throws Exception {
        mockMvc.perform(get("/api/v2/mail/messages")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("folder", "inbox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] Mail inbox loaded");
    }

    @Test @Order(11)
    void mailListFolders() throws Exception {
        mockMvc.perform(get("/api/v2/mail/folders")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] Mail folders loaded");
    }

    @Test @Order(12)
    void mailListContacts() throws Exception {
        mockMvc.perform(get("/api/v2/mail/contacts")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] Mail contacts loaded");
    }

    @Test @Order(13)
    void mailCreateDraft() throws Exception {
        mockMvc.perform(post("/api/v2/mail/drafts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toEmail": "%s", "subject": "E2E Test", "body": "Hello from E2E test"}
                                """.formatted(TEST_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] Mail draft created");
    }

    @Test @Order(14)
    void mailSend() throws Exception {
        mockMvc.perform(post("/api/v2/mail/send")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toEmail": "%s", "subject": "E2E Send Test", "body": "Sent from E2E", "idempotencyKey": "e2e-send-001"}
                                """.formatted(TEST_EMAIL)))
                .andExpect(status().isOk());
        System.out.println("[E2E] Mail sent successfully");
    }

    // ==================== 3. CALENDAR MODULE ====================

    @Test @Order(20)
    void calendarListEvents() throws Exception {
        mockMvc.perform(get("/api/v2/calendar/events")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] Calendar events loaded");
    }

    @Test @Order(21)
    void calendarGetSettings() throws Exception {
        mockMvc.perform(get("/api/v2/calendar/settings")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] Calendar settings loaded");
    }

    @Test @Order(22)
    void calendarCreateEvent() throws Exception {
        mockMvc.perform(post("/api/v2/calendar/events")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "E2E Meeting", "startAt": "2026-05-15T10:00:00", "endAt": "2026-05-15T11:00:00", "timezone": "Asia/Shanghai"}
                                """))
                .andExpect(status().isOk());
        System.out.println("[E2E] Calendar event created");
    }

    // ==================== 4. DRIVE MODULE ====================

    @Test @Order(30)
    void driveListFiles() throws Exception {
        mockMvc.perform(get("/api/v2/drive/files")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] Drive files loaded");
    }

    @Test @Order(31)
    void driveListFolders() throws Exception {
        mockMvc.perform(get("/api/v2/drive/folders")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] Drive folders loaded");
    }

    @Test @Order(32)
    void driveStorageSummary() throws Exception {
        mockMvc.perform(get("/api/v2/drive/storage/summary")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] Drive storage summary loaded");
    }

    // ==================== 5. PASS MODULE ====================

    @Test @Order(40)
    void passListVaults() throws Exception {
        mockMvc.perform(get("/api/v2/pass/vaults")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] Pass vaults loaded");
    }

    @Test @Order(41)
    void passListItems() throws Exception {
        mockMvc.perform(get("/api/v2/pass/items")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] Pass items loaded");
    }

    @Test @Order(42)
    void passCreateItem() throws Exception {
        mockMvc.perform(post("/api/v2/pass/items")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "E2E Test Login", "itemType": "LOGIN", "username": "testuser", "secretCiphertext": "testpass123"}
                                """))
                .andExpect(status().isOk());
        System.out.println("[E2E] Pass item created");
    }

    @Test @Order(43)
    void passMonitor() throws Exception {
        mockMvc.perform(get("/api/v2/pass/monitor")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
        System.out.println("[E2E] Pass monitor correctly gated");
    }

    // ==================== 6. WORKSPACE & COLLABORATION ====================

    @Test @Order(50)
    void workspaceSummary() throws Exception {
        mockMvc.perform(get("/api/v2/workspace/summary")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] Workspace summary loaded");
    }

    @Test @Order(51)
    void workspaceActivity() throws Exception {
        mockMvc.perform(get("/api/v2/workspace/activity")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] Workspace activity loaded");
    }

    @Test @Order(52)
    void workspaceTasks() throws Exception {
        mockMvc.perform(get("/api/v2/workspace/tasks")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] Workspace tasks loaded");
    }

    @Test @Order(53)
    void collaborationProjects() throws Exception {
        mockMvc.perform(get("/api/v2/collaboration/projects")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] Collaboration projects loaded");
    }

    @Test @Order(54)
    void collaborationTasks() throws Exception {
        mockMvc.perform(get("/api/v2/collaboration/tasks")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] Collaboration tasks loaded");
    }

    // ==================== 7. SETTINGS ====================

    @Test @Order(60)
    void settingsProfile() throws Exception {
        mockMvc.perform(get("/api/v2/settings/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] Settings profile loaded");
    }

    @Test @Order(61)
    void settingsUpdateProfile() throws Exception {
        mockMvc.perform(patch("/api/v2/settings/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName": "Updated E2E User", "signature": "", "timezone": "Asia/Shanghai", "preferredLocale": "zh-CN", "mailAddressMode": "PROTON_ADDRESS", "autoSaveSeconds": 30, "undoSendSeconds": 10, "driveVersionRetentionCount": 20, "driveVersionRetentionDays": 365}
                                """))
                .andExpect(status().isOk());
        System.out.println("[E2E] Settings profile updated");
    }

    @Test @Order(62)
    void settingsSecurity() throws Exception {
        mockMvc.perform(get("/api/v2/settings/security")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] Settings security loaded");
    }

    @Test @Order(63)
    void settingsDevices() throws Exception {
        mockMvc.perform(get("/api/v2/settings/devices")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] Settings devices loaded");
    }

    @Test @Order(64)
    void settingsNotifications() throws Exception {
        mockMvc.perform(get("/api/v2/settings/notifications")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] Settings notifications loaded");
    }

    // ==================== 8. NOTIFICATIONS & COMMAND CENTER ====================

    @Test @Order(70)
    void notificationsList() throws Exception {
        mockMvc.perform(get("/api/v2/notifications")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] Notifications loaded");
    }

    @Test @Order(71)
    void commandCenterCommands() throws Exception {
        mockMvc.perform(get("/api/v2/command-center/commands")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] Command center commands loaded");
    }

    @Test @Order(72)
    void commandCenterWorkflows() throws Exception {
        mockMvc.perform(get("/api/v2/command-center/workflows")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
        System.out.println("[E2E] Command center workflows correctly gated");
    }

    // ==================== 9. PLATFORM & ENTITLEMENTS ====================

    @Test @Order(80)
    void platformCapabilities() throws Exception {
        mockMvc.perform(get("/api/v2/ai-platform/capabilities")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] AI platform capabilities loaded");
    }

    @Test @Order(81)
    void entitlementsMatrix() throws Exception {
        mockMvc.perform(get("/api/v2/entitlements/matrix")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] Entitlements matrix loaded");
    }

    @Test @Order(82)
    void mcpRegistry() throws Exception {
        mockMvc.perform(get("/api/v2/mcp/registry")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] MCP registry loaded");
    }

    // ==================== 10. PUBLIC ENDPOINTS ====================

    @Test @Order(90)
    void publicHealthCheck() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/health"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isIn(HEALTH_STATUS_UP, HEALTH_STATUS_DOWN);
        assertThat(objectMapper.readTree(result.getResponse().getContentAsString()).path("status").asText())
                .isIn("UP", "DOWN");
        System.out.println("[E2E] Health check state exposed");
    }

    @Test @Order(91)
    void publicShareCapabilities() throws Exception {
        mockMvc.perform(get("/api/v2/public-share/capabilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        System.out.println("[E2E] Public share capabilities loaded");
    }

    @Test @Order(92)
    void unauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/api/v2/mail/messages"))
                .andExpect(status().isUnauthorized());
        System.out.println("[E2E] Unauthenticated access correctly denied");
    }

    // ==================== 11. LOGOUT ====================

    @Test @Order(99)
    void logoutAll() throws Exception {
        mockMvc.perform(post("/api/v2/auth/logout-all")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        System.out.println("[E2E] Logout all sessions successful");
    }

    // ==================== HELPERS ====================

    private JsonNode readData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }
}
