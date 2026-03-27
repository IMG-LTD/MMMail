package com.mmmail.server;

import com.mmmail.common.exception.ErrorCode;
import com.mmmail.common.exception.GlobalExceptionHandler;
import com.mmmail.common.model.Result;
import com.mmmail.common.observability.ErrorReporter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerUnitTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(report -> { });

    @Test
    void unhandledExceptionShouldBeSanitized() {
        RuntimeException exception = new RuntimeException("SQLIntegrityConstraintViolationException: duplicate key");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/system/health");

        ResponseEntity<Result<Void>> response = handler.handleException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode());
        assertThat(response.getBody().message()).isEqualTo(ErrorCode.INTERNAL_ERROR.getMessage());
        assertThat(response.getBody().message()).doesNotContain("SQLIntegrityConstraintViolationException");
    }
}
