-- DESCRIPTION: Add v2.2 Business OIDC SSO configuration and short-lived authorization state.
-- ROLLBACK: drop index idx_oidc_auth_state_org_expires on oidc_auth_state; drop index uk_oidc_auth_state_state on oidc_auth_state; drop table oidc_auth_state; drop index uk_org_oidc_config_org on org_oidc_config; drop table org_oidc_config; restore system_release_metadata.schema_version to 38 before rerunning later migrations.

create table if not exists org_oidc_config (
    id bigint primary key auto_increment,
    org_id bigint not null,
    enabled tinyint not null default 0,
    issuer_uri varchar(512) not null,
    client_id varchar(255) not null,
    client_secret_ref varchar(255) not null default '',
    callback_uri varchar(512) not null,
    scopes_json text not null,
    allowed_post_login_redirect_uris_json text not null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp
);

create unique index uk_org_oidc_config_org on org_oidc_config(org_id);

create table if not exists oidc_auth_state (
    id bigint primary key auto_increment,
    state varchar(128) not null,
    org_id bigint not null,
    nonce varchar(128) not null,
    code_verifier varchar(256) not null,
    callback_uri varchar(512) not null,
    post_login_redirect_uri varchar(512) not null,
    expires_at datetime not null,
    consumed_at datetime null,
    created_at datetime not null default current_timestamp
);

create unique index uk_oidc_auth_state_state on oidc_auth_state(state);
create index idx_oidc_auth_state_org_expires on oidc_auth_state(org_id, expires_at, consumed_at);

update system_release_metadata
set schema_version = '39',
    updated_at = current_timestamp
where id = 1;
