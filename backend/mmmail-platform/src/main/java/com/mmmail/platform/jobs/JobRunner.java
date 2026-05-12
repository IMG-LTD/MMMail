package com.mmmail.platform.jobs;

import java.time.LocalDateTime;

public interface JobRunner {

    JobRunResult runDue(int limit, LocalDateTime now);
}
