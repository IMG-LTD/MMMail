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
class DriveE2eeFoundationIntegrationTest {

    private static final String PASSWORD = "Password@123";
    private static final String E2EE_ALGORITHM = "openpgp";
    private static final String E2EE_FINGERPRINTS_JSON = "[\"DRIVE-SELF-FP\"]";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void ownerDriveE2eeUploadDownloadVersionAndPreviewBoundariesShouldHold() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerToken = register("drive-e2ee-" + suffix + "@mmmail.local", "Drive E2EE");

        String itemId = uploadEncryptedFile(ownerToken, "secret.txt", "ciphertext-v1");

        mockMvc.perform(get("/api/v1/drive/items").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(itemId))
                .andExpect(jsonPath("$.data[0].e2ee.enabled").value(true))
                .andExpect(jsonPath("$.data[0].e2ee.algorithm").value(E2EE_ALGORITHM));

        mockMvc.perform(get("/api/v1/drive/files/" + itemId + "/download")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(content().bytes("ciphertext-v1".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/api/v1/drive/files/" + itemId + "/preview")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Drive E2EE files must be decrypted locally before preview"));

        uploadEncryptedVersion(ownerToken, itemId, "ciphertext-v2");

        mockMvc.perform(get("/api/v1/drive/files/" + itemId + "/download")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(content().bytes("ciphertext-v2".getBytes(StandardCharsets.UTF_8)));

        MvcResult versionsResult = mockMvc.perform(get("/api/v1/drive/files/" + itemId + "/versions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].versionNo").value(1))
                .andExpect(jsonPath("$.data[0].e2ee.enabled").value(true))
                .andReturn();
        String versionId = readJson(versionsResult).at("/data/0/id").asText();

        mockMvc.perform(post("/api/v1/drive/files/" + itemId + "/versions/" + versionId + "/restore")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(itemId))
                .andExpect(jsonPath("$.data.e2ee.enabled").value(true));

        mockMvc.perform(get("/api/v1/drive/files/" + itemId + "/download")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(content().bytes("ciphertext-v1".getBytes(StandardCharsets.UTF_8)));
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
        MockMultipartFile file = encryptedBlob(fileName, ciphertext);
        MvcResult result = mockMvc.perform(multipart("/api/v1/drive/files/upload")
                        .file(file)
                        .param("fileName", fileName)
                        .param("contentType", "text/plain")
                        .param("fileSize", "11")
                        .param("e2eeEnabled", "true")
                        .param("e2eeAlgorithm", E2EE_ALGORITHM)
                        .param("e2eeRecipientFingerprintsJson", E2EE_FINGERPRINTS_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(fileName))
                .andExpect(jsonPath("$.data.e2ee.enabled").value(true))
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private void uploadEncryptedVersion(String token, String itemId, String ciphertext) throws Exception {
        MockMultipartFile file = encryptedBlob("secret.txt", ciphertext);
        mockMvc.perform(multipart("/api/v1/drive/files/" + itemId + "/versions")
                        .file(file)
                        .param("fileName", "secret.txt")
                        .param("contentType", "text/plain")
                        .param("fileSize", "11")
                        .param("e2eeEnabled", "true")
                        .param("e2eeAlgorithm", E2EE_ALGORITHM)
                        .param("e2eeRecipientFingerprintsJson", E2EE_FINGERPRINTS_JSON)
                        .header("Authorization", "Bearer " + token)
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(itemId))
                .andExpect(jsonPath("$.data.e2ee.enabled").value(true));
    }

    private MockMultipartFile encryptedBlob(String fileName, String ciphertext) {
        return new MockMultipartFile(
                "file",
                fileName + ".pgp",
                "application/octet-stream",
                ciphertext.getBytes(StandardCharsets.UTF_8)
        );
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.at("/code").asInt()).isZero();
        return json;
    }
}
