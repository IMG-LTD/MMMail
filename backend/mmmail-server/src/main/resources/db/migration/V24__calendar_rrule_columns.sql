-- DESCRIPTION: Add v2.1.2 calendar recurrence fields and owner/rrule lookup index.
-- ROLLBACK: drop index idx_calendar_event_owner_rrule on calendar_event; alter table calendar_event drop column recurrence_exdates_json; alter table calendar_event drop column recurrence_rdates_json; alter table calendar_event drop column recurrence_until; alter table calendar_event drop column rrule; alter table calendar_event drop column series_id;

alter table calendar_event
    add column series_id bigint;

alter table calendar_event
    add column rrule varchar(512);

alter table calendar_event
    add column recurrence_until timestamp;

alter table calendar_event
    add column recurrence_rdates_json text;

alter table calendar_event
    add column recurrence_exdates_json text;

create index idx_calendar_event_owner_rrule
    on calendar_event(owner_id, rrule, recurrence_until);
