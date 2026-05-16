package com.mmmail.server;

import com.mmmail.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiSpecV212ErrorCodeContractTest {

    private static final List<ErrorCode> V212_ERROR_CODES = List.of(
            ErrorCode.V2_ENTITLEMENT_REQUIRED,
            ErrorCode.V2_PERMISSION_DENIED,
            ErrorCode.CALENDAR_SUBSCRIPTION_NOT_FOUND,
            ErrorCode.MAIL_EXTERNAL_ACCOUNT_NOT_FOUND,
            ErrorCode.MAIL_EXTERNAL_ACCOUNT_CONFIG,
            ErrorCode.MAIL_EXTERNAL_AUTH_INVALID,
            ErrorCode.MAIL_EXTERNAL_TIMEOUT,
            ErrorCode.MAIL_EXTERNAL_RATE_LIMITED,
            ErrorCode.COMMUNITY_TITLE_REQUIRED,
            ErrorCode.COMMUNITY_TOPIC_NOT_FOUND,
            ErrorCode.COMMUNITY_POST_NOT_FOUND,
            ErrorCode.COMMUNITY_COMMENT_NOT_FOUND,
            ErrorCode.COMMUNITY_REPORT_NOT_FOUND,
            ErrorCode.COMMUNITY_NOT_AUTHOR,
            ErrorCode.COMMUNITY_ADMIN_REQUIRED,
            ErrorCode.COMMUNITY_POST_LOCKED,
            ErrorCode.COMMUNITY_TOPIC_NOT_EMPTY,
            ErrorCode.SEARCH_QUERY_TOO_SHORT,
            ErrorCode.SEARCH_REINDEX_JOB_NOT_FOUND,
            ErrorCode.SEARCH_MODULE_UNSUPPORTED,
            ErrorCode.SHEETS_CIRCULAR_REF
    );

    @Test
    void apiSpecShouldRegisterV212ErrorCodesAndI18nKeys() throws Exception {
        String apiSpec = Files.readString(repoRoot().resolve("docs/api-spec.md"));

        for (ErrorCode errorCode : V212_ERROR_CODES) {
            assertThat(apiSpec).contains(
                    String.valueOf(errorCode.getCode()),
                    errorCode.name(),
                    "errors." + errorCode.getCode() + ".title",
                    "errors." + errorCode.getCode() + ".message"
            );
        }
    }

    private Path repoRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null && !Files.exists(current.resolve("docs/api-spec.md"))) {
            current = current.getParent();
        }
        assertThat(current)
                .as("repo root containing docs/api-spec.md should exist")
                .isNotNull();
        return current;
    }
}
