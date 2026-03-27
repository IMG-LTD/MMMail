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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrgMonitorStatusIntegrationTest {

    private static final String PASSWORD = "Password@123";
    private static final int OFFICIAL_EXPORT_LIMIT = 10_000;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void monitorStatusShouldExposeImmutableRetentionAndRestrictMembers() throws Exception {
        OrgScenario scenario = createScenario("v104-monitor");
        createDomain(scenario.ownerToken(), scenario.orgId(), "monitor-" + scenario.suffix() + ".example.com");

        mockMvc.perform(get("/api/v1/orgs/" + scenario.orgId() + "/admin-console/monitor-status")
                        .header("Authorization", "Bearer " + scenario.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alwaysOn").value(true))
                .andExpect(jsonPath("$.data.canDisable").value(false))
                .andExpect(jsonPath("$.data.canDeleteEvents").value(false))
                .andExpect(jsonPath("$.data.canEditEvents").value(false))
                .andExpect(jsonPath("$.data.visibilityScope").value("ALL_ADMINS"))
                .andExpect(jsonPath("$.data.retentionMode").value("PERMANENT"))
                .andExpect(jsonPath("$.data.maximumExportSize").value(OFFICIAL_EXPORT_LIMIT))
                .andExpect(jsonPath("$.data.totalEvents").isNumber())
                .andExpect(jsonPath("$.data.coveredEventTypes").isNumber())
                .andExpect(jsonPath("$.data.latestEvent.eventType").value("ORG_DOMAIN_ADD"));

        mockMvc.perform(get("/api/v1/orgs/" + scenario.orgId() + "/admin-console/monitor-status")
                        .header("Authorization", "Bearer " + scenario.memberToken()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/orgs/" + scenario.orgId() + "/audit/events")
                        .header("Authorization", "Bearer " + scenario.memberToken()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/orgs/" + scenario.orgId() + "/audit/events/export")
                        .header("Authorization", "Bearer " + scenario.memberToken()))
                .andExpect(status().isForbidden());

        MvcResult exportResult = mockMvc.perform(get("/api/v1/orgs/" + scenario.orgId() + "/audit/events/export")
                        .header("Authorization", "Bearer " + scenario.ownerToken())
                        .param("limit", String.valueOf(OFFICIAL_EXPORT_LIMIT)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(exportResult.getResponse().getHeader("Content-Disposition"))
                .contains("organization-audit-" + scenario.orgId());

        assertThat(listAuditTypes(scenario.ownerToken(), scenario.orgId()))
                .contains("ORG_MONITOR_STATUS_VIEW", "ORG_AUDIT_EXPORT");
    }

    private OrgScenario createScenario(String prefix) throws Exception {
        String suffix = prefix + "-" + System.nanoTime();
        String ownerEmail = suffix + "-owner@mmmail.local";
        String adminEmail = suffix + "-admin@mmmail.local";
        String memberEmail = suffix + "-member@mmmail.local";
        String ownerToken = register(ownerEmail, "V104 Owner");
        String adminToken = register(adminEmail, "V104 Admin");
        String memberToken = register(memberEmail, "V104 Member");
        String orgId = createOrganization(ownerToken, "V104 Org " + suffix);
        inviteMember(ownerToken, orgId, adminEmail, "ADMIN");
        inviteMember(ownerToken, orgId, memberEmail, "MEMBER");
        acceptFirstIncomingInvite(adminToken);
        acceptFirstIncomingInvite(memberToken);
        return new OrgScenario(suffix, orgId, ownerToken, adminToken, memberToken);
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

    private void createDomain(String token, String orgId, String domain) throws Exception {
        mockMvc.perform(post("/api/v1/orgs/" + orgId + "/domains")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"domain\":\"" + domain + "\"}"))
                .andExpect(status().isOk());
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

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private record OrgScenario(
            String suffix,
            String orgId,
            String ownerToken,
            String adminToken,
            String memberToken
    ) {
    }
}
