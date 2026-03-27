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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DriveSecureShareIntegrationTest {

    private static final String SHARE_PASSWORD_HEADER = "X-Drive-Share-Password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void corsPreflightShouldAllowDriveSharePasswordHeader() throws Exception {
        mockMvc.perform(options("/api/v1/public/drive/shares/token/preview")
                        .header("Origin", "http://127.0.0.1:3011")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "x-drive-share-password"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:3011"))
                .andExpect(header().string("Access-Control-Allow-Headers", org.hamcrest.Matchers.containsString("x-drive-share-password")));
    }

    @Test
    void driveSecureShareWorkflowShouldWork() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerEmail = "v82-drive-owner-%s@mmmail.local".formatted(suffix);
        String ownerToken = register(ownerEmail, "Password@123", "V82 Drive Owner");
        String fileId = uploadFile(ownerToken, "v82-secure-share.txt", "v82 secure share body");
        String expiresAt = LocalDateTime.now().plusDays(2).withNano(0).toString();

        JsonNode createdShare = createShare(ownerToken, fileId, "VIEW", expiresAt, "Secret#123");
        String shareId = createdShare.path("id").asText();
        String shareToken = createdShare.path("token").asText();
        assertThat(createdShare.path("passwordProtected").asBoolean()).isTrue();

        JsonNode updatedShare = updateShare(ownerToken, shareId, "EDIT", LocalDateTime.now().plusDays(3).withNano(0).toString(), null, false);
        assertThat(updatedShare.path("id").asText()).isEqualTo(shareId);
        assertThat(updatedShare.path("token").asText()).isEqualTo(shareToken);
        assertThat(updatedShare.path("permission").asText()).isEqualTo("EDIT");
        assertThat(updatedShare.path("passwordProtected").asBoolean()).isTrue();

        mockMvc.perform(get("/api/v1/public/drive/shares/" + shareToken + "/metadata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passwordProtected").value(true))
                .andExpect(jsonPath("$.data.permission").value("EDIT"));

        mockMvc.perform(get("/api/v1/public/drive/shares/" + shareToken + "/download"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(10002))
                .andExpect(jsonPath("$.message").value("Drive share password is required"));

        mockMvc.perform(get("/api/v1/public/drive/shares/" + shareToken + "/preview")
                        .header(SHARE_PASSWORD_HEADER, "Wrong#123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(10002))
                .andExpect(jsonPath("$.message").value("Drive share password is invalid"));

        mockMvc.perform(get("/api/v1/public/drive/shares/" + shareToken + "/preview")
                        .header(SHARE_PASSWORD_HEADER, "Secret#123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Preview-Truncated", "false"))
                .andExpect(content().string("v82 secure share body"));

        JsonNode clearedShare = updateShare(ownerToken, shareId, "EDIT", LocalDateTime.now().plusDays(4).withNano(0).toString(), null, true);
        assertThat(clearedShare.path("passwordProtected").asBoolean()).isFalse();

        mockMvc.perform(get("/api/v1/public/drive/shares/" + shareToken + "/download"))
                .andExpect(status().isOk())
                .andExpect(content().bytes("v82 secure share body".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(post("/api/v1/drive/shares/" + shareId + "/revoke")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVOKED"));

        mockMvc.perform(get("/api/v1/public/drive/shares/" + shareToken + "/metadata"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Share link is unavailable"));

        mockMvc.perform(get("/api/v1/drive/shares/access-logs")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("accessStatus", "DENY_PASSWORD_REQUIRED")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].accessStatus").value("DENY_PASSWORD_REQUIRED"));

        mockMvc.perform(get("/api/v1/drive/shares/access-logs")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("accessStatus", "DENY_PASSWORD_INVALID")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].accessStatus").value("DENY_PASSWORD_INVALID"));
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
                .andExpect(jsonPath("$.data.itemType").value("FILE"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asText();
    }

    private JsonNode createShare(String token, String fileId, String permission, String expiresAt, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/drive/items/" + fileId + "/shares")
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

    private JsonNode updateShare(String token, String shareId, String permission, String expiresAt, String password, boolean clearPassword) throws Exception {
        String passwordJson = password == null ? "null" : '"' + password + '"';
        MvcResult result = mockMvc.perform(put("/api/v1/drive/shares/" + shareId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "permission": "%s",
                                  "expiresAt": "%s",
                                  "password": %s,
                                  "clearPassword": %s
                                }
                                """.formatted(permission, expiresAt, passwordJson, clearPassword)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }
}
