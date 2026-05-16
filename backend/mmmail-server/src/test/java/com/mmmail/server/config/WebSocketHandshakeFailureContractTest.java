package com.mmmail.server.config;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.observability.WebSocketMetricsService;
import com.mmmail.server.security.JwtPrincipal;
import com.mmmail.server.security.WebSocketTokenAuthenticator;
import com.mmmail.server.service.CollabCrdtService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Duration;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketHandshakeFailureContractTest {

    private static final long SPEC_COOKIE_MAX_AGE_SECONDS = Duration.ofDays(7).toSeconds();

    @Test
    void notificationHandshakeShouldExposeAuthFailureCloseCode() {
        WebSocketTokenAuthenticator authenticator = mock(WebSocketTokenAuthenticator.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        when(authenticator.authenticate(null))
                .thenThrow(new BizException(ErrorCode.UNAUTHORIZED, "websocket token is required"));
        NotificationWebSocketHandshakeInterceptor interceptor = new NotificationWebSocketHandshakeInterceptor(
                authenticator,
                affinityCookieService(),
                new WebSocketMetricsService(meterRegistry)
        );
        MockHttpServletResponse rawResponse = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(rawResponse);

        boolean allowed = interceptor.beforeHandshake(
                request("/ws/notifications", ""),
                response,
                new TextWebSocketHandler(),
                new HashMap<>()
        );

        assertThat(allowed).isFalse();
        assertThat(rawResponse.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getHeaders().getFirst(WebSocketHandshakeFailureResponder.CLOSE_CODE_HEADER))
                .isEqualTo(String.valueOf(WebSocketHandshakeFailureResponder.AUTH_FAILED_CLOSE_CODE));
        assertThat(disconnectCount(meterRegistry, "notifications", "auth")).isEqualTo(1.0);
    }

    @Test
    void collabHandshakeShouldExposePermissionDeniedCloseCode() {
        WebSocketTokenAuthenticator authenticator = mock(WebSocketTokenAuthenticator.class);
        CollabCrdtService collabCrdtService = mock(CollabCrdtService.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        JwtPrincipal principal = new JwtPrincipal(101L, "user@mmmail.local", "USER", 1, 201L);
        when(authenticator.authenticate("access-token")).thenReturn(principal);
        doThrow(new BizException(ErrorCode.FORBIDDEN, "Docs note is not readable"))
                .when(collabCrdtService).requireReadable(101L, "docs", "501");
        CollabWebSocketHandshakeInterceptor interceptor = new CollabWebSocketHandshakeInterceptor(
                authenticator,
                collabCrdtService,
                affinityCookieService(),
                new WebSocketMetricsService(meterRegistry)
        );
        MockHttpServletResponse rawResponse = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(rawResponse);

        boolean allowed = interceptor.beforeHandshake(
                request("/ws/collab/docs/501", "token=access-token"),
                response,
                new TextWebSocketHandler(),
                new HashMap<>()
        );

        assertThat(allowed).isFalse();
        assertThat(rawResponse.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getHeaders().getFirst(WebSocketHandshakeFailureResponder.CLOSE_CODE_HEADER))
                .isEqualTo(String.valueOf(WebSocketHandshakeFailureResponder.PERMISSION_DENIED_CLOSE_CODE));
        assertThat(disconnectCount(meterRegistry, "collab", "auth")).isEqualTo(1.0);
    }

    private ServletServerHttpRequest request(String path, String queryString) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServerName("127.0.0.1");
        request.setServerPort(8080);
        request.setQueryString(queryString);
        return new ServletServerHttpRequest(request);
    }

    private WebSocketAffinityCookieService affinityCookieService() {
        return new WebSocketAffinityCookieService("WS_AFFINITY", "node-a", false, SPEC_COOKIE_MAX_AGE_SECONDS);
    }

    private double disconnectCount(SimpleMeterRegistry meterRegistry, String module, String reason) {
        Counter counter = meterRegistry.find("ws_disconnect_total")
                .tag("module", module)
                .tag("reason", reason)
                .counter();
        return counter == null ? 0.0 : counter.count();
    }
}
