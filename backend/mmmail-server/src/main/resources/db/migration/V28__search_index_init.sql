-- DESCRIPTION: v2.1.2 global search index, staging, and reindex job state.
-- ROLLBACK: drop tables search_index_staging, search_index, and search_reindex_job.

create table if not exists search_index (
    id bigint primary key,
    module_type varchar(32) not null,
    resource_id varchar(128) not null,
    org_id bigint,
    owner_user_id bigint,
    acl_user_ids text,
    title varchar(255) not null,
    body text,
    route_path varchar(512) not null,
    updated_at timestamp not null,
    created_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_search_index_module_resource on search_index(module_type, resource_id);
create index idx_search_index_owner_module_updated on search_index(owner_user_id, module_type, updated_at);
create index idx_search_index_org_module_updated on search_index(org_id, module_type, updated_at);
create index idx_search_index_title on search_index(module_type, title);

create table if not exists search_index_staging (
    id bigint primary key,
    job_id varchar(64) not null,
    module_type varchar(32) not null,
    resource_id varchar(128) not null,
    org_id bigint,
    owner_user_id bigint,
    acl_user_ids text,
    title varchar(255) not null,
    body text,
    route_path varchar(512) not null,
    updated_at timestamp not null,
    created_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_search_stage_job on search_index_staging(job_id, module_type);

create table if not exists search_reindex_job (
    id varchar(64) primary key,
    module_type varchar(32) not null,
    status varchar(32) not null,
    processed int not null default 0,
    total int not null default 0,
    errors int not null default 0,
    error_message text,
    created_at timestamp not null,
    updated_at timestamp not null,
    completed_at timestamp
);

create index idx_search_reindex_status_created on search_reindex_job(status, created_at);
