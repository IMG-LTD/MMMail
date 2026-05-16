-- DESCRIPTION: Add v2.1.2 IMAP/SMTP external mail account configuration and sync state.
-- ROLLBACK: drop index idx_mail_external_account_owner_status on mail_external_account; drop index uk_mail_external_account_owner_email on mail_external_account; drop table mail_external_account;

create table if not exists mail_external_account (
    id bigint primary key,
    owner_id bigint not null,
    provider varchar(32) not null,
    auth_mode varchar(32) not null,
    email varchar(254) not null,
    username varchar(254) not null,
    secret_ciphertext text not null,
    imap_host varchar(255) not null,
    imap_port int not null,
    imap_ssl tinyint not null default 1,
    smtp_host varchar(255) not null,
    smtp_port int not null,
    smtp_starttls tinyint not null default 1,
    smtp_ssl tinyint not null default 0,
    sync_status varchar(32) not null,
    uid_high_watermark varchar(128),
    last_sync_at timestamp,
    last_error varchar(512),
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_mail_external_account_owner_email on mail_external_account(owner_id, email);
create index idx_mail_external_account_owner_status on mail_external_account(owner_id, sync_status, updated_at);
