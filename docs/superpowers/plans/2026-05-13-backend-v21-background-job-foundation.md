# Backend v2.1 Background Job Foundation 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 实现 v2.1 后端后台任务基础，让 Community 以数据库 job run + in-process runner 运行，同时为 Hosted/Premium worker 抽取保留稳定契约。

**架构：** `mmmail-platform` 定义不可变 job 类型、metadata、request、record、runner 契约；`mmmail-server` 提供 `platform_job_run` MyBatis 持久化、数据库 repository、显式 handler registry 和 in-process runner。当前切片不接外部队列，不抽 worker，不实现业务 command-center 端点。

**技术栈：** Java 21 records/enums, Spring Boot, MyBatis-Plus, H2 test profile, Flyway SQL, Micrometer, JUnit 5, AssertJ。

---

## 文件结构

- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/JobRunType.java`
  冻结 Phase 3 worker-ready job 类型目录，暴露 job name、owner module、metadata 规则、重试能力和 Hosted/Premium 抽取信号。
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/JobRunMetadata.java`
  不可变 metadata 记录，校验 tenant/user/request/trace/module/operation/requestedAt。
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/JobRunRequest.java`
  不可变 job 入队请求，校验 aggregate、payload、idempotency 和 maxAttempts。
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/JobRunRecord.java`
  不可变 job run 记录，负责显式状态流转和 progress 边界。
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/JobRunResult.java`
  runner 批处理结果。
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/JobRunHandler.java`
  单个 job handler 契约。
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/JobRunner.java`
  runner 契约。
- 修改：`backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/JobRunState.java`
  保留已有枚举值，补充显式流转所需的状态验证由 `JobRunRecord` 承担。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/jobs/PlatformJobRun.java`
  MyBatis-Plus entity，对应 `platform_job_run`。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/jobs/PlatformJobRunMapper.java`
  幂等键查询和 due job 查询。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/jobs/DatabaseJobRunRepository.java`
  入队、读取、更新、due 查询。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/jobs/ExplicitJobRunHandlerRegistry.java`
  显式 handler registry；默认无 handler，测试可注入 handler map。
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/jobs/InProcessJobRunner.java`
  Community in-process runner。
- 修改：`backend/mmmail-server/src/main/java/com/mmmail/server/config/MybatisPlusConfig.java`
  让 MyBatis 扫描 `com.mmmail.server.jobs`。
- 创建：`backend/mmmail-server/src/main/resources/db/migration/V12__platform_job_run.sql`
- 修改：`backend/mmmail-server/src/main/resources/schema.sql`
- 修改：`backend/mmmail-server/src/main/resources/db/baseline/community-v1-schema.sql`
- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21BackgroundJobFoundationTest.java`
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`

## 任务 1：记录活动切片并编写失败测试

**文件：**
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`
- 创建：`backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21BackgroundJobFoundationTest.java`

- [x] **步骤 1：记录活动切片**

在 `## Remaining v2.1 Risks` 之前插入：

```markdown
## Active Backend Slice

- Slice: `backend-v21-background-job-foundation`
- Status: `in_progress`
- Started: `2026-05-13`
- Scope: platform job contract, persisted job runs, database repository, in-process runner, migration, tests
- Verification target: `BackendV21BackgroundJobFoundationTest`, backend compile, frontend v2.1 test suite
```

- [x] **步骤 2：编写失败测试**

创建 `BackendV21BackgroundJobFoundationTest.java`，测试先引用还不存在的 job 类型与 server 实现：

