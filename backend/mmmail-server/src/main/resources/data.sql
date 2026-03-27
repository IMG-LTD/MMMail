insert ignore into user_account (id, email, password_hash, display_name, role, status, token_version, created_at, updated_at, deleted)
values (1, 'admin@mmmail.local', '$2a$10$oUT4YtfDuu3St4IuNYAZ0eVv4WELZxr8jvvyZxL/vC2DF/5phviEi', 'Admin', 'ADMIN', 1, 1, current_timestamp, current_timestamp, 0);

insert ignore into user_account (id, email, password_hash, display_name, role, status, token_version, created_at, updated_at, deleted)
values (2, 'demo@mmmail.local', '$2a$10$oUT4YtfDuu3St4IuNYAZ0eVv4WELZxr8jvvyZxL/vC2DF/5phviEi', 'Demo User', 'USER', 1, 1, current_timestamp, current_timestamp, 0);

insert ignore into user_preference (id, owner_id, signature, timezone, preferred_locale, mail_address_mode, auto_save_seconds, undo_send_seconds, created_at, updated_at, deleted)
values (101, 1, 'Best regards,\nAdmin', 'UTC', 'en', 'PROTON_ADDRESS', 15, 10, current_timestamp, current_timestamp, 0);

insert ignore into user_preference (id, owner_id, signature, timezone, preferred_locale, mail_address_mode, auto_save_seconds, undo_send_seconds, created_at, updated_at, deleted)
values (102, 2, 'Thanks,\nDemo User', 'Asia/Shanghai', 'zh-CN', 'PROTON_ADDRESS', 15, 10, current_timestamp, current_timestamp, 0);
