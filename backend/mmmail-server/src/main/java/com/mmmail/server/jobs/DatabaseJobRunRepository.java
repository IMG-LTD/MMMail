package com.mmmail.server.jobs;

import com.mmmail.platform.jobs.JobRunRecord;
import com.mmmail.platform.jobs.JobRunRepository;
import com.mmmail.platform.jobs.JobRunRequest;
import com.mmmail.platform.jobs.JobRunState;
import com.mmmail.platform.jobs.JobRunType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class DatabaseJobRunRepository implements JobRunRepository {

    private static final int MAX_BATCH_SIZE = 100;

    private final PlatformJobRunMapper mapper;

    public DatabaseJobRunRepository(PlatformJobRunMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public JobRunRecord enqueue(JobRunRequest request) {
        PlatformJobRun existing = mapper.findByIdempotencyKey(request.idempotencyKey());
        if (existing != null) {
            return duplicateResult(existing, request);
        }
        JobRunRecord queued = JobRunRecord.queued(null, request, LocalDateTime.now());
        PlatformJobRun entity = toEntity(queued);
        mapper.insert(entity);
        return toRecord(entity);
    }

    @Override
    public Optional<JobRunRecord> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(DatabaseJobRunRepository::toRecord);
    }

    @Override
    public List<JobRunRecord> findDue(int limit, LocalDateTime now) {
        return mapper.findDue(now, safeLimit(limit)).stream()
                .map(DatabaseJobRunRepository::toRecord)
                .toList();
    }

    @Override
    @Transactional
    public JobRunRecord update(JobRunRecord record) {
        if (record.id() == null) {
            throw new IllegalArgumentException("job run id is required");
        }
        mapper.updateById(toEntity(record));
        return record;
    }

    private JobRunRecord duplicateResult(PlatformJobRun existing, JobRunRequest request) {
        if (!sameJob(existing, request)) {
            throw new IllegalStateException("idempotency key already belongs to a different job");
        }
        return toRecord(existing);
    }

    private static PlatformJobRun toEntity(JobRunRecord record) {
        PlatformJobRun entity = new PlatformJobRun();
        entity.setId(record.id());
        entity.setJobType(record.jobName());
        entity.setOwnerModule(record.ownerModule());
        entity.setTenantId(record.tenantId());
        entity.setUserId(record.userId());
        entity.setRequestId(record.requestId());
        entity.setTraceId(record.traceId());
        entity.setAggregateType(record.aggregateType());
        entity.setAggregateId(record.aggregateId());
        entity.setPayloadJson(record.payloadJson());
        entity.setIdempotencyKey(record.idempotencyKey());
        entity.setStatus(record.status().name());
        entity.setProgressPercent(record.progressPercent());
        entity.setAttempts(record.attempts());
        entity.setMaxAttempts(record.maxAttempts());
        entity.setNextAttemptAt(record.nextAttemptAt());
        entity.setLastErrorCode(record.lastErrorCode());
        entity.setLastErrorMessage(record.lastErrorMessage());
        entity.setResultJson(record.resultJson());
        entity.setCreatedAt(record.createdAt());
        entity.setUpdatedAt(record.updatedAt());
        entity.setStartedAt(record.startedAt());
        entity.setCompletedAt(record.completedAt());
        return entity;
    }

    private static JobRunRecord toRecord(PlatformJobRun entity) {
        return new JobRunRecord(
                entity.getId(),
                JobRunType.fromJobName(entity.getJobType()),
                entity.getOwnerModule(),
                entity.getTenantId(),
                entity.getUserId(),
                entity.getRequestId(),
                entity.getTraceId(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getPayloadJson(),
                entity.getIdempotencyKey(),
                JobRunState.valueOf(entity.getStatus()),
                valueOrZero(entity.getProgressPercent()),
                valueOrZero(entity.getAttempts()),
                valueOrOne(entity.getMaxAttempts()),
                entity.getNextAttemptAt(),
                entity.getLastErrorCode(),
                entity.getLastErrorMessage(),
                entity.getResultJson(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getStartedAt(),
                entity.getCompletedAt()
        );
    }

    private static boolean sameJob(PlatformJobRun existing, JobRunRequest request) {
        return Objects.equals(existing.getJobType(), request.jobName())
                && Objects.equals(existing.getAggregateType(), request.aggregateType())
                && Objects.equals(existing.getAggregateId(), request.aggregateId())
                && Objects.equals(existing.getPayloadJson(), request.payloadJson());
    }

    private static int safeLimit(int limit) {
        return Math.max(1, Math.min(limit, MAX_BATCH_SIZE));
    }

    private static int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static int valueOrOne(Integer value) {
        return value == null ? 1 : value;
    }
}
