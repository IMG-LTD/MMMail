package com.mmmail.platform.jobs;

public record JobRunResult(
        int scanned,
        int succeeded,
        int retryable,
        int failed
) {
}
