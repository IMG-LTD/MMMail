package com.mmmail.server.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestObservationServiceTest {

    @Test
    void shouldExposeV212ModuleRedMetricsWithEndpointAndStatusTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RequestObservationService service = new RequestObservationService(registry);

        service.record(new RequestObservationService.RequestObservation(
                "mail",
                "/api/v1/mail/messages/{id}",
                "GET",
                200,
                17L
        ));

        assertThat(registry.get("mail_request_total")
                .tag("endpoint", "/api/v1/mail/messages/{id}")
                .tag("method", "GET")
                .tag("status", "200")
                .counter()
                .count()).isEqualTo(1.0);
        assertThat(registry.get("mail_request_duration_ms")
                .tag("endpoint", "/api/v1/mail/messages/{id}")
                .tag("method", "GET")
                .timer()
                .count()).isEqualTo(1);
    }

    @Test
    void shouldNormalizeModuleNameBeforeUsingItAsMetricPrefix() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RequestObservationService service = new RequestObservationService(registry);

        service.record(new RequestObservationService.RequestObservation(
                "web-push",
                "/api/v1/web-push/subscriptions",
                "POST",
                201,
                9L
        ));

        assertThat(registry.get("web_push_request_total")
                .tag("endpoint", "/api/v1/web-push/subscriptions")
                .tag("method", "POST")
                .tag("status", "201")
                .counter()
                .count()).isEqualTo(1.0);
    }
}
