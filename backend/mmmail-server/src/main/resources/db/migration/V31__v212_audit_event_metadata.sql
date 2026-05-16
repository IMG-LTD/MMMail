-- DESCRIPTION: Add v2.1.2 audit target and severity metadata for registered audit events.
-- ROLLBACK: drop index idx_audit_event_type_severity_created; alter table audit_event drop column severity; alter table audit_event drop column target_id; alter table audit_event drop column target_type;

alter table audit_event
    add column target_type varchar(64);

alter table audit_event
    add column target_id varchar(128);

alter table audit_event
    add column severity varchar(16) not null default 'low';

create index idx_audit_event_type_severity_created
    on audit_event(event_type, severity, created_at);
