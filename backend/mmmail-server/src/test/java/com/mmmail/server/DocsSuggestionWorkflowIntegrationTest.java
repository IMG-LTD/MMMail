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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocsSuggestionWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void docsSuggestionWorkflowShouldSupportPermissionUpdatesAndResponses() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerEmail = "v88-docs-owner-%s@mmmail.local".formatted(suffix);
        String collaboratorEmail = "v88-docs-collab-%s@mmmail.local".formatted(suffix);
        String ownerToken = register(ownerEmail, "Password@123", "V88 Docs Owner");
        String collaboratorToken = register(collaboratorEmail, "Password@123", "V88 Docs Collaborator");
        String noteId = createNote(ownerToken, "hello world", "hello world");

        String shareId = createShare(ownerToken, noteId, collaboratorEmail, "EDIT");
        String suggestionId = createSuggestion(collaboratorToken, noteId, 0, 5, "hello", "private", 1);

        mockMvc.perform(get("/api/v1/docs/notes/" + noteId + "/suggestions")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].suggestionId").value(suggestionId))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));

        updateSharePermission(ownerToken, noteId, shareId, "VIEW")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permission").value("VIEW"));

        updateNote(collaboratorToken, noteId, "blocked", "should-fail", 1)
                .andExpect(status().isForbidden());

        acceptSuggestion(ownerToken, noteId, suggestionId, 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        JsonNode acceptedNote = getNote(ownerToken, noteId);
        assertThat(acceptedNote.path("content").asText()).isEqualTo("private world");
        assertThat(acceptedNote.path("currentVersion").asInt()).isEqualTo(2);

        updateSharePermission(ownerToken, noteId, shareId, "EDIT")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permission").value("EDIT"));

        String rejectedSuggestionId = createSuggestion(collaboratorToken, noteId, 8, 13, "world", "suite", 2);
        mockMvc.perform(post("/api/v1/docs/notes/" + noteId + "/suggestions/" + rejectedSuggestionId + "/reject")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    void acceptingStaleSuggestionShouldReturnConflict() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerEmail = "v88-docs-owner-stale-%s@mmmail.local".formatted(suffix);
        String collaboratorEmail = "v88-docs-collab-stale-%s@mmmail.local".formatted(suffix);
        String ownerToken = register(ownerEmail, "Password@123", "V88 Docs Owner Stale");
        String collaboratorToken = register(collaboratorEmail, "Password@123", "V88 Docs Collaborator Stale");
        String noteId = createNote(ownerToken, "alpha beta", "alpha beta");

        createShare(ownerToken, noteId, collaboratorEmail, "EDIT");
        String suggestionId = createSuggestion(collaboratorToken, noteId, 0, 5, "alpha", "omega", 1);

        updateNote(ownerToken, noteId, "alpha beta", "alpha gamma", 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentVersion").value(2));

        acceptSuggestion(ownerToken, noteId, suggestionId, 2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(30041));
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
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/shareId").asText();
    }

    private String createSuggestion(
            String token,
            String noteId,
            int selectionStart,
            int selectionEnd,
            String originalText,
            String replacementText,
            int baseVersion
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/docs/notes/" + noteId + "/suggestions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "selectionStart": %d,
                                  "selectionEnd": %d,
                                  "originalText": "%s",
                                  "replacementText": "%s",
                                  "baseVersion": %d
                                }
                                """.formatted(selectionStart, selectionEnd, originalText, replacementText, baseVersion)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/suggestionId").asText();
    }

    private JsonNode getNote(String token, String noteId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/docs/notes/" + noteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private org.springframework.test.web.servlet.ResultActions updateSharePermission(
            String token,
            String noteId,
            String shareId,
            String permission
    ) throws Exception {
        return mockMvc.perform(put("/api/v1/docs/notes/" + noteId + "/shares/" + shareId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "permission": "%s"
                        }
                        """.formatted(permission)));
    }

    private org.springframework.test.web.servlet.ResultActions acceptSuggestion(
            String token,
            String noteId,
            String suggestionId,
            int currentVersion
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/docs/notes/" + noteId + "/suggestions/" + suggestionId + "/accept")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "currentVersion": %d
                        }
                        """.formatted(currentVersion)));
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
