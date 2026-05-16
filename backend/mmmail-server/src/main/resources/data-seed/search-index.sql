insert ignore into user_account (id, email, password_hash, display_name, role, status, token_version, created_at, updated_at, deleted)
values (9100000000001, 'seed@mmmail.local', '$2a$10$oUT4YtfDuu3St4IuNYAZ0eVv4WELZxr8jvvyZxL/vC2DF/5phviEi', 'Dev Seed User', 'USER', 1, 1, current_timestamp, current_timestamp, 0);

insert ignore into search_index (id, module_type, resource_id, org_id, owner_user_id, acl_user_ids, title, body, route_path, updated_at, created_at, deleted)
values (9100000004001, 'COMMUNITY', 'dev-seed-post-4', null, 9100000000001, '[9100000000001]', 'Search indexing sample', 'This item should appear in search seed data.', '/community/posts/dev-seed-post-4', current_timestamp, current_timestamp, 0);

insert ignore into search_reindex_job (id, module_type, status, processed, total, errors, error_message, created_at, updated_at, completed_at)
values ('dev-seed-reindex-001', 'ALL', 'QUEUED', 0, 0, 0, null, current_timestamp, current_timestamp, null);
