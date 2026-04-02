package db.migration;

import com.mmmail.server.migration.SqlScriptMigrationSupport;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class V7__mail_message_e2ee_columns extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        Connection connection = context.getConnection();
        SqlScriptMigrationSupport.addColumnIfMissing(connection, "mail_message", "body_e2ee_enabled", "tinyint not null default 0");
        SqlScriptMigrationSupport.addColumnIfMissing(connection, "mail_message", "body_e2ee_algorithm", "varchar(64) null");
        SqlScriptMigrationSupport.addColumnIfMissing(connection, "mail_message", "body_e2ee_fingerprints_json", "text null");
        execute(connection, "update mail_message set body_e2ee_enabled = 0 where body_e2ee_enabled is null");
        execute(connection, """
                update system_release_metadata
                set schema_version = '7',
                    updated_at = current_timestamp
                where id = 1
                """);
    }

    private void execute(Connection connection, String sql) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to execute V7 migration SQL: " + sql, exception);
        }
    }
}
