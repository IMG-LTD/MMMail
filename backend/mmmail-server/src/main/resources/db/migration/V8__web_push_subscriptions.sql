create table if not exists web_push_subscription (
    id bigint primary key,
    owner_id bigint not null,
    endpoint_hash varchar(64) not null,
    endpoint varchar(1024) not null,
    p256dh_key varchar(255) not null,
    auth_key varchar(255) not null,
    content_encoding varchar(32) not null,
    user_agent varchar(255),
    last_success_at timestamp,
    last_failure_at timestamp,
    last_error_message varchar(255),
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_web_push_subscription_owner_endpoint_hash
    on web_push_subscription(owner_id, endpoint_hash);

create index idx_web_push_subscription_owner_updated
    on web_push_subscription(owner_id, updated_at);

update system_release_metadata
set schema_version = '8',
    updated_at = current_timestamp
where id = 1;
