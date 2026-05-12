# Backend v2.1 Event Outbox Foundation 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 实现 v2.1 后端事件/outbox 基础，让 Community 以数据库 outbox + in-process dispatcher 运行，同时为 Hosted/Premium worker 抽取保留稳定契约。

**架构：** `mmmail-platform` 定义不可变事件模型、outbox 记录、publisher/dispatcher 接口；`mmmail-server` 提供 MyBatis 持久化、数据库 publisher、in-process dispatcher 和迁移。当前切片不接入 Kafka，不抽 worker，不改业务服务发事件。

**技术栈：** Java 21 records/enums, Spring Boot, MyBatis-Plus, H2 test profile, Flyway SQL, Micrometer, JUnit 5, AssertJ。

---

## 文件结构

- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/event/PlatformEventType.java`
  冻结 v2.1 事件目录，暴露 event name、owner module、tenant/user metadata 规则和 replayability。
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/event/PlatformEventMetadata.java`
  事件 metadata 不可变记录，负责 metadata 校验。
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/event/PlatformEvent.java`
  事件本体不可变记录，负责 aggregate、payload、idempotency 校验。
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/outbox/OutboxEventRecord.java`
  持久化 outbox 记录不可变模型，负责显式状态流转。
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/outbox/OutboxPublishRequest.java`
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/outbox/OutboxPublishResult.java`
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/outbox/OutboxDispatchResult.java`
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/outbox/OutboxPublisher.java`
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/outbox/OutboxDispatcher.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/outbox/PlatformOutboxEvent.java`
  MyBatis-Plus entity，对应 `platform_outbox_event`。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/outbox/PlatformOutboxEventMapper.java`
  outbox 查询、幂等键查询、due event 查询。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/outbox/PlatformOutboxHandler.java`
  in-process dispatcher 的 handler 契约。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/outbox/DatabaseOutboxPublisher.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/outbox/InProcessOutboxDispatcher.java`
- 修改：`backend/mmmail-server/src/main/java/com/mmmail/server/config/MybatisPlusConfig.java`
  让 MyBatis 扫描 `com.mmmail.server.outbox`。
- 创建：`backend/mmmail-server/src/main/resources/db/migration/V11__platform_outbox_event.sql`
- 修改：`backend/mmmail-server/src/main/resources/schema.sql`
- 修改：`backend/mmmail-server/src/main/resources/db/baseline/community-v1-schema.sql`
- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21EventOutboxFoundationTest.java`
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`

## 任务 1：记录活动切片并编写失败测试

**文件：**
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`
- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21EventOutboxFoundationTest.java`

- [x] **步骤 1：记录活动切片**

在 `## Remaining v2.1 Risks` 之前插入：

```markdown
## Active Backend Slice

- Slice: `backend-v21-event-outbox-foundation`
- Status: `in_progress`
- Started: `2026-05-12`
- Scope: platform event contract, outbox record contract, database-backed publisher, in-process dispatcher, migration, tests
- Verification target: `BackendV21EventOutboxFoundationTest`, backend compile, frontend v2.1 test suite
```

- [x] **步骤 2：编写失败测试**

创建 `BackendV21EventOutboxFoundationTest.java`，测试先引用还不存在的事件/outbox 类型，让红灯清晰暴露缺口：

