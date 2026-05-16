package com.mmmail.server.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestRouteModuleResolverTest {

    private final RequestRouteModuleResolver resolver = new RequestRouteModuleResolver();

    @Test
    void shouldResolveV212ApiV2RoutesToBusinessModules() {
        assertThat(resolver.resolve("/api/v2/calendar/events")).isEqualTo("calendar");
        assertThat(resolver.resolve("/api/v2/calendar/subscriptions")).isEqualTo("calendar");
        assertThat(resolver.resolve("/api/v2/notifications")).isEqualTo("notifications");
        assertThat(resolver.resolve("/api/v2/notifications/since")).isEqualTo("notifications");
        assertThat(resolver.resolve("/api/v2/command-center/commands")).isEqualTo("command-center");
    }

    @Test
    void shouldKeepNonApiRoutesExplicitlyPublic() {
        assertThat(resolver.resolve("/ws/notifications")).isEqualTo("public");
        assertThat(resolver.resolve("/share/file-token")).isEqualTo("public");
    }
}
