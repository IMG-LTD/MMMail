package com.mmmail.platform.jobs;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JobRunRepository {

    JobRunRecord enqueue(JobRunRequest request);

    Optional<JobRunRecord> findById(Long id);

    List<JobRunRecord> findDue(int limit, LocalDateTime now);

    JobRunRecord update(JobRunRecord record);
}
