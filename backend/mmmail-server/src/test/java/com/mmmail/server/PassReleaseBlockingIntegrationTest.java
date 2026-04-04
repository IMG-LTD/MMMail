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

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PassReleaseBlockingIntegrationTest {

    private static final String PASSWORD = "Password@123";
    private static final Pattern VERIFICATION_CODE_PATTERN = Pattern.compile("Verification code: (\\d{6})");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void personalWorkspaceMailboxAndAliasFlowsShouldRemainStable() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerEmail = "pass-rb-owner-" + suffix + "@mmmail.local";
        String mailboxEmail = "pass-rb-mailbox-" + suffix + "@mmmail.local";
        String ownerToken = register(ownerEmail, "Pass RB Owner");
        String mailboxToken = register(mailboxEmail, "Pass RB Mailbox");
        setUndoSendSeconds(ownerToken, "Pass RB Owner", 0);
        setUndoSendSeconds(mailboxToken, "Pass RB Mailbox", 0);

        String itemId = createPersonalItem(ownerToken, "Release Portal", "LOGIN", "Root#123456A");
        updatePersonalItem(ownerToken, itemId, "Release Portal", "LOGIN", "Root#654321B", "Updated during release blocking");
        toggleFavorite(ownerToken, itemId, true);
        assertPersonalFavoriteState(ownerToken, itemId, true);
        toggleFavorite(ownerToken, itemId, false);
        assertPersonalFavoriteState(ownerToken, itemId, false);

        String mailboxId = createMailbox(ownerToken, mailboxEmail);
        verifyMailbox(ownerToken, mailboxId, extractVerificationCode(mailboxToken));
        setDefaultMailbox(ownerToken, mailboxId);
        assertMailboxState(ownerToken, mailboxEmail, "VERIFIED", true);

        String aliasId = createAlias(ownerToken, List.of(mailboxEmail), "Release Relay", "release");
        setAliasStatus(ownerToken, aliasId, false);
        setAliasStatus(ownerToken, aliasId, true);
        assertAliasState(ownerToken, aliasId, "ENABLED", List.of(mailboxEmail));
    }

    @Test
    void sharedVaultIncomingShareAndSecureLinkFlowsShouldRemainStable() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerEmail = "pass-rb-shared-owner-" + suffix + "@mmmail.local";
        String memberEmail = "pass-rb-shared-member-" + suffix + "@mmmail.local";
        String auditorEmail = "pass-rb-shared-auditor-" + suffix + "@mmmail.local";
        String ownerToken = register(ownerEmail, "Pass Shared Owner");
        String memberToken = register(memberEmail, "Pass Shared Member");
        String auditorToken = register(auditorEmail, "Pass Shared Auditor");

        String orgId = createOrganization(ownerToken, "Pass RB Shared");
        inviteMember(ownerToken, orgId, memberEmail, "MEMBER");
        inviteMember(ownerToken, orgId, auditorEmail, "MEMBER");
        acceptFirstIncomingInvite(memberToken);
        acceptFirstIncomingInvite(auditorToken);
        updatePassPolicy(ownerToken, orgId);

        String vaultId = createSharedVault(ownerToken, orgId, "Release Vault", "Shared release credentials");
        addVaultMember(ownerToken, orgId, vaultId, memberEmail, "MEMBER");
        String itemId = createSharedItem(ownerToken, orgId, vaultId, "Deploy Root", "LOGIN", "Team#123456A");
        String shareId = createItemShare(ownerToken, orgId, itemId, auditorEmail);
        String secureLinkId = createSecureLink(ownerToken, orgId, itemId, 2);
        String publicToken = readSecureLinkToken(ownerToken, orgId, itemId);

        mockMvc.perform(get("/api/v1/pass/orgs/" + orgId + "/shared-vaults/" + vaultId + "/items")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(itemId));

        mockMvc.perform(get("/api/v1/pass/orgs/" + orgId + "/incoming-item-shares")
                        .header("Authorization", "Bearer " + auditorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].itemId").value(itemId));

        mockMvc.perform(get("/api/v1/pass/orgs/" + orgId + "/incoming-item-shares/" + itemId)
                        .header("Authorization", "Bearer " + auditorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemId").value(itemId))
                .andExpect(jsonPath("$.data.readOnly").value(true));

        mockMvc.perform(get("/api/v1/pass/orgs/" + orgId + "/items/" + itemId + "/item-shares")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(shareId));

        mockMvc.perform(get("/api/v1/public/pass/secure-links/" + publicToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemId").value(itemId))
                .andExpect(jsonPath("$.data.title").value("Deploy Root"));

        mockMvc.perform(delete("/api/v1/pass/orgs/" + orgId + "/secure-links/" + secureLinkId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(get("/api/v1/public/pass/secure-links/" + publicToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Pass secure link has been revoked"));
    }

    @Test
    void protectedPassEndpointsShouldRejectMissingSession() throws Exception {
        mockMvc.perform(get("/api/v1/pass/items"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/pass/monitor"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/pass/mailboxes"))
                .andExpect(status().isUnauthorized());
    }

    private String register(String email, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "email", email,
                                "password", PASSWORD,
                                "displayName", displayName
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/accessToken").asText();
    }

    private void setUndoSendSeconds(String token, String displayName, int undoSeconds) throws Exception {
        mockMvc.perform(put("/api/v1/settings/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "displayName", displayName,
                                "signature", "",
                                "timezone", "UTC",
                                "autoSaveSeconds", 15,
                                "undoSendSeconds", undoSeconds,
                                "driveVersionRetentionCount", 50,
                                "driveVersionRetentionDays", 365
                        ))))
                .andExpect(status().isOk());
    }

    private String createPersonalItem(String token, String title, String itemType, String secret) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/pass/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "title", title,
                                "itemType", itemType,
                                "website", "https://release.example.com",
                                "username", "release@example.com",
                                "secretCiphertext", secret,
                                "note", "release blocking personal item"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private void updatePersonalItem(String token, String itemId, String title, String itemType, String secret, String note) throws Exception {
        mockMvc.perform(put("/api/v1/pass/items/" + itemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "title", title,
                                "itemType", itemType,
                                "website", "https://release.example.com",
                                "username", "release@example.com",
                                "secretCiphertext", secret,
                                "note", note
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.note").value(note));
    }

    private void toggleFavorite(String token, String itemId, boolean favorite) throws Exception {
        if (favorite) {
            mockMvc.perform(post("/api/v1/pass/items/" + itemId + "/favorite")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.favorite").value(true));
            return;
        }
        mockMvc.perform(delete("/api/v1/pass/items/" + itemId + "/favorite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorite").value(false));
    }

    private void assertPersonalFavoriteState(String token, String itemId, boolean favorite) throws Exception {
        mockMvc.perform(get("/api/v1/pass/items")
                        .header("Authorization", "Bearer " + token)
                        .param("favoriteOnly", String.valueOf(favorite)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(favorite ? "$.data[0].id" : "$.data.length()").value(favorite ? itemId : 1));
    }

    private String createMailbox(String token, String mailboxEmail) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/pass/mailboxes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of("mailboxEmail", mailboxEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private void verifyMailbox(String token, String mailboxId, String verificationCode) throws Exception {
        mockMvc.perform(post("/api/v1/pass/mailboxes/" + mailboxId + "/verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of("verificationCode", verificationCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"));
    }

    private void setDefaultMailbox(String token, String mailboxId) throws Exception {
        mockMvc.perform(post("/api/v1/pass/mailboxes/" + mailboxId + "/default")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultMailbox").value(true));
    }

    private void assertMailboxState(String token, String mailboxEmail, String statusValue, boolean defaultMailbox) throws Exception {
        JsonNode mailbox = findByField(listData(token, "/api/v1/pass/mailboxes"), "mailboxEmail", mailboxEmail);
        assertThat(mailbox).isNotNull();
        assertThat(mailbox.path("status").asText()).isEqualTo(statusValue);
        assertThat(mailbox.path("defaultMailbox").asBoolean()).isEqualTo(defaultMailbox);
    }

    private String extractVerificationCode(String token) throws Exception {
        String mailId = latestFolderMailId(token, "inbox");
        MvcResult result = mockMvc.perform(get("/api/v1/mails/" + mailId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        Matcher matcher = VERIFICATION_CODE_PATTERN.matcher(readJson(result).at("/data/body").asText());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String latestFolderMailId(String token, String folder) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/mails/" + folder)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/items/0/id").asText();
    }

    private String createAlias(String token, List<String> forwardToEmails, String title, String prefix) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/pass/aliases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "title", title,
                                "note", "release alias",
                                "forwardToEmails", forwardToEmails,
                                "prefix", prefix
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENABLED"))
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private void setAliasStatus(String token, String aliasId, boolean enabled) throws Exception {
        String path = enabled ? "/enable" : "/disable";
        String expectedStatus = enabled ? "ENABLED" : "DISABLED";
        mockMvc.perform(post("/api/v1/pass/aliases/" + aliasId + path)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(expectedStatus));
    }

    private void assertAliasState(String token, String aliasId, String statusValue, List<String> routes) throws Exception {
        JsonNode alias = findByField(listData(token, "/api/v1/pass/aliases"), "id", aliasId);
        assertThat(alias).isNotNull();
        assertThat(alias.path("status").asText()).isEqualTo(statusValue);
        assertThat(alias.path("forwardToEmails")).hasSize(routes.size());
        for (int index = 0; index < routes.size(); index += 1) {
            assertThat(alias.path("forwardToEmails").get(index).asText()).isEqualTo(routes.get(index));
        }
    }

    private String createOrganization(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/orgs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"name\":\"" + name + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private void inviteMember(String token, String orgId, String email, String role) throws Exception {
        mockMvc.perform(post("/api/v1/orgs/" + orgId + "/invites")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "email", email,
                                "role", role
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private void acceptFirstIncomingInvite(String token) throws Exception {
        String inviteId = firstIncomingInviteId(token);
        mockMvc.perform(post("/api/v1/orgs/invites/" + inviteId + "/respond")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"response\":\"ACCEPT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    private String firstIncomingInviteId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/orgs/invites/incoming")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/0/inviteId").asText();
    }

    private void updatePassPolicy(String token, String orgId) throws Exception {
        mockMvc.perform(put("/api/v1/pass/orgs/" + orgId + "/policy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "minimumPasswordLength": 12,
                                  "maximumPasswordLength": 20,
                                  "requireUppercase": true,
                                  "requireDigits": true,
                                  "requireSymbols": true,
                                  "allowMemorablePasswords": false,
                                  "allowExternalSharing": true,
                                  "allowItemSharing": true,
                                  "allowSecureLinks": true,
                                  "allowMemberVaultCreation": true,
                                  "allowExport": false,
                                  "forceTwoFactor": true,
                                  "allowPasskeys": true,
                                  "allowAliases": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maximumPasswordLength").value(20));
    }

    private String createSharedVault(String token, String orgId, String name, String description) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/pass/orgs/" + orgId + "/shared-vaults")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "description": "%s"
                                }
                                """.formatted(name, description)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(name))
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private void addVaultMember(String token, String orgId, String vaultId, String email, String role) throws Exception {
        mockMvc.perform(post("/api/v1/pass/orgs/" + orgId + "/shared-vaults/" + vaultId + "/members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "role": "%s"
                                }
                                """.formatted(email, role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userEmail").value(email));
    }

    private String createSharedItem(String token, String orgId, String vaultId, String title, String itemType, String secret) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/pass/orgs/" + orgId + "/shared-vaults/" + vaultId + "/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "itemType": "%s",
                                  "website": "https://shared.example.com",
                                  "username": "shared@example.com",
                                  "secretCiphertext": "%s",
                                  "note": "release blocking shared item"
                                }
                                """.formatted(title, itemType, secret)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value(title))
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private String createItemShare(String token, String orgId, String itemId, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/pass/orgs/" + orgId + "/items/" + itemId + "/item-shares")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private String createSecureLink(String token, String orgId, String itemId, int maxViews) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/pass/orgs/" + orgId + "/items/" + itemId + "/secure-links")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "maxViews": %s
                                }
                                """.formatted(maxViews)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private String readSecureLinkToken(String token, String orgId, String itemId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/pass/orgs/" + orgId + "/items/" + itemId + "/secure-links")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/0/token").asText();
    }

    private JsonNode listData(String token, String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data");
    }

    private JsonNode findByField(JsonNode items, String field, String value) {
        for (JsonNode item : items) {
            if (value.equals(item.path(field).asText())) {
                return item;
            }
        }
        return null;
    }

    private String writeJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
