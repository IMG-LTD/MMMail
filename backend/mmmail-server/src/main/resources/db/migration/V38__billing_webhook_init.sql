-- DESCRIPTION: Initialize v2.2 billing webhook idempotency and subscription state.
-- ROLLBACK: drop index idx_org_subscription_state_status; drop table org_subscription_state; drop table billing_webhook_event; restore system_release_metadata.schema_version to 37 before rerunning later migrations.

create table if not exists billing_webhook_event (
    event_id varchar(128) not null primary key,
    provider varchar(32) not null,
    org_id bigint not null,
    plan varchar(32) not null,
    status varchar(32) not null,
    occurred_at timestamp not null,
    processed_at timestamp not null,
    signature_version varchar(16) not null,
    payload_json text not null
);

create table if not exists org_subscription_state (
    org_id bigint not null primary key,
    plan varchar(32) not null,
    status varchar(32) not null,
    provider varchar(32) not null,
    updated_at timestamp not null default current_timestamp
);

create index idx_org_subscription_state_status
    on org_subscription_state(status, updated_at);

update system_release_metadata
set schema_version = '38',
    updated_at = current_timestamp
where id = 1;
