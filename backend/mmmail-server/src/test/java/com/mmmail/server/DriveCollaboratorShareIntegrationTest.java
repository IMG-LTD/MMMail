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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DriveCollaboratorShareIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void driveCollaboratorShareLifecycleShouldWork() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerEmail = "v90-drive-owner-%s@mmmail.local".formatted(suffix);
        String collaboratorEmail = "v90-drive-collab-%s@mmmail.local".formatted(suffix);
        String ownerToken = register(ownerEmail, "Password@123", "V90 Drive Owner");
        String collaboratorToken = register(collaboratorEmail, "Password@123", "V90 Drive Collaborator");
        String folderId = createFolder(ownerToken, "v90-shared-root", null);
        String seedFileId = uploadOwnedFile(ownerToken, "v90-seed.txt", "seed-body", folderId);
        String shareId = createCollaboratorShare(ownerToken, folderId, collaboratorEmail, "VIEW");

        assertIncomingShare(collaboratorToken, shareId, folderId, ownerEmail, "VIEW", "NEEDS_ACTION");
        assertThat(listSharedWithMe(collaboratorToken)).isEmpty();

        respondIncomingShare(collaboratorToken, shareId, "ACCEPT")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.responseStatus").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.permission").value("VIEW"));

        assertSharedWithMe(collaboratorToken, shareId, folderId, "VIEW", "ACCEPTED", true);
        assertSharedItemVisible(collaboratorToken, shareId, seedFileId, "v90-seed.txt");
        assertSharedFilePreview(collaboratorToken, shareId, seedFileId, "seed-body");
        assertSharedFileDownload(collaboratorToken, shareId, seedFileId, "seed-body");
        assertViewPermissionBlocked(collaboratorToken, shareId);

        updateCollaboratorShare(ownerToken, folderId, shareId, "EDIT")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permission").value("EDIT"))
                .andExpect(jsonPath("$.data.responseStatus").value("ACCEPTED"));
        assertIncomingShare(collaboratorToken, shareId, folderId, ownerEmail, "EDIT", "ACCEPTED");

        createSharedFolder(collaboratorToken, shareId, "collab-folder", null);
        uploadSharedFile(collaboratorToken, shareId, "collab-note.txt", "collab-body", null);
        JsonNode items = listSharedItems(collaboratorToken, shareId);
        assertThat(items.toString()).contains("v90-seed.txt", "collab-folder", "collab-note.txt");

        revokeCollaboratorShare(ownerToken, folderId, shareId)
                .andExpect(status().isOk());
        assertSharedWithMe(collaboratorToken, shareId, folderId, "EDIT", "REVOKED", false);
        mockMvc.perform(get("/api/v1/drive/collaborator-shares/" + shareId + "/items")
                        .header("Authorization", "Bearer " + collaboratorToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(30044));
    }

    private void assertIncomingShare(
            String token,
            String shareId,
            String itemId,
            String ownerEmail,
            String permission,
            String responseStatus
    ) throws Exception {
        mockMvc.perform(get("/api/v1/drive/collaborator-shares/incoming")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].shareId").value(shareId))
                .andExpect(jsonPath("$.data[0].itemId").value(itemId))
                .andExpect(jsonPath("$.data[0].ownerEmail").value(ownerEmail))
                .andExpect(jsonPath("$.data[0].permission").value(permission))
                .andExpect(jsonPath("$.data[0].responseStatus").value(responseStatus));
    }

    private void assertSharedWithMe(
            String token,
            String shareId,
            String itemId,
            String permission,
            String statusValue,
            boolean available
    ) throws Exception {
        mockMvc.perform(get("/api/v1/drive/collaborator-shares/shared-with-me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].shareId").value(shareId))
                .andExpect(jsonPath("$.data[0].itemId").value(itemId))
                .andExpect(jsonPath("$.data[0].permission").value(permission))
                .andExpect(jsonPath("$.data[0].status").value(statusValue))
                .andExpect(jsonPath("$.data[0].available").value(available));
    }

    private void assertSharedItemVisible(String token, String shareId, String itemId, String itemName) throws Exception {
        mockMvc.perform(get("/api/v1/drive/collaborator-shares/" + shareId + "/items")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(itemId))
                .andExpect(jsonPath("$.data[0].name").value(itemName))
                .andExpect(jsonPath("$.data[0].itemType").value("FILE"));
    }

    private void assertSharedFilePreview(String token, String shareId, String itemId, String contentText) throws Exception {
        mockMvc.perform(get("/api/v1/drive/collaborator-shares/" + shareId + "/files/" + itemId + "/preview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Preview-Truncated", "false"))
                .andExpect(content().string(contentText));
    }

    private void assertSharedFileDownload(String token, String shareId, String itemId, String contentText) throws Exception {
        mockMvc.perform(get("/api/v1/drive/collaborator-shares/" + shareId + "/files/" + itemId + "/download")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().bytes(contentText.getBytes(StandardCharsets.UTF_8)));
    }

    private void assertViewPermissionBlocked(String token, String shareId) throws Exception {
        mockMvc.perform(post("/api/v1/drive/collaborator-shares/" + shareId + "/folders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "blocked-folder"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(10003));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "blocked.txt",
                "text/plain",
                "blocked".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/v1/drive/collaborator-shares/" + shareId + "/files/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(10003));
    }

    private JsonNode listSharedWithMe(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/drive/collaborator-shares/shared-with-me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private JsonNode listSharedItems(String token, String shareId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/drive/collaborator-shares/" + shareId + "/items")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
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

    private String createFolder(String token, String name, String parentId) throws Exception {
        String parentField = parentId == null ? "null" : '"' + parentId + '"';
        MvcResult result = mockMvc.perform(post("/api/v1/drive/folders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "parentId": %s
                                }
                                """.formatted(name, parentField)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asText();
    }

    private String uploadOwnedFile(String token, String fileName, String contentText, String parentId) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                "text/plain",
                contentText.getBytes(StandardCharsets.UTF_8)
        );
        MvcResult result = mockMvc.perform(multipart("/api/v1/drive/files/upload")
                        .file(file)
                        .param("parentId", parentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asText();
    }

    private String createCollaboratorShare(String token, String itemId, String email, String permission) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/drive/items/" + itemId + "/collaborator-shares")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetEmail": "%s",
                                  "permission": "%s"
                                }
                                """.formatted(email, permission)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/shareId").asText();
    }

    private ResultActions updateCollaboratorShare(String token, String itemId, String shareId, String permission) throws Exception {
        return mockMvc.perform(put("/api/v1/drive/items/" + itemId + "/collaborator-shares/" + shareId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "permission": "%s"
                        }
                        """.formatted(permission)));
    }

    private ResultActions revokeCollaboratorShare(String token, String itemId, String shareId) throws Exception {
        return mockMvc.perform(delete("/api/v1/drive/items/" + itemId + "/collaborator-shares/" + shareId)
                .header("Authorization", "Bearer " + token));
    }

    private ResultActions respondIncomingShare(String token, String shareId, String response) throws Exception {
        return mockMvc.perform(post("/api/v1/drive/collaborator-shares/" + shareId + "/respond")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "response": "%s"
                        }
                        """.formatted(response)));
    }

    private String createSharedFolder(String token, String shareId, String name, String parentId) throws Exception {
        String parentField = parentId == null ? "null" : '"' + parentId + '"';
        MvcResult result = mockMvc.perform(post("/api/v1/drive/collaborator-shares/" + shareId + "/folders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "parentId": %s
                                }
                                """.formatted(name, parentField)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asText();
    }

    private String uploadSharedFile(String token, String shareId, String fileName, String contentText, String parentId) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                "text/plain",
                contentText.getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartHttpServletRequestBuilder requestBuilder = multipart("/api/v1/drive/collaborator-shares/" + shareId + "/files/upload");
        requestBuilder.file(file);
        requestBuilder.header("Authorization", "Bearer " + token);
        if (parentId != null) {
            requestBuilder.param("parentId", parentId);
        }
        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asText();
    }
}
