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

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrgMailIdentityIntegrationTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void mailIdentityLifecycleShouldSupportGovernanceAndSenderDiscovery() throws Exception {
        OrgScenario scenario = createScenario("v67-identity");
        String domain = "mail-" + scenario.suffix() + ".example.com";
        String domainId = createDomain(scenario.ownerToken(), scenario.orgId(), domain);
        verifyDomain(scenario.adminToken(), scenario.orgId(), domainId);

        String primaryIdentityId = createMailIdentity(
                scenario.ownerToken(),
                scenario.orgId(),
                scenario.memberId(),
                domainId,
                "member",
                "Member Sender"
        );

        mockMvc.perform(post("/api/v1/orgs/" + scenario.orgId() + "/mail-identities")
                        .header("Authorization", "Bearer " + scenario.memberToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mailIdentityPayload(scenario.memberId(), domainId, "blocked", "Blocked")))
                .andExpect(status().isForbidden());

        String defaultIdentityId = createMailIdentity(
                scenario.adminToken(),
                scenario.orgId(),
                scenario.memberId(),
                domainId,
                "ops",
                "Ops Sender"
        );
        setDefaultMailIdentity(scenario.ownerToken(), scenario.orgId(), defaultIdentityId);
        disableMailIdentity(scenario.adminToken(), scenario.orgId(), defaultIdentityId);
        enableMailIdentity(scenario.ownerToken(), scenario.orgId(), defaultIdentityId);
        setDefaultMailIdentity(scenario.ownerToken(), scenario.orgId(), defaultIdentityId);
        removeMailIdentity(scenario.ownerToken(), scenario.orgId(), primaryIdentityId);

        MvcResult orgListResult = mockMvc.perform(get("/api/v1/orgs/" + scenario.orgId() + "/mail-identities")
                        .header("Authorization", "Bearer " + scenario.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].emailAddress").value("ops@" + domain))
                .andExpect(jsonPath("$.data[0].defaultIdentity").value(true))
                .andReturn();
        JsonNode orgIdentities = readJson(orgListResult).at("/data");
        assertThat(orgIdentities.get(0).path("status").asText()).isEqualTo("ENABLED");

        mockMvc.perform(get("/api/v1/orgs/" + scenario.orgId() + "/mail-identities")
                        .header("Authorization", "Bearer " + scenario.memberToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].memberEmail").value(scenario.memberEmail()));

        mockMvc.perform(get("/api/v1/mails/identities")
                        .header("Authorization", "Bearer " + scenario.memberToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].emailAddress").value("ops@" + domain))
                .andExpect(jsonPath("$.data[0].defaultIdentity").value(true));

        assertThat(listAuditTypes(scenario.ownerToken(), scenario.orgId()))
                .contains(
                        "ORG_MAIL_IDENTITY_CREATE",
                        "ORG_MAIL_IDENTITY_SET_DEFAULT",
                        "ORG_MAIL_IDENTITY_DISABLE",
                        "ORG_MAIL_IDENTITY_ENABLE",
                        "ORG_MAIL_IDENTITY_DELETE"
                );
    }

    @Test
    void composeShouldSendAndSaveDraftWithSelectedIdentity() throws Exception {
        OrgScenario scenario = createScenario("v67-compose");
        String recipientEmail = "external-" + scenario.suffix() + "@mmmail.local";
        String recipientToken = register(recipientEmail, "External Receiver");
        setUndoSendSeconds(scenario.memberToken(), "V67 Member", 0);

        String domain = "compose-" + scenario.suffix() + ".example.com";
        String domainId = createDomain(scenario.ownerToken(), scenario.orgId(), domain);
        verifyDomain(scenario.ownerToken(), scenario.orgId(), domainId);
        createMailIdentity(
                scenario.ownerToken(),
                scenario.orgId(),
                scenario.memberId(),
                domainId,
                "member",
                "Compose Sender"
        );

        mockMvc.perform(post("/api/v1/mails/send")
                        .header("Authorization", "Bearer " + scenario.memberToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sendPayload(recipientEmail, "member@" + domain, "Identity Mail", "sent via custom identity", "idemp-v67-send-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        long inboxMailId = latestInboxMailId(recipientToken);
        mockMvc.perform(get("/api/v1/mails/" + inboxMailId)
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.peerEmail").value("member@" + domain))
                .andExpect(jsonPath("$.data.senderEmail").value("member@" + domain));

        MvcResult draftResult = mockMvc.perform(post("/api/v1/mails/drafts")
                        .header("Authorization", "Bearer " + scenario.memberToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftPayload(recipientEmail, "member@" + domain, "Draft Identity", "draft body")))
                .andExpect(status().isOk())
                .andReturn();
        String draftId = readJson(draftResult).at("/data/draftId").asText();

        mockMvc.perform(get("/api/v1/mails/" + draftId)
                        .header("Authorization", "Bearer " + scenario.memberToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.senderEmail").value("member@" + domain))
                .andExpect(jsonPath("$.data.peerEmail").value(recipientEmail));

        String disabledIdentityId = createMailIdentity(
                scenario.ownerToken(),
                scenario.orgId(),
                scenario.memberId(),
                domainId,
                "disabled",
                "Disabled Sender"
        );
        disableMailIdentity(scenario.ownerToken(), scenario.orgId(), disabledIdentityId);

        mockMvc.perform(post("/api/v1/mails/send")
                        .header("Authorization", "Bearer " + scenario.memberToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sendPayload(recipientEmail, "disabled@" + domain, "Should fail", "disabled identity", "idemp-v67-send-2")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Sender identity is unavailable"));
    }

    private OrgScenario createScenario(String prefix) throws Exception {
        String suffix = prefix + "-" + System.nanoTime();
        String ownerEmail = suffix + "-owner@mmmail.local";
        String adminEmail = suffix + "-admin@mmmail.local";
        String memberEmail = suffix + "-member@mmmail.local";
        String ownerToken = register(ownerEmail, "V67 Owner");
        String adminToken = register(adminEmail, "V67 Admin");
        String memberToken = register(memberEmail, "V67 Member");
        String orgId = createOrganization(ownerToken, "V67 Org " + suffix);
        inviteMember(ownerToken, orgId, adminEmail, "ADMIN");
        inviteMember(ownerToken, orgId, memberEmail, "MEMBER");
        acceptFirstIncomingInvite(adminToken);
        acceptFirstIncomingInvite(memberToken);
        String memberId = findMemberId(ownerToken, orgId, memberEmail);
        return new OrgScenario(suffix, orgId, ownerToken, adminToken, memberToken, memberEmail, memberId);
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

    private String createOrganization(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/orgs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isOk())
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
                .andExpect(status().isOk());
    }

    private void acceptFirstIncomingInvite(String token) throws Exception {
        String inviteId = firstIncomingInviteId(token);
        mockMvc.perform(post("/api/v1/orgs/invites/" + inviteId + "/respond")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"response\":\"ACCEPT\"}"))
                .andExpect(status().isOk());
    }

    private String firstIncomingInviteId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/orgs/invites/incoming")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/0/inviteId").asText();
    }

    private String findMemberId(String token, String orgId, String email) throws Exception {
        JsonNode rows = readJson(mockMvc.perform(get("/api/v1/orgs/" + orgId + "/members")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()).at("/data");
        for (JsonNode row : rows) {
            if (email.equals(row.path("userEmail").asText())) {
                return row.path("id").asText();
            }
        }
        throw new IllegalStateException("member row not found: " + email);
    }

    private String createDomain(String token, String orgId, String domain) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/orgs/" + orgId + "/domains")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"domain\":\"" + domain + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private void verifyDomain(String token, String orgId, String domainId) throws Exception {
        mockMvc.perform(post("/api/v1/orgs/" + orgId + "/domains/" + domainId + "/verify")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"));
    }

    private String createMailIdentity(
            String token,
            String orgId,
            String memberId,
            String domainId,
            String localPart,
            String displayName
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/orgs/" + orgId + "/mail-identities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mailIdentityPayload(memberId, domainId, localPart, displayName)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private void setDefaultMailIdentity(String token, String orgId, String identityId) throws Exception {
        mockMvc.perform(post("/api/v1/orgs/" + orgId + "/mail-identities/" + identityId + "/default")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultIdentity").value(true));
    }

    private void disableMailIdentity(String token, String orgId, String identityId) throws Exception {
        mockMvc.perform(post("/api/v1/orgs/" + orgId + "/mail-identities/" + identityId + "/disable")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));
    }

    private void enableMailIdentity(String token, String orgId, String identityId) throws Exception {
        mockMvc.perform(post("/api/v1/orgs/" + orgId + "/mail-identities/" + identityId + "/enable")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENABLED"));
    }

    private void removeMailIdentity(String token, String orgId, String identityId) throws Exception {
        mockMvc.perform(delete("/api/v1/orgs/" + orgId + "/mail-identities/" + identityId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private long latestInboxMailId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/mails/inbox")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/items/0/id").asLong();
    }

    private Set<String> listAuditTypes(String token, String orgId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/orgs/" + orgId + "/audit/events")
                        .param("limit", "100")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        Set<String> types = new HashSet<>();
        for (JsonNode item : readJson(result).at("/data")) {
            types.add(item.path("eventType").asText());
        }
        return types;
    }

    private void setUndoSendSeconds(String token, String displayName, int undoSeconds) throws Exception {
        String payload = """
                {
                  "displayName": "%s",
                  "signature": "",
                  "timezone": "UTC",
                  "autoSaveSeconds": 15,
                  "undoSendSeconds": %d,
                  "driveVersionRetentionCount": 50,
                  "driveVersionRetentionDays": 365
                }
                """.formatted(displayName, undoSeconds);
        mockMvc.perform(put("/api/v1/settings/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    private String mailIdentityPayload(String memberId, String domainId, String localPart, String displayName) {
        return """
                {
                  "memberId": %s,
                  "customDomainId": %s,
                  "localPart": "%s",
                  "displayName": "%s"
                }
                """.formatted(memberId, domainId, localPart, displayName);
    }

    private String sendPayload(String toEmail, String fromEmail, String subject, String body, String idempotencyKey) {
        return """
                {
                  "toEmail": "%s",
                  "fromEmail": "%s",
                  "subject": "%s",
                  "body": "%s",
                  "idempotencyKey": "%s",
                  "labels": []
                }
                """.formatted(toEmail, fromEmail, subject, body, idempotencyKey);
    }

    private String draftPayload(String toEmail, String fromEmail, String subject, String body) {
        return """
                {
                  "toEmail": "%s",
                  "fromEmail": "%s",
                  "subject": "%s",
                  "body": "%s"
                }
                """.formatted(toEmail, fromEmail, subject, body);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private record OrgScenario(
            String suffix,
            String orgId,
            String ownerToken,
            String adminToken,
            String memberToken,
            String memberEmail,
            String memberId
    ) {
    }
}
