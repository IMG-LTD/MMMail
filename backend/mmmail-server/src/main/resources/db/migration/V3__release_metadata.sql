create table if not exists system_release_metadata (
    id bigint primary key,
    edition varchar(32) not null,
    release_channel varchar(32) not null,
    application_version varchar(32) not null,
    schema_version varchar(16) not null,
    installed_at timestamp not null,
    updated_at timestamp not null
);

insert into system_release_metadata (
    id,
    edition,
    release_channel,
    application_version,
    schema_version,
    installed_at,
    updated_at
) values (
    1,
    'COMMUNITY',
    'GA',
    'v1.0-draft',
    '3',
    current_timestamp,
    current_timestamp
)
on duplicate key update
    edition = values(edition),
    release_channel = values(release_channel),
    application_version = values(application_version),
    schema_version = values(schema_version),
    updated_at = current_timestamp;
