create table if not exists mail_attachment (
    id bigint primary key,
    owner_id bigint not null,
    mail_id bigint not null,
    file_name varchar(255) not null,
    content_type varchar(255) not null,
    file_size bigint not null,
    storage_path varchar(512) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_mail_attachment_owner_mail_created on mail_attachment(owner_id, mail_id, created_at);
create index idx_mail_attachment_storage_path on mail_attachment(storage_path);

update system_release_metadata
set schema_version = '5',
    updated_at = current_timestamp
where id = 1;
