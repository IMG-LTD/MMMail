package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.platform.access.AccessDecision;
import com.mmmail.platform.access.AccessDecisionReason;
import com.mmmail.platform.access.AccessEntitlement;
import com.mmmail.platform.access.AccessRequest;
import com.mmmail.platform.contract.V21ApiContract;
import com.mmmail.platform.contract.V21ApiContractCatalog;
import com.mmmail.server.access.V21ApiAccessGateService;
import com.mmmail.server.access.V21ApiContractMatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendV21AccessEntitlementGatesTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private V21ApiContractMatcher matcher;
    @Autowired
    private V21ApiAccessGateService gateService;

    @Test
    void catalogShouldExposeAccessMetadataForAllV21Contracts() {
        Map<String, V21ApiContract> contracts = V21ApiContractCatalog.defaultCatalog().contracts().stream()
                .collect(Collectors.toMap(V21ApiContract::identity, Function.identity()));

        assertThat(contracts).containsKeys(
                "GET /api/v2/workspace/aggregation",
                "GET /api/v2/billing/readiness",
                "GET /api/v2/platform/contracts",
                "GET /api/v2/platform/capabilities",
                "GET /api/v2/ai-platform/capabilities",
                "GET /api/v2/mcp/registry",
                "POST /api/v2/auth/refresh",
                "GET /api/v2/auth/sessions",
                "GET /api/v2/share/capabilities",
                "GET /api/v2/public-share/capabilities",
                "GET /api/v2/share/mail/:token/attachments/:id/download",
                "GET /api/v2/share/drive/:token/items",
                "GET /api/v2/share/drive/:token/items/:id/download"
        );
        for (V21ApiContract contract : contracts.values()) {
            assertThat(contract.permissions()).isNotEmpty();
            assertThat(AccessEntitlement.fromContractValue(contract.entitlement())).isNotNull();
        }
    }

    @Test
    void matcherShouldResolveDynamicV21RoutesAndRejectUnknownRoutes() {
        assertThat(matcher.match("GET", "/api/v2/docs/123/"))
                .hasValueSatisfying(contract -> assertThat(contract.path()).isEqualTo("/api/v2/docs/:id"));
        assertThat(matcher.match("GET", "/api/v2/share/pass/public-token"))
                .hasValueSatisfying(contract -> assertThat(contract.path()).isEqualTo("/api/v2/share/pass/:token"));
        assertThat(matcher.match("POST", "/api/v2/auth/sessions/session-1/revoke"))
                .hasValueSatisfying(contract -> assertThat(contract.path()).isEqualTo("/api/v2/auth/sessions/:id/revoke"));
        assertThat(matcher.match("GET", "/api/v2/share/mail/public-token/attachments/12/download"))
                .hasValueSatisfying(contract -> assertThat(contract.path()).isEqualTo("/api/v2/share/mail/:token/attachments/:id/download"));
        assertThat(matcher.match("GET", "/api/v2/share/drive/public-token/items/15/download"))
                .hasValueSatisfying(contract -> assertThat(contract.path()).isEqualTo("/api/v2/share/drive/:token/items/:id/download"));
        assertThat(matcher.match("GET", "/api/v2/unknown")).isEmpty();
    }

    @Test
    void publicContractsShouldNotRequireAuthentication() {
        V21ApiContract contract = matcher.match("GET", "/api/v2/share/pass/public-token").orElseThrow();
        AccessDecision decision = gateService.evaluate(new AccessRequest(
                "GET",
                "/api/v2/share/pass/public-token",
                null,
                null,
                null,
                null,
                contract
        ));

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.reason()).isEqualTo(AccessDecisionReason.PUBLIC_CONTRACT);
    }

    @Test
    void protectedContractsShouldRequireAuthentication() {
        V21ApiContract contract = matcher.match("GET", "/api/v2/platform/contracts").orElseThrow();

        AccessDecision decision = gateService.evaluate(new AccessRequest(
                "GET",
                "/api/v2/platform/contracts",
                null,
                null,
                null,
                null,
                contract
        ));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo(AccessDecisionReason.AUTHENTICATION_REQUIRED);
        assertThat(decision.errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED.getCode());
    }

    @Test
    void communityUserShouldBeDeniedForPremiumHostedAndGovernanceContracts() {
        Map<String, AccessDecision> decisions = Map.of(
                "premium", decisionFor("POST", "/api/v2/command-center/runs"),
                "hosted", decisionFor("GET", "/api/v2/billing/summary"),
                "governance", decisionFor("GET", "/api/v2/admin/summary")
        );

        assertThat(decisions.values()).allSatisfy(decision -> {
            assertThat(decision.allowed()).isFalse();
            assertThat(decision.reason()).isEqualTo(AccessDecisionReason.ENTITLEMENT_REQUIRED);
            assertThat(decision.errorCode()).isEqualTo(ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode());
        });
    }

    @Test
    void adminShouldAccessCommunityPlatformMetadataButNotBypassEntitlement() throws Exception {
        String token = login("admin@mmmail.local", PASSWORD);

        mockMvc.perform(get("/api/v2/platform/contracts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value("v2.1"));

        mockMvc.perform(get("/api/v2/admin/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode()));
    }

    @Test
    void unknownV21RouteShouldFailBeforeControllerFallback() throws Exception {
        String token = login("admin@mmmail.local", PASSWORD);

        mockMvc.perform(get("/api/v2/unknown")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.V2_API_CONTRACT_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("Unknown v2 API contract"));
    }

    @Test
    void openApiCatalogShouldIncludeGateManagedMetadataRoutes() throws Exception {
        Path root = resolveRepoRoot();
        String openApi = Files.readString(root.resolve("contracts/openapi/v21-api-catalog.yaml"));

        assertThat(openApi)
                .contains("/api/v2/platform/contracts:")
                .contains("/api/v2/platform/capabilities:")
                .contains("/api/v2/ai-platform/capabilities:")
                .contains("/api/v2/mcp/registry:")
                .contains("/api/v2/share/capabilities:")
                .contains("/api/v2/public-share/capabilities:")
                .contains("/api/v2/share/mail/{token}/attachments/{id}/download:")
                .contains("/api/v2/share/drive/{token}/items:")
                .contains("/api/v2/share/drive/{token}/items/{id}/download:");
    }

    private AccessDecision decisionFor(String method, String path) {
        V21ApiContract contract = matcher.match(method, path).orElseThrow();
        return gateService.evaluate(new AccessRequest(method, path, 101L, "USER", null, null, contract));
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/accessToken").asText();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private Path resolveRepoRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null && !Files.isDirectory(current.resolve("contracts"))) {
            current = current.getParent();
        }
        assertThat(current).isNotNull();
        return current;
    }
}
