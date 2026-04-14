package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class V11__mail_external_secure_links extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        Connection connection = context.getConnection();
        execute(connection, """
                create table if not exists mail_external_secure_link (
                    id bigint primary key,
                    mail_id bigint not null,
                    owner_id bigint not null,
                    recipient_email varchar(254) not null,
                    token varchar(64) not null,
                    public_url varchar(512) not null,
                    password_hint varchar(255),
                    expires_at timestamp,
                    revoked_at timestamp,
                    last_accessed_at timestamp,
                    created_at timestamp not null,
                    updated_at timestamp not null,
                    deleted tinyint not null default 0
                )
                """);
        createIndexIfMissing(connection, "mail_external_secure_link", "uk_mail_external_secure_link_mail", true, "(mail_id)");
        createIndexIfMissing(connection, "mail_external_secure_link", "uk_mail_external_secure_link_token", true, "(token)");
        createIndexIfMissing(connection, "mail_external_secure_link", "idx_mail_external_secure_link_owner", false, "(owner_id, revoked_at, expires_at)");
        execute(connection, """
                update system_release_metadata
                set schema_version = '11',
                    updated_at = current_timestamp
                where id = 1
                """);
    }

    private void createIndexIfMissing(
            Connection connection,
            String tableName,
            String indexName,
            boolean unique,
            String definition
    ) {
        if (indexExists(connection, tableName, indexName)) {
            return;
        }
        String sql = (unique ? "create unique index " : "create index ")
                + indexName
                + " on "
                + tableName
                + definition;
        execute(connection, sql);
    }

    private boolean indexExists(Connection connection, String tableName, String indexName) {
        try (ResultSet resultSet = connection.getMetaData().getIndexInfo(connection.getCatalog(), null, tableName, false, false)) {
            while (resultSet.next()) {
                String currentIndexName = resultSet.getString("INDEX_NAME");
                if (currentIndexName != null && currentIndexName.equalsIgnoreCase(indexName)) {
                    return true;
                }
            }
            return false;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to inspect index metadata for " + tableName + "." + indexName, exception);
        }
    }

    private void execute(Connection connection, String sql) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to execute V11 migration SQL: " + sql, exception);
        }
    }
}
