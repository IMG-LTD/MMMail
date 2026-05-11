package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.platform.contract.V21ApiContract;
import com.mmmail.platform.contract.V21ApiContractCatalog;
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
class BackendV21ApiContractCatalogTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void catalogShouldCoverV21NamespacesWithMetadata() {
        V21ApiContractCatalog catalog = V21ApiContractCatalog.defaultCatalog();
        Map<String, V21ApiContract> byPath = catalog.contracts().stream()
                .collect(Collectors.toMap(V21ApiContract::identity, Function.identity()));

        assertThat(catalog.moduleCount()).isGreaterThanOrEqualTo(15);
        assertContract(byPath, "GET /api/v2/workspace/summary", "workspace", "WorkspaceSummary", "community");
        assertContract(byPath, "POST /api/v2/mail/send", "mail", "MailSendResult", "community");
        assertContract(byPath, "POST /api/v2/calendar/bookings", "calendar", "CalendarEvent", "premium");
        assertContract(byPath, "POST /api/v2/drive/uploads", "drive", "DriveItem", "community");
        assertContract(byPath, "GET /api/v2/admin/summary", "admin-governance", "AdminSummary", "enterprise-governance");
        assertContract(byPath, "GET /api/v2/settings/profile", "settings", "UserPreference", "community");

        assertThat(byPath.get("GET /api/v2/admin/summary").designSource()).contains("docs/MMMail/UI/Admin");
        assertThat(byPath.get("POST /api/v2/mail/send").permissions()).contains("mail:send");
    }

    @Test
    void authenticatedPlatformEndpointShouldExposeCatalog() throws Exception {
        String token = login("admin@mmmail.local", PASSWORD);

        mockMvc.perform(get("/api/v2/platform/contracts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value("v2.1"))
                .andExpect(jsonPath("$.data.contracts.length()").value(116))
                .andExpect(jsonPath("$.data.contracts[0].method").value("GET"))
                .andExpect(jsonPath("$.data.contracts[0].path").value("/api/v2/workspace/summary"));
    }

    @Test
    void contractCatalogShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v2/platform/contracts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void openApiCatalogShouldFreezeV21NamespaceCoverage() throws Exception {
        Path repoRoot = resolveRepoRoot();
        String openApi = Files.readString(repoRoot.resolve("contracts/openapi/v21-api-catalog.yaml"));

        assertThat(openApi)
                .contains("title: MMMail v2.1 API Contract Catalog")
                .contains("/api/v2/workspace/summary:")
                .contains("/api/v2/mail/send:")
                .contains("/api/v2/admin/summary:")
                .contains("/api/v2/settings/profile:")
                .contains("x-permission:")
                .contains("x-entitlement:")
                .contains("x-design-source:");
    }

    private void assertContract(Map<String, V21ApiContract> byPath, String identity, String owner, String responseModel, String entitlement) {
        assertThat(byPath).containsKey(identity);
        V21ApiContract contract = byPath.get(identity);
        assertThat(contract.ownerModule()).isEqualTo(owner);
        assertThat(contract.responseModel()).isEqualTo(responseModel);
        assertThat(contract.entitlement()).isEqualTo(entitlement);
        assertThat(contract.permissions()).isNotEmpty();
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"email\": \"%s\",
                                  \"password\": \"%s\"
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