```java
package com.mmmail.server;

import com.mmmail.platform.jobs.JobRunHandler;
import com.mmmail.platform.jobs.JobRunMetadata;
import com.mmmail.platform.jobs.JobRunRecord;
import com.mmmail.platform.jobs.JobRunRequest;
import com.mmmail.platform.jobs.JobRunResult;
import com.mmmail.platform.jobs.JobRunState;
import com.mmmail.platform.jobs.JobRunType;
import com.mmmail.platform.jobs.JobRunner;
import com.mmmail.server.jobs.DatabaseJobRunRepository;
import com.mmmail.server.jobs.ExplicitJobRunHandlerRegistry;
import com.mmmail.server.jobs.InProcessJobRunner;
import com.mmmail.server.jobs.PlatformJobRun;
import com.mmmail.server.jobs.PlatformJobRunMapper;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class BackendV21BackgroundJobFoundationTest {

    private static final Set<String> REQUIRED_JOB_TYPES = Set.of(
            "notification.delivery",
            "mail.delivery",
            "file.preview",
            "command.run",
            "ai.labs",
            "billing.entitlement_sync",
            "audit.export"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private DatabaseJobRunRepository repository;
    @Autowired
    private PlatformJobRunMapper mapper;
    @Autowired
    private JobRunner jobRunner;
    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void cleanJobs() {
        jdbcTemplate.update("delete from platform_job_run");
    }

    @Test
    void jobCatalogShouldCoverWorkerExtractionCandidates() {
        Set<String> names = Arrays.stream(JobRunType.values())
                .map(JobRunType::jobName)
                .collect(Collectors.toSet());

        assertThat(names).containsAll(REQUIRED_JOB_TYPES);
        assertThat(JobRunType.COMMAND_RUN.ownerModule()).isEqualTo("command-center");
        assertThat(JobRunType.AUDIT_EXPORT.hostedExtractionCandidate()).isTrue();
        assertThat(JobRunType.NOTIFICATION_DELIVERY.retrySupported()).isTrue();
    }

    @Test
    void jobRequestShouldRejectMissingRequiredMetadata() {
        JobRunMetadata metadata = new JobRunMetadata(
                "",
                "7",
                "req-1",
                "trace-1",
                "command-center",
                "run",
                LocalDateTime.now()
        );

        assertThatThrownBy(() -> new JobRunRequest(
                JobRunType.COMMAND_RUN,
                metadata,
                "command",
                "run-100",
                "{\"command\":\"sync\"}",
                "command-run-100",
                3
        )).hasMessageContaining("tenantId is required");
    }

    @Test
    void jobRecordShouldEnforceExplicitStatusTransitionsAndProgressBounds() {
        JobRunRecord queued = JobRunRecord.queued(1L, request(), LocalDateTime.now());
        JobRunRecord running = queued.markRunning(LocalDateTime.now());
        JobRunRecord succeeded = running.withProgress(100, LocalDateTime.now()).markSucceeded("{\"ok\":true}", LocalDateTime.now());

        assertThat(succeeded.status()).isEqualTo(JobRunState.SUCCEEDED);
        assertThatThrownBy(() -> succeeded.markRetryable("late", "late retry", LocalDateTime.now(), LocalDateTime.now()))
                .hasMessageContaining("Invalid job run status transition");
        assertThatThrownBy(() -> running.withProgress(101, LocalDateTime.now()))
                .hasMessageContaining("progressPercent must be between 0 and 100");
    }

    @Test
    void repositoryShouldPersistQueuedJobsAndRejectIdempotencyMismatch() {
        JobRunRecord first = repository.enqueue(request());
        PlatformJobRun saved = mapper.selectById(first.id());

        assertThat(first.status()).isEqualTo(JobRunState.QUEUED);
        assertThat(saved.getJobType()).isEqualTo("command.run");
        assertThat(saved.getTenantId()).isEqualTo("tenant-1");
        assertThat(saved.getProgressPercent()).isZero();
        assertThat(saved.getStatus()).isEqualTo("QUEUED");

        JobRunRecord duplicate = repository.enqueue(request());
        assertThat(duplicate.id()).isEqualTo(first.id());

        assertThatThrownBy(() -> repository.enqueue(requestWithPayload("{\"command\":\"different\"}")))
                .hasMessageContaining("idempotency key already belongs to a different job");
    }

    @Test
    void runnerShouldMarkSuccessfulJobsAsSucceeded() {
        JobRunRecord queued = repository.enqueue(request());
        JobRunHandler handler = record -> "{\"handled\":\"" + record.aggregateId() + "\"}";
        InProcessJobRunner runner = new InProcessJobRunner(
                repository,
                new SimpleMeterRegistry(),
                new InProcessJobRunner.RunnerOptions(
                        ExplicitJobRunHandlerRegistry.of(Map.of(JobRunType.COMMAND_RUN, handler)),
                        10,
                        Duration.ZERO
                )
        );

        JobRunResult result = runner.runDue(10, LocalDateTime.now());
        PlatformJobRun saved = mapper.selectById(queued.id());

        assertThat(result.succeeded()).isEqualTo(1);
        assertThat(saved.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(saved.getProgressPercent()).isEqualTo(100);
        assertThat(saved.getResultJson()).contains("handled");
    }

    @Test
    void runnerShouldMarkRetryableFailuresWhenAttemptsRemain() {
        JobRunRecord queued = repository.enqueue(request());
        JobRunHandler handler = record -> {
            throw new IllegalStateException("handler failed");
        };
        InProcessJobRunner runner = new InProcessJobRunner(
                repository,
                new SimpleMeterRegistry(),
                new InProcessJobRunner.RunnerOptions(
                        ExplicitJobRunHandlerRegistry.of(Map.of(JobRunType.COMMAND_RUN, handler)),
                        10,
                        Duration.ZERO
                )
        );

        JobRunResult result = runner.runDue(10, LocalDateTime.now());
        PlatformJobRun saved = mapper.selectById(queued.id());

        assertThat(result.retryable()).isEqualTo(1);
        assertThat(saved.getStatus()).isEqualTo("RETRYABLE");
        assertThat(saved.getAttempts()).isEqualTo(1);
        assertThat(saved.getLastErrorCode()).isEqualTo("JOB_HANDLER_FAILED");
    }

    @Test
    void runnerShouldFailJobsWithoutRegisteredHandlers() {
        JobRunRecord queued = repository.enqueue(request());
        JobRunResult result = jobRunner.runDue(10, LocalDateTime.now());
        PlatformJobRun saved = mapper.selectById(queued.id());

        assertThat(result.failed()).isEqualTo(1);
        assertThat(saved.getStatus()).isEqualTo("FAILED");
        assertThat(saved.getLastErrorCode()).isEqualTo("JOB_HANDLER_MISSING");
    }

    @Test
    void migrationShouldFreezeJobRunTableAndIndexes() throws Exception {
        Path root = resolveRepoRoot();
        String migration = Files.readString(root.resolve("src/main/resources/db/migration/V12__platform_job_run.sql"));
        String schema = Files.readString(root.resolve("src/main/resources/schema.sql"));

        assertThat(migration).contains("create table if not exists platform_job_run");
        assertThat(migration).contains("uk_platform_job_run_idempotency");
        assertThat(migration).contains("idx_platform_job_run_status_next_attempt");
        assertThat(migration).contains("idx_platform_job_run_tenant_created");
        assertThat(schema).contains("create table if not exists platform_job_run");
    }

    @Test
    void communityTestProfileShouldUseInProcessRunnerWithoutExternalWorkerBeans() {
        assertThat(applicationContext.getBean(JobRunner.class)).isNotNull();
        assertThat(applicationContext.getBean(InProcessJobRunner.class)).isNotNull();
        assertThat(Arrays.stream(applicationContext.getBeanDefinitionNames())
                .noneMatch(name -> name.toLowerCase().contains("kafka") || name.toLowerCase().contains("externalworker")))
                .isTrue();
    }

    private JobRunRequest request() {
        return requestWithPayload("{\"command\":\"sync\"}");
    }

    private JobRunRequest requestWithPayload(String payloadJson) {
        return new JobRunRequest(
                JobRunType.COMMAND_RUN,
                metadata(),
                "command",
                "run-100",
                payloadJson,
                "command-run-100",
                3
        );
    }

    private JobRunMetadata metadata() {
        return new JobRunMetadata(
                "tenant-1",
                "7",
                "req-1",
                "trace-1",
                "command-center",
                "run",
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
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21BackgroundJobFoundationTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：编译失败，错误包含 `package com.mmmail.server.jobs does not exist` 或 `cannot find symbol JobRunType`。这证明测试正在覆盖未实现的切片。

## 任务 2：实现 platform job 契约

**文件：**
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/JobRunType.java`
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/JobRunMetadata.java`
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/JobRunRequest.java`
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/JobRunRecord.java`
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/JobRunResult.java`
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/JobRunHandler.java`
- 创建：`backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/JobRunner.java`

- [x] **步骤 1：创建 `JobRunType`**

实现 enum 常量与 `fromJobName()`。常量：

```java
NOTIFICATION_DELIVERY("notification.delivery", "notifications", true, true, true, true)
MAIL_DELIVERY("mail.delivery", "mail", true, true, true, true)
FILE_PREVIEW("file.preview", "drive", true, true, true, true)
COMMAND_RUN("command.run", "command-center", true, true, true, true)
AI_LABS("ai.labs", "labs", true, true, true, true)
BILLING_ENTITLEMENT_SYNC("billing.entitlement_sync", "billing", true, true, true, true)
AUDIT_EXPORT("audit.export", "admin-governance", true, true, true, true)
```

暴露方法：`jobName()`、`ownerModule()`、`tenantRequired()`、`userRequired()`、`retrySupported()`、`hostedExtractionCandidate()`、`fromJobName(String jobName)`。

- [x] **步骤 2：创建 `JobRunMetadata`**

字段固定为：

```java
public record JobRunMetadata(
        String tenantId,
        String userId,
        String requestId,
        String traceId,
        String module,
        String operation,
        LocalDateTime requestedAt
)
```

构造器校验 `module`、`operation`、`requestedAt`。方法 `validateFor(JobRunType type)` 校验必需 tenant/user，并要求 `module` 等于 `type.ownerModule()`。

- [x] **步骤 3：创建 `JobRunRequest`**

字段固定为：

```java
public record JobRunRequest(
        JobRunType type,
        JobRunMetadata metadata,
        String aggregateType,
        String aggregateId,
        String payloadJson,
        String idempotencyKey,
        int maxAttempts
)
```

校验规则：`type`、`metadata` 非空；`aggregateType`、`aggregateId`、`payloadJson`、`idempotencyKey` 非空；`maxAttempts >= 1`；调用 `metadata.validateFor(type)`。

- [x] **步骤 4：创建 `JobRunRecord`**

字段固定为：

```java
public record JobRunRecord(
        Long id,
        JobRunType type,
        String ownerModule,
        String tenantId,
        String userId,
        String requestId,
        String traceId,
        String aggregateType,
        String aggregateId,
        String payloadJson,
        String idempotencyKey,
        JobRunState status,
        int progressPercent,
        int attempts,
        int maxAttempts,
        LocalDateTime nextAttemptAt,
        String lastErrorCode,
        String lastErrorMessage,
        String resultJson,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt
)
```

实现方法：

```java
public static JobRunRecord queued(Long id, JobRunRequest request, LocalDateTime now)
public JobRunRecord markRunning(LocalDateTime now)
public JobRunRecord markWaitingApproval(LocalDateTime now)
public JobRunRecord markSucceeded(String resultJson, LocalDateTime now)
public JobRunRecord markFailed(String errorCode, String errorMessage, LocalDateTime now)
public JobRunRecord markRetryable(String errorCode, String errorMessage, LocalDateTime nextAttemptAt, LocalDateTime now)
public JobRunRecord markQueuedForRetry(LocalDateTime now)
public JobRunRecord withProgress(int progressPercent, LocalDateTime now)
public String jobName()
```

状态流只允许：

```text
QUEUED -> RUNNING
RUNNING -> WAITING_APPROVAL
RUNNING -> SUCCEEDED
RUNNING -> FAILED
RUNNING -> RETRYABLE
WAITING_APPROVAL -> RUNNING
WAITING_APPROVAL -> FAILED
RETRYABLE -> QUEUED
RETRYABLE -> FAILED
```

错误消息裁剪到 `512` 字符，progress 必须为 `0..100`。

- [x] **步骤 5：创建 runner 相关接口和结果**

签名固定为：

```java
public record JobRunResult(int scanned, int succeeded, int retryable, int failed) {
}
```

```java
@FunctionalInterface
public interface JobRunHandler {
    String handle(JobRunRecord record);
}
```

```java
public interface JobRunner {
    JobRunResult runDue(int limit, LocalDateTime now);
}
```

- [x] **步骤 6：运行 platform 编译**

运行：

```bash
timeout 60s mvn -pl mmmail-platform -am -f backend/pom.xml compile
```

预期：`BUILD SUCCESS`。如果失败，只修复 platform job 契约编译问题，不修改 server 端测试断言。

## 任务 3：实现 job run schema、entity 和 mapper

**文件：**
- 创建：`backend/mmmail-server/src/main/resources/db/migration/V12__platform_job_run.sql`
- 修改：`backend/mmmail-server/src/main/resources/schema.sql`
- 修改：`backend/mmmail-server/src/main/resources/db/baseline/community-v1-schema.sql`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/jobs/PlatformJobRun.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/jobs/PlatformJobRunMapper.java`
- 修改：`backend/mmmail-server/src/main/java/com/mmmail/server/config/MybatisPlusConfig.java`

