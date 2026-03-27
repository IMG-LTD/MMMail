package com.mmmail.server.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
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
public class WalletParitySchemaInitializer implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public WalletParitySchemaInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureColumn("wallet_account", "address_type",
                "alter table wallet_account add column address_type varchar(32) not null default 'NATIVE_SEGWIT'");
        ensureColumn("wallet_account", "account_index",
                "alter table wallet_account add column account_index int not null default 0");
        ensureColumn("wallet_account", "imported",
                "alter table wallet_account add column imported tinyint not null default 0");
        ensureColumn("wallet_account", "wallet_source_fingerprint",
                "alter table wallet_account add column wallet_source_fingerprint varchar(64) null");
        ensureColumn("wallet_account", "wallet_passphrase_protected",
                "alter table wallet_account add column wallet_passphrase_protected tinyint not null default 0");
        ensureColumn("wallet_account", "imported_at",
                "alter table wallet_account add column imported_at timestamp null");
        ensureTable("wallet_account_profile", """
                create table if not exists wallet_account_profile (
                    id bigint primary key,
                    account_id bigint not null,
                    owner_id bigint not null,
                    bitcoin_via_email_enabled tinyint not null default 0,
                    alias_email varchar(254),
                    balance_masked tinyint not null default 0,
                    address_privacy_enabled tinyint not null default 1,
                    address_pool_size int not null default 3,
                    recovery_phrase varchar(512) not null,
                    recovery_fingerprint varchar(64) not null,
                    passphrase_hint varchar(128),
                    last_recovery_viewed_at timestamp null,
                    created_at timestamp not null,
                    updated_at timestamp not null,
                    deleted tinyint not null default 0
                )
                """);
        ensureTable("wallet_receive_address", """
                create table if not exists wallet_receive_address (
                    id bigint primary key,
                    account_id bigint not null,
                    owner_id bigint not null,
                    address varchar(128) not null,
                    label varchar(64) not null,
                    source_type varchar(16) not null,
                    address_kind varchar(16) not null default 'RECEIVE',
                    address_index int not null default 0,
                    address_status varchar(16) not null default 'UNUSED',
                    value_minor bigint not null default 0,
                    reserved_for varchar(64),
                    created_at timestamp not null,
                    updated_at timestamp not null,
                    deleted tinyint not null default 0
                )
                """);
        ensureTable("wallet_email_transfer", """
                create table if not exists wallet_email_transfer (
                    id bigint primary key,
                    transaction_id bigint not null,
                    account_id bigint not null,
                    owner_id bigint not null,
                    recipient_email varchar(254) not null,
                    delivery_message varchar(256),
                    claim_code varchar(64) not null,
                    status varchar(32) not null,
                    invite_required tinyint not null default 0,
                    amount_minor bigint not null,
                    asset_symbol varchar(16) not null,
                    claimed_at timestamp null,
                    created_at timestamp not null,
                    updated_at timestamp not null,
                    deleted tinyint not null default 0
                )
                """);
        ensureIndex(
                "wallet_account_profile",
                "uk_wallet_account_profile_account",
                "create unique index uk_wallet_account_profile_account on wallet_account_profile(account_id, deleted)"
        );
        ensureColumn("wallet_receive_address", "address_kind",
                "alter table wallet_receive_address add column address_kind varchar(16) not null default 'RECEIVE'");
        ensureColumn("wallet_receive_address", "address_index",
                "alter table wallet_receive_address add column address_index int not null default 0");
        ensureColumn("wallet_receive_address", "address_status",
                "alter table wallet_receive_address add column address_status varchar(16) not null default 'UNUSED'");
        ensureColumn("wallet_receive_address", "value_minor",
                "alter table wallet_receive_address add column value_minor bigint not null default 0");
        ensureColumn("wallet_receive_address", "reserved_for",
                "alter table wallet_receive_address add column reserved_for varchar(64) null");
        normalizeWalletAccountSlots();
        normalizeWalletReceiveAddressIndexes();
        dropIndexIfExists("wallet_account", "uk_wallet_account_owner_slot");
        ensureIndex(
                "wallet_receive_address",
                "uk_wallet_receive_address_account_value",
                "create unique index uk_wallet_receive_address_account_value on wallet_receive_address(account_id, address, deleted)"
        );
        ensureIndex(
                "wallet_receive_address",
                "uk_wallet_receive_address_kind_index",
                "create unique index uk_wallet_receive_address_kind_index on wallet_receive_address(account_id, address_kind, address_index, deleted)"
        );
        ensureIndex(
                "wallet_receive_address",
                "idx_wallet_receive_address_owner_account",
                "create index idx_wallet_receive_address_owner_account on wallet_receive_address(owner_id, account_id, updated_at)"
        );
        ensureIndex(
                "wallet_receive_address",
                "idx_wallet_receive_address_owner_kind",
                "create index idx_wallet_receive_address_owner_kind on wallet_receive_address(owner_id, account_id, address_kind, address_index)"
        );
        ensureIndex(
                "wallet_email_transfer",
                "uk_wallet_email_transfer_transaction",
                "create unique index uk_wallet_email_transfer_transaction on wallet_email_transfer(transaction_id, deleted)"
        );
        ensureIndex(
                "wallet_email_transfer",
                "idx_wallet_email_transfer_owner_account",
                "create index idx_wallet_email_transfer_owner_account on wallet_email_transfer(owner_id, account_id, updated_at)"
        );
    }

    private void ensureTable(String tableName, String createSql) {
        if (hasTable(tableName)) {
            return;
        }
        jdbcTemplate.execute(createSql);
    }

    private void ensureColumn(String tableName, String columnName, String alterSql) {
        if (hasColumn(tableName, columnName)) {
            return;
        }
        jdbcTemplate.execute(alterSql);
    }

    private void ensureIndex(String tableName, String indexName, String createSql) {
        if (hasIndex(tableName, indexName)) {
            return;
        }
        jdbcTemplate.execute(createSql);
    }

    private void dropIndexIfExists(String tableName, String indexName) {
        if (!hasIndex(tableName, indexName)) {
            return;
        }
        jdbcTemplate.execute("drop index " + indexName + " on " + tableName);
    }

    private void normalizeWalletAccountSlots() {
        if (!hasTable("wallet_account") || !hasDuplicateAccountSlots()) {
            return;
        }
        jdbcTemplate.execute("""
                update wallet_account target
                join (
                    select id,
                           row_number() over (
                               partition by owner_id, asset_symbol, address_type
                               order by created_at, id
                           ) - 1 as slot_index
                    from wallet_account
                    where deleted = 0
                ) ordered on ordered.id = target.id
                set target.account_index = ordered.slot_index
                """);
    }

    private void normalizeWalletReceiveAddressIndexes() {
        if (!hasTable("wallet_receive_address") || !hasDuplicateAddressIndexes()) {
            return;
        }
        jdbcTemplate.execute("""
                update wallet_receive_address target
                join (
                    select id,
                           row_number() over (
                               partition by account_id, address_kind
                               order by created_at, id
                           ) - 1 as next_index
                    from wallet_receive_address
                    where deleted = 0
                ) ordered on ordered.id = target.id
                set target.address_index = ordered.next_index
                """);
    }

    private boolean hasDuplicateAccountSlots() {
        Integer duplicates = jdbcTemplate.queryForObject("""
                select count(*) from (
                    select owner_id, asset_symbol, address_type, account_index
                    from wallet_account
                    where deleted = 0
                    group by owner_id, asset_symbol, address_type, account_index
                    having count(*) > 1
                ) duplicated
                """, Integer.class);
        return duplicates != null && duplicates > 0;
    }

    private boolean hasDuplicateAddressIndexes() {
        Integer duplicates = jdbcTemplate.queryForObject("""
                select count(*) from (
                    select account_id, address_kind, address_index
                    from wallet_receive_address
                    where deleted = 0
                    group by account_id, address_kind, address_index
                    having count(*) > 1
                ) duplicated
                """, Integer.class);
        return duplicates != null && duplicates > 0;
    }

    private boolean hasTable(String tableName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getTables(connection.getCatalog(), null, tableName, null)) {
                if (resultSet.next()) {
                    return true;
                }
            }
            try (ResultSet resultSet = metaData.getTables(connection.getCatalog(), null, tableName.toUpperCase(), null)) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to inspect wallet parity schema table " + tableName, exception);
        }
    }

    private boolean hasColumn(String tableName, String columnName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), null, tableName, columnName)) {
                if (resultSet.next()) {
                    return true;
                }
            }
            try (ResultSet resultSet = metaData.getColumns(
                    connection.getCatalog(),
                    null,
                    tableName.toUpperCase(),
                    columnName.toUpperCase()
            )) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to inspect wallet parity schema column " + tableName + '.' + columnName,
                    exception
            );
        }
    }

    private boolean hasIndex(String tableName, String indexName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getIndexInfo(connection.getCatalog(), null, tableName, false, false)) {
                while (resultSet.next()) {
                    String existing = resultSet.getString("INDEX_NAME");
                    if (indexName.equalsIgnoreCase(existing)) {
                        return true;
                    }
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to inspect wallet parity schema index " + indexName, exception);
        }
        return false;
    }
}
