package com.mmmail.common.observability;

public final class TraceContext {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_ATTRIBUTE = "mmmail.requestId";
    public static final String REQUEST_ID_MDC = "requestId";
    public static final String USER_ID_MDC = "userId";
    public static final String SESSION_ID_MDC = "sessionId";
    public static final String ROLE_MDC = "role";
    public static final String ORG_ID_MDC = "orgId";

    private TraceContext() {
    }
}