- [x] **步骤 1：创建迁移 SQL**

创建 `V12__platform_job_run.sql`：

```sql
create table if not exists platform_job_run (
    id bigint primary key,
    job_type varchar(128) not null,
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
    progress_percent int not null default 0,
    attempts int not null default 0,
    max_attempts int not null default 3,
    next_attempt_at timestamp,
    last_error_code varchar(64),
    last_error_message varchar(512),
    result_json text,
    created_at timestamp not null,
    updated_at timestamp not null,
    started_at timestamp,
    completed_at timestamp
);

create unique index uk_platform_job_run_idempotency
    on platform_job_run(idempotency_key);
create index idx_platform_job_run_status_next_attempt
    on platform_job_run(status, next_attempt_at);
create index idx_platform_job_run_owner_created
    on platform_job_run(owner_module, created_at);
create index idx_platform_job_run_tenant_created
    on platform_job_run(tenant_id, created_at);
create index idx_platform_job_run_type_created
    on platform_job_run(job_type, created_at);

update system_release_metadata
set schema_version = '12',
    updated_at = current_timestamp
where id = 1;
```

- [x] **步骤 2：同步 schema 和 baseline**

把同一张表和五个索引追加到 `schema.sql` 与 `community-v1-schema.sql` 末尾。`schema.sql` 和 baseline 不需要追加 `update system_release_metadata`。

