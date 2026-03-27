package com.mmmail.server.observability;

import com.mmmail.server.model.vo.SystemHealthOverviewVo;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Service
public class JobRunMonitorService {

    private static final int MAX_RECENT_RUNS = 100;

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, ActiveRun> activeRuns = new ConcurrentHashMap<>();
    private final ArrayDeque<SystemHealthOverviewVo.JobRun> recentRuns = new ArrayDeque<>();
    private final Object monitor = new Object();
    private final AtomicLong sequence = new AtomicLong(0);
    private final LongAdder totalRuns = new LongAdder();
    private final LongAdder failedRuns = new LongAdder();
    private volatile LocalDateTime lastCompletedAt;

    public JobRunMonitorService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        Gauge.builder("mmmail.jobs.active.runs", this, JobRunMonitorService::activeCount).register(meterRegistry);
        Gauge.builder("mmmail.jobs.total.runs", this, JobRunMonitorService::totalCount).register(meterRegistry);
        Gauge.builder("mmmail.jobs.failed.runs", this, JobRunMonitorService::failedCount).register(meterRegistry);
    }

    public JobHandle start(JobDescriptor descriptor) {
        String runId = String.valueOf(sequence.incrementAndGet());
        LocalDateTime startedAt = LocalDateTime.now();
        activeRuns.put(runId, new ActiveRun(runId, descriptor.jobName(), descriptor.trigger(), descriptor.actorId(), descriptor.orgId(), startedAt));
        totalRuns.increment();
        counter(descriptor.jobName(), "STARTED").increment();
        return new JobHandle(runId, startedAt);
    }

    public void success(JobHandle handle, String detail) {
        complete(handle, "SUCCESS", detail, false);
    }

    public void fail(JobHandle handle, String detail) {
        complete(handle, "FAILED", detail, true);
    }

    public Summary summary() {
        return new Summary(activeCount(), totalCount(), failedCount(), lastCompletedAt);
    }

    public List<SystemHealthOverviewVo.JobRun> recent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_RECENT_RUNS));
        synchronized (monitor) {
            return recentRuns.stream().limit(safeLimit).toList();
        }
    }

    private void complete(JobHandle handle, String status, String detail, boolean failed) {
        ActiveRun activeRun = activeRuns.remove(handle.runId());
        if (activeRun == null) {
            throw new IllegalStateException("Job run does not exist: " + handle.runId());
        }
        LocalDateTime completedAt = LocalDateTime.now();
        long durationMs = Duration.between(activeRun.startedAt(), completedAt).toMillis();
        SystemHealthOverviewVo.JobRun run = new SystemHealthOverviewVo.JobRun(
                activeRun.runId(),
                activeRun.jobName(),
                activeRun.trigger(),
                status,
                detail,
                activeRun.actorId(),
                activeRun.orgId(),
                durationMs,
                activeRun.startedAt(),
                completedAt
        );
        synchronized (monitor) {
            recentRuns.addFirst(run);
            trimBuffer();
        }
        if (failed) {
            failedRuns.increment();
        }
        lastCompletedAt = completedAt;
        counter(activeRun.jobName(), status).increment();
        timer(activeRun.jobName(), status).record(Duration.ofMillis(durationMs));
    }

    private Counter counter(String jobName, String status) {
        return Counter.builder("mmmail.jobs.executions.total")
                .tag("job", jobName)
                .tag("status", status)
                .register(meterRegistry);
    }

    private Timer timer(String jobName, String status) {
        return Timer.builder("mmmail.jobs.execution.duration")
                .tag("job", jobName)
                .tag("status", status)
                .register(meterRegistry);
    }

    private void trimBuffer() {
        while (recentRuns.size() > MAX_RECENT_RUNS) {
            recentRuns.removeLast();
        }
    }

    private int activeCount() {
        return activeRuns.size();
    }

    private long totalCount() {
        return totalRuns.sum();
    }

    private long failedCount() {
        return failedRuns.sum();
    }

    private record ActiveRun(
            String runId,
            String jobName,
            String trigger,
            String actorId,
            String orgId,
            LocalDateTime startedAt
    ) {
    }

    public record JobDescriptor(
            String jobName,
            String trigger,
            String actorId,
            String orgId
    ) {
        public JobDescriptor {
            if (!StringUtils.hasText(jobName) || !StringUtils.hasText(trigger)) {
                throw new IllegalArgumentException("jobName and trigger are required");
            }
        }
    }

    public record JobHandle(
            String runId,
            LocalDateTime startedAt
    ) {
    }

    public record Summary(
            int activeRuns,
            long totalRuns,
            long failedRuns,
            LocalDateTime lastCompletedAt
    ) {
    }
}
