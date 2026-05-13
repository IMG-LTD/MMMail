package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.ErrorCode;
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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendV21RuntimeContractGapClosureTest {

    private static final String PASSWORD = "Password@123";
    private static final Pattern HTTP_CLIENT_CALL = Pattern.compile(
            "httpClient\\.(get|post|patch|delete)[^(]*\\((['`])([^'`]+)\\2"
    );
    private static final Pattern TEMPLATE_EXPRESSION = Pattern.compile("\\$\\{[^}]+}");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void frontendV21ApiClientRoutesShouldBeCataloged() throws Exception {
        Set<RouteIdentity> frontendRoutes = frontendV21Routes();
        Set<RouteIdentity> catalogRoutes = V21ApiContractCatalog.defaultCatalog().contracts().stream()
                .map(RouteIdentity::fromContract)
                .collect(Collectors.toSet());

        assertThat(frontendRoutes)
                .as("frontend-v2 service API /api/v2 routes must exist in V21ApiContractCatalog")
                .isSubsetOf(catalogRoutes);
    }

    @Test
    void v21AuthRoutesShouldUseRealAuthRuntime() throws Exception {
        String email = "v21-gap-auth-" + System.nanoTime() + "@mmmail.local";
        MvcResult register = mockMvc.perform(post("/api/v2/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "displayName": "V21 Gap"
                                }
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode auth = readJson(register).at("/data");
        String accessToken = auth.at("/accessToken").asText();
        String refreshToken = auth.at("/refreshToken").asText();
        mockMvc.perform(get("/api/v2/auth/sessions")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].current").value(true));

        MvcResult refresh = mockMvc.perform(post("/api/v2/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        String rotatedToken = readJson(refresh).at("/data/accessToken").asText();
        mockMvc.perform(post("/api/v2/auth/logout-all")
                        .header("Authorization", "Bearer " + rotatedToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v2/auth/sessions")
                        .header("Authorization", "Bearer " + rotatedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void v21CapabilityRoutesShouldBeCatalogedAndMapped() throws Exception {
        String token = login("admin@mmmail.local", PASSWORD);

        mockMvc.perform(get("/api/v2/ai-platform/capabilities")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supportsApproval").value(true));

        mockMvc.perform(get("/api/v2/mcp/registry")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supportsSecretMasking").value(true));
    }

    @Test
    void premiumHostedAndGovernanceFrontendRoutesShouldFailBeforeControllerFallback() throws Exception {
        String token = register("v21-gap-boundary-" + System.nanoTime() + "@mmmail.local");

        assertEntitlementRequired(token, "/api/v2/settings/integrations");
        assertEntitlementRequired(token, "/api/v2/settings/audit");
        assertEntitlementRequired(token, "/api/v2/billing/summary");
        assertEntitlementRequired(token, "/api/v2/admin/summary");
    }

    private void assertEntitlementRequired(String token, String path) throws Exception {
        mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode()));
    }

    private Set<RouteIdentity> frontendV21Routes() throws Exception {
        Path apiDir = resolveRepoRoot().resolve("frontend-v2/src/service/api");
        try (Stream<Path> files = Files.walk(apiDir)) {
            return files
                    .filter(path -> path.toString().endsWith(".ts"))
                    .flatMap(this::readRoutes)
                    .collect(Collectors.toSet());
        }
    }

    private Stream<RouteIdentity> readRoutes(Path file) {
        try {
            Matcher matcher = HTTP_CLIENT_CALL.matcher(Files.readString(file));
            Stream.Builder<RouteIdentity> routes = Stream.builder();
            while (matcher.find()) {
                String rawPath = matcher.group(3);
                if (rawPath.startsWith("/api/v2/")) {
                    routes.add(new RouteIdentity(matcher.group(1).toUpperCase(), canonicalPath(rawPath)));
                }
            }
            return routes.build();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read frontend API routes from " + file, exception);
        }
    }

    private static String canonicalPath(String path) {
        String withoutQuery = path.split("\\?", 2)[0];
        String templateNormalized = TEMPLATE_EXPRESSION.matcher(withoutQuery).replaceAll(":param");
        return templateNormalized.replaceAll(":[^/]+", ":param");
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

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "displayName": "V21 Gap"
                                }
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/accessToken").asText();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private Path resolveRepoRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null && !Files.isDirectory(current.resolve("frontend-v2"))) {
            current = current.getParent();
        }
        assertThat(current).isNotNull();
        return current;
    }

    private record RouteIdentity(String method, String path) {

        private static RouteIdentity fromContract(V21ApiContract contract) {
            return new RouteIdentity(contract.method(), canonicalPath(contract.path()));
        }
    }
}
