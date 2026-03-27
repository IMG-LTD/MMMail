package com.mmmail.server.migration;

import org.flywaydb.core.api.CoreErrorCode;
import org.flywaydb.core.api.ErrorDetails;
import org.flywaydb.core.api.output.InfoResult;
import org.flywaydb.core.api.output.ValidateOutput;
import org.flywaydb.core.api.output.ValidateResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationCliTest {

    @Test
    void shouldAllowPendingVersionedMigrationsOnEmptySchema() {
        ValidateResult result = validateResult(
                new ValidateOutput("1", "baseline schema", "classpath:db/migration/V1.sql",
                        new ErrorDetails(CoreErrorCode.RESOLVED_VERSIONED_MIGRATION_NOT_APPLIED, "pending baseline"))
        );
        InfoResult info = emptySchemaInfo();

        assertThat(MigrationCli.isPendingOnlyOnEmptySchema(result, info)).isTrue();
    }

    @Test
    void shouldRejectNonPendingValidationErrorsOnEmptySchema() {
        ValidateResult result = validateResult(
                new ValidateOutput("5", "mail attachments", "classpath:db/migration/V5.sql",
                        new ErrorDetails(CoreErrorCode.CHECKSUM_MISMATCH, "checksum mismatch"))
        );
        InfoResult info = emptySchemaInfo();

        assertThat(MigrationCli.isPendingOnlyOnEmptySchema(result, info)).isFalse();
    }

    @Test
    void shouldFormatInvalidMigrationDetails() {
        String formatted = MigrationCli.formatInvalidMigrations(List.of(
                new ValidateOutput("5", "mail attachments", "classpath:db/migration/V5.sql",
                        new ErrorDetails(CoreErrorCode.CHECKSUM_MISMATCH, "checksum mismatch"))
        ));

        assertThat(formatted).contains("5:mail attachments");
        assertThat(formatted).contains("CHECKSUM_MISMATCH");
        assertThat(formatted).contains("checksum mismatch");
    }

    private ValidateResult validateResult(ValidateOutput... outputs) {
        return new ValidateResult(
                "validate",
                "jdbc:mysql://localhost:3306/mmmail",
                new ErrorDetails(CoreErrorCode.VALIDATE_ERROR, "validation failed"),
                false,
                outputs.length,
                List.of(outputs),
                List.of()
        );
    }

    private InfoResult emptySchemaInfo() {
        InfoResult info = new InfoResult();
        info.setAllSchemasEmpty(true);
        info.setSchemaVersion(null);
        return info;
    }
}
