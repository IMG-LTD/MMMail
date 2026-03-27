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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DocsCollaborationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void docsCollaborationWorkflowShouldWork() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerEmail = "v61-docs-owner-%s@mmmail.local".formatted(suffix);
        String collaboratorEmail = "v61-docs-collab-%s@mmmail.local".formatted(suffix);
        String ownerToken = register(ownerEmail, "Password@123", "V61 Docs Owner");
        String collaboratorToken = register(collaboratorEmail, "Password@123", "V61 Docs Collaborator");
        String noteId = createNote(ownerToken, "v61-collab-note", "hello-collab");

        String shareId = createShare(ownerToken, noteId, collaboratorEmail, "EDIT");
        JsonNode ownerDetail = getNote(ownerToken, noteId);
        long initialCursor = ownerDetail.path("syncCursor").asLong();
        int staleVersion = ownerDetail.path("currentVersion").asInt();

        listNotesShouldContainSharedNote(collaboratorToken, noteId);
        heartbeatPresence(collaboratorToken, noteId);
        assertPresenceVisible(ownerToken, noteId, collaboratorEmail);

        createComment(collaboratorToken, noteId, "hello", "Please review this paragraph");
        assertSyncContainsComment(ownerToken, noteId, initialCursor, collaboratorEmail);

        updateNote(collaboratorToken, noteId, "v61-collab-note", "updated-by-collaborator", staleVersion)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentVersion").value(staleVersion + 1));

        updateNote(ownerToken, noteId, "owner-stale-save", "owner-stale-content", staleVersion)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(30018));

        revokeShare(ownerToken, noteId, shareId);
        assertCollaboratorRemoved(ownerToken, noteId);
        assertCollaboratorAccessRevoked(collaboratorToken, noteId);
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

    private String createShare(String token, String noteId, String email, String permission) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/docs/notes/" + noteId + "/shares")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "collaboratorEmail": "%s",
                                  "permission": "%s"
                                }
                                """.formatted(email, permission)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permission").value(permission))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/shareId").asText();
    }

    private JsonNode getNote(String token, String noteId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/docs/notes/" + noteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private void listNotesShouldContainSharedNote(String token, String noteId) throws Exception {
        mockMvc.perform(get("/api/v1/docs/notes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(noteId))
                .andExpect(jsonPath("$.data[0].scope").value("SHARED"))
                .andExpect(jsonPath("$.data[0].permission").value("EDIT"));
    }

    private void heartbeatPresence(String token, String noteId) throws Exception {
        mockMvc.perform(post("/api/v1/docs/notes/" + noteId + "/presence/heartbeat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activeMode": "EDIT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeMode").value("EDIT"));
    }

    private void assertPresenceVisible(String token, String noteId, String email) throws Exception {
        mockMvc.perform(get("/api/v1/docs/notes/" + noteId + "/collaboration")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.collaborators.length()").value(1))
                .andExpect(jsonPath("$.data.activeSessions[0].email").value(email));
    }

    private void revokeShare(String token, String noteId, String shareId) throws Exception {
        mockMvc.perform(delete("/api/v1/docs/notes/" + noteId + "/shares/" + shareId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shareId").value(shareId));
    }

    private void assertCollaboratorRemoved(String token, String noteId) throws Exception {
        mockMvc.perform(get("/api/v1/docs/notes/" + noteId + "/collaboration")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.collaborators.length()").value(0));
    }

    private void assertCollaboratorAccessRevoked(String token, String noteId) throws Exception {
        mockMvc.perform(get("/api/v1/docs/notes/" + noteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(30017));
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

    private void assertSyncContainsComment(String token, String noteId, long afterEventId, String actorEmail) throws Exception {
        mockMvc.perform(get("/api/v1/docs/notes/" + noteId + "/sync")
                        .header("Authorization", "Bearer " + token)
                        .param("afterEventId", String.valueOf(afterEventId))
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasUpdates").value(true))
                .andExpect(jsonPath("$.data.items[0].eventType").value("DOCS_NOTE_COMMENT_ADD"))
                .andExpect(jsonPath("$.data.items[0].actorEmail").value(actorEmail));
    }

    private org.springframework.test.web.servlet.ResultActions updateNote(
            String token,
            String noteId,
            String title,
            String content,
            int currentVersion
    ) throws Exception {
        return mockMvc.perform(put("/api/v1/docs/notes/" + noteId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "%s",
                          "content": "%s",
                          "currentVersion": %d
                        }
                        """.formatted(title, content, currentVersion)));
    }
}
