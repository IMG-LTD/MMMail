-- DESCRIPTION: Persist v2.1.2 collaboration task board columns and stable card positions.
-- ROLLBACK: drop index idx_v21_collab_task_project_board_position; alter table v21_collaboration_task drop column position; alter table v21_collaboration_task drop column board_column;

alter table v21_collaboration_task
    add column board_column varchar(32) not null default 'OPEN';

alter table v21_collaboration_task
    add column position varchar(64) not null default '0|i001000:';

update v21_collaboration_task
set board_column = status
where board_column = 'OPEN'
  and status is not null
  and status <> 'OPEN';

update v21_collaboration_task
set position = (
    select ranked.initial_position
    from (
        select id,
               concat('0|i', lpad(cast(row_number() over (
                   partition by project_id, board_column
                   order by created_at, id
               ) * 1000 as char), 6, '0'), ':') as initial_position
        from v21_collaboration_task
        where deleted = 0
    ) ranked
    where ranked.id = v21_collaboration_task.id
)
where deleted = 0;

create index idx_v21_collab_task_project_board_position
    on v21_collaboration_task(project_id, board_column, position, id);

update system_release_metadata
set schema_version = '22',
    updated_at = current_timestamp
where id = 1;