- [x] **步骤 3：创建 `PlatformJobRun` entity**

`PlatformJobRun` 使用：

```java
@TableName("platform_job_run")
public class PlatformJobRun {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String jobType;
    private String ownerModule;
    private String tenantId;
    private String userId;
    private String requestId;
    private String traceId;
    private String aggregateType;
    private String aggregateId;
    private String payloadJson;
    private String idempotencyKey;
    private String status;
    private Integer progressPercent;
    private Integer attempts;
    private Integer maxAttempts;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime nextAttemptAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String lastErrorCode;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String lastErrorMessage;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String resultJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime startedAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime completedAt;
}
```

为所有字段生成 getter/setter。不要添加 `@TableLogic`，该表没有 `deleted` 列。

- [x] **步骤 4：创建 `PlatformJobRunMapper`**

```java
@Mapper
public interface PlatformJobRunMapper extends BaseMapper<PlatformJobRun> {

    @Select("""
            select *
            from platform_job_run
            where idempotency_key = #{idempotencyKey}
            limit 1
            """)
    PlatformJobRun findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    @Select("""
            select *
            from platform_job_run
            where status in ('QUEUED', 'RETRYABLE')
              and (next_attempt_at is null or next_attempt_at <= #{now})
            order by created_at, id
            limit #{limit}
            """)
    List<PlatformJobRun> findDue(@Param("now") LocalDateTime now, @Param("limit") int limit);
}
```

