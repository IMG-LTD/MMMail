-- DESCRIPTION: Add v2.1.2 login anomaly attempts, lock state, and user/admin security events.
-- ROLLBACK: drop index idx_security_event_severity_created on security_event; drop index idx_security_event_user_created on security_event; drop table security_event; drop index idx_auth_login_lock_email_ip on auth_login_lock; drop table auth_login_lock; drop index idx_auth_login_attempt_email_ip_created on auth_login_attempt; drop index idx_auth_login_attempt_user_created on auth_login_attempt; drop table auth_login_attempt;

create table if not exists auth_login_attempt (
    id bigint primary key,
    user_id bigint,
    email varchar(255) not null,
    ip_address varchar(64) not null,
    success tinyint not null,
    city varchar(128),
    country varchar(64),
    latitude double,
    longitude double,
    geo_source varchar(64),
    created_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_auth_login_attempt_user_created on auth_login_attempt(user_id, created_at);
create index idx_auth_login_attempt_email_ip_created on auth_login_attempt(email, ip_address, created_at);

create table if not exists auth_login_lock (
    id bigint primary key,
    email varchar(255) not null,
    ip_address varchar(64) not null,
    failure_count int not null,
    locked_until timestamp not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_auth_login_lock_email_ip on auth_login_lock(email, ip_address);

create table if not exists security_event (
    id bigint primary key,
    user_id bigint,
    email varchar(255),
    type varchar(64) not null,
    severity varchar(16) not null,
    risk varchar(16) not null,
    reasons varchar(255),
    ip_address varchar(64),
    city varchar(128),
    country varchar(64),
    source varchar(64),
    detail text,
    locked_until timestamp,
    acknowledged_at timestamp,
    action_status varchar(32),
    action_taken varchar(64),
    action_by bigint,
    action_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_security_event_user_created on security_event(user_id, created_at);
create index idx_security_event_severity_created on security_event(severity, created_at);
