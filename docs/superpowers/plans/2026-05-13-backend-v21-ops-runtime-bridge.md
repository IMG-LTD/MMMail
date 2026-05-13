# Backend v2.1 Ops Runtime Bridge 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 将 v2.1 Collaboration、Notifications、Command Center 的 Community 路由接到真实 Suite 运行时服务，并保持 Premium 路由由现有 v2 entitlement gate 阻断。

**架构：** 新增一个 v2 operations bridge controller 和一个专用 bridge service。Controller 只负责 `/api/v2/collaboration/*`、`/api/v2/notifications/*`、`/api/v2/command-center/*` 的路由、认证上下文和 HTTP 参数绑定；service 负责调用现有 Suite 服务并映射 v2 前端契约记录。所有写入能力只在现有真实服务支持时开放，不支持的 Community 写路由返回显式 `INVALID_ARGUMENT`。

**技术栈：** Java 21, Spring Boot, MockMvc, JUnit 5, MyBatis-Plus, Maven, Vue 3 frontend contract tests, pnpm.

---

## 文件结构

- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21OpsRuntimeBridgeTest.java`
  - 覆盖 v2 ops Community 读路由、通知 read-state 变更、unsupported 写路由、Premium entitlement gate。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21OpsController.java`
  - 暴露 v2 Collaboration、Notifications、Command Center 路由。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/service/V21OpsRuntimeBridgeService.java`
  - 复用 `SuiteCollaborationService`、`SuiteCommandCenterService`、`WebPushService`、`SuiteOrgScopeService`，完成 v2 record 映射。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21NotificationPatchRequest.java`
  - 绑定 `PATCH /api/v2/notifications/{id}` 请求体。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21CollaborationProjectVo.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21CollaborationTaskVo.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21CollaborationActivityVo.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21NotificationVo.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21NotificationSubscriptionVo.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21CommandCenterCommandVo.java`
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`
  - 实现完成后记录切片、提交号和验证命令。

---

### 任务 1：添加失败的 v2 Ops runtime bridge 测试

