# Backend v2.1 Pass Runtime Bridge 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 为 v2.1 Pass 增加真实 `/api/v2/pass/*` 运行时桥接，让 Community Pass item 路径走后端真实状态，并保持 Premium Pass 路径由 v2 access gate 显式拦截。

**架构：** 新增 `V21PassController` 作为 v2 HTTP 入口，新增 `V21PassRuntimeBridgeService` 承担 v2 adapter 编排并复用现有 `PassService`、`PassBusinessService`、`PassAliasService`、`PassMonitorService`。只新增最小 v2 DTO/VO 处理 vault 输出和 secure-link org/item 上下文，不改写 Pass 领域规则，不添加 mock/fake success。

**技术栈：** Java 21、Spring Boot 3.5、MockMvc、JUnit 5、MyBatis Plus、Vue 3 frontend-v2 contract tests、Maven、pnpm。

---

## 参考资料

- 规格：`docs/superpowers/specs/2026-05-13-backend-v21-pass-runtime-bridge-design.md`
- 主方案：`docs/superpowers/specs/2026-04-28-frontend-v21-ui-upgrade-design.md`，section 14.7
- 前端 API：`frontend-v2/src/service/api/pass.ts`
- v1 controller：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/PassController.java`
- v1 business controller：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/PassBusinessController.java`
- existing v2 examples：`V21DriveController`、`V21MailController`

## 文件结构

- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21PassRuntimeBridgeTest.java`
  - v2 Pass runtime 红绿测试，覆盖 Community item/vault 成功路径、Premium gate 和 invalid id。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21PassController.java`
  - `/api/v2/pass` HTTP adapter，保持 controller 方法短小。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/service/V21PassRuntimeBridgeService.java`
  - v2 Pass runtime bridge 编排层，复用现有 Pass 服务，负责 v2 vault 输出、detail-to-summary 映射、id/query 校验。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21PassSecureLinkRequest.java`
  - `POST /api/v2/pass/share` 和 `POST /api/v2/pass/secure-links` 的 v2 请求体。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21PassSecureLinkQuery.java`
  - `GET /api/v2/pass/secure-links` 与 `DELETE /secure-links/:id` 的 org query 绑定。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21PassVaultVo.java`
  - v2 Pass vault rail 输出。
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`
  - 实现完成后记录切片、提交号和验证证据。
- 修改：`docs/superpowers/plans/2026-05-13-backend-v21-pass-runtime-bridge.md`
  - 实现完成后勾选步骤并记录实际验证证据。

## 任务 1：新增 v2 Pass runtime 红测

**文件：**
- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21PassRuntimeBridgeTest.java`
- 测试：`BackendV21PassRuntimeBridgeTest`

- [ ] **步骤 1：创建失败测试文件**

写入 `backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21PassRuntimeBridgeTest.java`：

