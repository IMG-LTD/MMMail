package db.migration;

import com.mmmail.server.migration.SqlScriptMigrationSupport;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class V4__user_preference_authenticator_columns extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        Connection connection = context.getConnection();
        SqlScriptMigrationSupport.addColumnIfMissing(connection, "user_preference", "authenticator_sync_enabled", "tinyint not null default 1");
        SqlScriptMigrationSupport.addColumnIfMissing(connection, "user_preference", "authenticator_encrypted_backup_enabled", "tinyint not null default 0");
        SqlScriptMigrationSupport.addColumnIfMissing(connection, "user_preference", "authenticator_pin_protection_enabled", "tinyint not null default 0");
        SqlScriptMigrationSupport.addColumnIfMissing(connection, "user_preference", "authenticator_pin_hash", "varchar(255) null");
        SqlScriptMigrationSupport.addColumnIfMissing(connection, "user_preference", "authenticator_lock_timeout_seconds", "int not null default 300");
        SqlScriptMigrationSupport.addColumnIfMissing(connection, "user_preference", "authenticator_last_synced_at", "timestamp null");
        SqlScriptMigrationSupport.addColumnIfMissing(connection, "user_preference", "authenticator_last_backup_at", "timestamp null");
        execute(connection, "update user_preference set authenticator_sync_enabled = 1 where authenticator_sync_enabled is null");
        execute(connection, "update user_preference set authenticator_encrypted_backup_enabled = 0 where authenticator_encrypted_backup_enabled is null");
        execute(connection, "update user_preference set authenticator_pin_protection_enabled = 0 where authenticator_pin_protection_enabled is null");
        execute(connection, "update user_preference set authenticator_lock_timeout_seconds = 300 where authenticator_lock_timeout_seconds is null");
        execute(connection, """
                update system_release_metadata
                set schema_version = '4',
                    updated_at = current_timestamp
                where id = 1
                """);
    }

    private void execute(Connection connection, String sql) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to execute V4 migration SQL: " + sql, exception);
        }
    }
}
