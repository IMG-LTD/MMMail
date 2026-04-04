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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DriveReadableShareE2eeIntegrationTest {

    private static final String PASSWORD = "Password@123";
    private static final String SHARE_PASSWORD = "Readable#123";
    private static final String OWNER_E2EE_ALGORITHM = "openpgp";
    private static final String OWNER_E2EE_FINGERPRINTS_JSON = "[\"DRIVE-SELF-FP\"]";
    private static final String SHARE_E2EE_ALGORITHM = "openpgp-password";
    private static final String SHARE_PASSWORD_HEADER = "X-Drive-Share-Password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void driveReadableShareE2eeShouldRequireDedicatedCreatePathAndReusePublicTokenReadFlow() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerToken = register("drive-readable-owner-" + suffix + "@mmmail.local", "Drive Share Owner");
        String viewerToken = register("drive-readable-viewer-" + suffix + "@mmmail.local", "Drive Share Viewer");

        String itemId = uploadEncryptedFile(ownerToken, "secret.txt", "owner-ciphertext");

        mockMvc.perform(post("/api/v1/drive/items/" + itemId + "/shares")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "permission": "VIEW",
                                  "password": "%s"
                                }
                                """.formatted(SHARE_PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Drive E2EE files must use the readable-share E2EE create flow"));

        MockMultipartFile shareCiphertext = new MockMultipartFile(
                "file",
                "secret.txt.pgp",
                "application/octet-stream",
                "share-ciphertext".getBytes(StandardCharsets.UTF_8)
        );

        MvcResult createResult = mockMvc.perform(multipart("/api/v1/drive/items/" + itemId + "/shares/e2ee")
                        .file(shareCiphertext)
                        .param("permission", "VIEW")
                        .param("password", SHARE_PASSWORD)
                        .param("e2eeAlgorithm", SHARE_E2EE_ALGORITHM)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permission").value("VIEW"))
                .andExpect(jsonPath("$.data.passwordProtected").value(true))
                .andExpect(jsonPath("$.data.e2ee.enabled").value(true))
                .andExpect(jsonPath("$.data.e2ee.algorithm").value(SHARE_E2EE_ALGORITHM))
                .andReturn();
        JsonNode createJson = readJson(createResult);
        String shareId = createJson.at("/data/id").asText();
        String shareToken = createJson.at("/data/token").asText();

        mockMvc.perform(get("/api/v1/public/drive/shares/" + shareToken + "/metadata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shareId").value(shareId))
                .andExpect(jsonPath("$.data.passwordProtected").value(true))
                .andExpect(jsonPath("$.data.e2ee.enabled").value(true))
                .andExpect(jsonPath("$.data.e2ee.algorithm").value(SHARE_E2EE_ALGORITHM));

        mockMvc.perform(get("/api/v1/public/drive/shares/" + shareToken + "/download")
                        .header(SHARE_PASSWORD_HEADER, SHARE_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(content().bytes("share-ciphertext".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/api/v1/public/drive/shares/" + shareToken + "/preview")
                        .header(SHARE_PASSWORD_HEADER, SHARE_PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Drive E2EE public shares must be decrypted locally before preview"));

        mockMvc.perform(post("/api/v1/drive/shared-with-me")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "%s",
                                  "password": "%s"
                                }
                                """.formatted(shareToken, SHARE_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.e2ee.enabled").value(true))
                .andExpect(jsonPath("$.data.e2ee.algorithm").value(SHARE_E2EE_ALGORITHM));

        mockMvc.perform(get("/api/v1/drive/shared-with-me")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].shareId").value(shareId))
                .andExpect(jsonPath("$.data[0].e2ee.enabled").value(true))
                .andExpect(jsonPath("$.data[0].e2ee.algorithm").value(SHARE_E2EE_ALGORITHM));
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

    private String uploadEncryptedFile(String token, String fileName, String ciphertext) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName + ".pgp",
                "application/octet-stream",
                ciphertext.getBytes(StandardCharsets.UTF_8)
        );
        MvcResult result = mockMvc.perform(multipart("/api/v1/drive/files/upload")
                        .file(file)
                        .param("fileName", fileName)
                        .param("contentType", "text/plain")
                        .param("fileSize", "16")
                        .param("e2eeEnabled", "true")
                        .param("e2eeAlgorithm", OWNER_E2EE_ALGORITHM)
                        .param("e2eeRecipientFingerprintsJson", OWNER_E2EE_FINGERPRINTS_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.e2ee.enabled").value(true))
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.at("/code").asInt()).isZero();
        return json;
    }
}