```java
package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendV21PassRuntimeBridgeTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void v21PassShouldUseRuntimePersonalVaultAndItemState() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String email = "v21-pass-owner-" + suffix + "@mmmail.local";
        String token = register(email, "V21 Pass Owner");

        assertPersonalVault(token, email, 0);
        String itemId = createV21Item(token, "Root Console", "LOGIN", "console.mmmail.local", "root@mmmail.local");

        mockMvc.perform(get("/api/v2/pass/items")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", "Root"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(itemId))
                .andExpect(jsonPath("$.data[0].title").value("Root Console"))
                .andExpect(jsonPath("$.data[0].itemType").value("LOGIN"))
                .andExpect(jsonPath("$.data[0].scopeType").value("PERSONAL"));

        assertPersonalVault(token, email, 1);

        mockMvc.perform(patch("/api/v2/pass/items/" + itemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Root Console Updated",
                                  "itemType": "NOTE",
                                  "website": "console.mmmail.local",
                                  "username": "root@mmmail.local",
                                  "secretCiphertext": "ciphertext-updated",
                                  "note": "Updated through v2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(itemId))
                .andExpect(jsonPath("$.data.title").value("Root Console Updated"))
                .andExpect(jsonPath("$.data.itemType").value("NOTE"));
    }

    @Test
    void v21PassShouldKeepPremiumPathsBehindAccessGate() throws Exception {
        String token = register("v21-pass-gate-" + System.nanoTime() + "@mmmail.local", "V21 Pass Gate");

        assertPremiumGate(token, "/api/v2/pass/monitor");
        assertPremiumGate(token, "/api/v2/pass/secure-links");
        assertPremiumGate(token, "/api/v2/pass/aliases");
    }

    @Test
    void v21PassShouldRejectInvalidItemIds() throws Exception {
        String token = register("v21-pass-invalid-" + System.nanoTime() + "@mmmail.local", "V21 Pass Invalid");

        mockMvc.perform(patch("/api/v2/pass/items/not-a-number")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Invalid",
                                  "itemType": "LOGIN",
                                  "secretCiphertext": "ciphertext"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()));
    }

    private void assertPersonalVault(String token, String ownerEmail, int itemCount) throws Exception {
        mockMvc.perform(get("/api/v2/pass/vaults")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("personal"))
                .andExpect(jsonPath("$.data[0].name").value("Personal"))
                .andExpect(jsonPath("$.data[0].scopeType").value("PERSONAL"))
                .andExpect(jsonPath("$.data[0].ownerEmail").value(ownerEmail))
                .andExpect(jsonPath("$.data[0].itemCount").value(itemCount));
    }

    private String createV21Item(
            String token,
            String title,
            String itemType,
            String website,
            String username
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v2/pass/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "itemType": "%s",
                                  "website": "%s",
                                  "username": "%s",
                                  "secretCiphertext": "ciphertext",
                                  "note": "Created through v2"
                                }
                                """.formatted(title, itemType, website, username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value(title))
                .andExpect(jsonPath("$.data.itemType").value(itemType))
                .andExpect(jsonPath("$.data.scopeType").value("PERSONAL"))
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private void assertPremiumGate(String token, String path) throws Exception {
        mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode()));
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

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
```

- [ ] **步骤 2：运行红测并确认失败**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21PassRuntimeBridgeTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：FAIL。失败应来自 `/api/v2/pass/vaults`、`/api/v2/pass/items` 或 `PATCH /api/v2/pass/items/{id}` 尚未有 handler，而不是测试编译失败。

## 任务 2：新增 v2 Pass adapter 类型

**文件：**
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21PassVaultVo.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21PassSecureLinkRequest.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21PassSecureLinkQuery.java`
- 测试：`BackendV21ApiContractCatalogTest`

- [ ] **步骤 1：创建 vault VO**

写入 `backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21PassVaultVo.java`：

```java
package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record V21PassVaultVo(
        String id,
        String name,
        String scopeType,
        String ownerEmail,
        int itemCount,
        LocalDateTime updatedAt
) {
}
```

- [ ] **步骤 2：创建 secure-link 请求 DTO**

写入 `backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21PassSecureLinkRequest.java`：

```java
package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record V21PassSecureLinkRequest(
        @NotNull Long orgId,
        @NotNull Long itemId,
        @Min(1) @Max(1000) Integer maxViews,
        LocalDateTime expiresAt
) {
    public CreatePassSecureLinkRequest toCreateRequest() {
        return new CreatePassSecureLinkRequest(maxViews, expiresAt);
    }
}
```

- [ ] **步骤 3：创建 secure-link query DTO**

写入 `backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21PassSecureLinkQuery.java`：

```java
package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotNull;

public record V21PassSecureLinkQuery(@NotNull Long orgId) {
}
```

- [ ] **步骤 4：运行编译范围测试**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21ApiContractCatalogTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：PASS。新增 adapter 类型不应影响现有 contract catalog。

## 任务 3：实现 `V21PassRuntimeBridgeService`

**文件：**
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/service/V21PassRuntimeBridgeService.java`
- 测试：`BackendV21PassRuntimeBridgeTest`

- [ ] **步骤 1：创建 runtime bridge service**

写入 `backend/mmmail-server/src/main/java/com/mmmail/server/service/V21PassRuntimeBridgeService.java`：

