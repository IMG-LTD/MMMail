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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DriveSharedWithMeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sharedWithMeSaveListRemoveAndRevokeShouldWork() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerToken = register("v86-drive-owner-%s@mmmail.local".formatted(suffix), "Password@123", "V86 Owner");
        String recipientToken = register("v86-drive-recipient-%s@mmmail.local".formatted(suffix), "Password@123", "V86 Recipient");

        String fileId = uploadFile(ownerToken, "v86-plan.txt", "shared drive plan");
        JsonNode protectedShare = createShare(ownerToken, fileId, "VIEW", "Later#123");
        String protectedShareId = protectedShare.path("id").asText();
        String protectedToken = protectedShare.path("token").asText();

        mockMvc.perform(post("/api/v1/drive/shared-with-me")
                        .header("Authorization", "Bearer " + recipientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"token\": \"%s\"
                                }
                                """.formatted(protectedToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Drive share password is required"));

        MvcResult savedResult = mockMvc.perform(post("/api/v1/drive/shared-with-me")
                        .header("Authorization", "Bearer " + recipientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"token\": \"%s\",
                                  \"password\": \"Later#123\"
                                }
                                """.formatted(protectedToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.itemName").value("v86-plan.txt"))
                .andReturn();
        String savedShareId = objectMapper.readTree(savedResult.getResponse().getContentAsString()).at("/data/id").asText();

        mockMvc.perform(post("/api/v1/drive/shared-with-me")
                        .header("Authorization", "Bearer " + recipientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"token\": \"%s\",
                                  \"password\": \"Later#123\"
                                }
                                """.formatted(protectedToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Drive share is already saved"));

        mockMvc.perform(get("/api/v1/drive/shared-with-me")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(savedShareId))
                .andExpect(jsonPath("$.data[0].shareId").value(protectedShareId))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data[0].ownerEmail").value("v86-drive-owner-%s@mmmail.local".formatted(suffix)));

        mockMvc.perform(post("/api/v1/drive/shares/" + protectedShareId + "/revoke")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVOKED"));

        mockMvc.perform(get("/api/v1/drive/shared-with-me")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("REVOKED"))
                .andExpect(jsonPath("$.data[0].available").value(false));

        mockMvc.perform(delete("/api/v1/drive/shared-with-me/" + savedShareId)
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isOk());

        JsonNode emptyList = listSharedWithMe(recipientToken);
        assertThat(emptyList).isEmpty();

        JsonNode editShare = createShare(ownerToken, fileId, "EDIT", null);
        String editToken = editShare.path("token").asText();
        mockMvc.perform(post("/api/v1/drive/shared-with-me")
                        .header("Authorization", "Bearer " + recipientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"token\": \"%s\"
                                }
                                """.formatted(editToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only view-only shared links can be saved"));
    }

    private JsonNode listSharedWithMe(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/drive/shared-with-me")
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
                                  \"email\": \"%s\",
                                  \"password\": \"%s\",
                                  \"displayName\": \"%s\"
                                }
                                """.formatted(email, password, displayName)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/accessToken").asText();
    }

    private String uploadFile(String token, String fileName, String content) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );
        MvcResult result = mockMvc.perform(multipart("/api/v1/drive/files/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asText();
    }

    private JsonNode createShare(String token, String itemId, String permission, String password) throws Exception {
        String passwordField = password == null ? "null" : '"' + password + '"';
        MvcResult result = mockMvc.perform(post("/api/v1/drive/items/" + itemId + "/shares")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"permission\": \"%s\",
                                  \"password\": %s
                                }
                                """.formatted(permission, passwordField)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }
}
