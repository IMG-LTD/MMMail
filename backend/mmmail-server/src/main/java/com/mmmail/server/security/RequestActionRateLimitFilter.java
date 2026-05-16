package com.mmmail.server.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.model.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

@Component
public class RequestActionRateLimitFilter extends OncePerRequestFilter {

    private static final String POST = "POST";
    private static final String V1_MAIL_SEND_PATH = "/api/v1/mails/send";
    private static final String V2_MAIL_SEND_PATH = "/api/v2/mail/send";
    private static final String WEB_PUSH_TEST_PATH = "/api/v1/web-push/test";
    private static final String COMMAND_RUN_PATH = "/api/v2/command-center/runs";
    private static final Pattern COMMAND_RUN_MUTATION_PATH =
            Pattern.compile("^/api/v2/command-center/runs/[^/]+/(cancel|retry)$");

    private final SecurityRateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    public RequestActionRateLimitFilter(SecurityRateLimitService rateLimitService, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        RateLimitedAction action = resolveAction(request);
        Long userId = currentUserId();
        if (action == null || userId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            action.record(rateLimitService, userId, request.getRemoteAddr());
            filterChain.doFilter(request, response);
        } catch (BizException exception) {
            writeRateLimitResponse(response, exception);
        }
    }

    private RateLimitedAction resolveAction(HttpServletRequest request) {
        if (!POST.equalsIgnoreCase(request.getMethod())) {
            return null;
        }

        String path = request.getRequestURI();
        if (V1_MAIL_SEND_PATH.equals(path) || V2_MAIL_SEND_PATH.equals(path)) {
            return RateLimitedAction.MAIL_SEND;
        }
        if (WEB_PUSH_TEST_PATH.equals(path)) {
            return RateLimitedAction.WEB_PUSH_TEST;
        }
        if (COMMAND_RUN_PATH.equals(path) || COMMAND_RUN_MUTATION_PATH.matcher(path).matches()) {
            return RateLimitedAction.COMMAND_RUN;
        }
        return null;
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            return null;
        }
        return principal.userId();
    }

    private void writeRateLimitResponse(HttpServletResponse response, BizException exception) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.failure(exception.getCode(), exception.getMessage()));
    }

    private enum RateLimitedAction {
        MAIL_SEND {
            @Override
            void record(SecurityRateLimitService service, Long userId, String ipAddress) {
                service.recordMailSendAttempt(userId, ipAddress);
            }
        },
        WEB_PUSH_TEST {
            @Override
            void record(SecurityRateLimitService service, Long userId, String ipAddress) {
                service.recordWebPushTestAttempt(userId, ipAddress);
            }
        },
        COMMAND_RUN {
            @Override
            void record(SecurityRateLimitService service, Long userId, String ipAddress) {
                service.recordCommandRunAttempt(userId, ipAddress);
            }
        };

        abstract void record(SecurityRateLimitService service, Long userId, String ipAddress);
    }
}