```java
package com.mmmail.server;

import com.mmmail.platform.event.PlatformEvent;
import com.mmmail.platform.event.PlatformEventMetadata;
import com.mmmail.platform.event.PlatformEventType;
import com.mmmail.platform.outbox.OutboxDispatchResult;
import com.mmmail.platform.outbox.OutboxEventRecord;
import com.mmmail.platform.outbox.OutboxEventStatus;
import com.mmmail.platform.outbox.OutboxPublishRequest;
import com.mmmail.platform.outbox.OutboxPublishResult;
import com.mmmail.platform.outbox.OutboxPublisher;
import com.mmmail.server.outbox.InProcessOutboxDispatcher;
import com.mmmail.server.outbox.PlatformOutboxEvent;
import com.mmmail.server.outbox.PlatformOutboxEventMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class BackendV21EventOutboxFoundationTest {

    private static final Set<String> REQUIRED_EVENTS = Set.of(
            "identity.user.created",
            "identity.session.revoked",
            "workspace.activity.recorded",
            "mail.message.created",
            "mail.message.sent",
            "mail.rule.matched",
            "calendar.event.created",
            "calendar.booking.created",
            "drive.file.uploaded",
            "drive.file.shared",
            "docs.document.updated",
            "docs.version.created",
            "sheets.workbook.imported",
            "pass.item.updated",
            "pass.secure_link.created",
            "collaboration.task.updated",
            "command.run.requested",
            "command.run.completed",
            "notification.delivery.requested",
            "admin.audit.recorded",
            "billing.entitlement.changed",
            "labs.ai_job.requested"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private OutboxPublisher publisher;
    @Autowired
    private PlatformOutboxEventMapper mapper;
    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void cleanOutbox() {
        jdbcTemplate.update("delete from platform_outbox_event");
    }

    @Test
    void eventCatalogShouldCoverBackendV21ArchitectureEvents() {
        Set<String> names = Arrays.stream(PlatformEventType.values())
                .map(PlatformEventType::eventName)
                .collect(Collectors.toSet());

        assertThat(names).containsAll(REQUIRED_EVENTS);
        assertThat(PlatformEventType.MAIL_MESSAGE_SENT.ownerModule()).isEqualTo("mail");
        assertThat(PlatformEventType.BILLING_ENTITLEMENT_CHANGED.tenantRequired()).isTrue();
        assertThat(PlatformEventType.COMMAND_RUN_REQUESTED.replayable()).isTrue();
    }

    @Test
    void platformEventShouldRejectMissingRequiredMetadata() {
        PlatformEventMetadata metadata = new PlatformEventMetadata(
                "",
                "7",
                "req-1",
                "trace-1",
                "mail",
                "send",
                LocalDateTime.now()
        );

        assertThatThrownBy(() -> new PlatformEvent(
                PlatformEventType.MAIL_MESSAGE_SENT,
                "mail_message",
                "100",
                metadata,
                "{\"messageId\":\"100\"}",
                "mail-send-100"
        )).hasMessageContaining("tenantId is required");
    }

    @Test
    void outboxRecordShouldEnforceExplicitStatusTransitions() {
        OutboxEventRecord pending = OutboxEventRecord.pending(
                1L,
                event(),
                LocalDateTime.now()
        );

        OutboxEventRecord published = pending.markPublished(LocalDateTime.now());
        assertThat(published.status()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThatThrownBy(() -> published.markFailed("late failure", LocalDateTime.now()))
                .hasMessageContaining("Invalid outbox status transition");
    }

    @Test
    void publisherShouldPersistPendingEventsWithMetadataAndRejectIdempotencyMismatch() {
        OutboxPublishResult first = publisher.publish(new OutboxPublishRequest(event()));
        PlatformOutboxEvent saved = mapper.selectById(first.eventId());

        assertThat(first.status()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(first.duplicate()).isFalse();
        assertThat(saved.getEventType()).isEqualTo("mail.message.sent");
        assertThat(saved.getTenantId()).isEqualTo("tenant-1");
        assertThat(saved.getRequestId()).isEqualTo("req-1");
        assertThat(saved.getStatus()).isEqualTo("PENDING");

        OutboxPublishResult duplicate = publisher.publish(new OutboxPublishRequest(event()));
        assertThat(duplicate.eventId()).isEqualTo(first.eventId());
        assertThat(duplicate.duplicate()).isTrue();

        assertThatThrownBy(() -> publisher.publish(new OutboxPublishRequest(eventWithPayload("{\"messageId\":\"101\"}"))))
                .hasMessageContaining("idempotency key already belongs to a different event");
    }

    @Test
    void dispatcherShouldPublishSuccessfulDueEvents() {
        OutboxPublishResult result = publisher.publish(new OutboxPublishRequest(event()));
        InProcessOutboxDispatcher dispatcher = new InProcessOutboxDispatcher(
                mapper,
                new SimpleMeterRegistry(),
                eventRecord -> {
                },
                2,
                Duration.ZERO
        );

        OutboxDispatchResult dispatchResult = dispatcher.dispatchDue(10, LocalDateTime.now());
        PlatformOutboxEvent saved = mapper.selectById(result.eventId());

        assertThat(dispatchResult.published()).isEqualTo(1);
        assertThat(saved.getStatus()).isEqualTo("PUBLISHED");
        assertThat(saved.getPublishedAt()).isNotNull();
    }

    @Test
    void dispatcherShouldDeadLetterRepeatedFailures() {
        OutboxPublishResult result = publisher.publish(new OutboxPublishRequest(event()));
        InProcessOutboxDispatcher dispatcher = new InProcessOutboxDispatcher(
                mapper,
                new SimpleMeterRegistry(),
                eventRecord -> {
                    throw new IllegalStateException("handler failed");
                },
                2,
                Duration.ZERO
        );

        OutboxDispatchResult first = dispatcher.dispatchDue(10, LocalDateTime.now());
        OutboxDispatchResult second = dispatcher.dispatchDue(10, LocalDateTime.now());
        PlatformOutboxEvent saved = mapper.selectById(result.eventId());

        assertThat(first.failed()).isEqualTo(1);
        assertThat(second.deadLettered()).isEqualTo(1);
        assertThat(saved.getStatus()).isEqualTo("DEAD_LETTER");
        assertThat(saved.getAttempts()).isEqualTo(2);
    }

    @Test
    void migrationShouldFreezeOutboxTableAndIndexes() throws Exception {
        Path root = resolveRepoRoot();
        String migration = Files.readString(root.resolve("src/main/resources/db/migration/V11__platform_outbox_event.sql"));
        String schema = Files.readString(root.resolve("src/main/resources/schema.sql"));

        assertThat(migration).contains("create table if not exists platform_outbox_event");
        assertThat(migration).contains("uk_platform_outbox_idempotency");
        assertThat(migration).contains("idx_platform_outbox_status_next_attempt");
        assertThat(migration).contains("idx_platform_outbox_tenant_created");
        assertThat(schema).contains("create table if not exists platform_outbox_event");
    }

    @Test
    void communityTestProfileShouldUseInProcessOutboxWithoutExternalBrokerBeans() {
        assertThat(applicationContext.getBean(OutboxPublisher.class)).isNotNull();
        assertThat(applicationContext.getBean(InProcessOutboxDispatcher.class)).isNotNull();
        assertThat(Arrays.stream(applicationContext.getBeanDefinitionNames())
                .noneMatch(name -> name.toLowerCase().contains("kafka"))).isTrue();
    }

    private PlatformEvent event() {
        return eventWithPayload("{\"messageId\":\"100\"}");
    }

    private PlatformEvent eventWithPayload(String payloadJson) {
        return new PlatformEvent(
                PlatformEventType.MAIL_MESSAGE_SENT,
                "mail_message",
                "100",
                metadata(),
                payloadJson,
                "mail-send-100"
        );
    }

    private PlatformEventMetadata metadata() {
        return new PlatformEventMetadata(
                "tenant-1",
                "7",
                "req-1",
                "trace-1",
                "mail",
                "send",
                LocalDateTime.now()
        );
    }

    private Path resolveRepoRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null && !Files.isDirectory(current.resolve("src/main/resources/db/migration"))) {
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
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21EventOutboxFoundationTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：退出码非 0，编译失败包含 `package com.mmmail.platform.event does not exist` 或缺失 outbox/server outbox 类型。

## 任务 2：实现平台事件模型

**文件：**
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/event/PlatformEventType.java`
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/event/PlatformEventMetadata.java`
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/event/PlatformEvent.java`

