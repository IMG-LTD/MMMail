-- DESCRIPTION: Initialize v2.2 Free / Pro / Business edition core.
-- ROLLBACK: delete inserted feature_flag rows, drop idx_org_workspace_edition, drop org_workspace.edition, and restore system_release_metadata.schema_version to 35 before rerunning later migrations.

alter table org_workspace add column edition varchar(16) not null default 'FREE';

create index idx_org_workspace_edition on org_workspace(edition, updated_at);

insert into feature_flag (flag_key, enabled, description)
values
    ('edition.free', 1, 'MMMail Free edition baseline'),
    ('edition.pro', 0, 'MMMail Pro edition contract gate'),
    ('edition.business', 0, 'MMMail Business edition contract gate'),
    ('feature.license.management', 0, 'License management requires Pro or Business'),
    ('feature.billing.admin', 0, 'Billing admin requires Pro or Business'),
    ('feature.oidc.sso', 0, 'OIDC SSO requires Business'),
    ('feature.audit.export', 0, 'Audit export requires Business'),
    ('feature.dsr.requests', 0, 'DSR requests require Business')
on duplicate key update
    enabled = values(enabled),
    description = values(description),
    updated_at = current_timestamp;

update system_release_metadata
set edition = 'FREE',
    schema_version = '36',
    updated_at = current_timestamp
where id = 1;
