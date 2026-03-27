package db.migration;

import com.mmmail.server.migration.SqlScriptMigrationSupport;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class V5__mail_attachments extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        Connection connection = context.getConnection();
        execute(connection, """
                create table if not exists mail_attachment (
                    id bigint primary key,
                    owner_id bigint not null,
                    mail_id bigint not null,
                    file_name varchar(255) not null,
                    content_type varchar(255) not null,
                    file_size bigint not null,
                    storage_path varchar(512) not null,
                    created_at timestamp not null,
                    updated_at timestamp not null,
                    deleted tinyint not null default 0
                )
                """);
        SqlScriptMigrationSupport.createIndexIfMissing(connection,
                "mail_attachment",
                "idx_mail_attachment_owner_mail_created",
                "(owner_id, mail_id, created_at)");
        SqlScriptMigrationSupport.createIndexIfMissing(connection,
                "mail_attachment",
                "idx_mail_attachment_storage_path",
                "(storage_path)");
        execute(connection, """
                update system_release_metadata
                set schema_version = '5',
                    updated_at = current_timestamp
                where id = 1
                """);
    }

    private void execute(Connection connection, String sql) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to execute V5 migration SQL: " + sql, exception);
        }
    }
}
