package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PassMonitorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void personalMonitorShouldReportWeakReusedAndExcludedItems() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String userToken = register("v106-personal-" + suffix + "@mmmail.local", "Password@123", "V106 Personal");

        String weakItemId = createPersonalItem(userToken, "Legacy VPN", "weakpass");
        createPersonalItem(userToken, "Shared CRM", "Strong#12345A");
        createPersonalItem(userToken, "Finance CRM", "Strong#12345A");
        String excludedItemId = createPersonalItem(userToken, "Recovery Portal", "Unique#12345A");

        mockMvc.perform(post("/api/v1/pass/items/" + excludedItemId + "/monitor/exclude")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/pass/monitor")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scopeType").value("PERSONAL"))
                .andExpect(jsonPath("$.data.totalItemCount").value(4))
                .andExpect(jsonPath("$.data.trackedItemCount").value(3))
                .andExpect(jsonPath("$.data.weakPasswordCount").value(1))
                .andExpect(jsonPath("$.data.reusedPasswordCount").value(2))
                .andExpect(jsonPath("$.data.inactiveTwoFactorCount").value(3))
                .andExpect(jsonPath("$.data.excludedItemCount").value(1))
                .andExpect(jsonPath("$.data.weakPasswords[0].id").value(weakItemId))
                .andExpect(jsonPath("$.data.reusedPasswords.length()").value(2))
                .andExpect(jsonPath("$.data.inactiveTwoFactorItems.length()").value(3))
                .andExpect(jsonPath("$.data.inactiveTwoFactorItems[0].canManageTwoFactor").value(true))
                .andExpect(jsonPath("$.data.excludedItems[0].id").value(excludedItemId))
                .andExpect(jsonPath("$.data.excludedItems[0].canToggleExclusion").value(true));

        upsertPersonalTwoFactor(userToken, weakItemId, "Legacy VPN", "legacy-vpn");

        MvcResult codeResult = mockMvc.perform(post("/api/v1/pass/items/" + weakItemId + "/two-factor/code")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.digits").value(6))
                .andExpect(jsonPath("$.data.periodSeconds").value(30))
                .andReturn();
        assertThat(readJson(codeResult).at("/data/code").asText()).hasSize(6);

        mockMvc.perform(get("/api/v1/pass/monitor")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inactiveTwoFactorCount").value(2))
                .andExpect(jsonPath("$.data.weakPasswords[0].twoFactor.enabled").value(true))
                .andExpect(jsonPath("$.data.weakPasswords[0].inactiveTwoFactor").value(false));

        mockMvc.perform(delete("/api/v1/pass/items/" + excludedItemId + "/monitor/exclude")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/pass/monitor")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trackedItemCount").value(4))
                .andExpect(jsonPath("$.data.excludedItemCount").value(0))
                .andExpect(jsonPath("$.data.inactiveTwoFactorCount").value(3));
    }

    @Test
    void sharedMonitorShouldRespectAccessAndManagerExclusionControls() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerEmail = "v106-owner-" + suffix + "@mmmail.local";
        String memberEmail = "v106-member-" + suffix + "@mmmail.local";
        String ownerToken = register(ownerEmail, "Password@123", "V106 Owner");
        String memberToken = register(memberEmail, "Password@123", "V106 Member");
        String orgId = createOrganization(ownerToken, "V106 Shared Monitor");

        inviteMember(ownerToken, orgId, memberEmail, "MEMBER");
        acceptFirstIncomingInvite(memberToken);
        updatePassPolicy(ownerToken, orgId);
        String vaultId = createSharedVault(ownerToken, orgId, "Ops Vault", "Shared monitor coverage");
        addVaultMember(ownerToken, orgId, vaultId, memberEmail, "MEMBER");

        String weakItemId = createSharedItem(ownerToken, orgId, vaultId, "Legacy Gateway", "weakpass");
        createSharedItem(ownerToken, orgId, vaultId, "CRM Primary", "Strong#12345A");
        createSharedItem(ownerToken, orgId, vaultId, "CRM Backup", "Strong#12345A");

        mockMvc.perform(get("/api/v1/pass/orgs/" + orgId + "/monitor")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scopeType").value("SHARED"))
                .andExpect(jsonPath("$.data.currentRole").value("MEMBER"))
                .andExpect(jsonPath("$.data.totalItemCount").value(3))
                .andExpect(jsonPath("$.data.weakPasswordCount").value(1))
                .andExpect(jsonPath("$.data.reusedPasswordCount").value(2))
                .andExpect(jsonPath("$.data.inactiveTwoFactorCount").value(3))
                .andExpect(jsonPath("$.data.weakPasswords[0].canToggleExclusion").value(false))
                .andExpect(jsonPath("$.data.weakPasswords[0].canManageTwoFactor").value(false));

        mockMvc.perform(post("/api/v1/pass/orgs/" + orgId + "/items/" + weakItemId + "/monitor/exclude")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/pass/orgs/" + orgId + "/items/" + weakItemId + "/two-factor")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(twoFactorPayload("Shared Gateway", "gateway-team")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/pass/orgs/" + orgId + "/items/" + weakItemId + "/two-factor")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(twoFactorPayload("Shared Gateway", "gateway-team")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.twoFactor.enabled").value(true));

        MvcResult ownerCodeResult = mockMvc.perform(post("/api/v1/pass/orgs/" + orgId + "/items/" + weakItemId + "/two-factor/code")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.digits").value(6))
                .andReturn();
        assertThat(readJson(ownerCodeResult).at("/data/code").asText()).hasSize(6);

        mockMvc.perform(post("/api/v1/pass/orgs/" + orgId + "/items/" + weakItemId + "/monitor/exclude")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/pass/orgs/" + orgId + "/monitor")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentRole").value("OWNER"))
                .andExpect(jsonPath("$.data.weakPasswordCount").value(0))
                .andExpect(jsonPath("$.data.reusedPasswordCount").value(2))
                .andExpect(jsonPath("$.data.inactiveTwoFactorCount").value(2))
                .andExpect(jsonPath("$.data.excludedItemCount").value(1))
                .andExpect(jsonPath("$.data.excludedItems[0].id").value(weakItemId))
                .andExpect(jsonPath("$.data.excludedItems[0].canToggleExclusion").value(true))
                .andExpect(jsonPath("$.data.excludedItems[0].twoFactor.enabled").value(true));
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
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andReturn();
        return readJson(result).at("/data/accessToken").asText();
    }

    private String createPersonalItem(String token, String title, String secret) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/pass/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "itemType": "LOGIN",
                                  "username": "%s",
                                  "secretCiphertext": "%s"
                                }
                                """.formatted(title, title.toLowerCase().replace(" ", "-"), secret)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private String createOrganization(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/orgs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private void inviteMember(String token, String orgId, String email, String role) throws Exception {
        mockMvc.perform(post("/api/v1/orgs/" + orgId + "/invites")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "role": "%s"
                                }
                                """.formatted(email, role)))
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
                .andExpect(jsonPath("$.data[0].inviteId").exists())
                .andReturn();
        return readJson(result).at("/data/0/inviteId").asText();
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
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private void updatePassPolicy(String token, String orgId) throws Exception {
        mockMvc.perform(put("/api/v1/pass/orgs/" + orgId + "/policy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "minimumPasswordLength": 8,
                                  "maximumPasswordLength": 64,
                                  "requireUppercase": false,
                                  "requireDigits": false,
                                  "requireSymbols": false,
                                  "allowMemorablePasswords": true,
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
                .andExpect(jsonPath("$.data.minimumPasswordLength").value(8));
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

    private String createSharedItem(String token, String orgId, String vaultId, String title, String secret) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/pass/orgs/" + orgId + "/shared-vaults/" + vaultId + "/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "itemType": "LOGIN",
                                  "username": "%s",
                                  "secretCiphertext": "%s"
                                }
                                """.formatted(title, title.toLowerCase().replace(" ", "-"), secret)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private void upsertPersonalTwoFactor(String token, String itemId, String issuer, String accountName) throws Exception {
        mockMvc.perform(put("/api/v1/pass/items/" + itemId + "/two-factor")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(twoFactorPayload(issuer, accountName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.twoFactor.enabled").value(true));
    }

    private String twoFactorPayload(String issuer, String accountName) {
        return """
                {
                  "issuer": "%s",
                  "accountName": "%s",
                  "secretCiphertext": "JBSWY3DPEHPK3PXP",
                  "algorithm": "SHA1",
                  "digits": 6,
                  "periodSeconds": 30
                }
                """.formatted(issuer, accountName);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(root.path("code").asInt()).isZero();
        return root;
    }
}
