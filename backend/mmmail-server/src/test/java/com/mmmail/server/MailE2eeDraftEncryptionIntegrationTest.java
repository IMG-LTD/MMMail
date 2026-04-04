package com.mmmail.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MailE2eeDraftEncryptionIntegrationTest {

    private static final String DRAFT_CIPHERTEXT = "-----BEGIN PGP MESSAGE-----MMMAIL-DRAFT-----END PGP MESSAGE-----";
    private static final String FINGERPRINT = "ABCD1234ABCD1234ABCD1234ABCD1234ABCD1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void encryptedDraftShouldPersistCiphertextAndMetadata() throws Exception {
        String token = register("mail-e2ee-draft-" + System.nanoTime() + "@mmmail.local");
        saveKeyProfile(token, FINGERPRINT);

        MvcResult saveResult = mockMvc.perform(post("/api/v1/mails/drafts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toEmail": "draft-target@example.com",
                                  "subject": "Encrypted draft",
                                  "e2ee": {
                                    "encryptedBody": "%s",
                                    "algorithm": "openpgp",
                                    "recipientFingerprints": ["%s"]
                                  }
                                }
                                """.formatted(DRAFT_CIPHERTEXT, FINGERPRINT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String draftId = objectMapper.readTree(saveResult.getResponse().getContentAsString()).at("/data/draftId").asText();

        mockMvc.perform(get("/api/v1/mails/" + draftId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDraft").value(true))
                .andExpect(jsonPath("$.data.body").value(DRAFT_CIPHERTEXT))
                .andExpect(jsonPath("$.data.e2ee.enabled").value(true))
                .andExpect(jsonPath("$.data.e2ee.algorithm").value("openpgp"))
                .andExpect(jsonPath("$.data.e2ee.recipientFingerprints[0]").value(FINGERPRINT));

        mockMvc.perform(get("/api/v1/mails/drafts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].preview").value("Mail E2EE encrypted body"));
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Password@123",
                                  "displayName": "Mail Draft E2EE"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();
    }

    private void saveKeyProfile(String token, String fingerprint) throws Exception {
        mockMvc.perform(put("/api/v1/settings/mail-e2ee")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": true,
                                  "publicKeyArmored": "-----BEGIN PGP PUBLIC KEY BLOCK-----%s-----END PGP PUBLIC KEY BLOCK-----",
                                  "encryptedPrivateKeyArmored": "-----BEGIN PGP PRIVATE KEY BLOCK-----%s-----END PGP PRIVATE KEY BLOCK-----",
                                  "fingerprint": "%s",
                                  "algorithm": "curve25519Legacy",
                                  "keyCreatedAt": "2026-04-02T15:00:00"
                                }
                                """.formatted(fingerprint, fingerprint, fingerprint)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