- [x] **步骤 1：创建 `PlatformEventType`**

每个 enum 常量使用稳定 event name，owner module 与 v2.1 架构文档一致。全部事件均 `tenantRequired=true`、`replayable=true`；`identity.session.revoked` 允许 user ID 缺失，其余事件要求 user ID。

```java
package com.mmmail.platform.event;

public enum PlatformEventType {
    IDENTITY_USER_CREATED("identity.user.created", "identity", true, true, true),
    IDENTITY_SESSION_REVOKED("identity.session.revoked", "identity", true, false, true),
    WORKSPACE_ACTIVITY_RECORDED("workspace.activity.recorded", "workspace", true, true, true),
    MAIL_MESSAGE_CREATED("mail.message.created", "mail", true, true, true),
    MAIL_MESSAGE_SENT("mail.message.sent", "mail", true, true, true),
    MAIL_RULE_MATCHED("mail.rule.matched", "mail", true, true, true),
    CALENDAR_EVENT_CREATED("calendar.event.created", "calendar", true, true, true),
    CALENDAR_BOOKING_CREATED("calendar.booking.created", "calendar", true, true, true),
    DRIVE_FILE_UPLOADED("drive.file.uploaded", "drive", true, true, true),
    DRIVE_FILE_SHARED("drive.file.shared", "drive", true, true, true),
    DOCS_DOCUMENT_UPDATED("docs.document.updated", "docs", true, true, true),
    DOCS_VERSION_CREATED("docs.version.created", "docs", true, true, true),
    SHEETS_WORKBOOK_IMPORTED("sheets.workbook.imported", "sheets", true, true, true),
    PASS_ITEM_UPDATED("pass.item.updated", "pass", true, true, true),
    PASS_SECURE_LINK_CREATED("pass.secure_link.created", "pass", true, true, true),
    COLLABORATION_TASK_UPDATED("collaboration.task.updated", "collaboration", true, true, true),
    COMMAND_RUN_REQUESTED("command.run.requested", "command-center", true, true, true),
    COMMAND_RUN_COMPLETED("command.run.completed", "command-center", true, true, true),
    NOTIFICATION_DELIVERY_REQUESTED("notification.delivery.requested", "notifications", true, true, true),
    ADMIN_AUDIT_RECORDED("admin.audit.recorded", "admin-governance", true, true, true),
    BILLING_ENTITLEMENT_CHANGED("billing.entitlement.changed", "billing", true, true, true),
    LABS_AI_JOB_REQUESTED("labs.ai_job.requested", "labs", true, true, true);

    private final String eventName;
    private final String ownerModule;
    private final boolean tenantRequired;
    private final boolean userRequired;
    private final boolean replayable;

    PlatformEventType(String eventName, String ownerModule, boolean tenantRequired, boolean userRequired, boolean replayable) {
        this.eventName = eventName;
        this.ownerModule = ownerModule;
        this.tenantRequired = tenantRequired;
        this.userRequired = userRequired;
        this.replayable = replayable;
    }

    public String eventName() {
        return eventName;
    }

    public String ownerModule() {
        return ownerModule;
    }

    public boolean tenantRequired() {
        return tenantRequired;
    }

    public boolean userRequired() {
        return userRequired;
    }

    public boolean replayable() {
        return replayable;
    }
}
```

