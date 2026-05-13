# Backend v2.1 Runtime Contract Gap Closure 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 建立并通过 v2.1 前端客户端、后端 API catalog、后端 controller/gate 三方一致性审计，消除剩余 `/api/v2/*` 运行时契约缺口。

**架构：** 先新增失败的 gap closure 测试，读取 `frontend-v2/src/service/api` 中真实 `/api/v2/*` 客户端路径并与 `V21ApiContractCatalog` 规范化对比。随后补齐 catalog/OpenAPI 和最小运行时 controller：Community 能力走真实服务，Premium/Hosted/Governance 保持 entitlement gate 显式失败，不添加 mock 成功或 silent fallback。

**技术栈：** Java 21、Spring Boot、MockMvc、JUnit 5、AssertJ、Vue frontend contract files、Maven、pnpm。

---

## 文件结构

- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21RuntimeContractGapClosureTest.java`
  - 职责：集中验证 frontend v2 API 客户端路径、`V21ApiContractCatalog`、运行时 controller/gate 的一致性。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/security/AuthCookieService.java`
  - 职责：复用 v1/v2 auth controller 的 refresh cookie、CSRF cookie、读取和清理逻辑，避免复制安全细节。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21AuthController.java`
  - 职责：暴露 `/api/v2/auth/*` Community 认证运行时，复用 `AuthService` 真实注册、登录、刷新、会话、撤销逻辑。
- 修改：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/AuthController.java`
  - 职责：改为使用 `AuthCookieService`，保持 `/api/v1/auth/*` 行为和 cookie path 不变。
- 修改：`backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiContractCatalog.java`
  - 职责：补齐 frontend v2 client 已使用但 catalog 未登记的 auth、AI platform、MCP registry 契约。
- 修改：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21ApiContractCatalogTest.java`
  - 职责：更新 owner module、catalog count、关键路径断言。
- 修改：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21AccessEntitlementGatesTest.java`
  - 职责：更新 matcher/gate metadata 覆盖断言。
- 修改：`contracts/openapi/v21-api-catalog.yaml`
  - 职责：登记新增 v2.1 catalog path 和 x-permission/x-entitlement。
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`
  - 职责：记录本 slice 完成状态、提交和验证命令。

## 任务 1：新增失败的 runtime gap closure 测试

**文件：**
- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21RuntimeContractGapClosureTest.java`

- [ ] **步骤 1：编写失败的测试**

创建 `BackendV21RuntimeContractGapClosureTest.java`：

```java
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
            "httpClient\\.(get|post|patch|delete)(?:<[^>]+>)?\\((['`])([^'`]+)\\2"
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

        String accessToken = readJson(register).at("/data/accessToken").asText();
        mockMvc.perform(get("/api/v2/auth/sessions")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].current").value(true));
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
```

- [ ] **步骤 2：运行测试验证失败**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21RuntimeContractGapClosureTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：FAIL。失败点应包括 frontend routes not subset of catalog，至少暴露 `/api/v2/auth/refresh`、`/api/v2/auth/logout-all`、`/api/v2/auth/sessions`、`/api/v2/auth/sessions/:param/revoke`、`/api/v2/ai-platform/capabilities`、`/api/v2/mcp/registry` 之一。

- [ ] **步骤 3：Commit 失败测试**

```bash
git add backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21RuntimeContractGapClosureTest.java
git diff --cached --check
git commit -m "test(backend-v21): cover runtime contract gaps"
```

## 任务 2：补齐 catalog、OpenAPI 和 catalog 测试

**文件：**
- 修改：`backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiContractCatalog.java`
- 修改：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21ApiContractCatalogTest.java`
- 修改：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21AccessEntitlementGatesTest.java`
- 修改：`contracts/openapi/v21-api-catalog.yaml`

- [ ] **步骤 1：更新 catalog 定义**

修改 `V21ApiContractCatalog.defaultCatalog()`，在 `publicAuthShareSystemContracts()` 后追加 `aiPlatformContracts()` 和 `mcpContracts()`：

```java
platformContracts(), settingsContracts(), publicAuthShareSystemContracts(),
aiPlatformContracts(), mcpContracts()
```

扩展 `identity` contracts：

```java
module("identity", "docs/MMMail/UI/首页", new String[][]{
        {"POST", "/api/v2/auth/login", "AuthPayload", COMMUNITY, "auth:public"},
        {"POST", "/api/v2/auth/register", "AuthPayload", COMMUNITY, "auth:public"},
        {"POST", "/api/v2/auth/refresh", "AuthPayload", COMMUNITY, "auth:public"},
        {"POST", "/api/v2/auth/logout-all", "Void", COMMUNITY, "auth:sessions:write"},
        {"GET", "/api/v2/auth/sessions", "UserSession[]", COMMUNITY, "auth:sessions:read"},
        {"POST", "/api/v2/auth/sessions/:id/revoke", "Void", COMMUNITY, "auth:sessions:write"}
})
```

新增两个 contract 方法：

```java
private static List<V21ApiContract> aiPlatformContracts() {
    return module("ai-platform", "docs/MMMail/UI/Admin", new String[][]{
            {"GET", "/api/v2/ai-platform/capabilities", "AiPlatformCapabilities", COMMUNITY, "ai-platform:capabilities:read"}
    });
}

private static List<V21ApiContract> mcpContracts() {
    return module("mcp", "docs/MMMail/UI/Admin", new String[][]{
            {"GET", "/api/v2/mcp/registry", "McpRegistryCapabilities", COMMUNITY, "mcp:registry:read"}
    });
}
```

- [ ] **步骤 2：更新 catalog 单测**

在 `BackendV21ApiContractCatalogTest.REQUIRED_OWNER_MODULES` 增加：

```java
"ai-platform",
"mcp"
```

在 `catalogShouldCoverBackendRuntimeFamiliesRequiredByFrontendV21()` 增加：

```java
assertContract(contractsByIdentity, "POST /api/v2/auth/refresh", "identity", "AuthPayload", "community");
assertContract(contractsByIdentity, "POST /api/v2/auth/logout-all", "identity", "Void", "community");
assertContract(contractsByIdentity, "GET /api/v2/auth/sessions", "identity", "UserSession[]", "community");
assertContract(contractsByIdentity, "POST /api/v2/auth/sessions/:id/revoke", "identity", "Void", "community");
assertContract(contractsByIdentity, "GET /api/v2/ai-platform/capabilities", "ai-platform", "AiPlatformCapabilities", "community");
assertContract(contractsByIdentity, "GET /api/v2/mcp/registry", "mcp", "McpRegistryCapabilities", "community");
```

将 `authenticatedPlatformEndpointShouldExposeCatalog()` 的 contracts length 从 `127` 改为 `133`。

在 `openApiCatalogShouldFreezeV21NamespaceCoverage()` 增加：

```java
.contains("/api/v2/auth/refresh:")
.contains("/api/v2/auth/sessions:")
.contains("/api/v2/ai-platform/capabilities:")
.contains("/api/v2/mcp/registry:")
```

- [ ] **步骤 3：更新 access gate 单测**

在 `BackendV21AccessEntitlementGatesTest.catalogShouldExposeAccessMetadataForAllV21Contracts()` 的 containsKeys 增加：

```java
"POST /api/v2/auth/refresh",
"GET /api/v2/ai-platform/capabilities",
"GET /api/v2/mcp/registry"
```

在 `matcherShouldResolveDynamicV21RoutesAndRejectUnknownRoutes()` 增加：

```java
assertThat(matcher.match("POST", "/api/v2/auth/sessions/123/revoke"))
        .hasValueSatisfying(contract -> assertThat(contract.path()).isEqualTo("/api/v2/auth/sessions/:id/revoke"));
```

- [ ] **步骤 4：更新 OpenAPI catalog**

在 `contracts/openapi/v21-api-catalog.yaml` 中加入：

```yaml
  /api/v2/auth/refresh:
    post: {summary: Refresh v2.1 auth session, x-permission: ["auth:public"], x-entitlement: community, x-design-source: docs/MMMail/UI/首页, responses: {"200": {description: ok}}}
  /api/v2/auth/logout-all:
    post: {summary: Revoke all v2.1 auth sessions, x-permission: ["auth:sessions:write"], x-entitlement: community, x-design-source: docs/MMMail/UI/首页, responses: {"200": {description: ok}}}
  /api/v2/auth/sessions:
    get: {summary: List v2.1 auth sessions, x-permission: ["auth:sessions:read"], x-entitlement: community, x-design-source: docs/MMMail/UI/首页, responses: {"200": {description: ok}}}
  /api/v2/auth/sessions/{id}/revoke:
    post: {summary: Revoke v2.1 auth session, x-permission: ["auth:sessions:write"], x-entitlement: community, x-design-source: docs/MMMail/UI/首页, responses: {"200": {description: ok}}}
  /api/v2/ai-platform/capabilities:
    get: {summary: Read AI platform capabilities, x-permission: ["ai-platform:capabilities:read"], x-entitlement: community, x-design-source: docs/MMMail/UI/Admin, responses: {"200": {description: ok}}}
  /api/v2/mcp/registry:
    get: {summary: Read MCP registry capabilities, x-permission: ["mcp:registry:read"], x-entitlement: community, x-design-source: docs/MMMail/UI/Admin, responses: {"200": {description: ok}}}
```

- [ ] **步骤 5：运行 catalog/gap 测试**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21RuntimeContractGapClosureTest,BackendV21ApiContractCatalogTest,BackendV21AccessEntitlementGatesTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：仍 FAIL。catalog alignment 应通过；runtime auth route 仍可能 404，因为 v2 auth controller 尚未实现。

- [ ] **步骤 6：Commit catalog 修复**

```bash
git add backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiContractCatalog.java \
  backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21ApiContractCatalogTest.java \
  backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21AccessEntitlementGatesTest.java \
  contracts/openapi/v21-api-catalog.yaml
git diff --cached --check
git commit -m "feat(backend-v21): align runtime contract catalog gaps"
```

## 任务 3：抽取 auth cookie helper 并保持 v1 行为

**文件：**
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/security/AuthCookieService.java`
- 修改：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/AuthController.java`

- [ ] **步骤 1：创建 AuthCookieService**

创建 `AuthCookieService.java`：

```java
package com.mmmail.server.security;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.UUID;

@Service
public class AuthCookieService {

    public static final String V1_AUTH_COOKIE_PATH = "/api/v1/auth";
    public static final String V2_AUTH_COOKIE_PATH = "/api/v2/auth";
    private static final String CSRF_HEADER_NAME = "X-MMMAIL-CSRF";
    private static final long MIN_REFRESH_COOKIE_MAX_AGE_SECONDS = 3600L;

    private final String refreshCookieName;
    private final String csrfCookieName;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final long refreshCookieMaxAgeSeconds;

    public AuthCookieService(
            @Value("${mmmail.auth.refresh-cookie-name:MMMAIL_REFRESH_TOKEN}") String refreshCookieName,
            @Value("${mmmail.auth.csrf-cookie-name:MMMAIL_CSRF_TOKEN}") String csrfCookieName,
            @Value("${mmmail.auth.cookie-secure:false}") boolean cookieSecure,
            @Value("${mmmail.auth.cookie-same-site:Lax}") String cookieSameSite,
            @Value("${mmmail.refresh-token-expire-hours:168}") long refreshExpireHours
    ) {
        this.refreshCookieName = refreshCookieName;
        this.csrfCookieName = csrfCookieName;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
        this.refreshCookieMaxAgeSeconds = Math.max(MIN_REFRESH_COOKIE_MAX_AGE_SECONDS, refreshExpireHours * 3600L);
    }

    public String readRefreshToken(HttpServletRequest request) {
        return readCookie(request, refreshCookieName);
    }

    public void verifyCsrf(HttpServletRequest request) {
        String csrfCookie = readCookie(request, csrfCookieName);
        String csrfHeader = request.getHeader(CSRF_HEADER_NAME);
        if (!StringUtils.hasText(csrfCookie) || !csrfCookie.equals(csrfHeader)) {
            throw new BizException(ErrorCode.FORBIDDEN, "CSRF token is invalid");
        }
    }

    public void attachAuthCookies(HttpServletResponse response, String refreshToken, String refreshCookiePath) {
        response.addHeader("Set-Cookie", refreshCookie(refreshToken, refreshCookiePath, refreshCookieMaxAgeSeconds));
        response.addHeader("Set-Cookie", csrfCookie(generateCsrfToken(), refreshCookieMaxAgeSeconds));
    }

    public void clearAuthCookies(HttpServletResponse response, String refreshCookiePath) {
        response.addHeader("Set-Cookie", refreshCookie("", refreshCookiePath, 0L));
        response.addHeader("Set-Cookie", csrfCookie("", 0L));
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String refreshCookie(String value, String path, long maxAgeSeconds) {
        return ResponseCookie.from(refreshCookieName, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .path(path)
                .sameSite(cookieSameSite)
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build()
                .toString();
    }

    private String csrfCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(csrfCookieName, value)
                .httpOnly(false)
                .secure(cookieSecure)
                .path("/")
                .sameSite(cookieSameSite)
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build()
                .toString();
    }

    private String generateCsrfToken() {
        return UUID.randomUUID() + "." + UUID.randomUUID();
    }
}
```

- [ ] **步骤 2：改造 AuthController 使用 helper**

在 `AuthController` 中：

- 删除 `@Value` imports、`Cookie`、`ResponseCookie`、`Duration`、`UUID` 和本地 cookie 字段。
- 构造函数改为接收 `AuthCookieService authCookieService`。
- 保存字段：

```java
private final AuthService authService;
private final AuthCookieService authCookieService;
```

注册、登录中替换为：

```java
authCookieService.attachAuthCookies(
        httpResponse,
        response.refreshToken(),
        AuthCookieService.V1_AUTH_COOKIE_PATH
);
```

刷新中替换读取和 CSRF 验证：

```java
String cookieToken = authCookieService.readRefreshToken(httpRequest);
String bodyToken = request == null ? null : request.refreshToken();
String refreshToken = StringUtils.hasText(cookieToken) ? cookieToken : bodyToken;
if (!StringUtils.hasText(refreshToken)) {
    throw new BizException(ErrorCode.SESSION_INVALID, "Refresh token is required");
}
if (StringUtils.hasText(cookieToken)) {
    authCookieService.verifyCsrf(httpRequest);
}
```

刷新成功后：

```java
authCookieService.attachAuthCookies(
        httpResponse,
        response.refreshToken(),
        AuthCookieService.V1_AUTH_COOKIE_PATH
);
```

登出和全部登出中替换为：

```java
authCookieService.clearAuthCookies(httpResponse, AuthCookieService.V1_AUTH_COOKIE_PATH);
```

删除 `verifyCsrf`、`readCookie`、`attachAuthCookies`、`clearAuthCookies`、`generateCsrfToken` 私有方法。

- [ ] **步骤 3：运行 v1 auth 回归**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=AuthIntegrationTest,BackendV21CommunityRuntimeClosureTest -Dsurefire.failIfNoSpecifiedTests=false
```

如果 `AuthIntegrationTest` 不存在，使用：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=*Auth*Test,BackendV21CommunityRuntimeClosureTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：PASS。若通配符没有命中 auth 测试，必须至少保证 `BackendV21CommunityRuntimeClosureTest` PASS，且下一任务的 v2 auth 测试覆盖新路径。

- [ ] **步骤 4：Commit helper 抽取**

```bash
git add backend/mmmail-server/src/main/java/com/mmmail/server/security/AuthCookieService.java \
  backend/mmmail-server/src/main/java/com/mmmail/server/controller/AuthController.java
git diff --cached --check
git commit -m "refactor(auth): share auth cookie handling"
```

## 任务 4：新增 v2 auth controller

**文件：**
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21AuthController.java`

- [ ] **步骤 1：创建 v2 controller**

创建 `V21AuthController.java`：

```java
package com.mmmail.server.controller;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.LoginRequest;
import com.mmmail.server.model.dto.RefreshRequest;
import com.mmmail.server.model.dto.RegisterRequest;
import com.mmmail.server.model.vo.AuthResponse;
import com.mmmail.server.model.vo.UserSessionVo;
import com.mmmail.server.security.AuthCookieService;
import com.mmmail.server.service.AuthService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/auth")
public class V21AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;

    public V21AuthController(AuthService authService, AuthCookieService authCookieService) {
        this.authService = authService;
        this.authCookieService = authCookieService;
    }

    @PostMapping("/register")
    public Result<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthResponse response = authService.register(request, httpRequest.getRemoteAddr());
        authCookieService.attachAuthCookies(httpResponse, response.refreshToken(), AuthCookieService.V2_AUTH_COOKIE_PATH);
        return Result.success(response);
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthResponse response = authService.login(request, httpRequest.getRemoteAddr());
        authCookieService.attachAuthCookies(httpResponse, response.refreshToken(), AuthCookieService.V2_AUTH_COOKIE_PATH);
        return Result.success(response);
    }

    @PostMapping("/refresh")
    public Result<AuthResponse> refresh(
            @RequestBody(required = false) RefreshRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String refreshToken = resolveRefreshToken(request, httpRequest);
        AuthResponse response = authService.refresh(refreshToken, httpRequest.getRemoteAddr());
        authCookieService.attachAuthCookies(httpResponse, response.refreshToken(), AuthCookieService.V2_AUTH_COOKIE_PATH);
        return Result.success(response);
    }

    @PostMapping("/logout-all")
    public Result<Void> logoutAll(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logoutAll(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr());
        authCookieService.clearAuthCookies(httpResponse, AuthCookieService.V2_AUTH_COOKIE_PATH);
        return Result.success(null);
    }

    @GetMapping("/sessions")
    public Result<List<UserSessionVo>> sessions() {
        return Result.success(authService.listSessions(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentPrincipal().sessionId()
        ));
    }

    @PostMapping("/sessions/{sessionId}/revoke")
    public Result<Void> revokeSession(@PathVariable Long sessionId, HttpServletRequest httpRequest) {
        authService.revokeSession(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentPrincipal().sessionId(),
                sessionId,
                httpRequest.getRemoteAddr()
        );
        return Result.success(null);
    }

    private String resolveRefreshToken(RefreshRequest request, HttpServletRequest httpRequest) {
        String cookieToken = authCookieService.readRefreshToken(httpRequest);
        String bodyToken = request == null ? null : request.refreshToken();
        String refreshToken = StringUtils.hasText(cookieToken) ? cookieToken : bodyToken;
        if (!StringUtils.hasText(refreshToken)) {
            throw new BizException(ErrorCode.SESSION_INVALID, "Refresh token is required");
        }
        if (StringUtils.hasText(cookieToken)) {
            authCookieService.verifyCsrf(httpRequest);
        }
        return refreshToken;
    }
}
```

- [ ] **步骤 2：运行 gap 测试验证通过**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21RuntimeContractGapClosureTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：PASS，`Tests run: 4, Failures: 0, Errors: 0`。

- [ ] **步骤 3：Commit v2 auth runtime**

```bash
git add backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21AuthController.java
git diff --cached --check
git commit -m "feat(backend-v21): add v2 auth runtime"
```

## 任务 5：运行组合回归并修正测试隔离

**文件：**
- 可能修改：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21RuntimeContractGapClosureTest.java`
- 可能修改：与失败原因直接相关的测试文件

- [ ] **步骤 1：运行后端组合回归**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21RuntimeContractGapClosureTest,BackendV21ApiContractCatalogTest,BackendV21AccessEntitlementGatesTest,BackendV21CommunityRuntimeClosureTest,BackendV21CollaborationWriteRuntimeTest,BackendV21OpsRuntimeBridgeTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：PASS，覆盖 runtime gap、catalog、gate、Community closure、Collaboration write、Ops runtime。

- [ ] **步骤 2：如果组合回归失败，先根因调试**

必须使用 `superpowers-zh:systematic-debugging`。只允许针对根因做最小修复。典型允许修复：

```java
int before = countEvents();
// perform current test writes
assertThat(countEvents() - before).isEqualTo(expectedDelta);
```

禁止用删除断言、吞异常、放宽为 `isGreaterThanOrEqualTo(0)` 这类方式掩盖问题。

- [ ] **步骤 3：运行前端 contract 测试**

运行：

```bash
timeout 60s pnpm --dir frontend-v2 test
```

预期：PASS，`pass 84` 或当前测试总数全部通过。

- [ ] **步骤 4：Commit 回归修复**

如果步骤 2 只修正本 slice 新增测试隔离问题，运行：

```bash
git add backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21RuntimeContractGapClosureTest.java
git diff --cached --check
git commit -m "test(backend-v21): stabilize runtime contract gap regression"
```

如果根因落在既有测试共享状态，先完成 `superpowers-zh:systematic-debugging` 根因记录，再精确暂存失败堆栈指向的测试文件。若步骤 2 没有产生改动，不提交。

## 任务 6：更新进度文档并提交

**文件：**
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`
- 修改：`docs/superpowers/plans/2026-05-13-backend-v21-runtime-contract-gap-closure.md`

- [ ] **步骤 1：更新进度文档**

在 `Completed v2.1 Slices` 表格新增：

```markdown
| Backend Runtime contract gap closure (`backend-v21-runtime-contract-gap-closure`) | `BackendV21RuntimeContractGapClosureTest`, `V21ApiContractCatalog`, `V21AuthController`, v2 AI/MCP capability catalog coverage |
```

将 `Latest Completed Backend Slice` 改为：

```markdown
## Latest Completed Backend Slice

- Slice: `backend-v21-runtime-contract-gap-closure`
- Commits:
  - `test(backend-v21): cover runtime contract gaps`
  - `feat(backend-v21): align runtime contract catalog gaps`
  - `refactor(auth): share auth cookie handling`
  - `feat(backend-v21): add v2 auth runtime`
- Files changed: added runtime gap closure tests, aligned frontend v2 client paths with backend catalog/OpenAPI, shared auth cookie handling, and exposed v2 auth session runtime through real AuthService.
- Verification:
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21RuntimeContractGapClosureTest -Dsurefire.failIfNoSpecifiedTests=false`: PASS
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21RuntimeContractGapClosureTest,BackendV21ApiContractCatalogTest,BackendV21AccessEntitlementGatesTest,BackendV21CommunityRuntimeClosureTest,BackendV21CollaborationWriteRuntimeTest,BackendV21OpsRuntimeBridgeTest -Dsurefire.failIfNoSpecifiedTests=false`: PASS
  - `timeout 60s pnpm --dir frontend-v2 test`: PASS
```

将 `Active Backend Slice` 改为本 slice completed，并记录相同验证命令。

- [ ] **步骤 2：更新计划复选框状态**

把本计划已完成的步骤由 `- [ ]` 改为 `- [x]`。只更新实际完成的步骤，不提前勾选未执行步骤。

- [ ] **步骤 3：运行文档相关前端 contract**

运行：

```bash
timeout 60s pnpm --dir frontend-v2 test
```

预期：PASS。

- [ ] **步骤 4：提交进度文档**

```bash
git add -f docs/superpowers/progress/v21-implementation-progress.md \
  docs/superpowers/plans/2026-05-13-backend-v21-runtime-contract-gap-closure.md
git diff --cached --check
git commit -m "docs(backend-v21): record runtime contract gap closure progress"
```

## 最终验证

- [ ] **步骤 1：确认工作树**

```bash
git status --short --branch
```

预期：只剩既有无关未跟踪路径：

```text
?? .superpowers/
?? .tmp/
?? docs/MMMail.zip
?? docs/MMMail/
?? frontend/
```

- [ ] **步骤 2：确认最近提交**

```bash
git log --oneline -8
```

预期：能看到本计划的 test、catalog、auth runtime、progress 提交。

- [ ] **步骤 3：最终回报**

回报必须包含：

- 本 slice 名称：`backend-v21-runtime-contract-gap-closure`
- 创建/修改的关键文件
- 已通过的验证命令和测试数量
- 本地 `main` 最新提交
- 明确说明未提交的无关路径仍未纳入
