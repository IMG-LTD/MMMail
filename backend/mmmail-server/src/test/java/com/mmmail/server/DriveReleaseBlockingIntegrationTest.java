package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "mmmail.drive.upload.max-file-size-bytes=64")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DriveReleaseBlockingIntegrationTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void ownedCrudUsageAndVersionRestoreShouldRemainStable() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerToken = register("drive-owner-" + suffix + "@mmmail.local", "Drive Owner");

        String rootFolderId = createFolder(ownerToken, "release-root", null);
        String nestedFolderId = createFolder(ownerToken, "release-archive", null);
        String fileId = uploadFile(ownerToken, "release-note.txt", "version-1", rootFolderId);

        mockMvc.perform(put("/api/v1/drive/items/" + fileId + "/rename")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "release-note-renamed.txt"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("release-note-renamed.txt"));

        mockMvc.perform(put("/api/v1/drive/items/" + fileId + "/move")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parentId": "%s"
                                }
                                """.formatted(nestedFolderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parentId").value(nestedFolderId));

        mockMvc.perform(get("/api/v1/drive/usage")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileCount").value(1))
                .andExpect(jsonPath("$.data.folderCount").value(2))
                .andExpect(jsonPath("$.data.storageBytes").value("version-1".getBytes(StandardCharsets.UTF_8).length));

        uploadVersion(ownerToken, fileId, "release-note-renamed.txt", "version-2");

        MvcResult versionsResult = mockMvc.perform(get("/api/v1/drive/files/" + fileId + "/versions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].versionNo").value(1))
                .andReturn();
        String versionId = readJson(versionsResult).at("/data/0/id").asText();

        mockMvc.perform(post("/api/v1/drive/files/" + fileId + "/versions/" + versionId + "/restore")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(fileId));

        mockMvc.perform(get("/api/v1/drive/files/" + fileId + "/download")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(content().bytes("version-1".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(delete("/api/v1/drive/items/" + fileId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/drive/trash/items")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(fileId));
    }

    @Test
    void batchShareShouldAllowPartialSuccessAndOutsiderMustNotCrossBoundary() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerToken = register("drive-share-owner-" + suffix + "@mmmail.local", "Drive Share Owner");
        String outsiderToken = register("drive-share-outsider-" + suffix + "@mmmail.local", "Drive Outsider");

        String folderId = createFolder(ownerToken, "share-batch-folder", null);
        String fileId = uploadFile(ownerToken, "share-batch.txt", "batch-share", null);

        mockMvc.perform(post("/api/v1/drive/items/batch/shares")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itemIds": ["%s", "%s", "99999999"],
                                  "permission": "VIEW",
                                  "password": "Later#123"
                                }
                                """.formatted(folderId, fileId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestedCount").value(3))
                .andExpect(jsonPath("$.data.successCount").value(2))
                .andExpect(jsonPath("$.data.failedCount").value(1))
                .andExpect(jsonPath("$.data.createdShares.length()").value(2))
                .andExpect(jsonPath("$.data.failedItems[0].itemId").value("99999999"));

        mockMvc.perform(get("/api/v1/drive/files/" + fileId + "/download")
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()));

        mockMvc.perform(put("/api/v1/drive/items/" + fileId + "/rename")
                        .header("Authorization", "Bearer " + outsiderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "blocked.txt"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()));

        mockMvc.perform(delete("/api/v1/drive/items/" + fileId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()));
    }

    @Test
    void uploadSizeLimitAndQuotaShouldReturnExplicitErrors() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerToken = register("drive-limits-" + suffix + "@mmmail.local", "Drive Limits");

        MockMultipartFile tooLargeFile = new MockMultipartFile(
                "file",
                "too-large.txt",
                "text/plain",
                "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-limit".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/v1/drive/files/upload")
                        .file(tooLargeFile)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()))
                .andExpect(jsonPath("$.message").value("Drive file size exceeded max upload limit"));

        mockMvc.perform(post("/api/v1/drive/files")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "quota-reject.bin",
                                  "sizeBytes": 629145600,
                                  "mimeType": "application/octet-stream"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.QUOTA_EXCEEDED.getCode()))
                .andExpect(jsonPath("$.message").value("Drive storage quota exceeded for current plan"));
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
        return readJson(result).at("/data/id").asText();
    }

    private String uploadFile(String token, String fileName, String body, String parentId) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                "text/plain",
                body.getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartHttpServletRequestBuilder request = multipart("/api/v1/drive/files/upload")
                .file(file);
        request.header("Authorization", "Bearer " + token);
        if (parentId != null) {
            request.param("parentId", parentId);
        }
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private void uploadVersion(String token, String itemId, String fileName, String body) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                "text/plain",
                body.getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/v1/drive/files/" + itemId + "/versions")
                        .file(file)
                        .header("Authorization", "Bearer " + token)
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(itemId));
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