- [x] **步骤 2：创建 `PlatformEventMetadata`**

```java
package com.mmmail.platform.event;

import java.time.LocalDateTime;

public record PlatformEventMetadata(
        String tenantId,
        String userId,
        String requestId,
        String traceId,
        String module,
        String operation,
        LocalDateTime occurredAt
) {

    public PlatformEventMetadata {
        if (!hasText(module)) {
            throw new IllegalArgumentException("module is required");
        }
        if (!hasText(operation)) {
            throw new IllegalArgumentException("operation is required");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt is required");
        }
    }

    public void validateFor(PlatformEventType type) {
        if (type.tenantRequired() && !hasText(tenantId)) {
            throw new IllegalArgumentException("tenantId is required for " + type.eventName());
        }
        if (type.userRequired() && !hasText(userId)) {
            throw new IllegalArgumentException("userId is required for " + type.eventName());
        }
        if (!type.ownerModule().equals(module)) {
            throw new IllegalArgumentException("module must match event owner " + type.ownerModule());
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
```

- [x] **步骤 3：创建 `PlatformEvent`**

```java
package com.mmmail.platform.event;

public record PlatformEvent(
        PlatformEventType type,
        String aggregateType,
        String aggregateId,
        PlatformEventMetadata metadata,
        String payloadJson,
        String idempotencyKey
) {

    public PlatformEvent {
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("metadata is required");
        }
        requireText(aggregateType, "aggregateType");
        requireText(aggregateId, "aggregateId");
        requireText(payloadJson, "payloadJson");
        requireText(idempotencyKey, "idempotencyKey");
        metadata.validateFor(type);
    }

    public String eventName() {
        return type.eventName();
    }

    public String ownerModule() {
        return type.ownerModule();
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
```

