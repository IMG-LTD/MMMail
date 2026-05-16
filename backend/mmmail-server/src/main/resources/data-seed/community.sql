insert ignore into user_account (id, email, password_hash, display_name, role, status, token_version, created_at, updated_at, deleted)
values (9100000000001, 'seed@mmmail.local', '$2a$10$oUT4YtfDuu3St4IuNYAZ0eVv4WELZxr8jvvyZxL/vC2DF/5phviEi', 'Dev Seed User', 'USER', 1, 1, current_timestamp, current_timestamp, 0);

insert ignore into community_topic (id, slug, title, description, sort_order, created_at, updated_at, deleted)
values ('dev-seed-topic', 'dev-seed', 'Dev Seed', 'Sample community content for local development.', 10, current_timestamp, current_timestamp, 0);

insert ignore into community_post (id, author_user_id, org_id, topic_id, title, body_md, body_html, tags_json, like_count, comment_count, view_count, pinned, locked, status, created_at, updated_at, deleted_at, deleted)
values
    ('dev-seed-post-1', 9100000000001, null, 'dev-seed-topic', 'Welcome to the local community', 'Use this seeded post to verify the community feed.', '<p>Use this seeded post to verify the community feed.</p>', '["onboarding","seed"]', 2, 1, 12, 1, 0, 'PUBLISHED', timestampadd(day, -5, current_timestamp), current_timestamp, null, 0),
    ('dev-seed-post-2', 9100000000001, null, 'dev-seed-topic', 'Workflow feedback thread', 'Collect quick workflow feedback here.', '<p>Collect quick workflow feedback here.</p>', '["feedback"]', 1, 1, 8, 0, 0, 'PUBLISHED', timestampadd(day, -4, current_timestamp), current_timestamp, null, 0),
    ('dev-seed-post-3', 9100000000001, null, 'dev-seed-topic', 'Release checklist notes', 'Share release checklist notes for testing.', '<p>Share release checklist notes for testing.</p>', '["release","qa"]', 0, 1, 5, 0, 0, 'PUBLISHED', timestampadd(day, -3, current_timestamp), current_timestamp, null, 0),
    ('dev-seed-post-4', 9100000000001, null, 'dev-seed-topic', 'Search indexing sample', 'This item should appear in search seed data.', '<p>This item should appear in search seed data.</p>', '["search"]', 0, 1, 6, 0, 0, 'PUBLISHED', timestampadd(day, -2, current_timestamp), current_timestamp, null, 0),
    ('dev-seed-post-5', 9100000000001, null, 'dev-seed-topic', 'Support handoff notes', 'Use this item for support handoff review.', '<p>Use this item for support handoff review.</p>', '["support"]', 3, 1, 15, 0, 0, 'PUBLISHED', timestampadd(day, -1, current_timestamp), current_timestamp, null, 0);

insert ignore into community_comment (id, post_id, parent_comment_id, author_user_id, body_md, body_html, status, created_at, updated_at, deleted_at, deleted)
values
    ('dev-seed-comment-1', 'dev-seed-post-1', null, 9100000000001, 'First seeded comment.', '<p>First seeded comment.</p>', 'PUBLISHED', timestampadd(day, -5, current_timestamp), current_timestamp, null, 0),
    ('dev-seed-comment-2', 'dev-seed-post-2', null, 9100000000001, 'Feedback sample comment.', '<p>Feedback sample comment.</p>', 'PUBLISHED', timestampadd(day, -4, current_timestamp), current_timestamp, null, 0),
    ('dev-seed-comment-3', 'dev-seed-post-3', null, 9100000000001, 'Checklist sample comment.', '<p>Checklist sample comment.</p>', 'PUBLISHED', timestampadd(day, -3, current_timestamp), current_timestamp, null, 0),
    ('dev-seed-comment-4', 'dev-seed-post-4', null, 9100000000001, 'Search sample comment.', '<p>Search sample comment.</p>', 'PUBLISHED', timestampadd(day, -2, current_timestamp), current_timestamp, null, 0),
    ('dev-seed-comment-5', 'dev-seed-post-5', null, 9100000000001, 'Support sample comment.', '<p>Support sample comment.</p>', 'PUBLISHED', timestampadd(day, -1, current_timestamp), current_timestamp, null, 0);
