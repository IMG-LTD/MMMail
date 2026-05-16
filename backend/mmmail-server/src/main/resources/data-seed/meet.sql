insert ignore into user_account (id, email, password_hash, display_name, role, status, token_version, created_at, updated_at, deleted)
values (9100000000001, 'seed@mmmail.local', '$2a$10$oUT4YtfDuu3St4IuNYAZ0eVv4WELZxr8jvvyZxL/vC2DF/5phviEi', 'Dev Seed User', 'USER', 1, 1, current_timestamp, current_timestamp, 0);

insert ignore into meet_room_session (id, owner_id, room_code, topic, access_level, max_participants, host_user_id, join_code, status, started_at, ended_at, duration_seconds, created_at, updated_at, deleted)
values (9100000002001, 9100000000001, 'DEV-SEED-ROOM', 'Dev seed product sync', 'TEAM', 12, 9100000000001, 'DEVJOIN42', 'ACTIVE', timestampadd(minute, -30, current_timestamp), null, 0, current_timestamp, current_timestamp, 0);

insert ignore into meet_room_participant (id, room_id, owner_id, user_id, display_name, role, status, audio_enabled, video_enabled, screen_sharing, joined_at, left_at, last_heartbeat_at, created_at, updated_at, deleted)
values (9100000002101, 9100000002001, 9100000000001, 9100000000001, 'Dev Seed User', 'HOST', 'JOINED', 1, 1, 0, timestampadd(minute, -30, current_timestamp), null, current_timestamp, current_timestamp, current_timestamp, 0);
