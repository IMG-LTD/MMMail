package com.mmmail.platform.jobs;

@FunctionalInterface
public interface JobRunHandler {

    String handle(JobRunRecord record);
}
