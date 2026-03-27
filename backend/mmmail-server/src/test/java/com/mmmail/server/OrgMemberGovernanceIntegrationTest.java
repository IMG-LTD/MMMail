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

import java.util.LinkedHashSet;
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
class OrgMemberGovernanceIntegrationTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void ownerShouldDisableAndRestoreMember() throws Exception {
        OrgScenario scenario = createScenario("status-owner");

        updateMemberStatus(scenario.ownerToken(), scenario.orgId(), scenario.memberId(), "DISABLED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        mockMvc.perform(get("/api/v1/orgs/" + scenario.orgId() + "/members")
                        .header("Authorization", "Bearer " + scenario.memberToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(30013));

        updateMemberStatus(scenario.ownerToken(), scenario.orgId(), scenario.memberId(), "ACTIVE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/orgs/" + scenario.orgId() + "/members")
                        .header("Authorization", "Bearer " + scenario.memberToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        assertThat(listAuditTypes(scenario.ownerToken(), scenario.orgId()))
                .contains("ORG_MEMBER_STATUS_UPDATE");
    }

    @Test
    void adminShouldOnlyDisableMembers() throws Exception {
        OrgScenario scenario = createScenario("status-admin");

        updateMemberStatus(scenario.adminToken(), scenario.orgId(), scenario.memberId(), "DISABLED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        updateMemberStatus(scenario.adminToken(), scenario.orgId(), scenario.memberId(), "ACTIVE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        updateMemberStatus(scenario.adminToken(), scenario.orgId(), scenario.adminId(), "DISABLED")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001));

        updateMemberStatus(scenario.adminToken(), scenario.orgId(), scenario.ownerId(), "DISABLED")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001));
    }

    @Test
    void crossOrganizationStatusMutationShouldBeRejected() throws Exception {
        OrgScenario source = createScenario("status-source");
        OrgScenario target = createScenario("status-target");

        updateMemberStatus(source.ownerToken(), target.orgId(), target.memberId(), "DISABLED")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(30013));
    }

    private org.springframework.test.web.servlet.ResultActions updateMemberStatus(
            String token,
            String orgId,
            String memberId,
            String status
    ) throws Exception {
        return mockMvc.perform(put("/api/v1/orgs/" + orgId + "/members/" + memberId + "/status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + status + "\"}"));
    }

    private OrgScenario createScenario(String prefix) throws Exception {
        String suffix = prefix + "-" + System.nanoTime();
        String ownerEmail = suffix + "-owner@mmmail.local";
        String adminEmail = suffix + "-admin@mmmail.local";
        String memberEmail = suffix + "-member@mmmail.local";
        String ownerToken = register(ownerEmail, "Status Owner");
        String adminToken = register(adminEmail, "Status Admin");
        String memberToken = register(memberEmail, "Status Member");
        String orgId = createOrganization(ownerToken, "Status Org " + suffix);
        inviteMember(ownerToken, orgId, adminEmail, "ADMIN");
        inviteMember(ownerToken, orgId, memberEmail, "MEMBER");
        acceptFirstIncomingInvite(adminToken);
        acceptFirstIncomingInvite(memberToken);
        String ownerId = findMemberId(ownerToken, orgId, ownerEmail);
        String adminId = findMemberId(ownerToken, orgId, adminEmail);
        String memberId = findMemberId(ownerToken, orgId, memberEmail);
        return new OrgScenario(orgId, ownerToken, adminToken, memberToken, ownerId, adminId, memberId);
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
        for (JsonNode row : rows) {
            if (email.equals(row.path("userEmail").asText())) {
                return row.path("id").asText();
            }
        }
        throw new AssertionError("member not found for email " + email);
    }

    private Set<String> listAuditTypes(String token, String orgId) throws Exception {
        JsonNode rows = readJson(mockMvc.perform(get("/api/v1/orgs/" + orgId + "/audit/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()).at("/data");
        LinkedHashSet<String> types = new LinkedHashSet<>();
        for (JsonNode row : rows) {
            types.add(row.path("eventType").asText());
        }
        return types;
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private record OrgScenario(
            String orgId,
            String ownerToken,
            String adminToken,
            String memberToken,
            String ownerId,
            String adminId,
            String memberId
    ) {
    }
}
