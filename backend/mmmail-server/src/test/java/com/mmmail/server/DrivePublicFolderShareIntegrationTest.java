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
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DrivePublicFolderShareIntegrationTest {

    private static final String SHARE_PASSWORD_HEADER = "X-Drive-Share-Password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void publicFolderShareWorkflowShouldWork() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerEmail = "v83-drive-owner-%s@mmmail.local".formatted(suffix);
        String ownerToken = register(ownerEmail, "Password@123", "V83 Drive Owner");

        String rootFolderId = createFolder(ownerToken, "v83-public-folder");
        String nestedFolderId = createFolder(ownerToken, "contracts", rootFolderId);
        String rootFileId = uploadFile(ownerToken, rootFolderId, "overview.txt", "folder overview body");
        String nestedFileId = uploadFile(ownerToken, nestedFolderId, "draft.txt", "nested draft body");
        assertThat(rootFileId).isNotBlank();

        JsonNode share = createShare(ownerToken, rootFolderId, "EDIT", LocalDateTime.now().plusDays(2).withNano(0).toString(), "Folder#123");
        String shareId = share.path("id").asText();
        String shareToken = share.path("token").asText();

        mockMvc.perform(get("/api/v1/public/drive/shares/" + shareToken + "/metadata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemType").value("FOLDER"))
                .andExpect(jsonPath("$.data.passwordProtected").value(true))
                .andExpect(jsonPath("$.data.permission").value("EDIT"));

        mockMvc.perform(get("/api/v1/public/drive/shares/" + shareToken + "/items"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Drive share password is required"));

        mockMvc.perform(get("/api/v1/public/drive/shares/" + shareToken + "/items")
                        .header(SHARE_PASSWORD_HEADER, "Wrong#123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Drive share password is invalid"));

        mockMvc.perform(get("/api/v1/public/drive/shares/" + shareToken + "/items")
                        .header(SHARE_PASSWORD_HEADER, "Folder#123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].itemType").value("FOLDER"))
                .andExpect(jsonPath("$.data[0].name").value("contracts"))
                .andExpect(jsonPath("$.data[1].itemType").value("FILE"))
                .andExpect(jsonPath("$.data[1].name").value("overview.txt"));

        mockMvc.perform(get("/api/v1/public/drive/shares/" + shareToken + "/items")
                        .param("parentId", nestedFolderId)
                        .header(SHARE_PASSWORD_HEADER, "Folder#123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("draft.txt"));

        mockMvc.perform(get("/api/v1/public/drive/shares/" + shareToken + "/items/" + nestedFileId + "/preview")
                        .header(SHARE_PASSWORD_HEADER, "Folder#123"))
                .andExpect(status().isOk())
                .andExpect(content().string("nested draft body"));

        mockMvc.perform(get("/api/v1/public/drive/shares/" + shareToken + "/items/" + nestedFileId + "/download")
                        .header(SHARE_PASSWORD_HEADER, "Folder#123"))
                .andExpect(status().isOk())
                .andExpect(content().bytes("nested draft body".getBytes(StandardCharsets.UTF_8)));

        MockMultipartFile upload = new MockMultipartFile(
                "file",
                "external-note.txt",
                "text/plain",
                "uploaded by public editor".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/v1/public/drive/shares/" + shareToken + "/files/upload")
                        .file(upload)
                        .param("parentId", rootFolderId)
                        .header(SHARE_PASSWORD_HEADER, "Folder#123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemType").value("FILE"))
                .andExpect(jsonPath("$.data.name").value("external-note.txt"));

        mockMvc.perform(get("/api/v1/drive/items")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("parentId", rootFolderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name=='external-note.txt')]").isNotEmpty());

        mockMvc.perform(post("/api/v1/drive/shares/" + shareId + "/revoke")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVOKED"));

        mockMvc.perform(get("/api/v1/public/drive/shares/" + shareToken + "/items")
                        .header(SHARE_PASSWORD_HEADER, "Folder#123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Share link is unavailable"));
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

    private String createFolder(String token, String name) throws Exception {
        return createFolder(token, name, null);
    }

    private String createFolder(String token, String name, String parentId) throws Exception {
        String parentJson = parentId == null ? "null" : '"' + parentId + '"';
        MvcResult result = mockMvc.perform(post("/api/v1/drive/folders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "parentId": %s
                                }
                                """.formatted(name, parentJson)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemType").value("FOLDER"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asText();
    }

    private String uploadFile(String token, String parentId, String fileName, String content) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );
        MvcResult result = mockMvc.perform(multipart("/api/v1/drive/files/upload")
                        .file(file)
                        .param("parentId", parentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemType").value("FILE"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asText();
    }

    private JsonNode createShare(String token, String itemId, String permission, String expiresAt, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/drive/items/" + itemId + "/shares")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "permission": "%s",
                                  "expiresAt": "%s",
                                  "password": "%s"
                                }
                                """.formatted(permission, expiresAt, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permission").value(permission))
                .andExpect(jsonPath("$.data.passwordProtected").value(true))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }
}
