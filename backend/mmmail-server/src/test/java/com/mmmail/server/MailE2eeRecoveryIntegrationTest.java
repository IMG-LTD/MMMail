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
class MailE2eeRecoveryIntegrationTest {

    private static final String FINGERPRINT = "AAAABBBBCCCCDDDDEEEEFFFF0000111122223333";
    private static final String RECOVERY_CIPHERTEXT = "-----BEGIN PGP PRIVATE KEY BLOCK-----MMMAIL-RECOVERY-----END PGP PRIVATE KEY BLOCK-----";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void recoveryPackageShouldPersistAndClearWithFoundationDisable() throws Exception {
        String token = register("mail-e2ee-recovery-" + System.nanoTime() + "@mmmail.local");
        saveKeyProfile(token, FINGERPRINT);

        mockMvc.perform(get("/api/v1/settings/mail-e2ee/recovery")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(put("/api/v1/settings/mail-e2ee/recovery")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": true,
                                  "encryptedPrivateKeyArmored": "%s"
                                }
                                """.formatted(RECOVERY_CIPHERTEXT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.encryptedPrivateKeyArmored").value(RECOVERY_CIPHERTEXT));

        mockMvc.perform(put("/api/v1/settings/mail-e2ee")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(get("/api/v1/settings/mail-e2ee/recovery")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.encryptedPrivateKeyArmored").isEmpty());
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Password@123",
                                  "displayName": "Mail Recovery E2EE"
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
                                  "keyCreatedAt": "2026-04-02T15:05:00"
                                }
                                """.formatted(fingerprint, fingerprint, fingerprint)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