```java
package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.PassVaultItemMapper;
import com.mmmail.server.model.dto.CreatePassItemRequest;
import com.mmmail.server.model.dto.UpdatePassAliasRequest;
import com.mmmail.server.model.dto.UpdatePassItemRequest;
import com.mmmail.server.model.dto.V21PassSecureLinkRequest;
import com.mmmail.server.model.entity.PassVaultItem;
import com.mmmail.server.model.vo.PassItemDetailVo;
import com.mmmail.server.model.vo.PassItemSummaryVo;
import com.mmmail.server.model.vo.PassMailAliasVo;
import com.mmmail.server.model.vo.PassMonitorOverviewVo;
import com.mmmail.server.model.vo.PassSecureLinkDashboardVo;
import com.mmmail.server.model.vo.PassSecureLinkVo;
import com.mmmail.server.model.vo.V21PassVaultVo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class V21PassRuntimeBridgeService {

    private static final String PERSONAL_VAULT_ID = "personal";
    private static final String PERSONAL_VAULT_NAME = "Personal";
    private static final String INVALID_PASS_ID = "Pass id is invalid";

    private final PassService passService;
    private final PassBusinessService passBusinessService;
    private final PassAliasService passAliasService;
    private final PassMonitorService passMonitorService;
    private final PassVaultItemMapper passVaultItemMapper;

    public V21PassRuntimeBridgeService(
            PassService passService,
            PassBusinessService passBusinessService,
            PassAliasService passAliasService,
            PassMonitorService passMonitorService,
            PassVaultItemMapper passVaultItemMapper
    ) {
        this.passService = passService;
        this.passBusinessService = passBusinessService;
        this.passAliasService = passAliasService;
        this.passMonitorService = passMonitorService;
        this.passVaultItemMapper = passVaultItemMapper;
    }

    public List<V21PassVaultVo> listVaults(Long userId, String ownerEmail) {
        Long itemCount = passVaultItemMapper.selectCount(personalItemQuery(userId));
        PassVaultItem latestItem = passVaultItemMapper.selectOne(personalItemQuery(userId)
                .orderByDesc(PassVaultItem::getUpdatedAt)
                .last("limit 1"));
        LocalDateTime updatedAt = latestItem == null ? null : latestItem.getUpdatedAt();
        return List.of(new V21PassVaultVo(
                PERSONAL_VAULT_ID,
                PERSONAL_VAULT_NAME,
                PassBusinessConstants.SCOPE_PERSONAL,
                ownerEmail,
                Math.toIntExact(itemCount),
                updatedAt
        ));
    }

    public List<PassItemSummaryVo> listItems(
            Long userId,
            String keyword,
            Boolean favoriteOnly,
            Integer limit,
            String itemType
    ) {
        return passService.list(userId, keyword, favoriteOnly, limit, itemType);
    }

    public PassItemSummaryVo createItem(Long userId, CreatePassItemRequest request, String ipAddress) {
        return toSummary(passService.create(
                userId,
                request.title(),
                request.itemType(),
                request.website(),
                request.username(),
                request.secretCiphertext(),
                request.note(),
                ipAddress
        ));
    }

    public PassItemSummaryVo updateItem(
            Long userId,
            String itemId,
            UpdatePassItemRequest request,
            String ipAddress
    ) {
        return toSummary(passService.update(
                userId,
                parseId(itemId),
                request.title(),
                request.itemType(),
                request.website(),
                request.username(),
                request.secretCiphertext(),
                request.note(),
                ipAddress
        ));
    }

    public PassMonitorOverviewVo readMonitor(Long userId, String ipAddress) {
        return passMonitorService.getPersonalMonitor(userId, ipAddress);
    }

    public List<PassMailAliasVo> listAliases(Long userId, String ipAddress) {
        return passAliasService.listAliases(userId, ipAddress);
    }

    public PassMailAliasVo updateAlias(
            Long userId,
            String aliasId,
            UpdatePassAliasRequest request,
            String ipAddress
    ) {
        return passAliasService.updateAlias(userId, parseId(aliasId), request, ipAddress);
    }

    public List<PassSecureLinkDashboardVo> listSecureLinks(
            Long userId,
            Long orgId,
            String publicBaseUrl,
            String ipAddress
    ) {
        return passBusinessService.listOrgSecureLinks(userId, orgId, publicBaseUrl, ipAddress);
    }

    public PassSecureLinkVo createSecureLink(
            Long userId,
            V21PassSecureLinkRequest request,
            String publicBaseUrl,
            String ipAddress
    ) {
        return passBusinessService.createSecureLink(
                userId,
                request.orgId(),
                request.itemId(),
                request.toCreateRequest(),
                publicBaseUrl,
                ipAddress
        );
    }

    public void revokeSecureLink(
            Long userId,
            Long orgId,
            String linkId,
            String publicBaseUrl,
            String ipAddress
    ) {
        passBusinessService.revokeSecureLink(userId, orgId, parseId(linkId), publicBaseUrl, ipAddress);
    }

    private PassItemSummaryVo toSummary(PassItemDetailVo detail) {
        return new PassItemSummaryVo(
                detail.id(),
                detail.title(),
                detail.website(),
                detail.username(),
                detail.favorite(),
                detail.updatedAt(),
                detail.scopeType(),
                detail.itemType(),
                detail.sharedVaultId(),
                detail.secureLinkCount()
        );
    }

    private Long parseId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, INVALID_PASS_ID);
        }
    }

    private LambdaQueryWrapper<PassVaultItem> personalItemQuery(Long userId) {
        return new LambdaQueryWrapper<PassVaultItem>()
                .eq(PassVaultItem::getOwnerId, userId)
                .eq(PassVaultItem::getScopeType, PassBusinessConstants.SCOPE_PERSONAL);
    }
}
```

