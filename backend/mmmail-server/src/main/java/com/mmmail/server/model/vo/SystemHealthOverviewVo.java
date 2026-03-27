package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record SystemHealthOverviewVo(
        String status,
        String applicationName,
        String applicationVersion,
        List<String> activeProfiles,
        Long uptimeSeconds,
        LocalDateTime generatedAt,
        List<ComponentStatus> components,
        MetricSummary metrics,
        ErrorTrackingSummary errorTracking,
        List<ErrorEvent> recentErrors,
        JobSummary jobs,
        List<JobRun> recentJobs,
        String prometheusPath
) {

    public record ComponentStatus(
            String name,
            String status,
            String details
    ) {
    }

    public record MetricSummary(
            long totalRequests,
            long failedRequests,
            Double processCpuUsage,
            Double systemCpuUsage,
            Double usedMemoryMb,
            Double maxMemoryMb,
            Double liveThreads,
            Double activeDbConnections,
            Double maxDbConnections,
            List<RequestMetric> modules
    ) {
    }

    public record RequestMetric(
            String module,
            long totalRequests,
            long failedRequests
    ) {
    }

    public record ErrorTrackingSummary(
            long totalEvents,
            long serverEvents,
            long clientEvents,
            LocalDateTime lastOccurredAt
    ) {
    }

    public record ErrorEvent(
            String eventId,
            String source,
            String category,
            String severity,
            String message,
            String detail,
            String path,
            String method,
            Integer status,
            Integer errorCode,
            String requestId,
            String userId,
            String sessionId,
            String orgId,
            LocalDateTime occurredAt
    ) {
    }

    public record JobSummary(
            int activeRuns,
            long totalRuns,
            long failedRuns,
            LocalDateTime lastCompletedAt
    ) {
    }

    public record JobRun(
            String runId,
            String jobName,
            String trigger,
            String status,
            String detail,
            String actorId,
            String orgId,
            Long durationMs,
            LocalDateTime startedAt,
            LocalDateTime completedAt
    ) {
    }
}
