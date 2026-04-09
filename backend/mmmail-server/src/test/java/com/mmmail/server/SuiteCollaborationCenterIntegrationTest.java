package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SuiteCollaborationCenterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void suiteCollaborationCenterShouldAggregateMainlineMailCalendarDriveAndPassEvents() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerEmail = "v62-owner-%s@mmmail.local".formatted(suffix);
        String collaboratorEmail = "v62-collab-%s@mmmail.local".formatted(suffix);
        String ownerToken = register(ownerEmail, "Password@123", "V62 Owner");
        register(collaboratorEmail, "Password@123", "V62 Collaborator");

        JsonNode initialCenter = getCollaborationCenter(ownerToken);
        long initialCursor = initialCenter.path("syncCursor").asLong();

        setUndoSendSeconds(ownerToken, "V62 Owner", 0);
        String draftId = saveDraft(ownerToken, collaboratorEmail, "v62 secure handoff", "v62 collaboration body");
        uploadDraftAttachment(ownerToken, draftId, "handoff.txt", "handoff-payload");
        sendMail(ownerToken, draftId, collaboratorEmail, "v62 secure handoff", "v62 collaboration body", "suite-collab-send-" + suffix);

        String eventId = createCalendarEvent(ownerToken, collaboratorEmail);
        createCalendarShare(ownerToken, eventId, collaboratorEmail);

        String folderId = createDriveFolder(ownerToken, "v62-collab-folder");
        createDriveShare(ownerToken, folderId, "EDIT");

        createPassItem(ownerToken, "V62 Root Secret");

        JsonNode center = getCollaborationCenter(ownerToken);
        Set<String> productCodes = collectFieldValues(center.path("items"), "productCode");
        assertThat(productCodes).contains("MAIL", "CALENDAR", "DRIVE", "PASS");
        assertThat(center.path("productCounts").path("MAIL").asInt()).isGreaterThan(0);
        assertThat(center.path("productCounts").path("CALENDAR").asInt()).isGreaterThan(0);
        assertThat(center.path("productCounts").path("DRIVE").asInt()).isGreaterThan(0);
        assertThat(center.path("productCounts").path("PASS").asInt()).isGreaterThan(0);

        JsonNode sync = getCollaborationSync(ownerToken, initialCursor);
        assertThat(sync.path("hasUpdates").asBoolean()).isTrue();
        Set<String> eventTypes = collectFieldValues(sync.path("items"), "eventType");
        assertThat(eventTypes).contains("MAIL_SENT", "CAL_SHARE_CREATE", "DRIVE_SHARE_CREATE", "PASS_ITEM_CREATE");
    }

    private String register(String email, String password, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "displayName": "%s"
                                }
                                """.formatted(email, password, displayName)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/accessToken").asText();
    }

    private void setUndoSendSeconds(String token, String displayName, int undoSeconds) throws Exception {
        mockMvc.perform(put("/api/v1/settings/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "%s",
                                  "signature": "",
                                  "timezone": "UTC",
                                  "autoSaveSeconds": 15,
                                  "undoSendSeconds": %d,
                                  "driveVersionRetentionCount": 50,
                                  "driveVersionRetentionDays": 365
                                }
                                """.formatted(displayName, undoSeconds)))
                .andExpect(status().isOk());
    }

    private String saveDraft(String token, String toEmail, String subject, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/mails/drafts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toEmail": "%s",
                                  "subject": "%s",
                                  "body": "%s"
                                }
                                """.formatted(toEmail, subject, body)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/draftId").asText();
    }

    private void uploadDraftAttachment(String token, String draftId, String fileName, String content) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                MediaType.TEXT_PLAIN_VALUE,
                content.getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/v1/mails/drafts/" + draftId + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.draftId").value(draftId));
    }

    private void sendMail(
            String token,
            String draftId,
            String toEmail,
            String subject,
            String body,
            String idempotencyKey
    ) throws Exception {
        mockMvc.perform(post("/api/v1/mails/send")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "draftId": %s,
                                  "toEmail": "%s",
                                  "subject": "%s",
                                  "body": "%s",
                                  "idempotencyKey": "%s",
                                  "labels": []
                                }
                                """.formatted(draftId, toEmail, subject, body, idempotencyKey)))
                .andExpect(status().isOk());
    }

    private String createCalendarEvent(String token, String attendeeEmail) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "V62 review checkpoint",
                                  "location": "Ops Room",
                                  "startAt": "2026-04-20T10:00:00",
                                  "endAt": "2026-04-20T11:00:00",
                                  "timezone": "UTC",
                                  "reminderMinutes": 15,
                                  "attendees": [
                                    {"email": "%s", "displayName": "Reviewer"}
                                  ]
                                }
                                """.formatted(attendeeEmail)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asText();
    }

    private void createCalendarShare(String token, String eventId, String targetEmail) throws Exception {
        mockMvc.perform(post("/api/v1/calendar/events/" + eventId + "/shares")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetEmail": "%s",
                                  "permission": "EDIT"
                                }
                                """.formatted(targetEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permission").value("EDIT"));
    }

    private String createDriveFolder(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/drive/folders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s"
                                }
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asText();
    }

    private void createDriveShare(String token, String itemId, String permission) throws Exception {
        mockMvc.perform(post("/api/v1/drive/items/" + itemId + "/shares")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "permission": "%s"
                                }
                                """.formatted(permission)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permission").value(permission));
    }

    private void createPassItem(String token, String title) throws Exception {
        mockMvc.perform(post("/api/v1/pass/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "itemType": "LOGIN",
                                  "website": "https://release.example.com",
                                  "username": "release@example.com",
                                  "secretCiphertext": "Team#123456A",
                                  "note": "suite collaboration pass item"
                                }
                                """.formatted(title)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists());
    }

    private JsonNode getCollaborationCenter(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/suite/collaboration-center")
                        .header("Authorization", "Bearer " + token)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private JsonNode getCollaborationSync(String token, long afterEventId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/suite/collaboration-center/sync")
                        .header("Authorization", "Bearer " + token)
                        .param("afterEventId", String.valueOf(afterEventId))
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private Set<String> collectFieldValues(JsonNode nodes, String fieldName) {
        Set<String> values = new HashSet<>();
        for (JsonNode node : nodes) {
            values.add(node.path(fieldName).asText());
        }
        return values;
    }
}
