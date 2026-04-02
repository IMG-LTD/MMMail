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
class MailE2eeRecipientDiscoveryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void recipientStatusShouldBeReadyWhenRouteHasPublicKey() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String senderEmail = "mail-e2ee-sender-%s@mmmail.local".formatted(suffix);
        String receiverEmail = "mail-e2ee-ready-%s@mmmail.local".formatted(suffix);

        String senderToken = register(senderEmail);
        String receiverToken = register(receiverEmail);
        saveKeyProfile(receiverToken);

        mockMvc.perform(get("/api/v1/mails/e2ee-recipient-status")
                        .header("Authorization", "Bearer " + senderToken)
                        .param("toEmail", receiverEmail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.deliverable").value(true))
                .andExpect(jsonPath("$.data.encryptionReady").value(true))
                .andExpect(jsonPath("$.data.readiness").value("READY"))
                .andExpect(jsonPath("$.data.routeCount").value(1))
                .andExpect(jsonPath("$.data.routes[0].keyAvailable").value(true))
                .andExpect(jsonPath("$.data.routes[0].fingerprint").value("ABCD1234ABCD1234ABCD1234ABCD1234ABCD1234"));
    }

    @Test
    void recipientStatusShouldBeNotReadyWhenRouteHasNoKey() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String senderEmail = "mail-e2ee-sender-%s@mmmail.local".formatted(suffix);
        String receiverEmail = "mail-e2ee-not-ready-%s@mmmail.local".formatted(suffix);

        String senderToken = register(senderEmail);
        register(receiverEmail);

        mockMvc.perform(get("/api/v1/mails/e2ee-recipient-status")
                        .header("Authorization", "Bearer " + senderToken)
                        .param("toEmail", receiverEmail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.deliverable").value(true))
                .andExpect(jsonPath("$.data.encryptionReady").value(false))
                .andExpect(jsonPath("$.data.readiness").value("NOT_READY"))
                .andExpect(jsonPath("$.data.routeCount").value(1))
                .andExpect(jsonPath("$.data.routes[0].keyAvailable").value(false));
    }

    @Test
    void recipientStatusShouldBeUndeliverableForUnknownAddress() throws Exception {
        String senderToken = register("mail-e2ee-sender-%s@mmmail.local".formatted(System.nanoTime()));

        mockMvc.perform(get("/api/v1/mails/e2ee-recipient-status")
                        .header("Authorization", "Bearer " + senderToken)
                        .param("toEmail", "unknown-recipient@mmmail.local"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.deliverable").value(false))
                .andExpect(jsonPath("$.data.encryptionReady").value(false))
                .andExpect(jsonPath("$.data.readiness").value("UNDELIVERABLE"))
                .andExpect(jsonPath("$.data.routeCount").value(0));
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Password@123",
                                  "displayName": "Mail E2EE Discovery"
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

    private void saveKeyProfile(String token) throws Exception {
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
                                  "keyCreatedAt": "2026-04-01T21:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
