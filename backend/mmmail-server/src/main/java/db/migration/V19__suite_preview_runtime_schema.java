package db.migration;

import com.mmmail.server.migration.SqlScriptMigrationSupport;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class V19__suite_preview_runtime_schema extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        Connection connection = context.getConnection();
        ensureSimpleLoginRelayPolicySchema(connection);
        ensureStandardNotesSchema(connection);
        updateReleaseMetadata(connection);
    }

    private void ensureSimpleLoginRelayPolicySchema(Connection connection) {
        execute(connection, """
                create table if not exists simplelogin_relay_policy (
                    id bigint primary key,
                    org_id bigint not null,
                    custom_domain_id bigint not null,
                    owner_id bigint not null,
                    catch_all_enabled tinyint not null default 0,
                    subdomain_mode varchar(32) not null,
                    default_mailbox_id bigint not null,
                    default_mailbox_email varchar(254) not null,
                    note varchar(500),
                    created_at timestamp not null,
                    updated_at timestamp not null,
                    deleted tinyint not null default 0
                )
                """);
        SqlScriptMigrationSupport.createUniqueIndexIfMissing(connection, "simplelogin_relay_policy",
                "uk_simplelogin_relay_policy_domain", "(custom_domain_id, deleted)");
        SqlScriptMigrationSupport.createIndexIfMissing(connection, "simplelogin_relay_policy",
                "idx_simplelogin_relay_policy_org_updated", "(org_id, updated_at)");
        SqlScriptMigrationSupport.createIndexIfMissing(connection, "simplelogin_relay_policy",
                "idx_simplelogin_relay_policy_owner_updated", "(owner_id, updated_at)");
        SqlScriptMigrationSupport.createIndexIfMissing(connection, "simplelogin_relay_policy",
                "idx_simplelogin_relay_policy_mailbox", "(default_mailbox_id, updated_at)");
    }

    private void ensureStandardNotesSchema(Connection connection) {
        execute(connection, """
                create table if not exists standard_note_profile (
                    note_id bigint primary key,
                    owner_id bigint not null,
                    note_type varchar(32) not null,
                    tags_json text,
                    folder_id bigint null,
                    pinned tinyint not null default 0,
                    archived tinyint not null default 0,
                    created_at timestamp not null,
                    updated_at timestamp not null,
                    deleted tinyint not null default 0
                )
                """);
        execute(connection, """
                create table if not exists standard_note_folder (
                    id bigint primary key,
                    owner_id bigint not null,
                    name varchar(64) not null,
                    color varchar(7) not null,
                    description varchar(160),
                    created_at timestamp not null,
                    updated_at timestamp not null,
                    deleted tinyint not null default 0
                )
                """);
        SqlScriptMigrationSupport.addColumnIfMissing(connection, "standard_note_profile", "folder_id", "bigint null");
        SqlScriptMigrationSupport.createIndexIfMissing(connection, "standard_note_profile",
                "idx_standard_note_profile_owner_folder", "(owner_id, folder_id, updated_at)");
        SqlScriptMigrationSupport.createUniqueIndexIfMissing(connection, "standard_note_folder",
                "uk_standard_note_folder_owner_name", "(owner_id, name, deleted)");
        SqlScriptMigrationSupport.createIndexIfMissing(connection, "standard_note_folder",
                "idx_standard_note_folder_owner_updated", "(owner_id, updated_at)");
    }

    private void updateReleaseMetadata(Connection connection) {
        execute(connection, """
                update system_release_metadata
                set schema_version = '19',
                    updated_at = current_timestamp
                where id = 1
                """);
    }

    private void execute(Connection connection, String sql) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to execute V19 migration SQL: " + sql, exception);
        }
    }
}
