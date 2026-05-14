package com.mmmail.server;

import com.mmmail.server.util.ClientIpResolver;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver();

    @Test
    void resolvesFirstForwardedForAddressBeforeProxyAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.10");
        request.addHeader("X-Forwarded-For", "203.0.113.42, 10.0.0.10");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.42");
    }

    @Test
    void fallsBackToRemoteAddressWhenForwardedHeadersAreAbsent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.7");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.7");
    }
}
