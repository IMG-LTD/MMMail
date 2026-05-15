package db.migration;

import com.mmmail.server.migration.SqlScriptMigrationSupport;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class V20__public_share_token_hash_indexes extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        Connection connection = context.getConnection();
        ensureTokenHashIndexes(connection);
        updateReleaseMetadata(connection);
    }

    private void ensureTokenHashIndexes(Connection connection) {
        SqlScriptMigrationSupport.createUniqueIndexIfMissing(connection, "mail_external_secure_link",
                "uk_mail_external_secure_link_token_hash", "(token_hash)");
        SqlScriptMigrationSupport.createUniqueIndexIfMissing(connection, "drive_share_link",
                "uk_drive_share_link_token_hash", "(token_hash)");
        SqlScriptMigrationSupport.createUniqueIndexIfMissing(connection, "pass_secure_link",
                "uk_pass_secure_link_token_hash", "(token_hash)");
    }

    private void updateReleaseMetadata(Connection connection) {
        execute(connection, """
                update system_release_metadata
                set schema_version = '20',
                    updated_at = current_timestamp
                where id = 1
                """);
    }

    private void execute(Connection connection, String sql) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to execute V20 migration SQL: " + sql, exception);
        }
    }
}
