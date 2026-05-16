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

    private static final Set<String> REQUIRED_JOBS = Set.of(
            "notification.delivery",
            "mail.delivery",
            "file.preview",
            "command.run",
            "ai.labs",
            "billing.entitlement_sync",
            "audit.export",
            "dsr.export",
            "dsr.erasure"
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

        assertThat(names).containsAll(REQUIRED_JOBS);
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
        LocalDateTime now = LocalDateTime.now();
        JobRunRecord queued = JobRunRecord.queued(1L, request(), now);
        JobRunRecord running = queued.markRunning(now);
        JobRunRecord succeeded = running.withProgress(100, now).markSucceeded("{\"ok\":true}", now);

        assertThat(succeeded.status()).isEqualTo(JobRunState.SUCCEEDED);
        assertThat(succeeded.progressPercent()).isEqualTo(100);
        assertThatThrownBy(() -> succeeded.markRetryable("late", "late retry", now.plusMinutes(1), now))
                .hasMessageContaining("Invalid job run status transition");
        assertThatThrownBy(() -> running.withProgress(101, now))
                .hasMessageContaining("progressPercent must be between 0 and 100");
    }

    @Test
    void repositoryShouldPersistQueuedJobsAndRejectIdempotencyMismatch() {
        JobRunRecord first = repository.enqueue(request());
        PlatformJobRun saved = mapper.selectById(first.id());

        assertThat(first.status()).isEqualTo(JobRunState.QUEUED);
        assertThat(saved.getJobType()).isEqualTo("command.run");
        assertThat(saved.getTenantId()).isEqualTo("tenant-1");
        assertThat(saved.getProgressPercent()).isEqualTo(0);
        assertThat(saved.getStatus()).isEqualTo("QUEUED");

        JobRunRecord duplicate = repository.enqueue(request());
        assertThat(duplicate.id()).isEqualTo(first.id());

        assertThatThrownBy(() -> repository.enqueue(requestWithPayload("{\"command\":\"archive\"}")))
                .hasMessageContaining("idempotency key already belongs to a different job");
    }

    @Test
    void runnerShouldMarkSuccessfulJobsAsSucceeded() {
        JobRunRecord queued = repository.enqueue(request());
        JobRunHandler handler = record -> "{\"handled\":\"" + record.aggregateId() + "\"}";
        JobRunner runner = new InProcessJobRunner(
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
        assertThat(saved.getResultJson()).contains("\"handled\":\"run-100\"");
    }

    @Test
    void runnerShouldMarkRetryableFailuresWhenAttemptsRemain() {
        JobRunRecord queued = repository.enqueue(request());
        JobRunHandler handler = record -> {
            throw new IllegalStateException("handler failed");
        };
        JobRunner runner = new InProcessJobRunner(
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
        String migration = Files.readString(root.resolve("src/main/resources/db/migration/V17__platform_job_run.sql"));
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
                .noneMatch(name -> name.toLowerCase().contains("kafka"))).isTrue();
        assertThat(Arrays.stream(applicationContext.getBeanDefinitionNames())
                .noneMatch(name -> name.toLowerCase().contains("externalworker"))).isTrue();
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
