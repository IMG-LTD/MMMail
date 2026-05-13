# Backend v2.1 Access Entitlement Gates 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 实现 v2.1 后端合同级 access/permission/entitlement gate，让 `/api/v2/*` 路由基于 `V21ApiContractCatalog` 明确判定公开访问、认证、权限、权益和未知路由。

**架构：** `mmmail-platform` 增加不可变 access 模型；`mmmail-server` 增加合同 matcher、Community entitlement provider、gate service 和 MVC interceptor。`V21ApiContractCatalog` 继续作为唯一 v2.1 路由访问元数据来源，现有 org product access guard 保持独立运行。

**技术栈：** Java 21 records/enums, Spring Boot MVC interceptor, Spring Security, JUnit 5, AssertJ, MockMvc, Maven, pnpm。

---

## 文件结构

- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/access/AccessEntitlement.java`
  定义 `community`、`premium`、`hosted`、`enterprise-governance` 的规范值和解析方法。
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/access/AccessPermission.java`
  校验 permission 字符串，识别 `auth:public`、`share:public`、`system:public`。
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/access/AccessDecisionReason.java`
  冻结 gate 决策原因。
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/access/AccessDecision.java`
  保存 allow/deny、HTTP status、错误码、消息、所需权益、所需权限。
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/access/AccessRequest.java`
  保存 method、path、user、role、org/scope 和已匹配 contract。
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/access/AccessGate.java`
  公开 `evaluate(AccessRequest)` 接口。
- 修改：`backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiAccess.java`
  强化 permissions 和 entitlement 校验。
