-- DESCRIPTION: Initialize v2.2 license verification state.
-- ROLLBACK: drop index idx_license_state_status_synced; drop table license_state; restore system_release_metadata.schema_version to 36 before rerunning later migrations.

create table if not exists license_state (
    org_id bigint not null primary key,
    claims_json text not null,
    status varchar(32) not null,
    synced_at timestamp not null,
    expires_at timestamp not null,
    updated_at timestamp not null default current_timestamp
);

create index idx_license_state_status_synced
    on license_state(status, synced_at);

update system_release_metadata
set schema_version = '37',
    updated_at = current_timestamp
where id = 1;
