package db.migration;

import com.mmmail.server.migration.SqlScriptMigrationSupport;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class V6__user_preference_mail_e2ee_columns extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        Connection connection = context.getConnection();
        SqlScriptMigrationSupport.addColumnIfMissing(connection, "user_preference", "mail_e2ee_enabled", "tinyint not null default 0");
        SqlScriptMigrationSupport.addColumnIfMissing(connection, "user_preference", "mail_e2ee_key_fingerprint", "varchar(64) null");
        SqlScriptMigrationSupport.addColumnIfMissing(connection, "user_preference", "mail_e2ee_public_key_armored", "longtext null");
        SqlScriptMigrationSupport.addColumnIfMissing(connection, "user_preference", "mail_e2ee_private_key_encrypted", "longtext null");
        SqlScriptMigrationSupport.addColumnIfMissing(connection, "user_preference", "mail_e2ee_key_algorithm", "varchar(64) null");
        SqlScriptMigrationSupport.addColumnIfMissing(connection, "user_preference", "mail_e2ee_key_created_at", "timestamp null");
        execute(connection, "update user_preference set mail_e2ee_enabled = 0 where mail_e2ee_enabled is null");
        execute(connection, """
                update system_release_metadata
                set schema_version = '6',
                    updated_at = current_timestamp
                where id = 1
                """);
    }

    private void execute(Connection connection, String sql) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to execute V6 migration SQL: " + sql, exception);
        }
    }
}