- 修改：`backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiContractCatalog.java`
  增加 platform metadata 和 public-share capability 兼容合同。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/access/V21ApiContractMatcher.java`
  基于合同目录做 method/path 动态段匹配。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/access/V21ApiEntitlementProvider.java`
  定义权益判断接口。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/access/CommunityV21ApiEntitlementProvider.java`
  Community runtime 只授予 `community`。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/access/V21ApiAccessGateService.java`
  执行公开访问、认证、权限和权益判定。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/access/V21ApiAccessGateInterceptor.java`
  将 gate 挂到 `/api/v2/**`。
- 修改：`backend/mmmail-server/src/main/java/com/mmmail/server/config/WebMvcConfig.java`
  在 org product access interceptor 之前注册 v2.1 access gate。
- 修改：`backend/mmmail-server/src/main/java/com/mmmail/server/config/SecurityConfig.java`
  显式 permit v2.1 public auth/share/system routes，并保留既有 public-share capability 兼容路径。
- 修改：`backend/mmmail-common/src/main/java/com/mmmail/common/exception/ErrorCode.java`
  增加 v2.1 gate 错误码。
- 修改：`backend/mmmail-common/src/main/java/com/mmmail/common/exception/GlobalExceptionHandler.java`
  将新增错误码映射到 403。
- 修改：`contracts/openapi/v21-api-catalog.yaml`
  同步 platform metadata 和 public share capability 合同。
- 修改：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21ApiContractCatalogTest.java`
  调整合同数量和 platform metadata 断言。
- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21AccessEntitlementGatesTest.java`
  新增本切片目标测试。
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`
  开始和完成时记录本切片状态。

## 任务 1：记录活动切片并编写失败测试

**文件：**
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`
- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21AccessEntitlementGatesTest.java`

- [x] **步骤 1：记录活动切片**

将 `## Active Backend Slice` 更新为：

```markdown
## Active Backend Slice

- Slice: `backend-v21-access-entitlement-gates`
- Status: `in_progress`
- Started: `2026-05-13`
- Scope: v2.1 access model, contract matcher, entitlement gate, MVC interceptor, public route security, tests
- Verification target: `BackendV21AccessEntitlementGatesTest`, backend compile, frontend v2.1 test suite
```

- [x] **步骤 2：创建失败测试**

创建 `BackendV21AccessEntitlementGatesTest.java`。测试先引用尚不存在的 access 模型与 server access 包：

```java
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
import java.util.Set;
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
                "GET /api/v2/platform/contracts",
                "GET /api/v2/platform/capabilities",
                "GET /api/v2/share/capabilities",
                "GET /api/v2/public-share/capabilities"
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
        assertThat(matcher.match("GET", "/api/v2/unknown")).isEmpty();
    }

    @Test
    void publicContractsShouldNotRequireAuthentication() {
        AccessDecision decision = gateService.evaluate(new AccessRequest(
                "GET",
                "/api/v2/share/pass/public-token",
                null,
                null,
                null,
                null,
                matcher.match("GET", "/api/v2/share/pass/public-token").orElseThrow()
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
                .contains("/api/v2/share/capabilities:")
                .contains("/api/v2/public-share/capabilities:");
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
```

- [x] **步骤 3：运行红灯测试**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21AccessEntitlementGatesTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：FAIL，编译报错包含 `package com.mmmail.platform.access does not exist` 和 `package com.mmmail.server.access does not exist`。

## 任务 2：实现 platform access 模型

**文件：**
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/access/AccessEntitlement.java`
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/access/AccessPermission.java`
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/access/AccessDecisionReason.java`
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/access/AccessDecision.java`
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/access/AccessRequest.java`
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/access/AccessGate.java`
- 修改：`backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiAccess.java`

- [x] **步骤 1：创建 `AccessEntitlement`**

```java
package com.mmmail.platform.access;

public enum AccessEntitlement {
    COMMUNITY("community"),
    PREMIUM("premium"),
    HOSTED("hosted"),
    ENTERPRISE_GOVERNANCE("enterprise-governance");

    private final String contractValue;

    AccessEntitlement(String contractValue) {
        this.contractValue = contractValue;
    }

    public String contractValue() {
        return contractValue;
    }

    public static AccessEntitlement fromContractValue(String value) {
        for (AccessEntitlement entitlement : values()) {
            if (entitlement.contractValue.equals(value)) {
                return entitlement;
            }
        }
        throw new IllegalArgumentException("Unknown access entitlement: " + value);
    }
}
```

- [x] **步骤 2：创建 `AccessPermission`**

```java
package com.mmmail.platform.access;

public record AccessPermission(String value) {

    public AccessPermission {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("permission is required");
        }
        value = value.trim();
        if (!value.contains(":")) {
            throw new IllegalArgumentException("permission must use module:action format");
        }
    }

    public boolean publicPermission() {
        return "auth:public".equals(value) || "share:public".equals(value) || "system:public".equals(value);
    }

    public static AccessPermission of(String value) {
        return new AccessPermission(value);
    }
}
```

- [x] **步骤 3：创建 `AccessDecisionReason`**

```java
package com.mmmail.platform.access;

public enum AccessDecisionReason {
    ALLOWED,
    PUBLIC_CONTRACT,
    AUTHENTICATION_REQUIRED,
    ENTITLEMENT_REQUIRED,
    PERMISSION_DENIED,
    UNKNOWN_CONTRACT
}
```

- [x] **步骤 4：创建 `AccessDecision`**

```java
package com.mmmail.platform.access;

import java.util.List;

public record AccessDecision(
        boolean allowed,
        AccessDecisionReason reason,
        int httpStatus,
        int errorCode,
        String message,
        AccessEntitlement requiredEntitlement,
        List<AccessPermission> requiredPermissions
) {

    public AccessDecision {
        if (reason == null) {
            throw new IllegalArgumentException("reason is required");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
        requiredPermissions = List.copyOf(requiredPermissions == null ? List.of() : requiredPermissions);
    }

    public static AccessDecision allowed(AccessDecisionReason reason, AccessEntitlement entitlement, List<AccessPermission> permissions) {
        return new AccessDecision(true, reason, 200, 0, "allowed", entitlement, permissions);
    }

    public static AccessDecision denied(
            AccessDecisionReason reason,
            int httpStatus,
            int errorCode,
            String message,
            AccessEntitlement entitlement,
            List<AccessPermission> permissions
    ) {
        return new AccessDecision(false, reason, httpStatus, errorCode, message, entitlement, permissions);
    }
}
```

- [x] **步骤 5：创建 `AccessRequest` 和 `AccessGate`**

```java
package com.mmmail.platform.access;

import com.mmmail.platform.contract.V21ApiContract;

public record AccessRequest(
        String method,
        String path,
        Long userId,
        String role,
        String orgId,
        String scopeId,
        V21ApiContract contract
) {

    public AccessRequest {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("method is required");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path is required");
        }
    }

    public boolean authenticated() {
        return userId != null;
    }

    public boolean admin() {
        return role != null && "ADMIN".equalsIgnoreCase(role);
    }
}
```

```java
package com.mmmail.platform.access;

public interface AccessGate {

    AccessDecision evaluate(AccessRequest request);
}
```

- [x] **步骤 6：强化 `V21ApiAccess` 校验**

将 `V21ApiAccess` 改为在构造时验证空权限和合法 entitlement：

```java
package com.mmmail.platform.contract;

import com.mmmail.platform.access.AccessEntitlement;
import com.mmmail.platform.access.AccessPermission;

import java.util.List;

public record V21ApiAccess(List<String> permissions, String entitlement) {

    public V21ApiAccess {
        if (permissions == null || permissions.isEmpty()) {
            throw new IllegalArgumentException("permissions are required");
        }
        permissions = permissions.stream()
                .map(AccessPermission::new)
                .map(AccessPermission::value)
                .toList();
        AccessEntitlement.fromContractValue(entitlement);
    }
}
```

- [x] **步骤 7：运行 platform 编译**

运行：

```bash
timeout 60s mvn -pl mmmail-platform -am -f backend/pom.xml compile
```

预期：BUILD SUCCESS。

## 任务 3：扩展合同目录与 matcher

**文件：**
- 修改：`backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiContractCatalog.java`
- 修改：`contracts/openapi/v21-api-catalog.yaml`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/access/V21ApiContractMatcher.java`
- 修改：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21ApiContractCatalogTest.java`

- [x] **步骤 1：将 platform metadata 和 share capability 纳入 `V21ApiContractCatalog`**

在 `defaultCatalog()` 的 `Stream.of` 列表中加入 `platformContracts()`，并在 `publicAuthShareSystemContracts()` 的 public-share 模块中加入 capability routes：

```java
private static List<V21ApiContract> platformContracts() {
    return module("platform", "docs/MMMail/UI/Admin", new String[][]{
            {"GET", "/api/v2/platform/contracts", "V21ApiContractCatalog", COMMUNITY, "platform:contracts:read"},
            {"GET", "/api/v2/platform/capabilities", "PlatformCapabilities", COMMUNITY, "platform:capabilities:read"}
    });
}
```

```java
module("public-share", "docs/MMMail/UI/首页", new String[][]{
        {"GET", "/api/v2/share/capabilities", "PublicShareCapabilities", COMMUNITY, "share:public"},
        {"GET", "/api/v2/public-share/capabilities", "PublicShareCapabilities", COMMUNITY, "share:public"},
        {"GET", "/api/v2/share/mail/:token", "PublicMailShare", COMMUNITY, "share:public"},
        {"GET", "/api/v2/share/drive/:token", "PublicDriveShareMetadata", COMMUNITY, "share:public"},
        {"GET", "/api/v2/share/pass/:token", "PublicPassShare", COMMUNITY, "share:public"}
})
```

- [x] **步骤 2：同步 OpenAPI 冻结文件**

在 `contracts/openapi/v21-api-catalog.yaml` 中加入：

```yaml
  /api/v2/platform/contracts:
    get: {summary: Read v2.1 platform contract catalog, x-permission: ["platform:contracts:read"], x-entitlement: community, x-design-source: docs/MMMail/UI/Admin, responses: {"200": {description: ok}}}
  /api/v2/platform/capabilities:
    get: {summary: Read v2.1 platform capabilities, x-permission: ["platform:capabilities:read"], x-entitlement: community, x-design-source: docs/MMMail/UI/Admin, responses: {"200": {description: ok}}}
  /api/v2/share/capabilities:
    get: {summary: Read public share capabilities, x-permission: ["share:public"], x-entitlement: community, x-design-source: docs/MMMail/UI/首页, responses: {"200": {description: ok}}}
  /api/v2/public-share/capabilities:
    get: {summary: Read public share compatibility capabilities, x-permission: ["share:public"], x-entitlement: community, x-design-source: docs/MMMail/UI/首页, responses: {"200": {description: ok}}}
```

- [x] **步骤 3：创建 `V21ApiContractMatcher`**

```java
package com.mmmail.server.access;

import com.mmmail.platform.contract.V21ApiContract;
import com.mmmail.platform.contract.V21ApiContractCatalog;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Component
public class V21ApiContractMatcher {

    private final List<V21ApiContract> contracts;

    public V21ApiContractMatcher() {
        this(V21ApiContractCatalog.defaultCatalog());
    }

    public V21ApiContractMatcher(V21ApiContractCatalog catalog) {
        this.contracts = catalog.contracts();
    }

    public Optional<V21ApiContract> match(String method, String path) {
        String normalizedMethod = normalizeMethod(method);
        String normalizedPath = normalizePath(path);
        if (normalizedMethod == null || normalizedPath == null) {
            return Optional.empty();
        }
        return contracts.stream()
                .filter(contract -> contract.method().equals(normalizedMethod))
                .filter(contract -> pathMatches(contract.path(), normalizedPath))
                .findFirst();
    }

    private static boolean pathMatches(String pattern, String path) {
        String[] patternSegments = split(pattern);
        String[] pathSegments = split(path);
        if (patternSegments.length != pathSegments.length) {
            return false;
        }
        for (int index = 0; index < patternSegments.length; index++) {
            if (!segmentMatches(patternSegments[index], pathSegments[index])) {
                return false;
            }
        }
        return true;
    }

    private static boolean segmentMatches(String patternSegment, String pathSegment) {
        return patternSegment.startsWith(":") || patternSegment.equals(pathSegment);
    }

    private static String[] split(String path) {
        String normalized = normalizePath(path);
        return normalized == null ? new String[0] : normalized.substring(1).split("/");
    }

    private static String normalizeMethod(String method) {
        return StringUtils.hasText(method) ? method.trim().toUpperCase() : null;
    }

    private static String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        String normalized = path.trim();
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }
}
```

- [x] **步骤 4：更新现有 API catalog 测试**

在 `BackendV21ApiContractCatalogTest` 中：

- 将 `REQUIRED_OWNER_MODULES` 增加 `"platform"`。
- 将 `authenticatedPlatformEndpointShouldExposeCatalog` 的 `contracts.length()` 从 `122` 改为 `126`。
- 增加断言：

```java
assertContract(contractsByIdentity, "GET /api/v2/platform/contracts", "platform", "V21ApiContractCatalog", "community");
assertContract(contractsByIdentity, "GET /api/v2/platform/capabilities", "platform", "PlatformCapabilities", "community");
assertContract(contractsByIdentity, "GET /api/v2/public-share/capabilities", "public-share", "PublicShareCapabilities", "community");
```

- [x] **步骤 5：运行目标测试确认仍有 gate 缺失失败**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：仍 FAIL，报错集中在 `V21ApiAccessGateService`、`AccessDecision` 等尚未完成的 gate 类型或行为。

## 任务 4：实现 gate service、interceptor 和安全规则

**文件：**
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/access/V21ApiEntitlementProvider.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/access/CommunityV21ApiEntitlementProvider.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/access/V21ApiAccessGateService.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/access/V21ApiAccessGateInterceptor.java`
- 修改：`backend/mmmail-server/src/main/java/com/mmmail/server/config/WebMvcConfig.java`
- 修改：`backend/mmmail-server/src/main/java/com/mmmail/server/config/SecurityConfig.java`
- 修改：`backend/mmmail-common/src/main/java/com/mmmail/common/exception/ErrorCode.java`
- 修改：`backend/mmmail-common/src/main/java/com/mmmail/common/exception/GlobalExceptionHandler.java`

- [x] **步骤 1：增加错误码**

在 `ErrorCode` 中 `AUTHENTICATOR_BACKUP_INVALID` 之后加入：

```java
V2_API_CONTRACT_NOT_FOUND(30051, "Unknown v2 API contract"),
V2_ENTITLEMENT_REQUIRED(30052, "Required entitlement is not enabled"),
V2_PERMISSION_DENIED(30053, "Required permission is not granted"),
```

在 `GlobalExceptionHandler.resolveStatus` 中把新 code 加入 403 分支：

```java
case 10003, 30013, 30045, 30046, 30047, 30051, 30052, 30053 -> HttpStatus.FORBIDDEN;
```

- [x] **步骤 2：创建 entitlement provider**

```java
package com.mmmail.server.access;

import com.mmmail.platform.access.AccessEntitlement;
import com.mmmail.platform.access.AccessRequest;

public interface V21ApiEntitlementProvider {

    boolean hasEntitlement(AccessRequest request, AccessEntitlement entitlement);
}
```

```java
package com.mmmail.server.access;

import com.mmmail.platform.access.AccessEntitlement;
import com.mmmail.platform.access.AccessRequest;
import org.springframework.stereotype.Component;

@Component
public class CommunityV21ApiEntitlementProvider implements V21ApiEntitlementProvider {

    @Override
    public boolean hasEntitlement(AccessRequest request, AccessEntitlement entitlement) {
        return entitlement == AccessEntitlement.COMMUNITY;
    }
}
```

- [x] **步骤 3：创建 `V21ApiAccessGateService`**

```java
package com.mmmail.server.access;

import com.mmmail.common.exception.ErrorCode;
import com.mmmail.platform.access.AccessDecision;
import com.mmmail.platform.access.AccessDecisionReason;
import com.mmmail.platform.access.AccessEntitlement;
import com.mmmail.platform.access.AccessGate;
import com.mmmail.platform.access.AccessPermission;
import com.mmmail.platform.access.AccessRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class V21ApiAccessGateService implements AccessGate {

    private static final int HTTP_OK = 200;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;

    private final V21ApiEntitlementProvider entitlementProvider;

    public V21ApiAccessGateService(V21ApiEntitlementProvider entitlementProvider) {
        this.entitlementProvider = entitlementProvider;
    }

    @Override
    public AccessDecision evaluate(AccessRequest request) {
        if (request.contract() == null) {
            return denyUnknownContract();
        }
        AccessEntitlement entitlement = AccessEntitlement.fromContractValue(request.contract().entitlement());
        List<AccessPermission> permissions = permissions(request);
        if (publicContract(permissions)) {
            return new AccessDecision(true, AccessDecisionReason.PUBLIC_CONTRACT, HTTP_OK, 0, "allowed", entitlement, permissions);
        }
        if (!request.authenticated()) {
            return AccessDecision.denied(
                    AccessDecisionReason.AUTHENTICATION_REQUIRED,
                    HTTP_UNAUTHORIZED,
                    ErrorCode.UNAUTHORIZED.getCode(),
                    ErrorCode.UNAUTHORIZED.getMessage(),
                    entitlement,
                    permissions
            );
        }
        if (!entitlementProvider.hasEntitlement(request, entitlement)) {
            return AccessDecision.denied(
                    AccessDecisionReason.ENTITLEMENT_REQUIRED,
                    HTTP_FORBIDDEN,
                    ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode(),
                    "Required entitlement is not enabled: " + entitlement.contractValue(),
                    entitlement,
                    permissions
            );
        }
        if (!permissionAllowed(request, entitlement)) {
            return AccessDecision.denied(
                    AccessDecisionReason.PERMISSION_DENIED,
                    HTTP_FORBIDDEN,
                    ErrorCode.V2_PERMISSION_DENIED.getCode(),
                    "Required permission is not granted: " + permissions.get(0).value(),
                    entitlement,
                    permissions
            );
        }
        return AccessDecision.allowed(AccessDecisionReason.ALLOWED, entitlement, permissions);
    }

    public AccessDecision denyUnknownContract() {
        return AccessDecision.denied(
                AccessDecisionReason.UNKNOWN_CONTRACT,
                HTTP_FORBIDDEN,
                ErrorCode.V2_API_CONTRACT_NOT_FOUND.getCode(),
                ErrorCode.V2_API_CONTRACT_NOT_FOUND.getMessage(),
                null,
                List.of()
        );
    }

    private static List<AccessPermission> permissions(AccessRequest request) {
        return request.contract().permissions().stream().map(AccessPermission::new).toList();
    }

    private static boolean publicContract(List<AccessPermission> permissions) {
        return permissions.stream().anyMatch(AccessPermission::publicPermission);
    }

    private static boolean permissionAllowed(AccessRequest request, AccessEntitlement entitlement) {
        return request.admin() || entitlement == AccessEntitlement.COMMUNITY;
    }
}
```

- [x] **步骤 4：创建 `V21ApiAccessGateInterceptor`**

```java
package com.mmmail.server.access;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.foundation.tenant.TenantScopeContext;
import com.mmmail.foundation.tenant.TenantScopeContextHolder;
import com.mmmail.platform.access.AccessDecision;
import com.mmmail.platform.access.AccessRequest;
import com.mmmail.platform.contract.V21ApiContract;
import com.mmmail.server.security.JwtPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class V21ApiAccessGateInterceptor implements HandlerInterceptor {

    private final V21ApiContractMatcher matcher;
    private final V21ApiAccessGateService gateService;

    public V21ApiAccessGateInterceptor(V21ApiContractMatcher matcher, V21ApiAccessGateService gateService) {
        this.matcher = matcher;
        this.gateService = gateService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        V21ApiContract contract = matcher.match(request.getMethod(), request.getRequestURI()).orElse(null);
        AccessDecision decision = gateService.evaluate(toAccessRequest(request, contract));
        if (decision.allowed()) {
            return true;
        }
        throw new BizException(errorCode(decision.errorCode()), decision.message());
    }

    private AccessRequest toAccessRequest(HttpServletRequest request, V21ApiContract contract) {
        JwtPrincipal principal = currentPrincipal();
        TenantScopeContext scope = TenantScopeContextHolder.get();
        return new AccessRequest(
                request.getMethod(),
                request.getRequestURI(),
                principal == null ? null : principal.userId(),
                principal == null ? null : principal.role(),
                scope == null ? null : scope.orgId(),
                scope == null ? null : scope.scopeId(),
                contract
        );
    }

    private static JwtPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof JwtPrincipal jwtPrincipal ? jwtPrincipal : null;
    }

    private static ErrorCode errorCode(int code) {
        for (ErrorCode value : ErrorCode.values()) {
            if (value.getCode() == code) {
                return value;
            }
        }
        return ErrorCode.FORBIDDEN;
    }
}
```

- [x] **步骤 5：注册 interceptor 顺序**

修改 `WebMvcConfig`：

```java
private final V21ApiAccessGateInterceptor v21ApiAccessGateInterceptor;
private final OrgProductAccessInterceptor orgProductAccessInterceptor;

public WebMvcConfig(
        V21ApiAccessGateInterceptor v21ApiAccessGateInterceptor,
        OrgProductAccessInterceptor orgProductAccessInterceptor
) {
    this.v21ApiAccessGateInterceptor = v21ApiAccessGateInterceptor;
    this.orgProductAccessInterceptor = orgProductAccessInterceptor;
}
```

在 `addInterceptors` 中先注册 v2.1 gate，再注册 org product access：

```java
registry.addInterceptor(v21ApiAccessGateInterceptor)
        .addPathPatterns("/api/v2/**");

registry.addInterceptor(orgProductAccessInterceptor)
        .addPathPatterns("/api/v1/**", "/api/v2/**")
        .excludePathPatterns(
                "/api/v1/auth/**",
                "/api/v1/orgs/**",
                "/api/v1/public/**",
                "/api/v1/settings/**",
                "/api/v1/suite/**",
                "/api/v1/audit/**",
                "/api/v2/auth/**",
                "/api/v2/share/**",
                "/api/v2/public-share/**",
                "/api/v2/system/status",
                "/actuator/**",
                "/v3/api-docs/**",
                "/swagger-ui/**"
        );
```

- [x] **步骤 6：同步 Spring Security permit 规则**

在 `SecurityConfig.securityFilterChain` 的 public matcher 中保留已有 v1 public routes，并加入：

```java
"/api/v2/auth/login",
"/api/v2/auth/register",
"/api/v2/share/capabilities",
"/api/v2/share/mail/**",
"/api/v2/share/drive/**",
"/api/v2/share/pass/**",
"/api/v2/public-share/capabilities",
"/api/v2/system/status",
```

不要移除现有 `/api/v2/public-share/**` 兼容 matcher，直到 PublicShareCapabilityIntegrationTest 更新为 canonical route。

- [x] **步骤 7：运行 gate 目标测试**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest,PublicShareCapabilityIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：PASS。

## 任务 5：验证并提交实现

**文件：**
- 所有任务 1-4 的源码、合同、测试文件

- [x] **步骤 1：运行后端目标测试**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest,PublicShareCapabilityIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：BUILD SUCCESS，目标测试全部通过。

- [x] **步骤 2：运行后端编译**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml compile
```

预期：BUILD SUCCESS。

- [x] **步骤 3：运行前端 v2.1 回归**

运行：

```bash
timeout 60s pnpm --dir frontend-v2 test
```

预期：`# pass 83`，失败数为 0。

- [x] **步骤 4：提交实现**

先检查工作树：

```bash
git status --short --branch
```

只暂存本切片实现文件：

```bash
git add \
  backend/mmmail-platform/src/main/java/com/mmmail/platform/access/AccessEntitlement.java \
  backend/mmmail-platform/src/main/java/com/mmmail/platform/access/AccessPermission.java \
  backend/mmmail-platform/src/main/java/com/mmmail/platform/access/AccessDecisionReason.java \
  backend/mmmail-platform/src/main/java/com/mmmail/platform/access/AccessDecision.java \
  backend/mmmail-platform/src/main/java/com/mmmail/platform/access/AccessRequest.java \
  backend/mmmail-platform/src/main/java/com/mmmail/platform/access/AccessGate.java \
  backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiAccess.java \
  backend/mmmail-platform/src/main/java/com/mmmail/platform/contract/V21ApiContractCatalog.java \
  backend/mmmail-server/src/main/java/com/mmmail/server/access/V21ApiContractMatcher.java \
  backend/mmmail-server/src/main/java/com/mmmail/server/access/V21ApiEntitlementProvider.java \
  backend/mmmail-server/src/main/java/com/mmmail/server/access/CommunityV21ApiEntitlementProvider.java \
  backend/mmmail-server/src/main/java/com/mmmail/server/access/V21ApiAccessGateService.java \
  backend/mmmail-server/src/main/java/com/mmmail/server/access/V21ApiAccessGateInterceptor.java \
  backend/mmmail-server/src/main/java/com/mmmail/server/config/WebMvcConfig.java \
  backend/mmmail-server/src/main/java/com/mmmail/server/config/SecurityConfig.java \
  backend/mmmail-common/src/main/java/com/mmmail/common/exception/ErrorCode.java \
  backend/mmmail-common/src/main/java/com/mmmail/common/exception/GlobalExceptionHandler.java \
  contracts/openapi/v21-api-catalog.yaml \
  backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21ApiContractCatalogTest.java \
  backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21AccessEntitlementGatesTest.java
git diff --cached --check
git diff --cached --stat
git commit -m "feat(backend-v21): add access entitlement gates"
```

## 任务 6：更新进度并提交文档

**文件：**
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`
- 修改：`docs/superpowers/plans/2026-05-13-backend-v21-access-entitlement-gates.md`

- [x] **步骤 1：更新进度文档**

更新：

- `Last updated: 2026-05-13`
- `Latest backend implementation commit` 写入 `git log --oneline -1` 返回的实现提交短哈希和 subject。
- `Local branch status at progress capture` 写入 `git status --short --branch | head -n 1` 返回的分支状态。
- 在 Completed v2.1 Slices 表中加入：

```markdown
| Backend access entitlement gates (`backend-v21-access-entitlement-gates`) | `BackendV21AccessEntitlementGatesTest`, `AccessGate`, `V21ApiContractMatcher`, `V21ApiAccessGateInterceptor` |
```

将 Latest Completed Backend Slice 更新为：

```markdown
## Latest Completed Backend Slice

- Slice: `backend-v21-access-entitlement-gates`
- Commit: 写入 `git log --oneline -1` 返回的实现提交短哈希和 subject。
- Files changed: added immutable platform access models, v2.1 contract matching, Community entitlement provider, access gate service, MVC interceptor, public route security alignment, OpenAPI contract metadata, and focused Spring Boot coverage.
- Verification:
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest,PublicShareCapabilityIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false`: PASS
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml compile`: PASS
  - `timeout 60s pnpm --dir frontend-v2 test`: PASS (`83/83`)
```

将 Active Backend Slice 更新为 completed：

```markdown
## Active Backend Slice

- Slice: `backend-v21-access-entitlement-gates`
- Status: `completed`
- Started: `2026-05-13`
- Completed: `2026-05-13`
- Scope: v2.1 access model, contract matcher, entitlement gate, MVC interceptor, public route security, tests
- Verification target: `BackendV21AccessEntitlementGatesTest`, backend compile, frontend v2.1 test suite
```

- [x] **步骤 2：勾选计划任务**

将本计划中已经完成的复选框从 `- [ ]` 改为 `- [x]`。

- [x] **步骤 3：提交进度**

因为 `docs/superpowers` 被 ignore，使用精确 `git add -f`：

```bash
git add -f \
  docs/superpowers/progress/v21-implementation-progress.md \
  docs/superpowers/plans/2026-05-13-backend-v21-access-entitlement-gates.md
git diff --cached --check
git diff --cached --stat
git commit -m "docs(backend-v21): update access entitlement gate progress"
git status --short --branch
```