- [ ] **步骤 2：运行红测确认仍失败于 controller 缺失**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21PassRuntimeBridgeTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：FAIL。新增 service 编译通过，失败原因仍是 `/api/v2/pass/*` controller 未实现。

## 任务 4：实现 `V21PassController`

**文件：**
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21PassController.java`
- 测试：`BackendV21PassRuntimeBridgeTest`

- [ ] **步骤 1：创建 v2 controller**

写入 `backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21PassController.java`：

```java
package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreatePassItemRequest;
import com.mmmail.server.model.dto.UpdatePassAliasRequest;
import com.mmmail.server.model.dto.UpdatePassItemRequest;
import com.mmmail.server.model.dto.V21PassSecureLinkQuery;
import com.mmmail.server.model.dto.V21PassSecureLinkRequest;
import com.mmmail.server.model.vo.PassItemSummaryVo;
import com.mmmail.server.model.vo.PassMailAliasVo;
import com.mmmail.server.model.vo.PassMonitorOverviewVo;
import com.mmmail.server.model.vo.PassSecureLinkDashboardVo;
import com.mmmail.server.model.vo.PassSecureLinkVo;
import com.mmmail.server.model.vo.V21PassVaultVo;
import com.mmmail.server.security.JwtPrincipal;
import com.mmmail.server.service.PublicBaseUrlResolver;
import com.mmmail.server.service.V21PassRuntimeBridgeService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v2/pass")
public class V21PassController {

    private final V21PassRuntimeBridgeService passRuntimeBridgeService;
    private final PublicBaseUrlResolver publicBaseUrlResolver;

    public V21PassController(
            V21PassRuntimeBridgeService passRuntimeBridgeService,
            PublicBaseUrlResolver publicBaseUrlResolver
    ) {
        this.passRuntimeBridgeService = passRuntimeBridgeService;
        this.publicBaseUrlResolver = publicBaseUrlResolver;
    }

    @GetMapping("/vaults")
    public Result<List<V21PassVaultVo>> vaults() {
        JwtPrincipal principal = SecurityUtils.currentPrincipal();
        return Result.success(passRuntimeBridgeService.listVaults(principal.userId(), principal.email()));
    }