- [x] **步骤 5：更新 MapperScan**

把 `MybatisPlusConfig` 改为：

```java
@MapperScan({"com.mmmail.server.mapper", "com.mmmail.server.outbox", "com.mmmail.server.jobs"})
```

- [x] **步骤 6：运行 schema 相关目标测试**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21BackgroundJobFoundationTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：不再因为 `platform_job_run` 表缺失失败；仍会因为 repository/runner 类缺失或未实现失败。

## 任务 4：实现数据库 job repository

**文件：**
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/jobs/DatabaseJobRunRepository.java`

- [x] **步骤 1：实现 repository API**

创建 Spring service：

```java
@Service
public class DatabaseJobRunRepository {
    private static final int MAX_BATCH_SIZE = 100;

    private final PlatformJobRunMapper mapper;

    public DatabaseJobRunRepository(PlatformJobRunMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public JobRunRecord enqueue(JobRunRequest request) {
        PlatformJobRun existing = mapper.findByIdempotencyKey(request.idempotencyKey());
        if (existing != null) {
            return duplicateResult(existing, request);
        }
        PlatformJobRun entity = toEntity(JobRunRecord.queued(null, request, LocalDateTime.now()));
        mapper.insert(entity);
        return toRecord(entity);
    }

    public JobRunRecord findById(Long id) {
        PlatformJobRun entity = mapper.selectById(id);
        if (entity == null) {
            throw new IllegalStateException("Job run does not exist: " + id);
        }
        return toRecord(entity);
    }

    public List<JobRunRecord> findDue(int limit, LocalDateTime now) {
        return mapper.findDue(now, safeLimit(limit)).stream().map(DatabaseJobRunRepository::toRecord).toList();
    }

    @Transactional
    public JobRunRecord update(JobRunRecord record) {
        PlatformJobRun entity = toEntity(record);
        mapper.updateById(entity);
        return toRecord(mapper.selectById(record.id()));
    }
}
```

同时实现私有 helper：`duplicateResult()`、`sameJob()`、`toEntity()`、`toRecord()`、`safeLimit()`。

- [x] **步骤 2：幂等冲突规则**

`sameJob()` 必须比较：

```java
existing.getJobType()
existing.getAggregateType()
existing.getAggregateId()
existing.getPayloadJson()
```

不一致时抛出：

```text
idempotency key already belongs to a different job
```

- [x] **步骤 3：运行 repository 相关测试**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21BackgroundJobFoundationTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：repository 入队与幂等测试通过；runner 相关测试仍失败。

## 任务 5：实现 in-process job runner

**文件：**
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/jobs/ExplicitJobRunHandlerRegistry.java`
- 创建：`backend/mmmail-server/src/main/java/com/mmmail/server/jobs/InProcessJobRunner.java`

- [x] **步骤 1：创建显式 handler registry**

实现：

```java
public class ExplicitJobRunHandlerRegistry {
    private final Map<JobRunType, JobRunHandler> handlers;

    public ExplicitJobRunHandlerRegistry(Map<JobRunType, JobRunHandler> handlers) {
        this.handlers = Map.copyOf(handlers);
    }

    public Optional<JobRunHandler> find(JobRunType type) {
        return Optional.ofNullable(handlers.get(type));
    }

    public static ExplicitJobRunHandlerRegistry empty() {
        return new ExplicitJobRunHandlerRegistry(Map.of());
    }

    public static ExplicitJobRunHandlerRegistry of(Map<JobRunType, JobRunHandler> handlers) {
        return new ExplicitJobRunHandlerRegistry(handlers);
    }
}
```

- [x] **步骤 2：实现 runner 构造器和 options**

`InProcessJobRunner` 使用 `@Service` 并实现 `JobRunner`：

```java
@Service
public class InProcessJobRunner implements JobRunner {
    private static final int DEFAULT_BATCH_LIMIT = 100;
    private static final int ERROR_MAX_LENGTH = 512;
    private static final Duration DEFAULT_RETRY_DELAY = Duration.ofMinutes(1);
    private static final String ERROR_HANDLER_MISSING = "JOB_HANDLER_MISSING";
    private static final String ERROR_HANDLER_FAILED = "JOB_HANDLER_FAILED";

    private final DatabaseJobRunRepository repository;
    private final MeterRegistry meterRegistry;
    private final RunnerOptions options;

    @Autowired
    public InProcessJobRunner(DatabaseJobRunRepository repository, MeterRegistry meterRegistry) {
        this(repository, meterRegistry, RunnerOptions.defaultOptions());
    }

    public InProcessJobRunner(DatabaseJobRunRepository repository, MeterRegistry meterRegistry, RunnerOptions options) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;
        this.options = options;
    }
}
```

`RunnerOptions`：

```java
public record RunnerOptions(
        ExplicitJobRunHandlerRegistry registry,
        int batchLimit,
        Duration retryDelay
) {
    public RunnerOptions {
        if (registry == null) {
            throw new IllegalArgumentException("registry is required");
        }
        if (batchLimit < 1) {
            throw new IllegalArgumentException("batchLimit must be positive");
        }
        if (retryDelay == null) {
            throw new IllegalArgumentException("retryDelay is required");
        }
    }

    public static RunnerOptions defaultOptions() {
        return new RunnerOptions(ExplicitJobRunHandlerRegistry.empty(), DEFAULT_BATCH_LIMIT, DEFAULT_RETRY_DELAY);
    }
}
```

- [x] **步骤 3：实现 `runDue()`**

行为：

1. 读取 due jobs。
2. `QUEUED` 直接 `markRunning(now)`。
3. `RETRYABLE` 先 `markQueuedForRetry(now)`，再 `markRunning(now)`。
4. 无 handler：`markFailed("JOB_HANDLER_MISSING", "No job handler registered for " + jobName, now)`。
5. handler 成功：`withProgress(100, now).markSucceeded(resultJson, now)`。
6. handler 抛异常且支持 retry 且 `attempts + 1 < maxAttempts`：`markRetryable("JOB_HANDLER_FAILED", message, now.plus(retryDelay), now)`。
7. handler 抛异常但不可重试或次数耗尽：`markFailed("JOB_HANDLER_FAILED", message, now)`。
8. 返回 `JobRunResult(scanned, succeeded, retryable, failed)`。

- [x] **步骤 4：实现 metrics**

使用 Micrometer counters/timer：

```text
mmmail.jobs.runs.total
mmmail.jobs.runs.failed.total
mmmail.jobs.runs.retryable.total
mmmail.jobs.run.duration
```

tags 固定为：

```text
job
module
status
```

不要把 `payloadJson`、`resultJson`、错误 detail 写进 tag。

- [x] **步骤 5：运行 runner 相关测试**

运行：

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21BackgroundJobFoundationTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：`BackendV21BackgroundJobFoundationTest` 全部通过，输出 `Failures: 0, Errors: 0`。

## 任务 6：验证并提交实现

**文件：**
- 所有任务 1-5 的源码、测试、SQL 文件。

- [x] **步骤 1：运行后端目标测试**

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21BackgroundJobFoundationTest -Dsurefire.failIfNoSpecifiedTests=false
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

只暂存本切片相关源码、测试和 SQL：

```bash
git status --short --branch
git add backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/JobRunType.java
git add backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/JobRunMetadata.java
git add backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/JobRunRequest.java
git add backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/JobRunRecord.java
git add backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/JobRunResult.java
git add backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/JobRunHandler.java
git add backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/JobRunner.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/config/MybatisPlusConfig.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/jobs/PlatformJobRun.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/jobs/PlatformJobRunMapper.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/jobs/DatabaseJobRunRepository.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/jobs/ExplicitJobRunHandlerRegistry.java
git add backend/mmmail-server/src/main/java/com/mmmail/server/jobs/InProcessJobRunner.java
git add backend/mmmail-server/src/main/resources/db/migration/V12__platform_job_run.sql
git add backend/mmmail-server/src/main/resources/schema.sql
git add backend/mmmail-server/src/main/resources/db/baseline/community-v1-schema.sql
git add backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21BackgroundJobFoundationTest.java
git diff --cached --check
git diff --cached --stat
git commit -m "feat(backend-v21): add background job foundation"
```

## 任务 7：更新进度并提交文档

**文件：**
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`
- 修改：`docs/superpowers/plans/2026-05-13-backend-v21-background-job-foundation.md`

- [x] **步骤 1：更新进度文档**

把 `Latest backend implementation commit` 更新为实现提交 hash，把 `Local branch status at progress capture` 更新为提交后的 ahead 数。

在 `Completed v2.1 Slices` 表格追加：

```markdown
| Backend background job foundation (`backend-v21-background-job-foundation`) | `BackendV21BackgroundJobFoundationTest`, `platform_job_run`, `DatabaseJobRunRepository`, `InProcessJobRunner` |
```

把 `Latest Completed Backend Slice` 改为下面结构，`Commit` 行使用任务 6 实现提交的真实 hash：

```markdown
## Latest Completed Backend Slice

- Slice: `backend-v21-background-job-foundation`
- Commit: 使用任务 6 完成后 `git log --oneline -1` 输出的实现提交，提交主题必须是 `feat(backend-v21): add background job foundation`
- Files changed: added immutable platform job contracts, MyBatis `platform_job_run` persistence, Flyway/schema/baseline table definitions, database-backed repository, in-process runner, and focused Spring Boot coverage.
- Verification:
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21BackgroundJobFoundationTest -Dsurefire.failIfNoSpecifiedTests=false`: PASS
  - `timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml compile`: PASS
  - `timeout 60s pnpm --dir frontend-v2 test`: PASS (`83/83`)
```

把 `Active Backend Slice` 的状态改为：

```markdown
- Status: `completed`
- Completed: `2026-05-13`
```

- [x] **步骤 2：勾选计划任务**

把本计划中所有 `- [ ]` 改为 `- [x]`，只改当前计划文件。

- [x] **步骤 3：提交进度**

`docs/superpowers` 被 ignore，必须只强制暂存这两个文档：

```bash
git status --short --branch
git add -f docs/superpowers/progress/v21-implementation-progress.md docs/superpowers/plans/2026-05-13-backend-v21-background-job-foundation.md
git diff --cached --check
git diff --cached --stat
git commit -m "docs(backend-v21): update background job progress"
git status --short --branch
```

预期：工作树只剩既有无关未跟踪项，例如 `.superpowers/`、`.tmp/`、`docs/MMMail.zip`、`docs/MMMail/`、`frontend/`。
