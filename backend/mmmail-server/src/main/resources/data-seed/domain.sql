insert ignore into user_account (id, email, password_hash, display_name, role, status, token_version, created_at, updated_at, deleted)
values (9100000000001, 'seed@mmmail.local', '$2a$10$oUT4YtfDuu3St4IuNYAZ0eVv4WELZxr8jvvyZxL/vC2DF/5phviEi', 'Dev Seed User', 'USER', 1, 1, current_timestamp, current_timestamp, 0);

insert ignore into org_workspace (id, owner_id, name, slug, created_at, updated_at, deleted)
values (9100000005001, 9100000000001, 'Dev Seed Workspace', 'dev-seed-workspace', current_timestamp, current_timestamp, 0);

insert ignore into org_member (id, org_id, user_id, user_email, role, status, invited_by, joined_at, created_at, updated_at, deleted)
values (9100000005101, 9100000005001, 9100000000001, 'seed@mmmail.local', 'OWNER', 'ACTIVE', 9100000000001, current_timestamp, current_timestamp, current_timestamp, 0);

-- expected DNS: TXT _mmmail.dev-seed.example.test = mmmail-verify-dev-seed-token
insert ignore into org_custom_domain (id, org_id, domain, verification_token, status, is_default, created_by, verified_at, created_at, updated_at, deleted)
values (9100000005201, 9100000005001, 'dev-seed.example.test', 'mmmail-verify-dev-seed-token', 'PENDING', 0, 9100000000001, null, current_timestamp, current_timestamp, 0);