    @GetMapping("/items")
    public Result<List<PassItemSummaryVo>> items(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "false") Boolean favoriteOnly,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String itemType
    ) {
        return Result.success(passRuntimeBridgeService.listItems(
                SecurityUtils.currentUserId(),
                keyword,
                favoriteOnly,
                limit,
                itemType
        ));
    }

    @PostMapping("/items")
    public Result<PassItemSummaryVo> createItem(
            @Valid @RequestBody CreatePassItemRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passRuntimeBridgeService.createItem(
                SecurityUtils.currentUserId(),
                request,
                ip(httpRequest)
        ));
    }

    @PatchMapping("/items/{itemId}")
    public Result<PassItemSummaryVo> updateItem(
            @PathVariable String itemId,
            @Valid @RequestBody UpdatePassItemRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passRuntimeBridgeService.updateItem(
                SecurityUtils.currentUserId(),
                itemId,
                request,
                ip(httpRequest)
        ));
    }

    @PostMapping("/share")
    public Result<PassSecureLinkVo> share(
            @Valid @RequestBody V21PassSecureLinkRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passRuntimeBridgeService.createSecureLink(
                SecurityUtils.currentUserId(),
                request,
                publicBaseUrlResolver.resolve(httpRequest),
                ip(httpRequest)
        ));
    }

    @GetMapping("/secure-links")
    public Result<List<PassSecureLinkDashboardVo>> secureLinks(
            @Valid @ModelAttribute V21PassSecureLinkQuery query,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passRuntimeBridgeService.listSecureLinks(
                SecurityUtils.currentUserId(),
                query.orgId(),
                publicBaseUrlResolver.resolve(httpRequest),
                ip(httpRequest)
        ));
    }

    @PostMapping("/secure-links")
    public Result<PassSecureLinkVo> createSecureLink(
            @Valid @RequestBody V21PassSecureLinkRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passRuntimeBridgeService.createSecureLink(
                SecurityUtils.currentUserId(),
                request,
                publicBaseUrlResolver.resolve(httpRequest),
                ip(httpRequest)
        ));
    }

    @DeleteMapping("/secure-links/{linkId}")
    public Result<Void> deleteSecureLink(
            @PathVariable String linkId,
            @Valid @ModelAttribute V21PassSecureLinkQuery query,
            HttpServletRequest httpRequest
    ) {
        passRuntimeBridgeService.revokeSecureLink(
                SecurityUtils.currentUserId(),
                query.orgId(),
                linkId,
                publicBaseUrlResolver.resolve(httpRequest),
                ip(httpRequest)
        );
        return Result.success(null);
    }

    @GetMapping("/aliases")
    public Result<List<PassMailAliasVo>> aliases(HttpServletRequest httpRequest) {
        return Result.success(passRuntimeBridgeService.listAliases(
                SecurityUtils.currentUserId(),
                ip(httpRequest)
        ));
    }

    @PatchMapping("/aliases/{aliasId}")
    public Result<PassMailAliasVo> updateAlias(
            @PathVariable String aliasId,
            @Valid @RequestBody UpdatePassAliasRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(passRuntimeBridgeService.updateAlias(
                SecurityUtils.currentUserId(),
                aliasId,
                request,
                ip(httpRequest)
        ));
    }

    @GetMapping("/monitor")
    public Result<PassMonitorOverviewVo> monitor(HttpServletRequest httpRequest) {
        return Result.success(passRuntimeBridgeService.readMonitor(
                SecurityUtils.currentUserId(),
                ip(httpRequest)
        ));
    }

    private String ip(HttpServletRequest httpRequest) {
        return httpRequest.getRemoteAddr();
    }
}
```

- [ ] **步骤 2：运行 v2 Pass 测试确认转绿**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21PassRuntimeBridgeTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：PASS，`BackendV21PassRuntimeBridgeTest` 3 个测试全部通过。

## 任务 5：运行 Pass 与 v2 gate/catalog 回归

**文件：**
- 测试：`PassReleaseBlockingIntegrationTest`
- 测试：`PassMonitorIntegrationTest`
- 测试：`PassAliasIntegrationTest`
- 测试：`PassBusinessIntegrationTest`
- 测试：`BackendV21AccessEntitlementGatesTest`
- 测试：`BackendV21ApiContractCatalogTest`

- [ ] **步骤 1：运行 Pass 相关回归**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=PassReleaseBlockingIntegrationTest,PassMonitorIntegrationTest,PassAliasIntegrationTest,PassBusinessIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：PASS。个人 item、monitor、alias、business secure-link 等 v1 Pass 行为不回归。

- [ ] **步骤 2：运行 v2 gate/catalog 回归**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：PASS。Premium Pass 端点仍由 v2 access gate 判定，catalog metadata 不回归。

## 任务 6：运行 frontend-v2 回归

**文件：**
- 测试：`frontend-v2/tests/pass-workspace-contract.test.mjs`
- 测试：`frontend-v2/tests/v21-pass-notifications-command-center-contract.test.mjs`
- 测试：`frontend-v2` 全量测试、类型检查、构建

- [ ] **步骤 1：运行 frontend-v2 测试**

运行：

```bash
pnpm --dir frontend-v2 test
```

预期：PASS，包含 Pass API contract 测试。

- [ ] **步骤 2：运行 frontend-v2 typecheck**

运行：

```bash
pnpm --dir frontend-v2 typecheck
```

预期：PASS。

- [ ] **步骤 3：运行 frontend-v2 build**

运行：

```bash
pnpm --dir frontend-v2 build
```

预期：PASS。

## 任务 7：提交 Pass runtime bridge 实现

**文件：**
- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21PassRuntimeBridgeTest.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21PassController.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/service/V21PassRuntimeBridgeService.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21PassSecureLinkRequest.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21PassSecureLinkQuery.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21PassVaultVo.java`

