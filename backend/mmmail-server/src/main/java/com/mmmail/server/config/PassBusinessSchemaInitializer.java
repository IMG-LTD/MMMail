package com.mmmail.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
@ConditionalOnProperty(
        name = "mmmail.schema.preview-initializers.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PassBusinessSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PassBusinessSchemaInitializer.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public PassBusinessSchemaInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("alter table audit_event modify column actor_id bigint null");
        ensureVpnSchema();
        ensurePassVaultItemColumns();
        ensurePassBusinessTables();
        ensurePassBusinessIndexes();
        ensureMailFolderSchema();
        ensurePassMailboxSchema();
        ensureAliasMailboxRouteSchema();
        ensureSimpleLoginRelayPolicySchema();
        ensureStandardNotesSchema();
        ensureMailMessageDeliveryTargetsColumn();
        ensureDriveShareSchema();
    }

    private void ensurePassVaultItemColumns() {
        ensureColumn("pass_vault_item", "org_id", "alter table pass_vault_item add column org_id bigint null");
        ensureColumn("pass_vault_item", "shared_vault_id", "alter table pass_vault_item add column shared_vault_id bigint null");
        ensureColumn("pass_vault_item", "scope_type", "alter table pass_vault_item add column scope_type varchar(16) not null default 'PERSONAL'");
        ensureColumn("pass_vault_item", "item_type", "alter table pass_vault_item add column item_type varchar(16) not null default 'LOGIN'");
        ensureColumn("pass_vault_item", "monitor_excluded", "alter table pass_vault_item add column monitor_excluded tinyint not null default 0");
        ensureColumn("pass_vault_item", "two_factor_issuer", "alter table pass_vault_item add column two_factor_issuer varchar(128) null");
        ensureColumn("pass_vault_item", "two_factor_account_name", "alter table pass_vault_item add column two_factor_account_name varchar(254) null");
        ensureColumn("pass_vault_item", "two_factor_secret_ciphertext", "alter table pass_vault_item add column two_factor_secret_ciphertext varchar(512) null");
        ensureColumn("pass_vault_item", "two_factor_algorithm", "alter table pass_vault_item add column two_factor_algorithm varchar(16) null");
        ensureColumn("pass_vault_item", "two_factor_digits", "alter table pass_vault_item add column two_factor_digits int null");
        ensureColumn("pass_vault_item", "two_factor_period_seconds", "alter table pass_vault_item add column two_factor_period_seconds int null");
        jdbcTemplate.execute("alter table pass_vault_item modify column secret_ciphertext varchar(512) null");
        jdbcTemplate.update("update pass_vault_item set scope_type = 'PERSONAL' where scope_type is null or scope_type = ''");
        jdbcTemplate.update("update pass_vault_item set item_type = 'LOGIN' where item_type is null or item_type = ''");
        jdbcTemplate.update("update pass_vault_item set monitor_excluded = 0 where monitor_excluded is null");
    }

    private void ensurePassBusinessTables() {
        ensureTable("pass_shared_vault", """
                create table if not exists pass_shared_vault (
                    id bigint primary key,
                    org_id bigint not null,
                    name varchar(128) not null,
                    description varchar(500),
                    created_by bigint not null,
                    created_at timestamp not null,
                    updated_at timestamp not null,
                    deleted tinyint not null default 0
                )
                """);
        ensureTable("pass_shared_vault_member", """
                create table if not exists pass_shared_vault_member (
                    id bigint primary key,
                    org_id bigint not null,
                    vault_id bigint not null,
                    user_id bigint not null,
                    user_email varchar(254) not null,
                    role varchar(16) not null,
                    created_at timestamp not null,
                    updated_at timestamp not null,
                    deleted tinyint not null default 0
                )
                """);
        ensureTable("pass_secure_link", """
                create table if not exists pass_secure_link (
                    id bigint primary key,
                    org_id bigint not null,
                    item_id bigint not null,
                    shared_vault_id bigint not null,
                    token varchar(64) not null,
                    max_views int not null default 1,
                    current_views int not null default 0,
                    expires_at timestamp,
                    revoked_at timestamp,
                    created_by bigint not null,
                    created_at timestamp not null,
                    updated_at timestamp not null,
                    deleted tinyint not null default 0
                )
                """);
        ensureTable("pass_item_share", """
                create table if not exists pass_item_share (
                    id bigint primary key,
                    org_id bigint not null,
                    item_id bigint not null,
                    shared_vault_id bigint not null,
                    owner_id bigint not null,
                    collaborator_user_id bigint not null,
                    collaborator_email varchar(254) not null,
                    created_by bigint not null,
                    created_at timestamp not null,
                    updated_at timestamp not null,
                    deleted tinyint not null default 0
                )
                """);
    }

    private void ensurePassBusinessIndexes() {
        ensureIndex("pass_vault_item", "idx_pass_item_scope_org_updated",
                "create index idx_pass_item_scope_org_updated on pass_vault_item(scope_type, org_id, updated_at)");
        ensureIndex("pass_vault_item", "idx_pass_item_vault_updated",
                "create index idx_pass_item_vault_updated on pass_vault_item(shared_vault_id, updated_at)");
        ensureIndex("pass_shared_vault", "idx_pass_shared_vault_org_updated",
                "create index idx_pass_shared_vault_org_updated on pass_shared_vault(org_id, updated_at)");
        ensureIndex("pass_shared_vault_member", "uk_pass_shared_vault_member_user",
                "create unique index uk_pass_shared_vault_member_user on pass_shared_vault_member(vault_id, user_id, deleted)");
        ensureIndex("pass_shared_vault_member", "idx_pass_shared_vault_member_org_user",
                "create index idx_pass_shared_vault_member_org_user on pass_shared_vault_member(org_id, user_id, updated_at)");
        ensureIndex("pass_secure_link", "uk_pass_secure_link_token",
                "create unique index uk_pass_secure_link_token on pass_secure_link(token)");
        ensureIndex("pass_secure_link", "idx_pass_secure_link_item",
                "create index idx_pass_secure_link_item on pass_secure_link(item_id, revoked_at, expires_at)");
        ensureIndex("pass_item_share", "uk_pass_item_share_item_user",
                "create unique index uk_pass_item_share_item_user on pass_item_share(item_id, collaborator_user_id, deleted)");
        ensureIndex("pass_item_share", "idx_pass_item_share_org_collaborator",
                "create index idx_pass_item_share_org_collaborator on pass_item_share(org_id, collaborator_user_id, updated_at)");
        ensureIndex("pass_item_share", "idx_pass_item_share_item_updated",
                "create index idx_pass_item_share_item_updated on pass_item_share(item_id, updated_at)");
    }

    private void ensureAliasMailboxRouteSchema() {
        ensureTable("pass_alias_mailbox_route", """
                create table if not exists pass_alias_mailbox_route (
                    id bigint primary key,
                    alias_id bigint not null,
                    owner_id bigint not null,
                    mailbox_id bigint not null,
                    mailbox_email varchar(254) not null,
                    created_at timestamp not null,
                    updated_at timestamp not null,
                    deleted tinyint not null default 0
                )
                """);
        ensureIndex("pass_alias_mailbox_route", "uk_pass_alias_route_alias_mailbox",
                "create unique index uk_pass_alias_route_alias_mailbox on pass_alias_mailbox_route(alias_id, mailbox_id, deleted)");
        ensureIndex("pass_alias_mailbox_route", "idx_pass_alias_route_owner_alias",
                "create index idx_pass_alias_route_owner_alias on pass_alias_mailbox_route(owner_id, alias_id, updated_at)");
        ensureIndex("pass_alias_mailbox_route", "idx_pass_alias_route_owner_mailbox",
                "create index idx_pass_alias_route_owner_mailbox on pass_alias_mailbox_route(owner_id, mailbox_email, updated_at)");
    }

    private void ensureSimpleLoginRelayPolicySchema() {
        ensureTable("simplelogin_relay_policy", """
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
        ensureIndex("simplelogin_relay_policy", "uk_simplelogin_relay_policy_domain",
                "create unique index uk_simplelogin_relay_policy_domain on simplelogin_relay_policy(custom_domain_id, deleted)");
        ensureIndex("simplelogin_relay_policy", "idx_simplelogin_relay_policy_org_updated",
                "create index idx_simplelogin_relay_policy_org_updated on simplelogin_relay_policy(org_id, updated_at)");
        ensureIndex("simplelogin_relay_policy", "idx_simplelogin_relay_policy_owner_updated",
                "create index idx_simplelogin_relay_policy_owner_updated on simplelogin_relay_policy(owner_id, updated_at)");
        ensureIndex("simplelogin_relay_policy", "idx_simplelogin_relay_policy_mailbox",
                "create index idx_simplelogin_relay_policy_mailbox on simplelogin_relay_policy(default_mailbox_id, updated_at)");
    }

    private void ensureStandardNotesSchema() {
        ensureTable("standard_note_folder", """
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
        ensureColumn("standard_note_profile", "folder_id", "alter table standard_note_profile add column folder_id bigint null");
        ensureIndex("standard_note_profile", "idx_standard_note_profile_owner_folder",
                "create index idx_standard_note_profile_owner_folder on standard_note_profile(owner_id, folder_id, updated_at)");
        ensureIndex("standard_note_folder", "uk_standard_note_folder_owner_name",
                "create unique index uk_standard_note_folder_owner_name on standard_note_folder(owner_id, name, deleted)");
        ensureIndex("standard_note_folder", "idx_standard_note_folder_owner_updated",
                "create index idx_standard_note_folder_owner_updated on standard_note_folder(owner_id, updated_at)");
    }

    private void ensureMailMessageDeliveryTargetsColumn() {
        ensureColumn("mail_message", "delivery_targets_json", "alter table mail_message add column delivery_targets_json text null");
    }

    private void ensureMailFolderSchema() {
        ensureTable("mail_folder", """
                create table if not exists mail_folder (
                    id bigint primary key,
                    owner_id bigint not null,
                    parent_id bigint,
                    name varchar(64) not null,
                    color varchar(7) not null,
                    notifications_enabled tinyint not null default 1,
                    created_at timestamp not null,
                    updated_at timestamp not null,
                    deleted tinyint not null default 0
                )
                """);
        ensureColumn("mail_message", "custom_folder_id", "alter table mail_message add column custom_folder_id bigint null");
        ensureColumn("mail_filter", "target_custom_folder_id", "alter table mail_filter add column target_custom_folder_id bigint null");
        ensureIndex("mail_folder", "idx_mail_folder_owner_parent",
                "create index idx_mail_folder_owner_parent on mail_folder(owner_id, parent_id, updated_at)");
        ensureIndex("mail_message", "idx_mail_owner_custom_folder_sent",
                "create index idx_mail_owner_custom_folder_sent on mail_message(owner_id, custom_folder_id, sent_at)");
        ensureIndex("mail_filter", "idx_mail_filter_owner_custom_folder",
                "create index idx_mail_filter_owner_custom_folder on mail_filter(owner_id, target_custom_folder_id)");
    }

    private void ensureDriveShareSchema() {
        ensureColumn("drive_share_link", "password_hash", "alter table drive_share_link add column password_hash varchar(255) null");
        ensureColumn("drive_share_link", "token_hash", "alter table drive_share_link add column token_hash varchar(64) null");
        ensureIndex("drive_share_link", "uk_drive_share_link_token_hash",
                "create unique index uk_drive_share_link_token_hash on drive_share_link(token_hash)");
    }

    private void ensureVpnSchema() {
        ensureColumn("user_preference", "vpn_netshield_mode",
                "alter table user_preference add column vpn_netshield_mode varchar(32) not null default 'OFF'");
        ensureColumn("user_preference", "vpn_kill_switch_enabled",
                "alter table user_preference add column vpn_kill_switch_enabled tinyint not null default 0");
        ensureColumn("user_preference", "vpn_default_connection_mode",
                "alter table user_preference add column vpn_default_connection_mode varchar(32) not null default 'FASTEST'");
        ensureColumn("user_preference", "vpn_default_profile_id",
                "alter table user_preference add column vpn_default_profile_id bigint null");
        jdbcTemplate.update("update user_preference set vpn_netshield_mode = 'OFF' where vpn_netshield_mode is null or trim(vpn_netshield_mode) = ''");
        jdbcTemplate.update("update user_preference set vpn_kill_switch_enabled = 0 where vpn_kill_switch_enabled is null");
        jdbcTemplate.update("update user_preference set vpn_default_connection_mode = 'FASTEST' where vpn_default_connection_mode is null or trim(vpn_default_connection_mode) = ''");

        ensureTable("vpn_connection_profile", """
                create table if not exists vpn_connection_profile (
                    id bigint primary key,
                    owner_id bigint not null,
                    name varchar(128) not null,
                    protocol varchar(32) not null,
                    routing_mode varchar(32) not null,
                    target_server_id varchar(64),
                    target_country varchar(64),
                    secure_core_enabled tinyint not null default 0,
                    netshield_mode varchar(32) not null default 'OFF',
                    kill_switch_enabled tinyint not null default 0,
                    created_at timestamp not null,
                    updated_at timestamp not null,
                    deleted tinyint not null default 0
                )
                """);
        ensureIndex("vpn_connection_profile", "uk_vpn_profile_owner_name",
                "create unique index uk_vpn_profile_owner_name on vpn_connection_profile(owner_id, name, deleted)");
        ensureIndex("vpn_connection_profile", "idx_vpn_profile_owner_updated",
                "create index idx_vpn_profile_owner_updated on vpn_connection_profile(owner_id, updated_at)");

        ensureColumn("vpn_connection_session", "profile_id", "alter table vpn_connection_session add column profile_id bigint null");
        ensureColumn("vpn_connection_session", "profile_name", "alter table vpn_connection_session add column profile_name varchar(128) null");
        ensureColumn("vpn_connection_session", "netshield_mode",
                "alter table vpn_connection_session add column netshield_mode varchar(32) not null default 'OFF'");
        ensureColumn("vpn_connection_session", "kill_switch_enabled",
                "alter table vpn_connection_session add column kill_switch_enabled tinyint not null default 0");
        ensureColumn("vpn_connection_session", "connection_source",
                "alter table vpn_connection_session add column connection_source varchar(32) not null default 'MANUAL'");
        jdbcTemplate.update("update vpn_connection_session set netshield_mode = 'OFF' where netshield_mode is null or trim(netshield_mode) = ''");
        jdbcTemplate.update("update vpn_connection_session set kill_switch_enabled = 0 where kill_switch_enabled is null");
        jdbcTemplate.update("update vpn_connection_session set connection_source = 'MANUAL' where connection_source is null or trim(connection_source) = ''");
    }

    private void ensurePassMailboxSchema() {
        ensureTable("pass_mailbox", """
                create table if not exists pass_mailbox (
                    id bigint primary key,
                    owner_id bigint not null,
                    mailbox_user_id bigint not null,
                    mailbox_email varchar(254) not null,
                    status varchar(16) not null,
                    verification_code varchar(32),
                    verification_sent_at timestamp not null,
                    verified_at timestamp,
                    is_default tinyint not null default 0,
                    created_at timestamp not null,
                    updated_at timestamp not null,
                    deleted tinyint not null default 0
                )
                """);
        ensureIndex("pass_mailbox", "uk_pass_mailbox_owner_email",
                "create unique index uk_pass_mailbox_owner_email on pass_mailbox(owner_id, mailbox_email)");
        ensureIndex("pass_mailbox", "idx_pass_mailbox_owner_status",
                "create index idx_pass_mailbox_owner_status on pass_mailbox(owner_id, status, updated_at)");
        ensureIndex("pass_mailbox", "idx_pass_mailbox_owner_default",
                "create index idx_pass_mailbox_owner_default on pass_mailbox(owner_id, is_default, updated_at)");
    }

    private void ensureTable(String tableName, String createSql) {
        if (tableExists(tableName)) {
            return;
        }
        log.info("Creating missing table {}", tableName);
        jdbcTemplate.execute(createSql);
    }

    private void ensureColumn(String tableName, String columnName, String alterSql) {
        if (columnExists(tableName, columnName)) {
            return;
        }
        log.info("Adding missing column {}.{}", tableName, columnName);
        jdbcTemplate.execute(alterSql);
    }

    private void ensureIndex(String tableName, String indexName, String createSql) {
        if (indexExists(tableName, indexName)) {
            return;
        }
        log.info("Creating missing index {} on {}", indexName, tableName);
        jdbcTemplate.execute(createSql);
    }

    private boolean tableExists(String tableName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet result = metaData.getTables(connection.getCatalog(), null, tableName, null)) {
                if (result.next()) {
                    return true;
                }
            }
            try (ResultSet result = metaData.getTables(connection.getCatalog(), null, tableName.toUpperCase(), null)) {
                return result.next();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to inspect table metadata for " + tableName, exception);
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet result = metaData.getColumns(connection.getCatalog(), null, tableName, columnName)) {
                if (result.next()) {
                    return true;
                }
            }
            try (ResultSet result = metaData.getColumns(connection.getCatalog(), null, tableName.toUpperCase(), columnName.toUpperCase())) {
                return result.next();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to inspect column metadata for " + tableName + '.' + columnName, exception);
        }
    }

    private boolean indexExists(String tableName, String indexName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet result = metaData.getIndexInfo(connection.getCatalog(), null, tableName, false, false)) {
                while (result.next()) {
                    String existing = result.getString("INDEX_NAME");
                    if (existing != null && existing.equalsIgnoreCase(indexName)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to inspect index metadata for " + tableName + '.' + indexName, exception);
        }
    }
}
