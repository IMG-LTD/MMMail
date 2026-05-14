create table if not exists platform_outbox_event (
    id bigint primary key,
    event_type varchar(128) not null,
    owner_module varchar(64) not null,
    tenant_id varchar(64) not null,
    user_id varchar(64),
    request_id varchar(64),
    trace_id varchar(64),
    aggregate_type varchar(64) not null,
    aggregate_id varchar(128) not null,
    payload_json text not null,
    idempotency_key varchar(128) not null,
    status varchar(32) not null,
    attempts int not null default 0,
    next_attempt_at timestamp,
    last_error varchar(512),
    created_at timestamp not null,
    updated_at timestamp not null,
    published_at timestamp
);

create unique index uk_platform_outbox_idempotency
    on platform_outbox_event(idempotency_key);
create index idx_platform_outbox_status_next_attempt
    on platform_outbox_event(status, next_attempt_at);
create index idx_platform_outbox_owner_created
    on platform_outbox_event(owner_module, created_at);
create index idx_platform_outbox_tenant_created
    on platform_outbox_event(tenant_id, created_at);

update system_release_metadata
set schema_version = '16',
    updated_at = current_timestamp
where id = 1;