- [ ] **步骤 1：检查工作树**

运行：

```bash
git status --short --branch
```

预期：只看到本任务相关源码、测试文件和既有无关未跟踪路径。

- [ ] **步骤 2：暂存本任务相关文件**

运行：

```bash
git add backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21PassRuntimeBridgeTest.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21PassController.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/service/V21PassRuntimeBridgeService.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21PassSecureLinkRequest.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21PassSecureLinkQuery.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21PassVaultVo.java
git diff --cached --check
git diff --cached --stat
```

预期：`git diff --cached --check` 无输出，stat 只包含本任务列出的六个源码和测试文件。

- [ ] **步骤 3：提交实现**

运行：

```bash
git commit -m "feat(backend-v21): add pass runtime bridge"
```

预期：提交成功，提交内容只包含 Pass v2 runtime bridge 源码和测试。

## 任务 8：更新 v2.1 进度记录

**文件：**
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`
- 修改：`docs/superpowers/plans/2026-05-13-backend-v21-pass-runtime-bridge.md`

- [ ] **步骤 1：更新完成切片表**

在 `docs/superpowers/progress/v21-implementation-progress.md` 的 `Completed v2.1 Slices` 表中新增：

```markdown
| Backend Pass runtime bridge (`backend-v21-pass-runtime-bridge`) | `BackendV21PassRuntimeBridgeTest`, `V21PassController`, `V21PassRuntimeBridgeService` |
```

- [ ] **步骤 2：更新 Latest Completed Backend Slice**

将 `Latest Completed Backend Slice` 改成：

```markdown
## Latest Completed Backend Slice

- Slice: `backend-v21-pass-runtime-bridge`
- Commit: 使用任务 7 完成后 `git log --oneline -1` 输出的实现提交，提交主题必须是 `feat(backend-v21): add pass runtime bridge`
- Files changed: added v2 Pass controller, runtime bridge service, v2 Pass vault and secure-link adapters, runtime bridge coverage for personal vaults/items, Premium Pass gates, and invalid id handling.
- Verification:
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21PassRuntimeBridgeTest -Dsurefire.failIfNoSpecifiedTests=false`: PASS
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=PassReleaseBlockingIntegrationTest,PassMonitorIntegrationTest,PassAliasIntegrationTest,PassBusinessIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false`: PASS
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest -Dsurefire.failIfNoSpecifiedTests=false`: PASS
  - `pnpm --dir frontend-v2 test`: PASS
  - `pnpm --dir frontend-v2 typecheck`: PASS
  - `pnpm --dir frontend-v2 build`: PASS
```

