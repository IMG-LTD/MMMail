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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendV21DriveRuntimeBridgeTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void v21DriveShouldUseRuntimeDriveStateForCommunityPaths() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String token = register("v21-drive-owner-" + suffix + "@mmmail.local", "V21 Drive Owner");
        String folderId = createV1Folder(token, "v21-root", null);
        String fileId = createV21Upload(token, "v21-plan.txt", folderId, 42);

        mockMvc.perform(get("/api/v2/drive/uploads/" + fileId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(fileId))
                .andExpect(jsonPath("$.data.name").value("v21-plan.txt"));

        mockMvc.perform(get("/api/v2/drive/files")
                        .header("Authorization", "Bearer " + token)
                        .param("parentId", folderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(fileId));

        mockMvc.perform(get("/api/v2/drive/folders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(folderId));

        mockMvc.perform(get("/api/v2/drive/storage/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileCount").value(1))
                .andExpect(jsonPath("$.data.folderCount").value(1))
                .andExpect(jsonPath("$.data.storageBytes").value(42));

        mockMvc.perform(get("/api/v2/drive/files/" + fileId + "/share")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(post("/api/v2/drive/files/" + fileId + "/share")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "permission": "VIEW",
                                  "password": "Share#123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemId").value(fileId))
                .andExpect(jsonPath("$.data.permission").value("VIEW"));

        mockMvc.perform(get("/api/v2/drive/files/" + fileId + "/share")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(patch("/api/v2/drive/files/" + fileId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "v21-plan-renamed.txt",
                                  "parentId": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("v21-plan-renamed.txt"));

        mockMvc.perform(delete("/api/v2/drive/files/" + fileId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void v21DriveShouldRejectEmptyPatchAndKeepVersionsPremiumGated() throws Exception {
        String token = register("v21-drive-gate-" + System.nanoTime() + "@mmmail.local", "V21 Drive Gate");
        String fileId = createV21Upload(token, "v21-gated.bin", null, 7);

        mockMvc.perform(patch("/api/v2/drive/files/" + fileId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()));

        mockMvc.perform(get("/api/v2/drive/files/" + fileId + "/versions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode()));
    }

    @Test
    void v21DrivePatchShouldRejectUnknownFieldsBeforeMutation() throws Exception {
        String token = register("v21-drive-strict-" + System.nanoTime() + "@mmmail.local", "V21 Drive Strict");
        String fileId = createV21Upload(token, "v21-strict.bin", null, 7);

        mockMvc.perform(patch("/api/v2/drive/files/" + fileId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "v21-strict-renamed.bin",
                                  "visibility": "public"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()));

        mockMvc.perform(get("/api/v2/drive/uploads/" + fileId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("v21-strict.bin"));
    }

    private String createV21Upload(String token, String fileName, String parentId, long sizeBytes) throws Exception {
        String parentField = parentId == null ? "null" : '"' + parentId + '"';
        MvcResult result = mockMvc.perform(post("/api/v2/drive/uploads")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fileName": "%s",
                                  "parentId": %s,
                                  "sizeBytes": %d
                                }
                                """.formatted(fileName, parentField, sizeBytes)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private String createV1Folder(String token, String name, String parentId) throws Exception {
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

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
