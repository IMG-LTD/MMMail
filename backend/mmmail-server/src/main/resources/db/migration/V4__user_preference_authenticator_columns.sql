alter table user_preference add column if not exists authenticator_sync_enabled tinyint not null default 1;
alter table user_preference add column if not exists authenticator_encrypted_backup_enabled tinyint not null default 0;
alter table user_preference add column if not exists authenticator_pin_protection_enabled tinyint not null default 0;
alter table user_preference add column if not exists authenticator_pin_hash varchar(255) null;
alter table user_preference add column if not exists authenticator_lock_timeout_seconds int not null default 300;
alter table user_preference add column if not exists authenticator_last_synced_at timestamp null;
alter table user_preference add column if not exists authenticator_last_backup_at timestamp null;

update user_preference
set authenticator_sync_enabled = 1
where authenticator_sync_enabled is null;

update user_preference
set authenticator_encrypted_backup_enabled = 0
where authenticator_encrypted_backup_enabled is null;

update user_preference
set authenticator_pin_protection_enabled = 0
where authenticator_pin_protection_enabled is null;

update user_preference
set authenticator_lock_timeout_seconds = 300
where authenticator_lock_timeout_seconds is null;

update system_release_metadata
set schema_version = '4',
    updated_at = current_timestamp
where id = 1;
