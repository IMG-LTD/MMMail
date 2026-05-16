package com.mmmail.server.observability;

import com.mmmail.common.observability.TraceContext;
import com.mmmail.foundation.tenant.TenantScopeContext;
import com.mmmail.foundation.tenant.TenantScopeContextHolder;
import com.mmmail.foundation.tenant.TenantScopeHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class RequestTracingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestTracingFilter.class);

    private final RequestObservationService requestObservationService;
    private final RequestRouteModuleResolver moduleResolver;
    private final RuntimeTraceService runtimeTraceService;

    public RequestTracingFilter(
            RequestObservationService requestObservationService,
            RequestRouteModuleResolver moduleResolver,
            RuntimeTraceService runtimeTraceService
    ) {
        this.requestObservationService = requestObservationService;
        this.moduleResolver = moduleResolver;
        this.runtimeTraceService = runtimeTraceService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        long startedAt = System.nanoTime();
        prepareContext(request, response, requestId);
        RuntimeTraceService.TraceScope traceScope = runtimeTraceService.start(
                "mmmail.http.request",
                Map.of(
                        "component", "http",
                        "module", moduleResolver.resolve(request.getRequestURI()),
                        "method", request.getMethod()
                )
        );
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException ex) {
            traceScope.error(ex);
            throw ex;
        } finally {
            traceScope.tag("route", endpointPattern(request));
            traceScope.tag("status", String.valueOf(response.getStatus()));
            traceScope.close();
            logRequest(request, response, startedAt);
            TenantScopeContextHolder.clear();
            MDC.clear();
        }
    }

    private void prepareContext(HttpServletRequest request, HttpServletResponse response, String requestId) {
        MDC.clear();
        MDC.put(TraceContext.REQUEST_ID_MDC, requestId);
        MDC.put(TraceContext.TRACE_ID_MDC, requestId);
        String orgId = request.getHeader(TenantScopeHeaders.ORG_ID);
        String scopeId = request.getHeader(TenantScopeHeaders.SCOPE_ID);
        String normalizedOrgId = StringUtils.hasText(orgId) ? orgId.trim() : null;
        String normalizedScopeId = StringUtils.hasText(scopeId) ? scopeId.trim() : null;
        TenantScopeContextHolder.set(new TenantScopeContext(normalizedOrgId, normalizedScopeId));
        if (normalizedOrgId != null) {
            MDC.put(TraceContext.ORG_ID_MDC, normalizedOrgId);
        }
        if (normalizedScopeId != null) {
            MDC.put("scopeId", normalizedScopeId);
        }
        request.setAttribute(TraceContext.REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(TraceContext.REQUEST_ID_HEADER, requestId);
    }

    private void logRequest(HttpServletRequest request, HttpServletResponse response, long startedAt) {
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        int status = response.getStatus();
        String module = moduleResolver.resolve(request.getRequestURI());
        requestObservationService.record(new RequestObservationService.RequestObservation(
                module,
                endpointPattern(request),
                request.getMethod(),
                status,
                durationMs
        ));
        log.atInfo()
                .addKeyValue("event", "http_request")
                .addKeyValue("module", module)
                .addKeyValue("method", request.getMethod())
                .addKeyValue("path", request.getRequestURI())
                .addKeyValue("status", status)
                .addKeyValue("durationMs", durationMs)
                .log("HTTP request completed");
    }

    private String resolveRequestId(HttpServletRequest request) {
        String header = request.getHeader(TraceContext.REQUEST_ID_HEADER);
        return StringUtils.hasText(header) ? header.trim() : UUID.randomUUID().toString();
    }

    private String endpointPattern(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern instanceof String value && StringUtils.hasText(value)) {
            return value.trim();
        }
        return request.getRequestURI();
    }
}
