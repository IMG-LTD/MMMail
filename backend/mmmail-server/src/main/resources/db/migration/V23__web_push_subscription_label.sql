-- DESCRIPTION: Add a user-facing label to Web Push subscriptions for v2.1.2 settings management.
-- ROLLBACK: alter table web_push_subscription drop column label;

alter table web_push_subscription
    add column label varchar(64);
