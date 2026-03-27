create table if not exists migration_upgrade_probe (
    id bigint primary key,
    applied_at timestamp not null
);

insert into migration_upgrade_probe (id, applied_at)
values (1, current_timestamp)
on duplicate key update applied_at = values(applied_at);
