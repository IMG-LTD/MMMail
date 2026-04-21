package com.mmmail.platform.jobs;

public enum JobRunState {
    QUEUED,
    RUNNING,
    WAITING_APPROVAL,
    SUCCEEDED,
    FAILED,
    RETRYABLE
}