提交进度文档前必须把这一行改成任务 7 产生的实际提交号和提交主题。

- [ ] **步骤 3：更新 Active Backend Slice**

将 `Active Backend Slice` 改成：

```markdown
## Active Backend Slice

- Slice: `backend-v21-pass-runtime-bridge`
- Status: `completed`
- Started: `2026-05-13`
- Completed: `2026-05-13`
- Scope: v2 Pass runtime bridge for personal vaults/items, Premium Pass gates, invalid id handling, and v2 adapter types
- Verification:
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21PassRuntimeBridgeTest -Dsurefire.failIfNoSpecifiedTests=false`
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=PassReleaseBlockingIntegrationTest,PassMonitorIntegrationTest,PassAliasIntegrationTest,PassBusinessIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false`
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest -Dsurefire.failIfNoSpecifiedTests=false`
  - `pnpm --dir frontend-v2 test`
  - `pnpm --dir frontend-v2 typecheck`
  - `pnpm --dir frontend-v2 build`
```

- [ ] **步骤 4：记录实际执行状态**

在本计划文档顶部加入：

```markdown
**执行状态：** completed on 2026-05-13.

**实现提交：** 使用任务 7 产生的实际提交号和提交主题。

**实际验证证据：**
- 写入本计划中实际执行过的每条验证命令及 PASS 结果。
```

提交进度文档前，必须把上方两行改成实际提交号、实际验证命令和实际结果。

- [ ] **步骤 5：提交进度文档**

运行：

```bash
git status --short --branch
git add -f docs/superpowers/progress/v21-implementation-progress.md docs/superpowers/plans/2026-05-13-backend-v21-pass-runtime-bridge.md
git diff --cached --check
git diff --cached --stat
git commit -m "docs(backend-v21): update pass runtime bridge progress"
```

预期：提交成功，提交内容只包含进度文档和计划执行状态。

## 任务 9：合并回本地 main 并清理 worktree

**文件：**
- 测试：git 状态和最近提交

- [ ] **步骤 1：确认功能分支最新提交**

运行：

```bash
git log --oneline -5
git status --short --branch
```

预期：最新提交包含：

```text
docs(backend-v21): update pass runtime bridge progress
feat(backend-v21): add pass runtime bridge
```

工作树干净。

- [ ] **步骤 2：在主工作区 fast-forward 合并**

在仓库主工作区运行：

```bash
git status --short --branch
git merge --ff-only backend-v21-pass-runtime-bridge
git status --short --branch
```

预期：本地 `main` fast-forward 到最新进度提交；只保留既有无关未跟踪路径。

- [ ] **步骤 3：清理临时 worktree 和分支**

在仓库主工作区运行：

```bash
git worktree remove .worktrees/backend-v21-pass-runtime-bridge
git branch -d backend-v21-pass-runtime-bridge
git worktree list
```

预期：`backend-v21-pass-runtime-bridge` worktree 和本地临时分支均已删除。

## 计划自检

- 规格覆盖度：本计划覆盖规格中的 controller、runtime bridge service、vault VO、secure-link request/query、Community item/vault 成功路径、Premium gate、invalid id、回归验证、进度记录和 main 合并清理。
- 待替换文本扫描：计划未保留需要先补充才能执行的章节或尖括号文本；进度更新任务要求执行者写入实际提交号和实际验证命令。
- 类型一致性：`V21PassSecureLinkRequest.toCreateRequest()` 返回现有 `CreatePassSecureLinkRequest`；`V21PassRuntimeBridgeService.updateItem()` 使用字符串 path id 并内部解析；`V21PassController` 返回前端已有 `PassItemSummaryVo`、`PassMonitorOverviewVo`、`PassMailAliasVo`、`PassSecureLinkVo` 形状。
