alter table calendar_event_share
    add column source varchar(24) not null default 'MANUAL';

update calendar_event_share
set source = 'MANUAL'
where source is null or source = '';

create index idx_calendar_share_owner_event_source
    on calendar_event_share(owner_id, event_id, source, updated_at);

update system_release_metadata
set schema_version = '9',
    updated_at = current_timestamp
where id = 1;