## 任务 3：实现平台 outbox 契约

**文件：**
- 修改：`backend/mmmail-platform/src/main/java/com/mmmail/platform/outbox/OutboxEventStatus.java`
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/outbox/OutboxEventRecord.java`
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/outbox/OutboxPublishRequest.java`
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/outbox/OutboxPublishResult.java`
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/outbox/OutboxDispatchResult.java`
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/outbox/OutboxPublisher.java`
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/outbox/OutboxDispatcher.java`

- [x] **步骤 1：扩展状态枚举**

保持现有四个值，不新增隐式状态：

```java
package com.mmmail.platform.outbox;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED,
    DEAD_LETTER
}
```

- [x] **步骤 2：创建 `OutboxEventRecord`**

实现 `pending()`、`markPublished()`、`markFailed()`、`markPendingForRetry()`、`markDeadLetter()` 五个状态入口，只允许规格中的状态流。

- [x] **步骤 3：创建请求、结果和接口**

签名固定为：

```java
public record OutboxPublishRequest(PlatformEvent event) {
    public OutboxPublishRequest {
        if (event == null) {
            throw new IllegalArgumentException("event is required");
        }
    }
}

public record OutboxPublishResult(Long eventId, OutboxEventStatus status, boolean duplicate) {
}

public record OutboxDispatchResult(int scanned, int published, int failed, int deadLettered) {
}

public interface OutboxPublisher {
    OutboxPublishResult publish(OutboxPublishRequest request);
}

public interface OutboxDispatcher {
    OutboxDispatchResult dispatchDue(int limit, LocalDateTime now);
}
```

- [x] **步骤 4：运行编译红灯推进**

运行：

```bash
timeout 60s mvn -pl mmmail-platform -am -f backend/pom.xml compile
```

预期：`mmmail-platform` 编译通过；server 测试仍因 server outbox 实现缺失失败。

## 任务 4：添加迁移、schema、entity 和 mapper

**文件：**
- 创建：`backend/mmmail-server/src/main/resources/db/migration/V11__platform_outbox_event.sql`
- 修改：`backend/mmmail-server/src/main/resources/schema.sql`
- 修改：`backend/mmmail-server/src/main/resources/db/baseline/community-v1-schema.sql`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/outbox/PlatformOutboxEvent.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/outbox/PlatformOutboxEventMapper.java`
- 修改：`backend/mmmail-server/src/main/java/com/mmmail/server/config/MybatisPlusConfig.java`

- [x] **步骤 1：创建迁移 SQL**

使用以下表结构：

```sql
create table if not exists platform_outbox_event (
    id bigint primary key,
    event_type varchar(128) not null,
    owner_module varchar(64) not null,
    tenant_id varchar(64) not null,
    user_id varchar(64),
    request_id varchar(64),
    trace_id varchar(64),
    aggregate_type varchar(64) not null,
    aggregate_id varchar(128) not null,
    payload_json text not null,
    idempotency_key varchar(128) not null,
    status varchar(32) not null,
    attempts int not null default 0,
    next_attempt_at timestamp,
    last_error varchar(512),
    created_at timestamp not null,
    updated_at timestamp not null,
    published_at timestamp
);

create unique index uk_platform_outbox_idempotency
    on platform_outbox_event(idempotency_key);

create index idx_platform_outbox_status_next_attempt
    on platform_outbox_event(status, next_attempt_at);

create index idx_platform_outbox_owner_created
    on platform_outbox_event(owner_module, created_at);

