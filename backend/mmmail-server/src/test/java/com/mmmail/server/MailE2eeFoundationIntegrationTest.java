package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MailE2eeFoundationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void profileShouldDefaultToDisabled() throws Exception {
        String token = register("v12-mail-e2ee-default");

        JsonNode profile = getProfile(token);

        assertThat(profile.path("enabled").asBoolean()).isFalse();
        assertThat(profile.path("fingerprint").isNull()).isTrue();
        assertThat(profile.path("algorithm").isNull()).isTrue();
        assertThat(profile.path("publicKeyArmored").isNull()).isTrue();
        assertThat(profile.path("encryptedPrivateKeyArmored").isNull()).isTrue();
        assertThat(profile.path("keyCreatedAt").isNull()).isTrue();
    }

    @Test
    void updateProfileShouldPersistAndAllowDisable() throws Exception {
        String token = register("v12-mail-e2ee-update");

        mockMvc.perform(put("/api/v1/settings/mail-e2ee")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": true,
                                  "publicKeyArmored": "-----BEGIN PGP PUBLIC KEY BLOCK-----MMMAIL-----END PGP PUBLIC KEY BLOCK-----",
                                  "encryptedPrivateKeyArmored": "-----BEGIN PGP PRIVATE KEY BLOCK-----MMMAIL-----END PGP PRIVATE KEY BLOCK-----",
                                  "fingerprint": "ABCD1234ABCD1234ABCD1234ABCD1234ABCD1234",
                                  "algorithm": "curve25519Legacy",
                                  "keyCreatedAt": "2026-04-01T18:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.fingerprint").value("ABCD1234ABCD1234ABCD1234ABCD1234ABCD1234"))
                .andExpect(jsonPath("$.data.algorithm").value("curve25519Legacy"));

        JsonNode reopened = getProfile(token);
        assertThat(reopened.path("enabled").asBoolean()).isTrue();
        assertThat(reopened.path("fingerprint").asText()).isEqualTo("ABCD1234ABCD1234ABCD1234ABCD1234ABCD1234");
        assertThat(reopened.path("algorithm").asText()).isEqualTo("curve25519Legacy");
        assertThat(reopened.path("publicKeyArmored").asText()).contains("PUBLIC KEY BLOCK");
        assertThat(reopened.path("encryptedPrivateKeyArmored").asText()).contains("PRIVATE KEY BLOCK");
        assertThat(reopened.path("keyCreatedAt").asText()).isEqualTo("2026-04-01T18:00:00");

        mockMvc.perform(put("/api/v1/settings/mail-e2ee")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.enabled").value(false));

        JsonNode disabled = getProfile(token);
        assertThat(disabled.path("enabled").asBoolean()).isFalse();
        assertThat(disabled.path("fingerprint").isNull()).isTrue();
        assertThat(disabled.path("algorithm").isNull()).isTrue();
        assertThat(disabled.path("publicKeyArmored").isNull()).isTrue();
        assertThat(disabled.path("encryptedPrivateKeyArmored").isNull()).isTrue();
        assertThat(disabled.path("keyCreatedAt").isNull()).isTrue();
    }

    private JsonNode getProfile(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/settings/mail-e2ee")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String register(String prefix) throws Exception {
        String email = prefix + "-" + System.nanoTime() + "@mmmail.local";
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Password@123",
                                  "displayName": "Mail E2EE Tester"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();
    }
}
