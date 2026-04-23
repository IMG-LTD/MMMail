package db.migration;

import com.mmmail.foundation.security.PublicShareTokenCodec;
import com.mmmail.server.migration.SqlScriptMigrationSupport;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class V15__public_share_token_hash extends BaseJavaMigration {

    private final PublicShareTokenCodec codec = new PublicShareTokenCodec();

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        addColumnIfMissing(connection, "mail_external_secure_link");
        addColumnIfMissing(connection, "pass_secure_link");
        addColumnIfMissing(connection, "drive_share_link");
        backfill(connection, "mail_external_secure_link");
        backfill(connection, "pass_secure_link");
        backfill(connection, "drive_share_link");
        execute(connection, """
                update system_release_metadata
                set schema_version = '15',
                    updated_at = current_timestamp
                where id = 1
                """);
    }

    private void addColumnIfMissing(Connection connection, String tableName) {
        SqlScriptMigrationSupport.addColumnIfMissing(connection, tableName, "token_hash", "varchar(64) null");
    }

    private void backfill(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "select id, token from " + tableName + " where token is not null and (token_hash is null or trim(token_hash) = '')"
        ); ResultSet rows = select.executeQuery()) {
            while (rows.next()) {
                try (PreparedStatement update = connection.prepareStatement(
                        "update " + tableName + " set token_hash = ? where id = ?"
                )) {
                    update.setString(1, codec.hash(rows.getString("token")));
                    update.setLong(2, rows.getLong("id"));
                    update.executeUpdate();
                }
            }
        }
    }

    private void execute(Connection connection, String sql) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to execute V15 migration SQL: " + sql, exception);
        }
    }
}
