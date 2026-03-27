package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.server.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
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
@ActiveProfiles("test")
class OrgAdminConsoleIntegrationTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtService jwtService;

    @Test
    void customDomainLifecycleShouldAllowOwnerAndAdminButRejectMember() throws Exception {
        OrgScenario scenario = createScenario("v66-domain");
        String firstDomain = "alpha-" + scenario.suffix() + ".example.com";
        String secondDomain = "beta-" + scenario.suffix() + ".example.com";

        String firstDomainId = createDomain(scenario.adminToken(), scenario.orgId(), firstDomain);
        mockMvc.perform(post("/api/v1/orgs/" + scenario.orgId() + "/domains")
                        .header("Authorization", "Bearer " + scenario.memberToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"domain\":\"blocked-" + scenario.suffix() + ".example.com\"}"))
                .andExpect(status().isForbidden());

        String secondDomainId = createDomain(scenario.ownerToken(), scenario.orgId(), secondDomain);
        verifyDomain(scenario.adminToken(), scenario.orgId(), secondDomainId);
        setDefaultDomain(scenario.ownerToken(), scenario.orgId(), secondDomainId);
        removeDomain(scenario.adminToken(), scenario.orgId(), firstDomainId);

        mockMvc.perform(get("/api/v1/orgs/" + scenario.orgId() + "/domains")
                        .header("Authorization", "Bearer " + scenario.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].domain").value(secondDomain))
                .andExpect(jsonPath("$.data[0].status").value("VERIFIED"))
                .andExpect(jsonPath("$.data[0].defaultDomain").value(true));

        mockMvc.perform(get("/api/v1/orgs/" + scenario.orgId() + "/admin-console/summary")
                        .header("Authorization", "Bearer " + scenario.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentRole").value("OWNER"))
                .andExpect(jsonPath("$.data.memberCount").value(3))
                .andExpect(jsonPath("$.data.adminCount").value(2))
                .andExpect(jsonPath("$.data.domainCount").value(1))
                .andExpect(jsonPath("$.data.verifiedDomainCount").value(1))
                .andExpect(jsonPath("$.data.defaultDomain").value(secondDomain));

        assertThat(listAuditTypes(scenario.ownerToken(), scenario.orgId()))
                .contains("ORG_DOMAIN_ADD", "ORG_DOMAIN_VERIFY", "ORG_DOMAIN_SET_DEFAULT", "ORG_DOMAIN_REMOVE");
    }

    @Test
    void productAccessShouldRespectRoleVisibilityAndManagerUpdates() throws Exception {
        OrgScenario scenario = createScenario("v66-access");
        int totalProducts = listProductAccess(scenario.ownerToken(), scenario.orgId())
                .get(0)
                .path("products")
                .size();

        JsonNode adminRows = listProductAccess(scenario.adminToken(), scenario.orgId());
        assertThat(adminRows).hasSize(3);
        JsonNode adminMemberProducts = findMemberRow(adminRows, scenario.memberEmail()).at("/products");
        assertThat(adminMemberProducts).hasSizeGreaterThanOrEqualTo(10);
        assertThat(productState(adminMemberProducts, "MAIL")).isEqualTo("ENABLED");
        assertThat(productState(adminMemberProducts, "VPN")).isEqualTo("ENABLED");

        JsonNode memberRows = listProductAccess(scenario.memberToken(), scenario.orgId());
        assertThat(memberRows).hasSize(1);
        assertThat(memberRows.get(0).path("userEmail").asText()).isEqualTo(scenario.memberEmail());
        assertThat(memberRows.get(0).path("currentUser").asBoolean()).isTrue();

        mockMvc.perform(put("/api/v1/orgs/" + scenario.orgId() + "/admin-console/product-access/" + scenario.memberId())
                        .header("Authorization", "Bearer " + scenario.memberToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productAccessPayload("MAIL", "DISABLED")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/orgs/" + scenario.orgId() + "/admin-console/product-access/" + scenario.memberId())
                        .header("Authorization", "Bearer " + scenario.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productAccessPayload("MAIL", "DISABLED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabledProductCount").value(totalProducts - 1));

        MvcResult adminUpdate = mockMvc.perform(put("/api/v1/orgs/" + scenario.orgId() + "/admin-console/product-access/" + scenario.memberId())
                        .header("Authorization", "Bearer " + scenario.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productAccessPayload("MAIL", "ENABLED", "VPN", "DISABLED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentUser").value(false))
                .andExpect(jsonPath("$.data.enabledProductCount").value(totalProducts - 1))
                .andReturn();

        JsonNode updatedMember = readJson(adminUpdate).at("/data");
        assertThat(productState(updatedMember.path("products"), "MAIL")).isEqualTo("ENABLED");
        assertThat(productState(updatedMember.path("products"), "VPN")).isEqualTo("DISABLED");

        JsonNode memberRow = listProductAccess(scenario.memberToken(), scenario.orgId()).get(0);
        assertThat(productState(memberRow.path("products"), "MAIL")).isEqualTo("ENABLED");
        assertThat(productState(memberRow.path("products"), "VPN")).isEqualTo("DISABLED");
        assertThat(listAuditTypes(scenario.ownerToken(), scenario.orgId())).contains("ORG_PRODUCT_ACCESS_UPDATE");
    }

    @Test
    void auditMonitorShouldFilterSortAndExportCsv() throws Exception {
        OrgScenario scenario = createScenario("v95-audit");
        String today = LocalDate.now().toString();
        String domain = "audit-" + scenario.suffix() + ".example.com";
        createDomain(scenario.ownerToken(), scenario.orgId(), domain);

        MvcResult ascResult = mockMvc.perform(get("/api/v1/orgs/" + scenario.orgId() + "/audit/events")
                        .header("Authorization", "Bearer " + scenario.ownerToken())
                        .param("limit", "8")
                        .param("sortDirection", "ASC")
                        .param("fromDate", today)
                        .param("toDate", today))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode items = readJson(ascResult).at("/data");
        assertThat(items.size()).isGreaterThan(1);
        assertThat(items.get(0).path("id").asLong()).isLessThan(items.get(items.size() - 1).path("id").asLong());

        mockMvc.perform(get("/api/v1/orgs/" + scenario.orgId() + "/audit/events")
                        .header("Authorization", "Bearer " + scenario.ownerToken())
                        .param("fromDate", "2100-01-01")
                        .param("toDate", "2100-01-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        MvcResult exportResult = mockMvc.perform(get("/api/v1/orgs/" + scenario.orgId() + "/audit/events/export")
                        .header("Authorization", "Bearer " + scenario.ownerToken())
                        .param("eventType", "ORG_DOMAIN_ADD")
                        .param("keyword", domain)
                        .param("fromDate", today)
                        .param("toDate", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").doesNotExist())
                .andReturn();

        String disposition = exportResult.getResponse().getHeader("Content-Disposition");
        String body = exportResult.getResponse().getContentAsString();
        assertThat(disposition).contains("organization-audit-" + scenario.orgId());
        assertThat(body).contains("eventType");
        assertThat(body).contains("ORG_DOMAIN_ADD");
        assertThat(body).contains(domain);
    }

    @Test
    void organizationManagersShouldListAndRevokeMemberSessions() throws Exception {
        OrgScenario scenario = createScenario("v98-sessions");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(scenario.memberEmail(), PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        String memberSecondToken = readJson(loginResult).at("/data/accessToken").asText();
        Long memberSecondSessionId = jwtService.parseToken(memberSecondToken).sessionId();

        MvcResult sessionsResult = mockMvc.perform(get("/api/v1/orgs/" + scenario.orgId() + "/admin-console/member-sessions")
                        .header("Authorization", "Bearer " + scenario.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").isNumber())
                .andReturn();

        JsonNode sessions = readJson(sessionsResult).at("/data");
        assertThat(sessions.size()).isGreaterThanOrEqualTo(4);
        JsonNode memberSession = findSessionById(sessions, memberSecondSessionId);
        assertThat(memberSession.path("memberEmail").asText()).isEqualTo(scenario.memberEmail());
        assertThat(memberSession.path("role").asText()).isEqualTo("MEMBER");

        mockMvc.perform(get("/api/v1/orgs/" + scenario.orgId() + "/admin-console/member-sessions")
                        .header("Authorization", "Bearer " + scenario.memberToken()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/orgs/" + scenario.orgId() + "/admin-console/member-sessions/" + memberSession.path("sessionId").asText() + "/revoke")
                        .header("Authorization", "Bearer " + scenario.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/orgs/" + scenario.orgId() + "/members")
                        .header("Authorization", "Bearer " + memberSecondToken))
                .andExpect(status().isUnauthorized());

        assertThat(listAuditTypes(scenario.ownerToken(), scenario.orgId()))
                .contains("ORG_MEMBER_SESSION_LIST", "ORG_MEMBER_SESSION_REVOKE");
    }

    @Test
    void activeOrgScopeShouldExposeCurrentUserAccessAndBlockDisabledProductApi() throws Exception {
        OrgScenario scenario = createScenario("v96-guard");

        mockMvc.perform(put("/api/v1/orgs/" + scenario.orgId() + "/admin-console/product-access/" + scenario.memberId())
                        .header("Authorization", "Bearer " + scenario.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productAccessPayload("DOCS", "DISABLED")))
                .andExpect(status().isOk());

        MvcResult contextResult = mockMvc.perform(get("/api/v1/orgs/access-context")
                        .header("Authorization", "Bearer " + scenario.memberToken()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode scopes = readJson(contextResult).at("/data");
        assertThat(scopes).hasSize(1);
        assertThat(scopes.get(0).path("orgId").asText()).isEqualTo(scenario.orgId());
        assertThat(scopes.get(0).path("role").asText()).isEqualTo("MEMBER");
        assertThat(productState(scopes.get(0).path("products"), "DOCS")).isEqualTo("DISABLED");

        mockMvc.perform(get("/api/v1/docs/notes")
                        .header("Authorization", "Bearer " + scenario.memberToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/docs/notes")
                        .header("Authorization", "Bearer " + scenario.memberToken())
                        .header("X-MMMAIL-ORG-ID", scenario.orgId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(30045));

        mockMvc.perform(put("/api/v1/orgs/" + scenario.orgId() + "/admin-console/product-access/" + scenario.memberId())
                        .header("Authorization", "Bearer " + scenario.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productAccessPayload("DOCS", "ENABLED")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/docs/notes")
                        .header("Authorization", "Bearer " + scenario.memberToken())
                        .header("X-MMMAIL-ORG-ID", scenario.orgId()))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/orgs/" + scenario.orgId() + "/admin-console/product-access/" + scenario.memberId())
                        .header("Authorization", "Bearer " + scenario.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productAccessPayload("DOCS", "DISABLED")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/docs/notes")
                        .header("Authorization", "Bearer " + scenario.memberToken())
                        .header("X-MMMAIL-ORG-ID", scenario.orgId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(30045));
    }

    @Test
    void adminConsoleShouldRejectCrossOrganizationAccess() throws Exception {
        OrgScenario sourceScenario = createScenario("v103-source");
        OrgScenario targetScenario = createScenario("v103-target");

        assertCrossOrgAdminForbidden(sourceScenario.ownerToken(), targetScenario.orgId());
        assertCrossOrgAdminForbidden(sourceScenario.adminToken(), targetScenario.orgId());
        assertCrossOrgAdminForbidden(sourceScenario.memberToken(), targetScenario.orgId());
    }

    private OrgScenario createScenario(String prefix) throws Exception {
        String suffix = prefix + "-" + System.nanoTime();
        String ownerEmail = suffix + "-owner@mmmail.local";
        String adminEmail = suffix + "-admin@mmmail.local";
        String memberEmail = suffix + "-member@mmmail.local";
        String ownerToken = register(ownerEmail, "V66 Owner");
        String adminToken = register(adminEmail, "V66 Admin");
        String memberToken = register(memberEmail, "V66 Member");
        String orgId = createOrganization(ownerToken, "V66 Org " + suffix);
        inviteMember(ownerToken, orgId, adminEmail, "ADMIN");
        inviteMember(ownerToken, orgId, memberEmail, "MEMBER");
        acceptFirstIncomingInvite(adminToken);
        acceptFirstIncomingInvite(memberToken);
        String adminId = findMemberId(ownerToken, orgId, adminEmail);
        String memberId = findMemberId(ownerToken, orgId, memberEmail);
        return new OrgScenario(suffix, orgId, ownerToken, adminToken, memberToken, adminEmail, memberEmail, adminId, memberId);
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

    private String createDomain(String token, String orgId, String domain) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/orgs/" + orgId + "/domains")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"domain\":\"" + domain + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.domain").value(domain))
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private void verifyDomain(String token, String orgId, String domainId) throws Exception {
        mockMvc.perform(post("/api/v1/orgs/" + orgId + "/domains/" + domainId + "/verify")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"));
    }

    private void setDefaultDomain(String token, String orgId, String domainId) throws Exception {
        mockMvc.perform(post("/api/v1/orgs/" + orgId + "/domains/" + domainId + "/default")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultDomain").value(true));
    }

    private void removeDomain(String token, String orgId, String domainId) throws Exception {
        mockMvc.perform(delete("/api/v1/orgs/" + orgId + "/domains/" + domainId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private JsonNode listProductAccess(String token, String orgId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/orgs/" + orgId + "/admin-console/product-access")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data");
    }

    private void assertCrossOrgAdminForbidden(String token, String orgId) throws Exception {
        mockMvc.perform(get("/api/v1/orgs/" + orgId + "/admin-console/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(30013));

        mockMvc.perform(get("/api/v1/orgs/" + orgId + "/admin-console/product-access")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(30013));
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

    private String productAccessPayload(String... changes) {
        StringBuilder builder = new StringBuilder("{\"products\":[");
        for (int index = 0; index < changes.length; index += 2) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append("{\"productKey\":\"")
                    .append(changes[index])
                    .append("\",\"accessState\":\"")
                    .append(changes[index + 1])
                    .append("\"}");
        }
        return builder.append("]}").toString();
    }

    private JsonNode findMemberRow(JsonNode rows, String email) {
        for (JsonNode row : rows) {
            if (email.equals(row.path("userEmail").asText())) {
                return row;
            }
        }
        throw new IllegalStateException("member row not found: " + email);
    }

    private JsonNode findSessionById(JsonNode sessions, Long sessionId) {
        for (JsonNode session : sessions) {
            if (String.valueOf(sessionId).equals(session.path("sessionId").asText())) {
                return session;
            }
        }
        throw new IllegalStateException("session not found: " + sessionId);
    }

    private String productState(JsonNode products, String productKey) {
        for (JsonNode product : products) {
            if (productKey.equals(product.path("productKey").asText())) {
                return product.path("accessState").asText();
            }
        }
        throw new IllegalStateException("product not found: " + productKey);
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
            String adminEmail,
            String memberEmail,
            String adminId,
            String memberId
    ) {
    }
}
