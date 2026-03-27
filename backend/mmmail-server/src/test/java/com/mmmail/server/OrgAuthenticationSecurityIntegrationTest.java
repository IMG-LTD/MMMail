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

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrgAuthenticationSecurityIntegrationTest {

    private static final String PASSWORD = "Password@123";
    private static final String AUTH_ISSUER = "MMMail Security";
    private static final String AUTH_SECRET = "JBSWY3DPEHPK3PXP";
    private static final String TWO_FACTOR_LEVEL_ALL = "ALL";
    private static final int GRACE_PERIOD_ONE_DAY = 1;
    private static final String SECURITY_KEYWORD = "Enable two-factor authentication";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void authenticationSecurityOverviewShouldAllowManagersAndExposeCoverage() throws Exception {
        OrgScenario scenario = createScenario("v99-overview");
        createAuthenticatorEntry(scenario.adminToken(), scenario.suffix() + "-admin");
        updatePolicy(scenario.ownerToken(), scenario.orgId(), TWO_FACTOR_LEVEL_ALL, GRACE_PERIOD_ONE_DAY)
                .andExpect(jsonPath("$.data.twoFactorGracePeriodDays").value(GRACE_PERIOD_ONE_DAY));

        JsonNode ownerOverview = getAuthenticationSecurity(scenario.ownerToken(), scenario.orgId(), null);
        JsonNode adminOverview = getAuthenticationSecurity(scenario.adminToken(), scenario.orgId(), "onlyWithoutTwoFactor=true");
        JsonNode memberRow = findMemberRow(ownerOverview.path("members"), scenario.memberEmail());

        assertThat(ownerOverview.path("totalActiveMembers").asInt()).isEqualTo(3);
        assertThat(ownerOverview.path("twoFactorGracePeriodDays").asInt()).isEqualTo(GRACE_PERIOD_ONE_DAY);
        assertThat(ownerOverview.path("protectedMembers").asInt()).isEqualTo(1);
        assertThat(ownerOverview.path("unprotectedMembers").asInt()).isEqualTo(2);
        assertThat(ownerOverview.path("protectedManagerSeats").asInt()).isEqualTo(1);
        assertThat(ownerOverview.path("unprotectedManagerSeats").asInt()).isEqualTo(1);
        assertThat(findMemberRow(ownerOverview.path("members"), scenario.adminEmail()).path("twoFactorEnabled").asBoolean()).isTrue();
        assertThat(memberRow.path("twoFactorEnabled").asBoolean()).isFalse();
        assertThat(memberRow.path("inGracePeriod").asBoolean()).isTrue();
        assertThat(memberRow.path("blockedByPolicy").asBoolean()).isFalse();
        assertThat(memberRow.path("gracePeriodEndsAt").asText()).isNotBlank();
        assertThat(adminOverview.path("members")).hasSize(2);

        mockMvc.perform(get("/api/v1/orgs/" + scenario.orgId() + "/admin-console/authentication-security")
                        .header("Authorization", "Bearer " + scenario.memberToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(30013));

        assertThat(listAuditTypes(scenario.ownerToken(), scenario.orgId()))
                .contains("ORG_AUTH_SECURITY_VIEW", "ORG_AUTH_2FA_REQUIREMENT_ENABLED", "ORG_AUTH_2FA_GRACE_PERIOD_CHANGED");
    }

    @Test
    void authenticationSecurityRemindersShouldDeliverInboxMail() throws Exception {
        OrgScenario scenario = createScenario("v99-reminder");
        createAuthenticatorEntry(scenario.adminToken(), scenario.suffix() + "-admin");
        updatePolicy(scenario.ownerToken(), scenario.orgId(), TWO_FACTOR_LEVEL_ALL, 3);

        MvcResult reminderResult = mockMvc.perform(post("/api/v1/orgs/" + scenario.orgId() + "/admin-console/authentication-security/reminders")
                        .header("Authorization", "Bearer " + scenario.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberIds": [%s, %s]
                                }
                                """.formatted(scenario.adminId(), scenario.memberId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestedCount").value(2))
                .andExpect(jsonPath("$.data.deliveredCount").value(1))
                .andExpect(jsonPath("$.data.skippedProtectedCount").value(1))
                .andReturn();

        JsonNode reminderData = readJson(reminderResult).path("data");
        assertThat(reminderData.path("deliveredMemberIds")).containsExactly(objectMapper.convertValue(scenario.memberId(), JsonNode.class));

        JsonNode inboxItems = listInboxItems(scenario.memberToken(), scenario.orgName());
        JsonNode reminderMail = findMailBySubject(inboxItems, scenario.orgName());
        assertThat(reminderMail.path("senderEmail").asText()).isEqualTo("security@mmmail.local");
        assertThat(reminderMail.path("subject").asText()).contains(SECURITY_KEYWORD);
        JsonNode reminderMailDetail = getMailDetail(scenario.memberToken(), reminderMail.path("id").asText());
        assertThat(reminderMailDetail.path("body").asText()).contains("Grace period: 3 day(s)");

        assertThat(listAuditTypes(scenario.ownerToken(), scenario.orgId())).contains("ORG_AUTH_2FA_REMINDER_SENT");
    }

    @Test
    void twoFactorEnforcementShouldHonorGracePeriodBeforeAuthenticatorConfigured() throws Exception {
        OrgScenario scenario = createScenario("v99-enforcement");

        updatePolicy(scenario.ownerToken(), scenario.orgId(), TWO_FACTOR_LEVEL_ALL, GRACE_PERIOD_ONE_DAY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.twoFactorEnforcementLevel").value(TWO_FACTOR_LEVEL_ALL))
                .andExpect(jsonPath("$.data.twoFactorGracePeriodDays").value(GRACE_PERIOD_ONE_DAY));

        mockMvc.perform(get("/api/v1/docs/notes")
                        .header("Authorization", "Bearer " + scenario.memberToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/docs/notes")
                        .header("Authorization", "Bearer " + scenario.memberToken())
                        .header("X-MMMAIL-ORG-ID", scenario.orgId()))
                .andExpect(status().isOk());

        JsonNode graceOverview = getAuthenticationSecurity(scenario.ownerToken(), scenario.orgId(), null);
        JsonNode graceMemberRow = findMemberRow(graceOverview.path("members"), scenario.memberEmail());
        assertThat(graceMemberRow.path("inGracePeriod").asBoolean()).isTrue();
        assertThat(graceMemberRow.path("blockedByPolicy").asBoolean()).isFalse();

        mockMvc.perform(put("/api/v1/orgs/" + scenario.orgId() + "/policy")
                        .header("Authorization", "Bearer " + scenario.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "twoFactorGracePeriodDays": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.twoFactorGracePeriodDays").value(0));

        mockMvc.perform(get("/api/v1/docs/notes")
                        .header("Authorization", "Bearer " + scenario.memberToken())
                        .header("X-MMMAIL-ORG-ID", scenario.orgId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(30046));

        createAuthenticatorEntry(scenario.memberToken(), scenario.suffix() + "-member");

        mockMvc.perform(get("/api/v1/docs/notes")
                        .header("Authorization", "Bearer " + scenario.memberToken())
                        .header("X-MMMAIL-ORG-ID", scenario.orgId()))
                .andExpect(status().isOk());

        JsonNode overview = getAuthenticationSecurity(scenario.ownerToken(), scenario.orgId(), null);
        assertThat(findMemberRow(overview.path("members"), scenario.memberEmail()).path("blockedByPolicy").asBoolean()).isFalse();
        assertThat(listAuditTypes(scenario.ownerToken(), scenario.orgId()))
                .contains(
                        "ORG_POLICY_UPDATE",
                        "ORG_AUTH_2FA_REQUIREMENT_ENABLED",
                        "ORG_AUTH_2FA_GRACE_PERIOD_CHANGED",
                        "ORG_AUTH_2FA_ENFORCEMENT_BLOCK"
                );
    }

    private OrgScenario createScenario(String prefix) throws Exception {
        String suffix = prefix + "-" + System.nanoTime();
        String ownerEmail = suffix + "-owner@mmmail.local";
        String adminEmail = suffix + "-admin@mmmail.local";
        String memberEmail = suffix + "-member@mmmail.local";
        String ownerToken = register(ownerEmail, "V100 Owner");
        String adminToken = register(adminEmail, "V100 Admin");
        String memberToken = register(memberEmail, "V100 Member");
        String orgName = "V100 Org " + suffix;
        String orgId = createOrganization(ownerToken, orgName);
        inviteMember(ownerToken, orgId, adminEmail, "ADMIN");
        inviteMember(ownerToken, orgId, memberEmail, "MEMBER");
        acceptFirstIncomingInvite(adminToken);
        acceptFirstIncomingInvite(memberToken);
        String adminId = findMemberId(ownerToken, orgId, adminEmail);
        String memberId = findMemberId(ownerToken, orgId, memberEmail);
        return new OrgScenario(suffix, orgId, orgName, ownerToken, adminToken, memberToken, ownerEmail, adminEmail, memberEmail, adminId, memberId);
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

    private String findMemberId(String token, String orgId, String email) throws Exception {
        JsonNode rows = readJson(mockMvc.perform(get("/api/v1/orgs/" + orgId + "/members")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()).at("/data");
        return findMemberRow(rows, email).path("id").asText();
    }

    private void createAuthenticatorEntry(String token, String accountName) throws Exception {
        mockMvc.perform(post("/api/v1/authenticator/entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "issuer": "%s",
                                  "accountName": "%s",
                                  "secretCiphertext": "%s",
                                  "algorithm": "SHA1",
                                  "digits": 6,
                                  "periodSeconds": 30
                                }
                                """.formatted(AUTH_ISSUER, accountName, AUTH_SECRET)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private org.springframework.test.web.servlet.ResultActions updatePolicy(
            String token,
            String orgId,
            String enforcementLevel,
            int gracePeriodDays
    ) throws Exception {
        return mockMvc.perform(put("/api/v1/orgs/" + orgId + "/policy")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "twoFactorEnforcementLevel": "%s",
                          "twoFactorGracePeriodDays": %s
                        }
                        """.formatted(enforcementLevel, gracePeriodDays)));
    }

    private JsonNode getAuthenticationSecurity(String token, String orgId, String queryString) throws Exception {
        String path = "/api/v1/orgs/" + orgId + "/admin-console/authentication-security";
        if (queryString != null && !queryString.isBlank()) {
            path += "?" + queryString;
        }
        MvcResult result = mockMvc.perform(get(path)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    private JsonNode listInboxItems(String token, String keyword) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/mails/inbox")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", keyword))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/items");
    }

    private JsonNode getMailDetail(String token, String mailId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/mails/" + mailId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    private Set<String> listAuditTypes(String token, String orgId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/orgs/" + orgId + "/audit/events")
                        .param("limit", "100")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        Set<String> eventTypes = new HashSet<>();
        for (JsonNode item : readJson(result).path("data")) {
            eventTypes.add(item.path("eventType").asText());
        }
        return eventTypes;
    }

    private JsonNode findMemberRow(JsonNode rows, String email) {
        for (JsonNode row : rows) {
            if (email.equals(row.path("memberEmail").asText()) || email.equals(row.path("userEmail").asText())) {
                return row;
            }
        }
        throw new IllegalStateException("member row not found: " + email);
    }

    private JsonNode findMailBySubject(JsonNode items, String orgName) {
        for (JsonNode item : items) {
            if (item.path("subject").asText().contains(orgName)) {
                return item;
            }
        }
        throw new IllegalStateException("security reminder mail not found: " + orgName);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private record OrgScenario(
            String suffix,
            String orgId,
            String orgName,
            String ownerToken,
            String adminToken,
            String memberToken,
            String ownerEmail,
            String adminEmail,
            String memberEmail,
            String adminId,
            String memberId
    ) {
    }
}
