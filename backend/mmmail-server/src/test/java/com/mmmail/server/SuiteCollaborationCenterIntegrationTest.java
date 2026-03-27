package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SuiteCollaborationCenterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void suiteCollaborationCenterShouldAggregateDocsDriveMeetEvents() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerEmail = "v62-owner-%s@mmmail.local".formatted(suffix);
        String collaboratorEmail = "v62-collab-%s@mmmail.local".formatted(suffix);
        String ownerToken = register(ownerEmail, "Password@123", "V62 Owner");
        String collaboratorToken = register(collaboratorEmail, "Password@123", "V62 Collaborator");

        String noteId = createNote(ownerToken, "v62-suite-note", "hello-suite");
        createDocsShare(ownerToken, noteId, collaboratorEmail, "EDIT");
        String folderId = createDriveFolder(ownerToken, "v62-collab-folder");
        String roomId = createMeetRoom(ownerToken, "V62 Collaboration Room", "PUBLIC", 8);

        JsonNode initialCenter = getCollaborationCenter(ownerToken);
        long initialCursor = initialCenter.path("syncCursor").asLong();

        createDriveShare(ownerToken, folderId, "EDIT");
        createComment(collaboratorToken, noteId, "Need review", "Please update the action items");
        endMeetRoom(ownerToken, roomId);

        JsonNode center = getCollaborationCenter(ownerToken);
        Set<String> productCodes = new HashSet<>();
        for (JsonNode item : center.path("items")) {
            productCodes.add(item.path("productCode").asText());
        }
        assertThat(productCodes).contains("DOCS", "DRIVE", "MEET");
        assertThat(center.path("productCounts").path("DOCS").asInt()).isGreaterThan(0);
        assertThat(center.path("productCounts").path("DRIVE").asInt()).isGreaterThan(0);
        assertThat(center.path("productCounts").path("MEET").asInt()).isGreaterThan(0);

        JsonNode sync = getCollaborationSync(ownerToken, initialCursor);
        assertThat(sync.path("hasUpdates").asBoolean()).isTrue();

        Set<String> eventTypes = new HashSet<>();
        String docsActorEmail = null;
        for (JsonNode item : sync.path("items")) {
            eventTypes.add(item.path("eventType").asText());
            if ("DOCS_NOTE_COMMENT_ADD".equals(item.path("eventType").asText())) {
                docsActorEmail = item.path("actorEmail").asText();
            }
        }
        assertThat(eventTypes).contains("DOCS_NOTE_COMMENT_ADD", "DRIVE_SHARE_CREATE", "MEET_ROOM_END");
        assertThat(docsActorEmail).isEqualTo(collaboratorEmail);
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

    private String createNote(String token, String title, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/docs/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "content": "%s"
                                }
                                """.formatted(title, content)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asText();
    }

    private void createDocsShare(String token, String noteId, String email, String permission) throws Exception {
        mockMvc.perform(post("/api/v1/docs/notes/" + noteId + "/shares")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "collaboratorEmail": "%s",
                                  "permission": "%s"
                                }
                                """.formatted(email, permission)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permission").value(permission));
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

    private String createMeetRoom(String token, String topic, String accessLevel, int maxParticipants) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/meet/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topic": "%s",
                                  "accessLevel": "%s",
                                  "maxParticipants": %d
                                }
                                """.formatted(topic, accessLevel, maxParticipants)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/roomId").asText();
    }

    private void endMeetRoom(String token, String roomId) throws Exception {
        mockMvc.perform(post("/api/v1/meet/rooms/" + roomId + "/end")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENDED"));
    }

    private void createComment(String token, String noteId, String excerpt, String content) throws Exception {
        mockMvc.perform(post("/api/v1/docs/notes/" + noteId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "excerpt": "%s",
                                  "content": "%s"
                                }
                                """.formatted(excerpt, content)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.excerpt").value(excerpt));
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
}
