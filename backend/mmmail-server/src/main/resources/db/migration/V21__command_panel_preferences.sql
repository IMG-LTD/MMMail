-- DESCRIPTION: Persist v2.1.2 command panel pins and recent command usage per user.
-- ROLLBACK: drop table command_panel_preference;

create table if not exists command_panel_preference (
    id bigint primary key,
    owner_id bigint not null,
    command_id varchar(120) not null,
    pinned tinyint not null default 0,
    usage_count int not null default 0,
    last_used_at timestamp,
    pinned_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_command_panel_pref_owner_command
    on command_panel_preference(owner_id, command_id, deleted);
create index idx_command_panel_pref_owner_recent
    on command_panel_preference(owner_id, last_used_at);
create index idx_command_panel_pref_owner_pinned
    on command_panel_preference(owner_id, pinned, pinned_at);

update system_release_metadata
set schema_version = '21',
    updated_at = current_timestamp
where id = 1;
