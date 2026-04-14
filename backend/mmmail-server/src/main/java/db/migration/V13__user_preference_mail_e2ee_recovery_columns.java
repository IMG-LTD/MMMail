package db.migration;

import com.mmmail.server.migration.SqlScriptMigrationSupport;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class V13__user_preference_mail_e2ee_recovery_columns extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        Connection connection = context.getConnection();
        SqlScriptMigrationSupport.addColumnIfMissing(
                connection,
                "user_preference",
                "mail_e2ee_recovery_private_key_encrypted",
                "longtext null"
        );
        SqlScriptMigrationSupport.addColumnIfMissing(
                connection,
                "user_preference",
                "mail_e2ee_recovery_updated_at",
                "timestamp null"
        );
        execute(connection, """
                update system_release_metadata
                set schema_version = '13',
                    updated_at = current_timestamp
                where id = 1
                """);
    }

    private void execute(Connection connection, String sql) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to execute V13 migration SQL: " + sql, exception);
        }
    }
}
