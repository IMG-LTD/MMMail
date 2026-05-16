-- DESCRIPTION: Add v2.1.2 external calendar subscription state and imported event mapping.
-- ROLLBACK: drop index idx_calendar_subscription_event_owner on calendar_subscription_event; drop index idx_calendar_subscription_event_subscription on calendar_subscription_event; drop table calendar_subscription_event; drop index idx_calendar_subscription_owner on calendar_subscription; drop table calendar_subscription;

create table if not exists calendar_subscription (
    id bigint primary key,
    owner_id bigint not null,
    url varchar(2048) not null,
    label varchar(128) not null,
    auth_mode varchar(32) not null default 'none',
    color varchar(32),
    sync_status varchar(32) not null default 'PENDING',
    last_sync_at timestamp,
    last_error varchar(512),
    next_sync_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_calendar_subscription_owner
    on calendar_subscription(owner_id, updated_at);

create table if not exists calendar_subscription_event (
    id bigint primary key,
    owner_id bigint not null,
    subscription_id bigint not null,
    event_id bigint not null,
    created_at timestamp not null
);

create index idx_calendar_subscription_event_subscription
    on calendar_subscription_event(subscription_id, event_id);

create index idx_calendar_subscription_event_owner
    on calendar_subscription_event(owner_id, subscription_id);