create index idx_platform_outbox_tenant_created
    on platform_outbox_event(tenant_id, created_at);

update system_release_metadata
set schema_version = '11',
    updated_at = current_timestamp
where id = 1;
```

- [x] **步骤 2：同步 schema 和 baseline**

把同一张表和四个索引追加到 `schema.sql` 与 `community-v1-schema.sql` 的末尾，确保 H2 test profile 和 legacy baseline 都能看到 `platform_outbox_event`。

- [x] **步骤 3：创建 entity 和 mapper**

`PlatformOutboxEvent` 使用 `@TableName("platform_outbox_event")`、`@TableId(type = IdType.ASSIGN_ID)`，字段与 SQL 列一一对应。`PlatformOutboxEventMapper` 放在 `com.mmmail.server.outbox`，提供：

```java
PlatformOutboxEvent findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);
List<PlatformOutboxEvent> findDue(@Param("now") LocalDateTime now, @Param("limit") int limit);
```

`findDue` 查询 `status in ('PENDING', 'FAILED')` 且 `next_attempt_at is null or next_attempt_at <= #{now}`。

- [x] **步骤 4：更新 MapperScan**

修改为：

```java
@MapperScan({"com.mmmail.server.mapper", "com.mmmail.server.outbox"})
```

## 任务 5：实现 DatabaseOutboxPublisher

**文件：**
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/outbox/DatabaseOutboxPublisher.java`

- [x] **步骤 1：实现 publisher**

实现规则：

- `publish()` 写入 `PENDING` 记录。
- 相同 `idempotencyKey` 且 event type、aggregate type、aggregate ID、payload JSON 一致时返回已有记录，`duplicate=true`。
- 相同 `idempotencyKey` 但 event identity 或 payload 不一致时抛 `IllegalStateException`。
- 记录 Micrometer counter `mmmail.outbox.events.published.total`，tags 为 `event`、`module`、`status`。

- [x] **步骤 2：运行 publisher 相关测试**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21EventOutboxFoundationTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：publisher 测试通过；dispatcher 测试仍可能因 dispatcher 未实现失败。

## 任务 6：实现 InProcessOutboxDispatcher

**文件：**
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/outbox/PlatformOutboxHandler.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/outbox/InProcessOutboxDispatcher.java`

- [x] **步骤 1：创建 handler 契约**

```java
package com.mmmail.server.outbox;

import com.mmmail.platform.outbox.OutboxEventRecord;

@FunctionalInterface
public interface PlatformOutboxHandler {
    void handle(OutboxEventRecord eventRecord);
}
```

- [x] **步骤 2：实现 dispatcher**

实现规则：

- Spring 默认 bean 使用一个显式 no-op handler：只允许测试/平台合同路径验证 dispatcher 能跑通，不代表业务事件已被消费。
- 测试构造器可以注入自定义 handler、`maxAttempts` 和 `retryDelay`。
- 成功时 `PUBLISHED`。
- 失败时 attempts + 1；未到 max attempts 设置 `FAILED` 和 `nextAttemptAt`；达到 max attempts 设置 `DEAD_LETTER`。
- 记录 Micrometer counters：`mmmail.outbox.dispatch.total`、`mmmail.outbox.dispatch.failed.total`、`mmmail.outbox.dead_letter.total`。
- 不记录 payload JSON。

- [x] **步骤 3：运行 dispatcher 相关测试**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21EventOutboxFoundationTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：`BackendV21EventOutboxFoundationTest` 全部通过，输出 `Failures: 0`、`Errors: 0`。

## 任务 7：完整验证并提交实现

**文件：**
- 所有任务 1-6 中的源码、测试、SQL、进度文件。

- [x] **步骤 1：运行后端目标测试**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21EventOutboxFoundationTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：退出码 0，`Tests run` 大于 0，`Failures: 0`，`Errors: 0`。

- [x] **步骤 2：运行后端编译**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml compile
```

预期：退出码 0，`BUILD SUCCESS`。

- [x] **步骤 3：运行前端 v2.1 回归**

```bash
timeout 60s pnpm --dir frontend-v2 test
```

预期：退出码 0，`# fail 0`。

