insert ignore into user_account (id, email, password_hash, display_name, role, status, token_version, created_at, updated_at, deleted)
values (9100000000001, 'seed@mmmail.local', '$2a$10$oUT4YtfDuu3St4IuNYAZ0eVv4WELZxr8jvvyZxL/vC2DF/5phviEi', 'Dev Seed User', 'USER', 1, 1, current_timestamp, current_timestamp, 0);

insert ignore into user_preference (id, owner_id, signature, timezone, preferred_locale, mail_address_mode, auto_save_seconds, undo_send_seconds, created_at, updated_at, deleted)
values (9100000000101, 9100000000001, 'Regards,\nDev Seed User', 'Asia/Shanghai', 'en', 'PROTON_ADDRESS', 15, 10, current_timestamp, current_timestamp, 0);

insert ignore into wallet_account (id, owner_id, wallet_name, asset_symbol, address, balance_minor, address_type, account_index, imported, wallet_source_fingerprint, wallet_passphrase_protected, imported_at, created_at, updated_at, deleted)
values
    (9100000001001, 9100000000001, 'dev-wallet-primary', 'BTC', 'bc1qdevseedprimary000000000000000000000000', 125000000, 'NATIVE_SEGWIT', 0, 0, 'devseed01', 0, null, current_timestamp, current_timestamp, 0),
    (9100000001002, 9100000000001, 'dev-wallet-savings', 'BTC', 'bc1qdevseedsavings000000000000000000000000', 87500000, 'NATIVE_SEGWIT', 1, 0, 'devseed02', 1, null, current_timestamp, current_timestamp, 0),
    (9100000001003, 9100000000001, 'dev-wallet-ops', 'BTC', 'bc1qdevseedops0000000000000000000000000000', 24000000, 'NATIVE_SEGWIT', 2, 1, 'devseed03', 0, current_timestamp, current_timestamp, current_timestamp, 0);

insert ignore into wallet_transaction (id, owner_id, account_id, tx_type, counterparty_address, amount_minor, asset_symbol, memo, status, confirmations, signature_hash, network_tx_hash, created_at, updated_at, deleted)
values
    (9100000001101, 9100000000001, 9100000001001, 'RECEIVE', 'bc1qexternalclient000000000000000000000000', 50000000, 'BTC', 'Initial client deposit', 'CONFIRMED', 18, 'seed-signature-001', 'seed-network-tx-001', timestampadd(day, -5, current_timestamp), current_timestamp, 0),
    (9100000001102, 9100000000001, 9100000001001, 'SEND', 'bc1qexternalvendor000000000000000000000000', -7500000, 'BTC', 'Vendor payout', 'CONFIRMED', 11, 'seed-signature-002', 'seed-network-tx-002', timestampadd(day, -4, current_timestamp), current_timestamp, 0),
    (9100000001103, 9100000000001, 9100000001002, 'RECEIVE', 'bc1qexternaltreasury000000000000000000000', 87500000, 'BTC', 'Savings top up', 'CONFIRMED', 42, 'seed-signature-003', 'seed-network-tx-003', timestampadd(day, -3, current_timestamp), current_timestamp, 0),
    (9100000001104, 9100000000001, 9100000001003, 'RECEIVE', 'bc1qexternalimport00000000000000000000000', 24000000, 'BTC', 'Imported wallet balance', 'PENDING', 1, 'seed-signature-004', 'seed-network-tx-004', timestampadd(day, -2, current_timestamp), current_timestamp, 0),
    (9100000001105, 9100000000001, 9100000001001, 'SEND', 'bc1qexternalrefund00000000000000000000000', -12000000, 'BTC', 'Refund test transfer', 'BROADCAST', 0, 'seed-signature-005', 'seed-network-tx-005', timestampadd(day, -1, current_timestamp), current_timestamp, 0);
