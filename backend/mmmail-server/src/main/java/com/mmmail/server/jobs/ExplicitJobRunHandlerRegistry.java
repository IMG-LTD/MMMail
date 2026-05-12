package com.mmmail.server.jobs;

import com.mmmail.platform.jobs.JobRunHandler;
import com.mmmail.platform.jobs.JobRunType;

import java.util.Map;
import java.util.Optional;

public final class ExplicitJobRunHandlerRegistry {

    private final Map<JobRunType, JobRunHandler> handlers;

    private ExplicitJobRunHandlerRegistry(Map<JobRunType, JobRunHandler> handlers) {
        this.handlers = Map.copyOf(handlers);
    }

    public static ExplicitJobRunHandlerRegistry of(Map<JobRunType, JobRunHandler> handlers) {
        if (handlers == null) {
            throw new IllegalArgumentException("handlers is required");
        }
        return new ExplicitJobRunHandlerRegistry(handlers);
    }

    public static ExplicitJobRunHandlerRegistry empty() {
        return new ExplicitJobRunHandlerRegistry(Map.of());
    }

    public Optional<JobRunHandler> find(JobRunType type) {
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        return Optional.ofNullable(handlers.get(type));
    }
}
