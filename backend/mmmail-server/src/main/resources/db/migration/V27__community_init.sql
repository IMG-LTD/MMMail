-- DESCRIPTION: Add v2.1.2 community topics, posts, comments, engagement, view, and report tables.
-- ROLLBACK: drop index idx_community_report_target on community_report; drop index idx_community_report_status_created on community_report; drop table community_report; drop index idx_community_view_last_seen on community_post_view; drop index uk_community_view_post_user on community_post_view; drop table community_post_view; drop index idx_community_bookmark_user_created on community_post_bookmark; drop index uk_community_bookmark_post_user on community_post_bookmark; drop table community_post_bookmark; drop index uk_community_like_post_user on community_post_like; drop table community_post_like; drop index idx_community_comment_parent_created on community_comment; drop index idx_community_comment_post_created on community_comment; drop table community_comment; drop index idx_community_post_author_updated on community_post; drop index idx_community_post_topic_status_updated on community_post; drop index idx_community_post_status_updated on community_post; drop table community_post; drop index idx_community_topic_sort on community_topic; drop index uk_community_topic_slug on community_topic; drop table community_topic;

create table if not exists community_topic (
    id varchar(64) primary key,
    slug varchar(64) not null,
    title varchar(120) not null,
    description varchar(500),
    sort_order int not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_community_topic_slug on community_topic(slug, deleted);
create index idx_community_topic_sort on community_topic(sort_order, title);

create table if not exists community_post (
    id varchar(64) primary key,
    author_user_id bigint not null,
    org_id bigint,
    topic_id varchar(64) not null,
    title varchar(180) not null,
    body_md text not null,
    body_html text not null,
    tags_json text not null,
    like_count int not null default 0,
    comment_count int not null default 0,
    view_count int not null default 0,
    pinned tinyint not null default 0,
    locked tinyint not null default 0,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted_at timestamp,
    deleted tinyint not null default 0
);

create index idx_community_post_status_updated on community_post(status, pinned, updated_at);
create index idx_community_post_topic_status_updated on community_post(topic_id, status, updated_at);
create index idx_community_post_author_updated on community_post(author_user_id, updated_at);

create table if not exists community_comment (
    id varchar(64) primary key,
    post_id varchar(64) not null,
    parent_comment_id varchar(64),
    author_user_id bigint not null,
    body_md text not null,
    body_html text not null,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted_at timestamp,
    deleted tinyint not null default 0
);

create index idx_community_comment_post_created on community_comment(post_id, created_at);
create index idx_community_comment_parent_created on community_comment(parent_comment_id, created_at);

create table if not exists community_post_like (
    id bigint primary key,
    post_id varchar(64) not null,
    user_id bigint not null,
    created_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_community_like_post_user on community_post_like(post_id, user_id, deleted);

create table if not exists community_post_bookmark (
    id bigint primary key,
    post_id varchar(64) not null,
    user_id bigint not null,
    created_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_community_bookmark_post_user on community_post_bookmark(post_id, user_id, deleted);
create index idx_community_bookmark_user_created on community_post_bookmark(user_id, created_at);

create table if not exists community_post_view (
    id bigint primary key,
    post_id varchar(64) not null,
    user_id bigint not null,
    last_viewed_at timestamp not null,
    view_count int not null default 1
);

create unique index uk_community_view_post_user on community_post_view(post_id, user_id);
create index idx_community_view_last_seen on community_post_view(last_viewed_at);

create table if not exists community_report (
    id varchar(64) primary key,
    target_type varchar(32) not null,
    target_id varchar(64) not null,
    reporter_user_id bigint not null,
    reason varchar(64) not null,
    detail varchar(1000),
    status varchar(32) not null,
    assignee_user_id bigint,
    action varchar(32),
    action_note varchar(1000),
    created_at timestamp not null,
    actioned_at timestamp,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_community_report_status_created on community_report(status, created_at);
create index idx_community_report_target on community_report(target_type, target_id);
