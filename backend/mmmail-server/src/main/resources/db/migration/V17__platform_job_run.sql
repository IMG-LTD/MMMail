create table if not exists platform_job_run (
    id bigint primary key,
    job_type varchar(128) not null,
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
    progress_percent int not null default 0,
    attempts int not null default 0,
    max_attempts int not null default 1,
    next_attempt_at timestamp,
    last_error_code varchar(64),
    last_error_message varchar(512),
    result_json text,
    created_at timestamp not null,
    updated_at timestamp not null,
    started_at timestamp,
    completed_at timestamp
);

create unique index uk_platform_job_run_idempotency
    on platform_job_run(idempotency_key);
create index idx_platform_job_run_status_next_attempt
    on platform_job_run(status, next_attempt_at);
create index idx_platform_job_run_owner_created
    on platform_job_run(owner_module, created_at);
create index idx_platform_job_run_tenant_created
    on platform_job_run(tenant_id, created_at);

update system_release_metadata
set schema_version = '17',
    updated_at = current_timestamp
where id = 1;