- [x] **步骤 4：提交实现**

```bash
git status --short --branch
git add backend/mmmail-platform/src/main/java/com/mmmail/platform/event/PlatformEventType.java
git add backend/mmmail-platform/src/main/java/com/mmmail/platform/event/PlatformEventMetadata.java
git add backend/mmmail-platform/src/main/java/com/mmmail/platform/event/PlatformEvent.java
git add backend/mmmail-platform/src/main/java/com/mmmail/platform/outbox/OutboxEventStatus.java
git add backend/mmmail-platform/src/main/java/com/mmmail/platform/outbox/OutboxEventRecord.java
git add backend/mmmail-platform/src/main/java/com/mmmail/platform/outbox/OutboxPublishRequest.java
git add backend/mmmail-platform/src/main/java/com/mmmail/platform/outbox/OutboxPublishResult.java
git add backend/mmmail-platform/src/main/java/com/mmmail/platform/outbox/OutboxDispatchResult.java
git add backend/mmmail-platform/src/main/java/com/mmmail/platform/outbox/OutboxPublisher.java
git add backend/mmmail-platform/src/main/java/com/mmmail/platform/outbox/OutboxDispatcher.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/outbox/PlatformOutboxEvent.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/outbox/PlatformOutboxEventMapper.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/outbox/PlatformOutboxHandler.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/outbox/DatabaseOutboxPublisher.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/outbox/InProcessOutboxDispatcher.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/config/MybatisPlusConfig.java
git add backend/mmmail-server/src/main/resources/db/migration/V11__platform_outbox_event.sql
git add backend/mmmail-server/src/main/resources/schema.sql
git add backend/mmmail-server/src/main/resources/db/baseline/community-v1-schema.sql
git add backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21EventOutboxFoundationTest.java
git add -f docs/superpowers/progress/v21-implementation-progress.md
git diff --cached --check
git diff --cached --stat
git commit -m "feat(backend-v21): add event outbox foundation"
```

## 任务 8：更新完成进度并提交

**文件：**
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`

- [x] **步骤 1：更新完成记录**

移除 `## Active Backend Slice`，增加：

```markdown
## Latest Completed Backend Slice

- Slice: `backend-v21-event-outbox-foundation`
- Commit: use the exact output of `git log --oneline -1` after the implementation commit `feat(backend-v21): add event outbox foundation`
- Files changed: added immutable platform event contracts, database-backed outbox persistence, in-process dispatcher, outbox migration, and backend event/outbox foundation tests.
- Verification:
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21EventOutboxFoundationTest -Dsurefire.failIfNoSpecifiedTests=false`: PASS
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml compile`: PASS
  - `timeout 60s pnpm --dir frontend-v2 test`: PASS
```

在 `Completed v2.1 Slices` 表加入：

```markdown
| Backend event outbox foundation (`backend-v21-event-outbox-foundation`) | `BackendV21EventOutboxFoundationTest`, `platform_outbox_event`, `OutboxPublisher`, `InProcessOutboxDispatcher` |
```

- [x] **步骤 2：提交进度**

```bash
git status --short --branch
git add -f docs/superpowers/progress/v21-implementation-progress.md
git diff --cached --check
git diff --cached --stat
git commit -m "docs(backend-v21): update event outbox progress"
git status --short --branch
```

## 自检清单

- 规格第 4 节事件目录由任务 2 覆盖。
- 规格第 5 节 outbox 契约由任务 3 覆盖。
- 规格第 6 节 server persistence 由任务 4 覆盖。
- 规格第 7 节 runtime implementation 由任务 5 和任务 6 覆盖。
- 规格第 8 节 failure handling 由任务 3 和任务 6 覆盖。
- 规格第 9 节 observability 由任务 5 和任务 6 覆盖。
- 规格第 10 节 tests 由任务 1、任务 7 覆盖。
- 不添加 Kafka、外部队列、worker 服务或业务 API mock。
- 不改动现有业务服务去发事件。
- 所有新增 Java 文件保持单一职责，单文件低于 500 行，函数低于 50 行。
