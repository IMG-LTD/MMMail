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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PassBusinessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sharedVaultPolicySecureLinkAndDirectItemShareFlowShouldWork() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerEmail = "v101-pass-owner-%s@mmmail.local".formatted(suffix);
        String memberEmail = "v101-pass-member-%s@mmmail.local".formatted(suffix);
        String auditorEmail = "v101-pass-auditor-%s@mmmail.local".formatted(suffix);

        String ownerToken = register(ownerEmail, "Password@123", "V64 Pass Owner");
        String memberToken = register(memberEmail, "Password@123", "V64 Pass Member");
        String auditorToken = register(auditorEmail, "Password@123", "V64 Pass Auditor");
        String orgId = createOrganization(ownerToken, "V101 Pass Business");

        inviteMember(ownerToken, orgId, memberEmail, "MEMBER");
        inviteMember(ownerToken, orgId, auditorEmail, "MEMBER");
        acceptFirstIncomingInvite(memberToken);
        acceptFirstIncomingInvite(auditorToken);
        updatePassPolicy(ownerToken, orgId, new PassPolicyInput(12, 20, false, true));

        String primaryVaultId = createSharedVault(ownerToken, orgId, "Operations Vault", "Production accounts");
        addVaultMember(ownerToken, orgId, primaryVaultId, memberEmail, "MEMBER");
        String memberVaultId = createSharedVault(memberToken, orgId, "Member Vault", "Member-created vault");
        assertThat(memberVaultId).isNotBlank();

        String itemId = createSharedItem(ownerToken, orgId, primaryVaultId, "PagerDuty Root", "LOGIN", "Team#123456A");
        String secureLinkId = createSecureLink(ownerToken, orgId, itemId);
        String publicToken = readSecureLinkToken(ownerToken, orgId, itemId);
        String itemShareId = createItemShare(ownerToken, orgId, itemId, auditorEmail);

        updateSharedItem(ownerToken, orgId, primaryVaultId, itemId, "PagerDuty Root", "LOGIN", "Team#123456A", "Escalation owner");

        mockMvc.perform(get("/api/v1/pass/orgs/" + orgId + "/overview")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orgId").value(orgId))
                .andExpect(jsonPath("$.data.sharedVaultCount").value(2))
                .andExpect(jsonPath("$.data.memberCount").value(3));

        mockMvc.perform(get("/api/v1/pass/orgs/" + orgId + "/shared-vaults")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/v1/pass/orgs/" + orgId + "/shared-vaults/" + primaryVaultId + "/items")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(itemId))
                .andExpect(jsonPath("$.data[0].itemType").value("LOGIN"));

        mockMvc.perform(get("/api/v1/pass/orgs/" + orgId + "/items/" + itemId + "/item-shares")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(itemShareId))
                .andExpect(jsonPath("$.data[0].collaboratorEmail").value(auditorEmail));

        mockMvc.perform(get("/api/v1/pass/orgs/" + orgId + "/incoming-item-shares")
                        .header("Authorization", "Bearer " + auditorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].itemId").value(itemId))
                .andExpect(jsonPath("$.data[0].sourceVaultName").value("Operations Vault"))
                .andExpect(jsonPath("$.data[0].readOnly").value(true));

        mockMvc.perform(get("/api/v1/pass/orgs/" + orgId + "/incoming-item-shares/" + itemId)
                        .header("Authorization", "Bearer " + auditorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemId").value(itemId))
                .andExpect(jsonPath("$.data.note").value("Escalation owner"))
                .andExpect(jsonPath("$.data.readOnly").value(true));

        mockMvc.perform(get("/api/v1/pass/orgs/" + orgId + "/activity")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].eventType").value(startsWith("PASS_")));

        mockMvc.perform(get("/api/v1/public/pass/secure-links/" + publicToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemId").value(itemId))
                .andExpect(jsonPath("$.data.title").value("PagerDuty Root"));

        mockMvc.perform(get("/api/v1/public/pass/secure-links/" + publicToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/pass/orgs/" + orgId + "/items/" + itemId + "/secure-links")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(secureLinkId));
    }

    @Test
    void orgPolicyShouldRestrictGeneratorAndDirectItemSharing() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerEmail = "v101-pass-owner-policy-%s@mmmail.local".formatted(suffix);
        String memberEmail = "v101-pass-member-policy-%s@mmmail.local".formatted(suffix);

        String ownerToken = register(ownerEmail, "Password@123", "V101 Policy Owner");
        String memberToken = register(memberEmail, "Password@123", "V101 Policy Member");
        String orgId = createOrganization(ownerToken, "V101 Pass Policy");

        inviteMember(ownerToken, orgId, memberEmail, "MEMBER");
        acceptFirstIncomingInvite(memberToken);
        updatePassPolicy(ownerToken, orgId, new PassPolicyInput(12, 18, false, false));

        String vaultId = createSharedVault(ownerToken, orgId, "Security Vault", "Policy governed");
        String itemId = createSharedItem(ownerToken, orgId, vaultId, "Root Secret", "LOGIN", "Team#123456A");

        mockMvc.perform(post("/api/v1/pass/password/generate")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orgId": %s,
                                  "length": 20
                                }
                                """.formatted(orgId)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/pass/password/generate")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orgId": %s,
                                  "length": 16,
                                  "memorable": true
                                }
                                """.formatted(orgId)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/pass/orgs/" + orgId + "/items/" + itemId + "/item-shares")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s"
                                }
                                """.formatted(memberEmail)))
                .andExpect(status().isForbidden());
    }

    @Test
    void revokedSecureLinkShouldRejectPublicAccess() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerEmail = "v107-pass-owner-%s@mmmail.local".formatted(suffix);
        String ownerToken = register(ownerEmail, "Password@123", "V107 Pass Owner");
        String orgId = createOrganization(ownerToken, "V107 Pass Secure Link");
        updatePassPolicy(ownerToken, orgId, new PassPolicyInput(12, 20, false, true));
        String vaultId = createSharedVault(ownerToken, orgId, "Secure Links", "Public credential handoff");
        String itemId = createSharedItem(ownerToken, orgId, vaultId, "VPN Gateway", "LOGIN", "Team#654321AB");
        String linkId = createSecureLink(ownerToken, orgId, itemId, 3);
        String publicToken = readSecureLinkToken(ownerToken, orgId, itemId);

        mockMvc.perform(get("/api/v1/public/pass/secure-links/" + publicToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("VPN Gateway"))
                .andExpect(jsonPath("$.data.remainingViews").value(2));

        mockMvc.perform(delete("/api/v1/pass/orgs/" + orgId + "/secure-links/" + linkId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revokedAt").exists())
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(get("/api/v1/public/pass/secure-links/" + publicToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Pass secure link has been revoked"));
    }

    @Test
    void secureLinksShouldDefaultToSevenDaysExposeDashboardAndRejectExpiryOverThirtyDays() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerEmail = "v108-pass-owner-%s@mmmail.local".formatted(suffix);
        String ownerToken = register(ownerEmail, "Password@123", "V108 Pass Owner");
        String orgId = createOrganization(ownerToken, "V108 Secure Link Dashboard");
        updatePassPolicy(ownerToken, orgId, new PassPolicyInput(12, 20, false, true));
        String vaultId = createSharedVault(ownerToken, orgId, "Ops Vault", "Secure links dashboard");
        String itemId = createSharedItem(ownerToken, orgId, vaultId, "Root Console", "LOGIN", "Team#987654AB");
        String secondItemId = createSharedItem(ownerToken, orgId, vaultId, "DB Console", "LOGIN", "Team#123456CD");

        String linkId = createSecureLink(ownerToken, orgId, itemId, 4);
        createSecureLink(ownerToken, orgId, secondItemId, 2);

        MvcResult defaultList = mockMvc.perform(get("/api/v1/pass/orgs/" + orgId + "/items/" + itemId + "/secure-links")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(linkId))
                .andReturn();
        JsonNode defaultLink = readJson(defaultList).at("/data/0");
        assertThat(defaultLink.at("/expiresAt").asText()).isNotBlank();
        LocalDateTime expiresAt = LocalDateTime.parse(defaultLink.at("/expiresAt").asText());
        LocalDateTime now = LocalDateTime.now();
        assertThat(expiresAt).isAfter(now.plusDays(6));
        assertThat(expiresAt).isBefore(now.plusDays(8));

        mockMvc.perform(get("/api/v1/pass/orgs/" + orgId + "/secure-links")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].sharedVaultName").value("Ops Vault"))
                .andExpect(jsonPath("$.data[0].itemTitle").exists())
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));

        String invalidExpiry = LocalDateTime.now().plusDays(31).withNano(0).toString();
        mockMvc.perform(post("/api/v1/pass/orgs/" + orgId + "/items/" + itemId + "/secure-links")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "maxViews": 4,
                                  "expiresAt": "%s"
                                }
                                """.formatted(invalidExpiry)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("expiresAt must be within 30 days"));
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

    private void updatePassPolicy(String token, String orgId, PassPolicyInput input) throws Exception {
        mockMvc.perform(put("/api/v1/pass/orgs/" + orgId + "/policy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "minimumPasswordLength": %s,
                                  "maximumPasswordLength": %s,
                                  "requireUppercase": true,
                                  "requireDigits": true,
                                  "requireSymbols": true,
                                  "allowMemorablePasswords": %s,
                                  "allowExternalSharing": true,
                                  "allowItemSharing": %s,
                                  "allowSecureLinks": true,
                                  "allowMemberVaultCreation": true,
                                  "allowExport": false,
                                  "forceTwoFactor": true,
                                  "allowPasskeys": true,
                                  "allowAliases": true
                                }
                                """.formatted(
                                        input.minimumPasswordLength(),
                                        input.maximumPasswordLength(),
                                        input.allowMemorablePasswords(),
                                        input.allowItemSharing()
                                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowMemberVaultCreation").value(true))
                .andExpect(jsonPath("$.data.maximumPasswordLength").value(input.maximumPasswordLength()))
                .andExpect(jsonPath("$.data.allowItemSharing").value(input.allowItemSharing()));
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
                                  "website": "https://pagerduty.example.com",
                                  "username": "ops@example.com",
                                  "secretCiphertext": "%s",
                                  "note": "Root responder account"
                                }
                                """.formatted(title, itemType, secret)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value(title))
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private String createSecureLink(String token, String orgId, String itemId) throws Exception {
        return createSecureLink(token, orgId, itemId, 1);
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
                .andExpect(jsonPath("$.data.id").exists())
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
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private void updateSharedItem(
            String token,
            String orgId,
            String vaultId,
            String itemId,
            String title,
            String itemType,
            String secret,
            String note
    ) throws Exception {
        mockMvc.perform(put("/api/v1/pass/orgs/" + orgId + "/shared-vaults/" + vaultId + "/items/" + itemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "itemType": "%s",
                                  "website": "https://pagerduty.example.com",
                                  "username": "ops@example.com",
                                  "secretCiphertext": "%s",
                                  "note": "%s"
                                }
                                """.formatted(title, itemType, secret, note)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.note").value(note));
    }

    private String readSecureLinkToken(String token, String orgId, String itemId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/pass/orgs/" + orgId + "/items/" + itemId + "/secure-links")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].token").exists())
                .andReturn();
        return readJson(result).at("/data/0/token").asText();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private record PassPolicyInput(
            int minimumPasswordLength,
            int maximumPasswordLength,
            boolean allowMemorablePasswords,
            boolean allowItemSharing
    ) {
    }
}
