package com.mmmail.common.exception;

import com.mmmail.common.model.Result;
import com.mmmail.common.observability.ErrorReporter;
import com.mmmail.common.observability.ObservedErrorReport;
import com.mmmail.common.observability.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String SOURCE_SERVER = "SERVER";

    private final ErrorReporter errorReporter;

    public GlobalExceptionHandler(ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException ex, HttpServletRequest request) {
        HttpStatus status = resolveStatus(ex.getCode());
        log.atWarn()
                .addKeyValue("event", "biz_exception")
                .addKeyValue("status", status.value())
                .addKeyValue("errorCode", ex.getCode())
                .addKeyValue("path", request.getRequestURI())
                .log(ex.getMessage());
        report(request, "BUSINESS", "WARN", status.value(), ex.getCode(), ex.getMessage(), ex.getClass().getSimpleName());
        return ResponseEntity.status(status).body(Result.failure(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public ResponseEntity<Result<Void>> handleValidationException(Exception ex, HttpServletRequest request) {
        log.atWarn()
                .addKeyValue("event", "validation_exception")
                .addKeyValue("status", HttpStatus.BAD_REQUEST.value())
                .addKeyValue("path", request.getRequestURI())
                .log(ex.getClass().getSimpleName());
        report(
                request,
                "VALIDATION",
                "WARN",
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.INVALID_ARGUMENT.getCode(),
                ErrorCode.INVALID_ARGUMENT.getMessage(),
                ex.getClass().getSimpleName()
        );
        return ResponseEntity.badRequest()
                .body(Result.failure(ErrorCode.INVALID_ARGUMENT.getCode(), ErrorCode.INVALID_ARGUMENT.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception ex, HttpServletRequest request) {
        log.atError()
                .setCause(ex)
                .addKeyValue("event", "unhandled_exception")
                .addKeyValue("status", HttpStatus.INTERNAL_SERVER_ERROR.value())
                .addKeyValue("path", request.getRequestURI())
                .log("Unhandled exception caught by GlobalExceptionHandler");
        report(
                request,
                "UNHANDLED",
                "ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ErrorCode.INTERNAL_ERROR.getCode(),
                ErrorCode.INTERNAL_ERROR.getMessage(),
                ex.toString()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.failure(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage()));
    }

    private void report(
            HttpServletRequest request,
            String category,
            String severity,
            int status,
            Integer errorCode,
            String message,
            String detail
    ) {
        errorReporter.record(new ObservedErrorReport(
                SOURCE_SERVER,
                category,
                severity,
                request.getRequestURI(),
                request.getMethod(),
                status,
                errorCode,
                message,
                detail,
                requestId(request),
                MDC.get(TraceContext.USER_ID_MDC),
                MDC.get(TraceContext.SESSION_ID_MDC),
                MDC.get(TraceContext.ORG_ID_MDC),
                request.getHeader("User-Agent"),
                LocalDateTime.now()
        ));
    }

    private String requestId(HttpServletRequest request) {
        Object attribute = request.getAttribute(TraceContext.REQUEST_ID_ATTRIBUTE);
        return attribute == null ? null : String.valueOf(attribute);
    }

    private HttpStatus resolveStatus(int code) {
        return switch (code) {
            case 10002 -> HttpStatus.UNAUTHORIZED;
            case 10003, 30013, 30045, 30046, 30047 -> HttpStatus.FORBIDDEN;
            case 10004 -> HttpStatus.TOO_MANY_REQUESTS;
            case 20001, 30002, 30003, 30011, 30015, 30018, 30019, 30025, 30032, 30041 -> HttpStatus.CONFLICT;
            case 20003, 30001, 30009, 30010, 30012, 30014, 30017, 30020, 30021, 30023, 30024, 30031, 30039, 30042, 30043, 30044 -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
