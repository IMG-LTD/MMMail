package com.mmmail.server.config;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpResponse;

import java.util.Set;

public final class WebSocketHandshakeFailureResponder {

    public static final String CLOSE_CODE_HEADER = "X-WS-Close-Code";
    public static final int AUTH_FAILED_CLOSE_CODE = 4401;
    public static final int PERMISSION_DENIED_CLOSE_CODE = 4403;
    public static final int RATE_LIMIT_CLOSE_CODE = 4429;
    public static final String METRIC_REASON_AUTH = "auth";
    public static final String METRIC_REASON_THROTTLE = "throttle";

    private static final Set<Integer> PERMISSION_ERROR_CODES = Set.of(
            ErrorCode.FORBIDDEN.getCode(),
            ErrorCode.ORG_FORBIDDEN.getCode(),
            ErrorCode.V2_PERMISSION_DENIED.getCode(),
            ErrorCode.DOCS_NOTE_NOT_FOUND.getCode()
    );

    private WebSocketHandshakeFailureResponder() {
    }

    public static int reject(ServerHttpResponse response, RuntimeException exception) {
        Failure failure = resolve(exception);
        response.setStatusCode(failure.status());
        response.getHeaders().set(CLOSE_CODE_HEADER, String.valueOf(failure.closeCode()));
        return failure.closeCode();
    }

    public static String metricReason(int closeCode) {
        return closeCode == RATE_LIMIT_CLOSE_CODE ? METRIC_REASON_THROTTLE : METRIC_REASON_AUTH;
    }

    private static Failure resolve(RuntimeException exception) {
        if (exception instanceof BizException bizException) {
            return resolveBizException(bizException);
        }
        return new Failure(HttpStatus.UNAUTHORIZED, AUTH_FAILED_CLOSE_CODE);
    }

    private static Failure resolveBizException(BizException exception) {
        int code = exception.getCode();
        if (code == ErrorCode.RATE_LIMITED.getCode()) {
            return new Failure(HttpStatus.TOO_MANY_REQUESTS, RATE_LIMIT_CLOSE_CODE);
        }
        if (PERMISSION_ERROR_CODES.contains(code)) {
            return new Failure(HttpStatus.FORBIDDEN, PERMISSION_DENIED_CLOSE_CODE);
        }
        return new Failure(HttpStatus.UNAUTHORIZED, AUTH_FAILED_CLOSE_CODE);
    }

    private record Failure(HttpStatus status, int closeCode) {
    }
}
