create table if not exists v21_collaboration_project (
    id bigint primary key,
    owner_id bigint not null,
    name varchar(160) not null,
    product varchar(32) not null,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_v21_collab_project_owner_name
    on v21_collaboration_project(owner_id, name, deleted);
create index idx_v21_collab_project_owner_updated
    on v21_collaboration_project(owner_id, updated_at);
create index idx_v21_collab_project_owner_product_status
    on v21_collaboration_project(owner_id, product, status);

create table if not exists v21_collaboration_task (
    id bigint primary key,
    project_id bigint not null,
    owner_id bigint not null,
    title varchar(220) not null,
    product varchar(32) not null,
    status varchar(32) not null,
    assignee_email varchar(190),
    due_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_v21_collab_task_owner_updated
    on v21_collaboration_task(owner_id, updated_at);
create index idx_v21_collab_task_project_status_updated
    on v21_collaboration_task(project_id, status, updated_at);
create index idx_v21_collab_task_owner_status_due
    on v21_collaboration_task(owner_id, status, due_at);

create table if not exists v21_collaboration_comment (
    id bigint primary key,
    task_id bigint not null,
    project_id bigint not null,
    owner_id bigint not null,
    author_user_id bigint not null,
    body text not null,
    created_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_v21_collab_comment_task_created
    on v21_collaboration_comment(task_id, created_at);
create index idx_v21_collab_comment_owner_created
    on v21_collaboration_comment(owner_id, created_at);

update system_release_metadata
set schema_version = '13',
    updated_at = current_timestamp
where id = 1;
