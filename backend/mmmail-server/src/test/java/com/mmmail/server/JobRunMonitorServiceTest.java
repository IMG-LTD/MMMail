package com.mmmail.server;

import com.mmmail.server.observability.JobRunMonitorService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobRunMonitorServiceTest {

    @Test
    void shouldTrackSuccessfulRunsAndExposeRecentHistory() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        JobRunMonitorService service = new JobRunMonitorService(registry);

        JobRunMonitorService.JobHandle handle = service.start(new JobRunMonitorService.JobDescriptor(
                "MAIL_EASY_SWITCH_IMPORT",
                "USER_ACTION",
                "7",
                null
        ));
        service.success(handle, "session=101,status=COMPLETED");

        assertThat(service.summary().activeRuns()).isZero();
        assertThat(service.summary().totalRuns()).isEqualTo(1);
        assertThat(service.summary().failedRuns()).isZero();
        assertThat(service.recent(10)).hasSize(1);
        assertThat(service.recent(10).getFirst().status()).isEqualTo("SUCCESS");
        assertThat(service.recent(10).getFirst().jobName()).isEqualTo("MAIL_EASY_SWITCH_IMPORT");
        assertThat(registry.find("mmmail.jobs.executions.total")
                .tags("job", "MAIL_EASY_SWITCH_IMPORT", "status", "SUCCESS")
                .counter()
                .count()).isEqualTo(1.0d);
    }

    @Test
    void shouldTrackFailedRunsSeparately() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        JobRunMonitorService service = new JobRunMonitorService(registry);

        JobRunMonitorService.JobHandle handle = service.start(new JobRunMonitorService.JobDescriptor(
                "MAIL_EASY_SWITCH_IMPORT",
                "USER_ACTION",
                "7",
                null
        ));
        service.fail(handle, "session=101,reason=Import failed");

        assertThat(service.summary().totalRuns()).isEqualTo(1);
        assertThat(service.summary().failedRuns()).isEqualTo(1);
        assertThat(service.recent(10).getFirst().status()).isEqualTo("FAILED");
        assertThat(registry.find("mmmail.jobs.executions.total")
                .tags("job", "MAIL_EASY_SWITCH_IMPORT", "status", "FAILED")
                .counter()
                .count()).isEqualTo(1.0d);
    }
}
