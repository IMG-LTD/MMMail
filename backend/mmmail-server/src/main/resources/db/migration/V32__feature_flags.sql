-- DESCRIPTION: Add v2.1.2 feature flag registry used by SettingsController and auth responses.

create table if not exists feature_flag (
    flag_key varchar(128) primary key,
    enabled tinyint not null default 0,
    description varchar(255),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

insert into feature_flag (flag_key, enabled, description)
values
    ('feat.community.enabled', 1, 'Enable v2.1.2 community module'),
    ('feat.wallet.enabled', 0, 'Enable v2.1.2 wallet module'),
    ('feat.vpn.enabled', 0, 'Enable v2.1.2 vpn module'),
    ('feat.meet.enabled', 0, 'Enable v2.1.2 meet module'),
    ('feat.simplelogin.enabled', 0, 'Enable v2.1.2 SimpleLogin integration'),
    ('feat.notes.enabled', 0, 'Enable v2.1.2 Standard Notes integration')
on duplicate key update
    enabled = values(enabled),
    description = values(description),
    updated_at = current_timestamp;

-- ROLLBACK:
-- delete from feature_flag where flag_key in (
--   'feat.community.enabled',
--   'feat.wallet.enabled',
--   'feat.vpn.enabled',
--   'feat.meet.enabled',
--   'feat.simplelogin.enabled',
--   'feat.notes.enabled'
-- );
-- drop table feature_flag;
