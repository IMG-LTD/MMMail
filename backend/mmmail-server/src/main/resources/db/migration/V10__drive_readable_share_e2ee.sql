alter table drive_share_link
    add column readable_e2ee_enabled tinyint not null default 0;

alter table drive_share_link
    add column readable_e2ee_algorithm varchar(64);

alter table drive_share_link
    add column readable_e2ee_storage_path varchar(512);

alter table drive_share_link
    add column readable_e2ee_checksum varchar(128);

update system_release_metadata
set schema_version = '10',
    updated_at = current_timestamp
where id = 1;