**文件：**
- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21OpsRuntimeBridgeTest.java`

- [ ] **步骤 1：编写失败测试**

创建测试文件，先覆盖当前缺失的 v2 ops runtime routes。

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendV21OpsRuntimeBridgeTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void v21CollaborationShouldExposeRealSuiteActivityProjectsAndTasks() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String token = register("v21-ops-collab-" + suffix + "@mmmail.local", "V21 Ops Collab");
        createPassItem(token, "V21 Ops Secret");

        MvcResult activityResult = mockMvc.perform(get("/api/v2/collaboration/activity")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").isNotEmpty())
                .andExpect(jsonPath("$.data[0].title").isNotEmpty())
                .andExpect(jsonPath("$.data[0].product").value("PASS"))
                .andReturn();

        JsonNode firstActivity = readJson(activityResult).at("/data/0");
        assertThat(firstActivity.path("occurredAt").asText()).isNotBlank();

        mockMvc.perform(get("/api/v2/collaboration/projects")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("pass"))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data[0].taskCount").isNumber());

        mockMvc.perform(get("/api/v2/collaboration/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].projectId").value("pass"))
                .andExpect(jsonPath("$.data[0].status").isNotEmpty());
    }

    @Test
    void v21NotificationsShouldExposeAndMutateRealSuiteNotificationState() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String token = register("v21-ops-notify-" + suffix + "@mmmail.local", "V21 Ops Notify");
        seedNotification(token, suffix);

        MvcResult result = mockMvc.perform(get("/api/v2/notifications")
                        .header("Authorization", "Bearer " + token)
                        .param("limit", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").isNotEmpty())
                .andExpect(jsonPath("$.data[0].status").value("UNREAD"))
                .andReturn();

        String notificationId = readJson(result).at("/data/0/id").asText();
        mockMvc.perform(patch("/api/v2/notifications/" + notificationId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "READ"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(notificationId))
                .andExpect(jsonPath("$.data.status").value("READ"))
                .andExpect(jsonPath("$.data.readAt").isNotEmpty());
    }

    @Test
    void v21CommandCenterShouldExposeRealCommunityCommandTemplates() throws Exception {
        String token = register("v21-ops-command-" + System.nanoTime() + "@mmmail.local", "V21 Ops Command");

        MvcResult result = mockMvc.perform(get("/api/v2/command-center/commands")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").isNotEmpty())
                .andExpect(jsonPath("$.data[0].name").isNotEmpty())
                .andExpect(jsonPath("$.data[0].enabled").value(true))
                .andReturn();

        String commandId = readJson(result).at("/data/0/id").asText();
        mockMvc.perform(get("/api/v2/command-center/commands/" + commandId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(commandId));
    }

    @Test
    void v21OpsShouldRejectUnsupportedCommunityWritesAndGatePremiumRoutes() throws Exception {
        String token = register("v21-ops-gate-" + System.nanoTime() + "@mmmail.local", "V21 Ops Gate");

        mockMvc.perform(post("/api/v2/collaboration/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Unsupported\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()));

        assertPremiumGate(token, "/api/v2/command-center/runs");
        assertPremiumGate(token, "/api/v2/command-center/workflows");
        assertPremiumGate(token, "/api/v2/command-center/audit");
        assertPremiumGate(token, "/api/v2/notifications/rules");
        assertPremiumGate(token, "/api/v2/notifications/templates");
        assertPremiumGate(token, "/api/v2/notifications/analytics");
    }

    @Test
    void v21NotificationSubscriptionsShouldMapRealWebPushStatus() throws Exception {
        String token = register("v21-ops-sub-" + System.nanoTime() + "@mmmail.local", "V21 Ops Sub");

        mockMvc.perform(get("/api/v2/notifications/subscriptions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("web-push-mail-inbox"))
                .andExpect(jsonPath("$.data[0].product").value("MAIL"))
                .andExpect(jsonPath("$.data[0].channel").value("WEB_PUSH"))
                .andExpect(jsonPath("$.data[0].enabled").isBoolean());
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

    private void createPassItem(String token, String title) throws Exception {
        mockMvc.perform(post("/api/v1/pass/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "itemType": "LOGIN",
                                  "website": "https://ops.example.com",
                                  "username": "ops@example.com",
                                  "secretCiphertext": "Ops#123456A",
                                  "note": "v2 ops collaboration seed"
                                }
                                """.formatted(title)))
                .andExpect(status().isOk());
    }

    private void seedNotification(String token, String suffix) throws Exception {
        mockMvc.perform(post("/api/v1/suite/subscription/change")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planCode\":\"UNLIMITED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/suite/governance/change-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode": "SECURITY_BASELINE_HARDENING",
                                  "reason": "v21 ops notification seed %s"
                                }
                                """.formatted(suffix)))
                .andExpect(status().isOk());
    }

    private void assertPremiumGate(String token, String path) throws Exception {
        mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode()));
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21OpsRuntimeBridgeTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：FAIL。失败应来自 `/api/v2/collaboration/*`、`/api/v2/notifications*`、`/api/v2/command-center/*` 没有匹配的 v2 runtime handler，不能是测试编译错误。

- [ ] **步骤 3：Commit 红测**

```bash
git status --short --branch
git add backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21OpsRuntimeBridgeTest.java
git diff --cached --check
git commit -m "test(backend-v21): cover ops runtime bridge"
```

---

### 任务 2：添加 v2 Ops adapter request 和 response records

**文件：**
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21NotificationPatchRequest.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21CollaborationProjectVo.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21CollaborationTaskVo.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21CollaborationActivityVo.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21NotificationVo.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21NotificationSubscriptionVo.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21CommandCenterCommandVo.java`
- 测试：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21OpsRuntimeBridgeTest.java`

- [ ] **步骤 1：创建 notification patch DTO**

```java
package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;

public record V21NotificationPatchRequest(
        @NotBlank String status
) {
}
```

- [ ] **步骤 2：创建 Collaboration response records**

`V21CollaborationProjectVo.java`：

```java
package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record V21CollaborationProjectVo(
        String id,
        String name,
        String status,
        int taskCount,
        LocalDateTime updatedAt
) {
}
```

`V21CollaborationTaskVo.java`：

```java
package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record V21CollaborationTaskVo(
        String id,
        String projectId,
        String title,
        String status,
        String assigneeEmail,
        LocalDateTime dueAt
) {
}
```

`V21CollaborationActivityVo.java`：

```java
package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record V21CollaborationActivityVo(
        String id,
        String title,
        String product,
        LocalDateTime occurredAt
) {
}
```

- [ ] **步骤 3：创建 Notifications 和 Command Center response records**

`V21NotificationVo.java`：

```java
package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record V21NotificationVo(
        String id,
        String title,
        String body,
        String product,
        String severity,
        String status,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
}
```

`V21NotificationSubscriptionVo.java`：

```java
package com.mmmail.server.model.vo;

public record V21NotificationSubscriptionVo(
        String id,
        String product,
        String channel,
        boolean enabled
) {
}
```

`V21CommandCenterCommandVo.java`：

```java
package com.mmmail.server.model.vo;

public record V21CommandCenterCommandVo(
        String id,
        String name,
        String description,
        String product,
        boolean enabled,
        int parameterCount
) {
}
```

- [ ] **步骤 4：运行编译范围测试验证 records 可见**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21OpsRuntimeBridgeTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：仍然 FAIL，失败应来自缺少 controller 或 runtime handler。

- [ ] **步骤 5：Commit adapter records**

```bash
git status --short --branch
git add backend/mmmail-server/src/main/java/com/mmmail/server/model/dto/V21NotificationPatchRequest.java \
  backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21CollaborationProjectVo.java \
  backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21CollaborationTaskVo.java \
  backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21CollaborationActivityVo.java \
  backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21NotificationVo.java \
  backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21NotificationSubscriptionVo.java \
  backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/V21CommandCenterCommandVo.java
git diff --cached --check
git commit -m "feat(backend-v21): add ops adapter records"
```

---

### 任务 3：实现 v2 Ops runtime bridge service

**文件：**
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/service/V21OpsRuntimeBridgeService.java`
- 测试：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21OpsRuntimeBridgeTest.java`

- [ ] **步骤 1：创建 service 骨架和依赖注入**

```java
package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.orggovernance.scope.OrgScopeAccessDecision;
import com.mmmail.server.model.dto.V21NotificationPatchRequest;
import com.mmmail.server.model.vo.SuiteCollaborationCenterVo;
import com.mmmail.server.model.vo.SuiteCollaborationEventVo;
import com.mmmail.server.model.vo.SuiteCommandCenterVo;
import com.mmmail.server.model.vo.SuiteCommandItemVo;
import com.mmmail.server.model.vo.SuiteNotificationCenterVo;
import com.mmmail.server.model.vo.SuiteNotificationItemVo;
import com.mmmail.server.model.vo.SuiteRemediationActionVo;
import com.mmmail.server.model.vo.SuiteWebPushStatusVo;
import com.mmmail.server.model.vo.V21CollaborationActivityVo;
import com.mmmail.server.model.vo.V21CollaborationProjectVo;
import com.mmmail.server.model.vo.V21CollaborationTaskVo;
import com.mmmail.server.model.vo.V21CommandCenterCommandVo;
import com.mmmail.server.model.vo.V21NotificationSubscriptionVo;
import com.mmmail.server.model.vo.V21NotificationVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class V21OpsRuntimeBridgeService {

    private static final int DEFAULT_OPS_LIMIT = 24;
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_READ = "READ";
    private static final String STATUS_UNREAD = "UNREAD";
    private static final String CHANNEL_WEB_PUSH = "WEB_PUSH";
    private static final String PRODUCT_MAIL = "MAIL";
    private static final String WEB_PUSH_SUBSCRIPTION_ID = "web-push-mail-inbox";

    private final SuiteCollaborationService collaborationService;
    private final SuiteCommandCenterService commandCenterService;
    private final SuiteOrgScopeService orgScopeService;
    private final WebPushService webPushService;

    public V21OpsRuntimeBridgeService(
            SuiteCollaborationService collaborationService,
            SuiteCommandCenterService commandCenterService,
            SuiteOrgScopeService orgScopeService,
            WebPushService webPushService
    ) {
        this.collaborationService = collaborationService;
        this.commandCenterService = commandCenterService;
        this.orgScopeService = orgScopeService;
        this.webPushService = webPushService;
    }
}
```

- [ ] **步骤 2：添加 Collaboration 映射方法**

在 service 类中加入：

```java
public List<V21CollaborationActivityVo> listCollaborationActivity(
        Long userId,
        Integer limit,
        HttpServletRequest request
) {
    return collaborationCenter(userId, limit, request).items().stream()
            .map(this::toActivityVo)
            .toList();
}

public List<V21CollaborationProjectVo> listCollaborationProjects(
        Long userId,
        Integer limit,
        HttpServletRequest request
) {
    Map<String, ProjectAccumulator> projects = new LinkedHashMap<>();
    for (SuiteCollaborationEventVo item : collaborationCenter(userId, limit, request).items()) {
        String projectId = normalizeProjectId(item.productCode());
        ProjectAccumulator current = projects.computeIfAbsent(
                projectId,
                ignored -> new ProjectAccumulator(projectId, projectName(item.productCode()))
        );
        current.accept(item);
    }
    return projects.values().stream()
            .map(ProjectAccumulator::toVo)
            .toList();
}

public V21CollaborationProjectVo readCollaborationProject(
        Long userId,
        String projectId,
        HttpServletRequest request
) {
    String normalized = normalizeProjectId(projectId);
    return listCollaborationProjects(userId, DEFAULT_OPS_LIMIT, request).stream()
            .filter(project -> project.id().equals(normalized))
            .findFirst()
            .orElseThrow(() -> new BizException(ErrorCode.INVALID_ARGUMENT, "v2 collaboration project is not found"));
}

public List<V21CollaborationTaskVo> listCollaborationTasks(
        Long userId,
        Integer limit,
        HttpServletRequest request
) {
    return collaborationCenter(userId, limit, request).items().stream()
            .map(this::toTaskVo)
            .toList();
}

private SuiteCollaborationCenterVo collaborationCenter(Long userId, Integer limit, HttpServletRequest request) {
    OrgScopeAccessDecision scope = orgScopeService.resolveContext(request, userId);
    return collaborationService.getCenter(userId, limit, request.getRemoteAddr(), scope.visibleProductCodes());
}

private V21CollaborationActivityVo toActivityVo(SuiteCollaborationEventVo item) {
    return new V21CollaborationActivityVo(
            String.valueOf(item.eventId()),
            item.title(),
            item.productCode(),
            item.createdAt()
    );
}

private V21CollaborationTaskVo toTaskVo(SuiteCollaborationEventVo item) {
    return new V21CollaborationTaskVo(
            "event-" + item.eventId(),
            normalizeProjectId(item.productCode()),
            item.title(),
            taskStatus(item.eventType()),
            item.actorEmail(),
            null
    );
}
```

- [ ] **步骤 3：添加 Notifications 映射方法**

在 service 类中加入：

```java
public List<V21NotificationVo> listNotifications(
        Long userId,
        Integer limit,
        Boolean unreadOnly,
        String status,
        Boolean includeSnoozed,
        HttpServletRequest request
) {
    return notificationCenter(userId, limit, unreadOnly, status, includeSnoozed, request).items().stream()
            .map(this::toNotificationVo)
            .toList();
}

public V21NotificationVo patchNotification(
        Long userId,
        Long sessionId,
        String notificationId,
        V21NotificationPatchRequest requestBody,
        HttpServletRequest request
) {
    if (!STATUS_READ.equalsIgnoreCase(requestBody.status())) {
        throw new BizException(ErrorCode.INVALID_ARGUMENT, "v2 notifications only support status=READ");
    }
    commandCenterService.markNotificationsRead(
            userId,
            sessionId,
            List.of(requireText(notificationId, "notification id")),
            request.getRemoteAddr()
    );
    return listNotifications(userId, DEFAULT_OPS_LIMIT, false, null, true, request).stream()
            .filter(item -> item.id().equals(notificationId))
            .findFirst()
            .orElseThrow(() -> new BizException(ErrorCode.INVALID_ARGUMENT, "v2 notification is not found"));
}

public List<V21NotificationSubscriptionVo> listNotificationSubscriptions(Long userId, HttpServletRequest request) {
    SuiteWebPushStatusVo status = webPushService.getStatus(userId, request.getRemoteAddr());
    return List.of(new V21NotificationSubscriptionVo(
            WEB_PUSH_SUBSCRIPTION_ID,
            PRODUCT_MAIL,
            CHANNEL_WEB_PUSH,
            status.enabled()
    ));
}

private SuiteNotificationCenterVo notificationCenter(
        Long userId,
        Integer limit,
        Boolean unreadOnly,
        String status,
        Boolean includeSnoozed,
        HttpServletRequest request
) {
    OrgScopeAccessDecision scope = orgScopeService.resolveContext(request, userId);
    return commandCenterService.getNotificationCenter(
            userId,
            limit,
            unreadOnly,
            status,
            includeSnoozed,
            request.getRemoteAddr(),
            scope.visibleProductCodes()
    );
}

private V21NotificationVo toNotificationVo(SuiteNotificationItemVo item) {
    return new V21NotificationVo(
            item.notificationId(),
            item.title(),
            item.message(),
            item.productCode(),
            item.severity(),
            item.read() ? STATUS_READ : STATUS_UNREAD,
            item.createdAt(),
            item.readAt()
    );
}
```

- [ ] **步骤 4：添加 Command Center 映射方法和 shared helpers**

在 service 类中加入：

```java
public List<V21CommandCenterCommandVo> listCommandCenterCommands(Long userId, HttpServletRequest request) {
    SuiteCommandCenterVo center = commandCenter(userId, request);
    List<V21CommandCenterCommandVo> routeCommands = center.quickRoutes().stream()
            .map(this::toCommandVo)
            .toList();
    List<V21CommandCenterCommandVo> actionCommands = center.recommendedActions().stream()
            .map(this::toCommandVo)
            .toList();
    return java.util.stream.Stream.concat(routeCommands.stream(), actionCommands.stream()).toList();
}

public V21CommandCenterCommandVo readCommandCenterCommand(
        Long userId,
        String commandId,
        HttpServletRequest request
) {
    String normalized = requireText(commandId, "command id");
    return listCommandCenterCommands(userId, request).stream()
            .filter(command -> command.id().equals(normalized))
            .findFirst()
            .orElseThrow(() -> new BizException(ErrorCode.INVALID_ARGUMENT, "v2 command center command is not found"));
}

public void rejectUnsupported(String message) {
    throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
}

private SuiteCommandCenterVo commandCenter(Long userId, HttpServletRequest request) {
    OrgScopeAccessDecision scope = orgScopeService.resolveContext(request, userId);
    return commandCenterService.getCommandCenter(userId, request.getRemoteAddr(), scope.visibleProductCodes());
}

private V21CommandCenterCommandVo toCommandVo(SuiteCommandItemVo item) {
    String id = "route-" + slug(defaultText(item.actionCode(), item.routePath(), item.label()));
    return new V21CommandCenterCommandVo(
            id,
            item.label(),
            item.description(),
            item.productCode(),
            true,
            0
    );
}

private V21CommandCenterCommandVo toCommandVo(SuiteRemediationActionVo item) {
    String id = "action-" + slug(item.actionCode());
    return new V21CommandCenterCommandVo(
            id,
            item.action(),
            "Recommended " + item.priority() + " remediation action",
            item.productCode(),
            true,
            0
    );
}

private String normalizeProjectId(String productCode) {
    return requireText(productCode, "product code").toLowerCase(Locale.ROOT);
}

private String projectName(String productCode) {
    String normalized = requireText(productCode, "product code").toUpperCase(Locale.ROOT);
    return switch (normalized) {
        case "MAIL" -> "Mail collaboration";
        case "CALENDAR" -> "Calendar collaboration";
        case "DRIVE" -> "Drive collaboration";
        case "DOCS" -> "Docs collaboration";
        case "SHEETS" -> "Sheets collaboration";
        case "PASS" -> "Pass collaboration";
        default -> normalized + " collaboration";
    };
}

private String taskStatus(String eventType) {
    String normalized = StringUtils.hasText(eventType) ? eventType.toUpperCase(Locale.ROOT) : "";
    if (normalized.contains("DELETE") || normalized.contains("REMOVE") || normalized.contains("REVOKE")) {
        return "DONE";
    }
    return "OPEN";
}

private String slug(String value) {
    return requireText(value, "slug source")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
}

private String defaultText(String first, String second, String third) {
    if (StringUtils.hasText(first)) {
        return first;
    }
    if (StringUtils.hasText(second)) {
        return second;
    }
    return third;
}

private String requireText(String value, String fieldName) {
    if (!StringUtils.hasText(value)) {
        throw new BizException(ErrorCode.INVALID_ARGUMENT, fieldName + " is required");
    }
    return value.trim();
}

private static final class ProjectAccumulator {
    private final String id;
    private final String name;
    private int taskCount;
    private LocalDateTime updatedAt;

    private ProjectAccumulator(String id, String name) {
        this.id = id;
        this.name = name;
    }

    private void accept(SuiteCollaborationEventVo item) {
        taskCount++;
        if (updatedAt == null || item.createdAt().isAfter(updatedAt)) {
            updatedAt = item.createdAt();
        }
    }

    private V21CollaborationProjectVo toVo() {
        return new V21CollaborationProjectVo(id, name, STATUS_ACTIVE, taskCount, updatedAt);
    }
}
```

- [ ] **步骤 5：运行 service 编译测试**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21OpsRuntimeBridgeTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：仍然 FAIL，失败应来自没有 `V21OpsController` handler。

- [ ] **步骤 6：Commit bridge service**

```bash
git status --short --branch
git add backend/mmmail-server/src/main/java/com/mmmail/server/service/V21OpsRuntimeBridgeService.java
git diff --cached --check
git commit -m "feat(backend-v21): add ops runtime bridge service"
```

---

### 任务 4：添加 v2 Ops controller

**文件：**
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21OpsController.java`
- 修改：`backend/mmmail-server/src/main/java/com/mmmail/server/service/V21OpsRuntimeBridgeService.java`，仅在 controller 编译暴露遗漏方法时补齐
- 测试：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21OpsRuntimeBridgeTest.java`

- [ ] **步骤 1：创建 controller**

```java
package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.V21NotificationPatchRequest;
import com.mmmail.server.model.vo.V21CollaborationActivityVo;
import com.mmmail.server.model.vo.V21CollaborationProjectVo;
import com.mmmail.server.model.vo.V21CollaborationTaskVo;
import com.mmmail.server.model.vo.V21CommandCenterCommandVo;
import com.mmmail.server.model.vo.V21NotificationSubscriptionVo;
import com.mmmail.server.model.vo.V21NotificationVo;
import com.mmmail.server.service.V21OpsRuntimeBridgeService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v2")
public class V21OpsController {

    private final V21OpsRuntimeBridgeService opsRuntimeBridgeService;

    public V21OpsController(V21OpsRuntimeBridgeService opsRuntimeBridgeService) {
        this.opsRuntimeBridgeService = opsRuntimeBridgeService;
    }

    @GetMapping("/collaboration/projects")
    public Result<List<V21CollaborationProjectVo>> collaborationProjects(
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request
    ) {
        return Result.success(opsRuntimeBridgeService.listCollaborationProjects(
                SecurityUtils.currentUserId(),
                limit,
                request
        ));
    }

    @PostMapping("/collaboration/projects")
    public Result<Void> createCollaborationProject(@RequestBody(required = false) Map<String, Object> body) {
        opsRuntimeBridgeService.rejectUnsupported("v2 collaboration project creation is not supported by current runtime bridge");
        return Result.success(null);
    }

    @GetMapping("/collaboration/projects/{id}")
    public Result<V21CollaborationProjectVo> collaborationProject(
            @PathVariable String id,
            HttpServletRequest request
    ) {
        return Result.success(opsRuntimeBridgeService.readCollaborationProject(
                SecurityUtils.currentUserId(),
                id,
                request
        ));
    }

    @GetMapping("/collaboration/tasks")
    public Result<List<V21CollaborationTaskVo>> collaborationTasks(
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request
    ) {
        return Result.success(opsRuntimeBridgeService.listCollaborationTasks(
                SecurityUtils.currentUserId(),
                limit,
                request
        ));
    }

    @PostMapping("/collaboration/tasks")
    public Result<Void> createCollaborationTask(@RequestBody(required = false) Map<String, Object> body) {
        opsRuntimeBridgeService.rejectUnsupported("v2 collaboration task creation is not supported by current runtime bridge");
        return Result.success(null);
    }

    @PatchMapping("/collaboration/tasks/{id}")
    public Result<Void> patchCollaborationTask(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        opsRuntimeBridgeService.rejectUnsupported("v2 collaboration task update is not supported by current runtime bridge");
        return Result.success(null);
    }

    @PostMapping("/collaboration/tasks/{id}/comments")
    public Result<Void> commentCollaborationTask(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        opsRuntimeBridgeService.rejectUnsupported("v2 collaboration task comments are not supported by current runtime bridge");
        return Result.success(null);
    }

    @GetMapping("/collaboration/activity")
    public Result<List<V21CollaborationActivityVo>> collaborationActivity(
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request
    ) {
        return Result.success(opsRuntimeBridgeService.listCollaborationActivity(
                SecurityUtils.currentUserId(),
                limit,
                request
        ));
    }

    @GetMapping("/notifications")
    public Result<List<V21NotificationVo>> notifications(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean includeSnoozed,
            HttpServletRequest request
    ) {
        return Result.success(opsRuntimeBridgeService.listNotifications(
                SecurityUtils.currentUserId(),
                limit,
                unreadOnly,
                status,
                includeSnoozed,
                request
        ));
    }

    @PatchMapping("/notifications/{id}")
    public Result<V21NotificationVo> patchNotification(
            @PathVariable String id,
            @Valid @RequestBody V21NotificationPatchRequest body,
            HttpServletRequest request
    ) {
        return Result.success(opsRuntimeBridgeService.patchNotification(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentSessionId(),
                id,
                body,
                request
        ));
    }

    @GetMapping("/notifications/subscriptions")
    public Result<List<V21NotificationSubscriptionVo>> notificationSubscriptions(HttpServletRequest request) {
        return Result.success(opsRuntimeBridgeService.listNotificationSubscriptions(
                SecurityUtils.currentUserId(),
                request
        ));
    }

    @PatchMapping("/notifications/subscriptions/{id}")
    public Result<Void> patchNotificationSubscription(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        opsRuntimeBridgeService.rejectUnsupported("v2 notification subscription update is not supported by current runtime bridge");
        return Result.success(null);
    }

    @GetMapping("/command-center/commands")
    public Result<List<V21CommandCenterCommandVo>> commandCenterCommands(HttpServletRequest request) {
        return Result.success(opsRuntimeBridgeService.listCommandCenterCommands(
                SecurityUtils.currentUserId(),
                request
        ));
    }

    @GetMapping("/command-center/commands/{id}")
    public Result<V21CommandCenterCommandVo> commandCenterCommand(
            @PathVariable String id,
            HttpServletRequest request
    ) {
        return Result.success(opsRuntimeBridgeService.readCommandCenterCommand(
                SecurityUtils.currentUserId(),
                id,
                request
        ));
    }

    @PostMapping("/command-center/runs")
    public Result<Void> createCommandRun(@RequestBody(required = false) Map<String, Object> body) {
        opsRuntimeBridgeService.rejectUnsupported("v2 command runs require Premium command runner runtime");
        return Result.success(null);
    }

    @GetMapping("/command-center/runs/{id}")
    public Result<Void> readCommandRun(@PathVariable String id) {
        opsRuntimeBridgeService.rejectUnsupported("v2 command runs require Premium command runner runtime");
        return Result.success(null);
    }

    @PostMapping("/command-center/runs/{id}/cancel")
    public Result<Void> cancelCommandRun(@PathVariable String id) {
        opsRuntimeBridgeService.rejectUnsupported("v2 command run cancellation requires Premium command runner runtime");
        return Result.success(null);
    }

    @PostMapping("/command-center/runs/{id}/retry")
    public Result<Void> retryCommandRun(@PathVariable String id) {
        opsRuntimeBridgeService.rejectUnsupported("v2 command run retry requires Premium command runner runtime");
        return Result.success(null);
    }

    @GetMapping("/command-center/workflows")
    public Result<Void> commandWorkflows() {
        opsRuntimeBridgeService.rejectUnsupported("v2 command workflows require Premium command runner runtime");
        return Result.success(null);
    }

    @PostMapping("/command-center/workflows")
    public Result<Void> createCommandWorkflow(@RequestBody(required = false) Map<String, Object> body) {
        opsRuntimeBridgeService.rejectUnsupported("v2 command workflows require Premium command runner runtime");
        return Result.success(null);
    }

    @GetMapping("/command-center/audit")
    public Result<Void> commandAudit() {
        opsRuntimeBridgeService.rejectUnsupported("v2 command audit requires Premium command runner runtime");
        return Result.success(null);
    }

    @GetMapping("/notifications/rules")
    public Result<Void> notificationRules() {
        opsRuntimeBridgeService.rejectUnsupported("v2 notification rules require Premium notification runtime");
        return Result.success(null);
    }

    @PostMapping("/notifications/rules")
    public Result<Void> createNotificationRule(@RequestBody(required = false) Map<String, Object> body) {
        opsRuntimeBridgeService.rejectUnsupported("v2 notification rules require Premium notification runtime");
        return Result.success(null);
    }

    @GetMapping("/notifications/templates")
    public Result<Void> notificationTemplates() {
        opsRuntimeBridgeService.rejectUnsupported("v2 notification templates require Premium notification runtime");
        return Result.success(null);
    }

    @PostMapping("/notifications/send")
    public Result<Void> sendNotification(@RequestBody(required = false) Map<String, Object> body) {
        opsRuntimeBridgeService.rejectUnsupported("v2 notification send requires Premium notification runtime");
        return Result.success(null);
    }

    @GetMapping("/notifications/analytics")
    public Result<Void> notificationAnalytics() {
        opsRuntimeBridgeService.rejectUnsupported("v2 notification analytics requires Premium notification runtime");
        return Result.success(null);
    }
}
```

- [ ] **步骤 2：运行 v2 Ops 测试验证通过**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21OpsRuntimeBridgeTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：PASS，测试数 `5/5`。

- [ ] **步骤 3：Commit controller**

```bash
git status --short --branch
git add backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21OpsController.java \
  backend/mmmail-server/src/main/java/com/mmmail/server/service/V21OpsRuntimeBridgeService.java
git diff --cached --check
git commit -m "feat(backend-v21): add ops runtime bridge controller"
```

---

### 任务 5：运行后端回归并修复发现的问题

**文件：**
- 可能修改：`backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21OpsController.java`
- 可能修改：`backend/mmmail-server/src/main/java/com/mmmail/server/service/V21OpsRuntimeBridgeService.java`
- 可能修改：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21OpsRuntimeBridgeTest.java`

- [ ] **步骤 1：运行 Ops 直接回归**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21OpsRuntimeBridgeTest,SuiteCollaborationCenterIntegrationTest,WebPushSubscriptionIntegrationTest,SuiteOrgAccessIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：PASS。若失败，读取第一个失败断言，修正真实映射或测试期望；不得通过返回固定空数组、固定成功对象或吞掉异常来绕过失败。

- [ ] **步骤 2：运行 v2 access/catalog 回归**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：PASS。若 Premium 路由返回非 `V2_ENTITLEMENT_REQUIRED`，确认 controller handler 是否存在并让 existing gate 在方法执行前拦截。

- [ ] **步骤 3：运行前端验证**

```bash
pnpm --dir frontend-v2 test
pnpm --dir frontend-v2 typecheck
pnpm --dir frontend-v2 build
```

预期：三条命令均退出码 `0`。如果本地缺少 `frontend-v2/node_modules`，先运行 `pnpm --dir frontend-v2 install --frozen-lockfile`，只允许产生被忽略的依赖目录。

- [ ] **步骤 4：Commit 回归修复**

仅当步骤 1 到步骤 3 过程中修改了文件时执行：

```bash
git status --short --branch
git add backend/mmmail-server/src/main/java/com/mmmail/server/controller/V21OpsController.java \
  backend/mmmail-server/src/main/java/com/mmmail/server/service/V21OpsRuntimeBridgeService.java \
  backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21OpsRuntimeBridgeTest.java
git diff --cached --check
git commit -m "fix(backend-v21): stabilize ops runtime bridge"
```

---

### 任务 6：更新 v2.1 进度并提交实现切片

**文件：**
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`

- [ ] **步骤 1：更新进度文档**

在 `Completed v2.1 Slices` 表格末尾加入：

```markdown
| Backend Ops runtime bridge (`backend-v21-ops-runtime-bridge`) | `BackendV21OpsRuntimeBridgeTest`, `V21OpsController`, `V21OpsRuntimeBridgeService` |
```

将 `Latest backend implementation commit` 更新为本轮实现提交号。先执行：

```bash
git log --oneline -1
```

然后把命令输出的完整一行写入 `Latest backend implementation commit`。

将 `Latest Completed Backend Slice` 改为：

```markdown
## Latest Completed Backend Slice

- Slice: `backend-v21-ops-runtime-bridge`
- Commit: 使用 `git log --oneline -1` 返回的完整实现提交行
- Files changed: added v2 Ops controller, runtime bridge service, v2 adapter records, backend coverage for Collaboration, Notifications, Command Center, supported notification read-state updates, unsupported Community writes, and Premium operation gates.
- Verification:
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21OpsRuntimeBridgeTest -Dsurefire.failIfNoSpecifiedTests=false`: PASS
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21OpsRuntimeBridgeTest,SuiteCollaborationCenterIntegrationTest,WebPushSubscriptionIntegrationTest,SuiteOrgAccessIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false`: PASS
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest -Dsurefire.failIfNoSpecifiedTests=false`: PASS
  - `pnpm --dir frontend-v2 test`: PASS
  - `pnpm --dir frontend-v2 typecheck`: PASS
  - `pnpm --dir frontend-v2 build`: PASS
```

将 `Active Backend Slice` 改为：

```markdown
## Active Backend Slice

- Slice: `backend-v21-ops-runtime-bridge`
- Status: `completed`
- Started: `2026-05-13`
- Completed: `2026-05-13`
- Scope: v2 Collaboration, Notifications, and Command Center runtime bridge for Community reads, notification read-state mutation, unsupported Community writes, and Premium operation gates
- Verification:
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21OpsRuntimeBridgeTest -Dsurefire.failIfNoSpecifiedTests=false`
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21OpsRuntimeBridgeTest,SuiteCollaborationCenterIntegrationTest,WebPushSubscriptionIntegrationTest,SuiteOrgAccessIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false`
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest -Dsurefire.failIfNoSpecifiedTests=false`
  - `pnpm --dir frontend-v2 test`
  - `pnpm --dir frontend-v2 typecheck`
  - `pnpm --dir frontend-v2 build`
```

上面 `Commit` 行必须写成本地仓库真实实现提交行。

- [ ] **步骤 2：自检进度文档没有示例文本**

```bash
rg -n "使用 `git log --oneline -1`" docs/superpowers/progress/v21-implementation-progress.md
```

预期：退出码 `1`，没有匹配行。

- [ ] **步骤 3：提交进度文档**

```bash
git status --short --branch
git add -f docs/superpowers/progress/v21-implementation-progress.md
git diff --cached --check
git commit -m "docs(backend-v21): update ops runtime bridge progress"
```

---

## 最终验证清单

实现完成后，在最终回复前运行：

```bash
git status --short --branch
git log --oneline -5
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21OpsRuntimeBridgeTest -Dsurefire.failIfNoSpecifiedTests=false
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21OpsRuntimeBridgeTest,SuiteCollaborationCenterIntegrationTest,WebPushSubscriptionIntegrationTest,SuiteOrgAccessIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21AccessEntitlementGatesTest,BackendV21ApiContractCatalogTest -Dsurefire.failIfNoSpecifiedTests=false
pnpm --dir frontend-v2 test
pnpm --dir frontend-v2 typecheck
pnpm --dir frontend-v2 build
```

最终状态必须满足：

- 本轮实现和进度文档已提交到本地分支。
- `git status --short --branch` 没有本轮相关未暂存文件。
- 已知无关未跟踪路径 `.superpowers/`、`.tmp/`、`docs/MMMail.zip`、`docs/MMMail/`、`frontend/` 不纳入提交。
- 没有执行 `git push`。

---

## 计划自检

- 规格覆盖：Collaboration、Notifications、Command Center 的 Community reads、notification read-state mutation、unsupported writes、Premium gate、progress recording 均有任务覆盖。
- 未解析标记扫描：计划不保留虚构提交号；进度文档任务要求执行者替换真实提交号并用 `rg` 验证。
- 类型一致性：Controller 使用的 request/response records 均在任务 2 定义；service 使用的 public 方法均在任务 3 定义；测试断言字段与 v2 frontend API 类型一致。
- 范围控制：计划不新增持久化表，不实现 Premium command runner、notification rules/templates/send/analytics，不改 Admin/Billing/Settings/Public Share/Workspace summary。
