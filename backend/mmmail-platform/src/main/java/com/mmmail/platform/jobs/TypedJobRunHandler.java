package com.mmmail.platform.jobs;

public interface TypedJobRunHandler extends JobRunHandler {

    JobRunType type();
}
