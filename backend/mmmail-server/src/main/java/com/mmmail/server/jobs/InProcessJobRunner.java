package com.mmmail.server.jobs;

import com.mmmail.platform.jobs.JobRunHandler;
import com.mmmail.platform.jobs.JobRunRecord;
import com.mmmail.platform.jobs.JobRunRepository;
import com.mmmail.platform.jobs.JobRunResult;
import com.mmmail.platform.jobs.JobRunState;
import com.mmmail.platform.jobs.JobRunner;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InProcessJobRunner implements JobRunner {

    private static final int DEFAULT_BATCH_LIMIT = 100;
    private static final int ERROR_MAX_LENGTH = 512;
    private static final String ERROR_HANDLER_FAILED = "JOB_HANDLER_FAILED";
    private static final String ERROR_HANDLER_MISSING = "JOB_HANDLER_MISSING";
    private static final String METRIC_RUNS_TOTAL = "mmmail.jobs.runs.total";
    private static final Duration DEFAULT_RETRY_DELAY = Duration.ofMinutes(1);

    private final JobRunRepository repository;
    private final MeterRegistry meterRegistry;
    private final RunnerOptions options;

    @Autowired
    public InProcessJobRunner(JobRunRepository repository, MeterRegistry meterRegistry) {
        this(repository, meterRegistry, RunnerOptions.defaultOptions());
    }

    public InProcessJobRunner(
            JobRunRepository repository,
            MeterRegistry meterRegistry,
            RunnerOptions options
    ) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;
        this.options = options;
    }

    @Override
    @Transactional
    public JobRunResult runDue(int limit, LocalDateTime now) {
        LocalDateTime runAt = requireNow(now);
        List<JobRunRecord> records = repository.findDue(effectiveLimit(limit), runAt);
        RunCounters counters = new RunCounters(records.size());
        for (JobRunRecord record : records) {
            runOne(record, runAt, counters);
        }
        return counters.toResult();
    }

    private void runOne(JobRunRecord record, LocalDateTime now, RunCounters counters) {
        JobRunRecord running = start(record, now);
        Optional<JobRunHandler> handler = options.registry().find(running.type());
        if (handler.isEmpty()) {
            markMissingHandler(running, now, counters);
            return;
        }
        handleWithRegisteredHandler(running, handler.get(), now, counters);
    }

    private JobRunRecord start(JobRunRecord record, LocalDateTime now) {
        JobRunRecord queued = record;
        if (record.status() == JobRunState.RETRYABLE) {
            queued = repository.update(record.markQueuedForRetry(now));
        }
        return repository.update(queued.markRunning(now));
    }

    private void markMissingHandler(JobRunRecord record, LocalDateTime now, RunCounters counters) {
        JobRunRecord failed = record.markFailed(
                ERROR_HANDLER_MISSING,
                "No job handler registered for " + record.jobName(),
                now
        );
        repository.update(failed);
        counters.recordFailed();
        counter(record, JobRunState.FAILED).increment();
    }

    private void handleWithRegisteredHandler(
            JobRunRecord record,
            JobRunHandler handler,
            LocalDateTime now,
            RunCounters counters
    ) {
        try {
            String resultJson = handler.handle(record);
            JobRunRecord succeeded = record.withProgress(100, now).markSucceeded(resultJson, now);
            repository.update(succeeded);
            counters.recordSucceeded();
            counter(record, JobRunState.SUCCEEDED).increment();
        } catch (RuntimeException ex) {
            markHandlerFailure(record, now, ex, counters);
        }
    }

    private void markHandlerFailure(
            JobRunRecord record,
            LocalDateTime now,
            RuntimeException ex,
            RunCounters counters
    ) {
        if (shouldRetry(record)) {
            JobRunRecord retryable = record.markRetryable(
                    ERROR_HANDLER_FAILED,
                    trimError(ex.getMessage()),
                    now.plus(options.retryDelay()),
                    now
            );
            repository.update(retryable);
            counters.recordRetryable();
            counter(record, JobRunState.RETRYABLE).increment();
            return;
        }
        JobRunRecord failed = record.markFailed(ERROR_HANDLER_FAILED, trimError(ex.getMessage()), now);
        repository.update(failed);
        counters.recordFailed();
        counter(record, JobRunState.FAILED).increment();
    }

    private boolean shouldRetry(JobRunRecord record) {
        return record.type().retrySupported() && record.attempts() + 1 < record.maxAttempts();
    }

    private int effectiveLimit(int limit) {
        return Math.max(1, Math.min(limit, options.batchLimit()));
    }

    private Counter counter(JobRunRecord record, JobRunState status) {
        return Counter.builder(METRIC_RUNS_TOTAL)
                .tag("job", record.jobName())
                .tag("module", record.ownerModule())
                .tag("status", status.name())
                .register(meterRegistry);
    }

    private static String trimError(String message) {
        if (message == null || message.length() <= ERROR_MAX_LENGTH) {
            return message;
        }
        return message.substring(0, ERROR_MAX_LENGTH);
    }

    private static LocalDateTime requireNow(LocalDateTime now) {
        if (now == null) {
            throw new IllegalArgumentException("now is required");
        }
        return now;
    }

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
            return new RunnerOptions(
                    ExplicitJobRunHandlerRegistry.empty(),
                    DEFAULT_BATCH_LIMIT,
                    DEFAULT_RETRY_DELAY
            );
        }
    }

    private static final class RunCounters {

        private final int scanned;
        private int succeeded;
        private int retryable;
        private int failed;

        private RunCounters(int scanned) {
            this.scanned = scanned;
        }

        private void recordSucceeded() {
            succeeded++;
        }

        private void recordRetryable() {
            retryable++;
        }

        private void recordFailed() {
            failed++;
        }

        private JobRunResult toResult() {
            return new JobRunResult(scanned, succeeded, retryable, failed);
        }
    }
}
