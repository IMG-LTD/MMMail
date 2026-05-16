-- DESCRIPTION: Add v2.1.2 CRDT collaboration snapshots and incremental update log.
-- ROLLBACK: drop index idx_collab_update_resource_created on collab_update; drop index uk_collab_update_resource_seq on collab_update; drop table collab_update; drop index idx_collab_snapshot_resource_updated on collab_snapshot; drop index uk_collab_snapshot_resource_version on collab_snapshot; drop table collab_snapshot;

create table if not exists collab_snapshot (
    id bigint primary key,
    resource_type varchar(32) not null,
    resource_id varchar(64) not null,
    version int not null,
    snapshot blob not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_collab_snapshot_resource_version
    on collab_snapshot(resource_type, resource_id, version, deleted);
create index idx_collab_snapshot_resource_updated
    on collab_snapshot(resource_type, resource_id, updated_at);

create table if not exists collab_update (
    id bigint primary key,
    resource_type varchar(32) not null,
    resource_id varchar(64) not null,
    seq bigint not null,
    update_payload blob not null,
    created_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_collab_update_resource_seq
    on collab_update(resource_type, resource_id, seq, deleted);
create index idx_collab_update_resource_created
    on collab_update(resource_type, resource_id, created_at);
