package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticatorPortabilityIntegrationTest {

    private static final String PASSWORD = "Password@123";
    private static final String PASSPHRASE = "Backup@123456";
    private static final String OTPAUTH_CONTENT = """
            otpauth://totp/GitHub:dev@example.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub&algorithm=SHA1&digits=6&period=30
            otpauth://totp/Proton:ops@example.com?secret=MZXW6YTBOI======&issuer=Proton&algorithm=SHA256&digits=8&period=45
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void otpauthImportExportAndEncryptedBackupRoundTripShouldWork() throws Exception {
        String token = register("v103-auth-port");

        MvcResult importResult = mockMvc.perform(post("/api/v1/authenticator/import")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "format": "AUTO",
                                  "content": %s
                                }
                                """.formatted(objectMapper.writeValueAsString(OTPAUTH_CONTENT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.importedCount").value(2))
                .andReturn();

        JsonNode imported = readData(importResult);
        String firstEntryId = imported.path("entries").get(0).path("id").asText();

        mockMvc.perform(post("/api/v1/authenticator/entries/" + firstEntryId + "/code")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value(org.hamcrest.Matchers.matchesRegex("^\\d{6}$")));

        MvcResult exportResult = mockMvc.perform(get("/api/v1/authenticator/export")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.format").value("MMMAIL_JSON"))
                .andExpect(jsonPath("$.data.entryCount").value(2))
                .andReturn();
        JsonNode exportData = readData(exportResult);
        String exportContent = exportData.path("content").asText();
        assertThat(exportContent).contains("MMMAIL_AUTHENTICATOR_EXPORT");
        assertThat(exportContent).contains("dev@example.com");

        MvcResult backupResult = mockMvc.perform(post("/api/v1/authenticator/backup/export")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "passphrase": "%s"
                                }
                                """.formatted(PASSPHRASE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entryCount").value(2))
                .andExpect(jsonPath("$.data.encryption").value("AES-256-GCM"))
                .andReturn();
        String backupContent = readData(backupResult).path("content").asText();
        assertThat(backupContent).contains("MMMAIL_AUTHENTICATOR_BACKUP");
        assertThat(backupContent).doesNotContain("JBSWY3DPEHPK3PXP");
        assertThat(backupContent).doesNotContain("dev@example.com");

        deleteAllEntries(token);

        mockMvc.perform(post("/api/v1/authenticator/backup/import")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": %s,
                                  "passphrase": "%s"
                                }
                                """.formatted(objectMapper.writeValueAsString(backupContent), PASSPHRASE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.importedCount").value(2))
                .andExpect(jsonPath("$.data.totalCount").value(2));

        mockMvc.perform(get("/api/v1/authenticator/entries")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        deleteAllEntries(token);

        mockMvc.perform(post("/api/v1/authenticator/import")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "format": "MMMAIL_JSON",
                                  "content": %s
                                }
                                """.formatted(objectMapper.writeValueAsString(exportContent))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.importedCount").value(2));
    }

    @Test
    void invalidBackupPassphraseShouldFailExplicitly() throws Exception {
        String token = register("v103-auth-invalid");

        mockMvc.perform(post("/api/v1/authenticator/import")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "format": "OTPAUTH_URI",
                                  "content": %s
                                }
                                """.formatted(objectMapper.writeValueAsString(OTPAUTH_CONTENT))))
                .andExpect(status().isOk());

        MvcResult backupResult = mockMvc.perform(post("/api/v1/authenticator/backup/export")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "passphrase": "%s"
                                }
                                """.formatted(PASSPHRASE)))
                .andExpect(status().isOk())
                .andReturn();
        String backupContent = readData(backupResult).path("content").asText();

        mockMvc.perform(post("/api/v1/authenticator/backup/import")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": %s,
                                  "passphrase": "Wrong@123"
                                }
                                """.formatted(objectMapper.writeValueAsString(backupContent))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(30050))
                .andExpect(jsonPath("$.message").value("Authenticator backup passphrase is invalid"));
    }

    @Test
    void securityPreferenceAndQrImageImportShouldWork() throws Exception {
        String token = register("v109-auth-security");

        mockMvc.perform(put("/api/v1/authenticator/security")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "syncEnabled": true,
                                  "encryptedBackupEnabled": true,
                                  "pinProtectionEnabled": true,
                                  "lockTimeoutSeconds": 600,
                                  "pin": "246824"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.syncEnabled").value(true))
                .andExpect(jsonPath("$.data.encryptedBackupEnabled").value(true))
                .andExpect(jsonPath("$.data.pinProtectionEnabled").value(true))
                .andExpect(jsonPath("$.data.pinConfigured").value(true))
                .andExpect(jsonPath("$.data.lockTimeoutSeconds").value(600));

        mockMvc.perform(post("/api/v1/authenticator/security/verify-pin")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pin": "111111"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Authenticator PIN is invalid"));

        mockMvc.perform(post("/api/v1/authenticator/security/verify-pin")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pin": "246824"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verified").value(true))
                .andExpect(jsonPath("$.data.lockTimeoutSeconds").value(600));

        String otpauthUri = "otpauth://totp/Proton:sync@example.com?secret=JBSWY3DPEHPK3PXP&issuer=Proton&algorithm=SHA1&digits=6&period=30";

        mockMvc.perform(post("/api/v1/authenticator/import/qr-image")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dataUrl": %s
                                }
                                """.formatted(objectMapper.writeValueAsString(buildQrDataUrl(otpauthUri)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.importedCount").value(1))
                .andExpect(jsonPath("$.data.entries[0].issuer").value("Proton"))
                .andExpect(jsonPath("$.data.entries[0].accountName").value("sync@example.com"));

        mockMvc.perform(get("/api/v1/authenticator/security")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.syncEnabled").value(true))
                .andExpect(jsonPath("$.data.lastSyncedAt").isNotEmpty());

        mockMvc.perform(post("/api/v1/authenticator/backup/export")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "passphrase": "%s"
                                }
                                """.formatted(PASSPHRASE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entryCount").value(1));

        mockMvc.perform(get("/api/v1/authenticator/security")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lastBackupAt").isNotEmpty());
    }

    private void deleteAllEntries(String token) throws Exception {
        MvcResult listResult = mockMvc.perform(get("/api/v1/authenticator/entries")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode entries = readData(listResult);
        for (JsonNode entry : entries) {
            mockMvc.perform(delete("/api/v1/authenticator/entries/" + entry.path("id").asText())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    private JsonNode readData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String buildQrDataUrl(String payload) throws Exception {
        BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 320, 320);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    private String register(String prefix) throws Exception {
        String email = prefix + "-" + System.nanoTime() + "@mmmail.local";
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "displayName": "Authenticator Portability Tester"
                                }
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("accessToken").asText();
    }
}
