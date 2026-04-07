create table if not exists user_account (
    id bigint primary key,
    email varchar(254) not null,
    password_hash varchar(255) not null,
    display_name varchar(64) not null,
    role varchar(16) not null,
    status tinyint not null,
    token_version int not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_user_account_email on user_account(email);

create table if not exists mail_message (
    id bigint primary key,
    owner_id bigint not null,
    peer_id bigint,
    peer_email varchar(254),
    direction varchar(16) not null,
    folder_type varchar(16) not null,
    subject varchar(255),
    body_ciphertext text,
    body_e2ee_enabled tinyint not null default 0,
    body_e2ee_algorithm varchar(64),
    body_e2ee_fingerprints_json text,
    is_read tinyint not null,
    is_starred tinyint not null default 0,
    is_draft tinyint not null,
    labels_json text,
    idempotency_key varchar(128),
    sent_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_mail_owner_folder_sent on mail_message(owner_id, folder_type, sent_at);
create unique index uk_mail_idempotency on mail_message(owner_id, idempotency_key);
alter table mail_message add column sender_email varchar(254);
alter table mail_message add column if not exists delivery_targets_json text;
alter table mail_message add column if not exists body_e2ee_enabled tinyint not null default 0;
alter table mail_message add column if not exists body_e2ee_algorithm varchar(64);
alter table mail_message add column if not exists body_e2ee_fingerprints_json text;
create index idx_mail_owner_sender_sent on mail_message(owner_id, sender_email, sent_at);

create table if not exists mail_attachment (
    id bigint primary key,
    owner_id bigint not null,
    mail_id bigint not null,
    file_name varchar(255) not null,
    content_type varchar(255) not null,
    file_size bigint not null,
    e2ee_enabled tinyint not null default 0,
    e2ee_algorithm varchar(64),
    e2ee_fingerprints_json text,
    storage_path varchar(512) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

alter table mail_attachment add column if not exists e2ee_enabled tinyint not null default 0;
alter table mail_attachment add column if not exists e2ee_algorithm varchar(64);
alter table mail_attachment add column if not exists e2ee_fingerprints_json text;
create index idx_mail_attachment_owner_mail_created on mail_attachment(owner_id, mail_id, created_at);
create index idx_mail_attachment_storage_path on mail_attachment(storage_path);

create table if not exists mail_label (
    id bigint primary key,
    owner_id bigint not null,
    name varchar(32) not null,
    color varchar(7) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_mail_label_owner_name on mail_label(owner_id, name);

create table if not exists mail_folder (
    id bigint primary key,
    owner_id bigint not null,
    parent_id bigint,
    name varchar(64) not null,
    color varchar(7) not null,
    notifications_enabled tinyint not null default 1,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_mail_folder_owner_parent on mail_folder(owner_id, parent_id, updated_at);

create table if not exists mail_filter (
    id bigint primary key,
    owner_id bigint not null,
    name varchar(64) not null,
    sender_contains varchar(254),
    subject_contains varchar(255),
    keyword_contains varchar(255),
    target_folder varchar(16),
    labels_json text,
    mark_read tinyint not null default 0,
    enabled tinyint not null default 1,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_mail_filter_owner_name on mail_filter(owner_id, name);
create index idx_mail_filter_owner_enabled_created on mail_filter(owner_id, enabled, created_at);
alter table mail_filter add column if not exists target_custom_folder_id bigint;
create index idx_mail_filter_owner_custom_folder on mail_filter(owner_id, target_custom_folder_id);

alter table mail_message add column if not exists custom_folder_id bigint;
create index idx_mail_owner_custom_folder_sent on mail_message(owner_id, custom_folder_id, sent_at);

create table if not exists mail_external_secure_link (
    id bigint primary key,
    mail_id bigint not null,
    owner_id bigint not null,
    recipient_email varchar(254) not null,
    token varchar(64) not null,
    public_url varchar(512) not null,
    password_hint varchar(255),
    expires_at timestamp,
    revoked_at timestamp,
    last_accessed_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_mail_external_secure_link_mail on mail_external_secure_link(mail_id);
create unique index uk_mail_external_secure_link_token on mail_external_secure_link(token);
create index idx_mail_external_secure_link_owner on mail_external_secure_link(owner_id, revoked_at, expires_at);

create table if not exists contact_entry (
    id bigint primary key,
    owner_id bigint not null,
    display_name varchar(64) not null,
    email varchar(254) not null,
    note varchar(256),
    is_favorite tinyint not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_contact_entry_owner_email on contact_entry(owner_id, email);
create index idx_contact_entry_owner_favorite on contact_entry(owner_id, is_favorite, updated_at);
create index idx_contact_entry_owner_name on contact_entry(owner_id, display_name);

create table if not exists contact_group (
    id bigint primary key,
    owner_id bigint not null,
    name varchar(64) not null,
    description varchar(256),
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_contact_group_owner_name on contact_group(owner_id, name);
create index idx_contact_group_owner_updated on contact_group(owner_id, updated_at);

create table if not exists contact_group_member (
    id bigint primary key,
    owner_id bigint not null,
    group_id bigint not null,
    contact_id bigint not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_contact_group_member_group_contact on contact_group_member(group_id, contact_id);
create index idx_contact_group_member_group on contact_group_member(owner_id, group_id, updated_at);

create table if not exists blocked_sender (
    id bigint primary key,
    owner_id bigint not null,
    email varchar(254) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_blocked_sender_owner_email on blocked_sender(owner_id, email);
create index idx_blocked_sender_owner_created on blocked_sender(owner_id, created_at);

create table if not exists trusted_sender (
    id bigint primary key,
    owner_id bigint not null,
    email varchar(254) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_trusted_sender_owner_email on trusted_sender(owner_id, email);
create index idx_trusted_sender_owner_created on trusted_sender(owner_id, created_at);

create table if not exists blocked_domain (
    id bigint primary key,
    owner_id bigint not null,
    domain varchar(253) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_blocked_domain_owner_domain on blocked_domain(owner_id, domain);
create index idx_blocked_domain_owner_created on blocked_domain(owner_id, created_at);

create table if not exists trusted_domain (
    id bigint primary key,
    owner_id bigint not null,
    domain varchar(253) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_trusted_domain_owner_domain on trusted_domain(owner_id, domain);
create index idx_trusted_domain_owner_created on trusted_domain(owner_id, created_at);

create table if not exists search_preset (
    id bigint primary key,
    owner_id bigint not null,
    name varchar(64) not null,
    keyword varchar(512),
    folder varchar(16),
    unread tinyint,
    starred tinyint,
    from_at timestamp,
    to_at timestamp,
    label_name varchar(32),
    is_pinned tinyint not null default 0,
    pinned_at timestamp,
    usage_count int not null default 0,
    last_used_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

alter table search_preset add column is_pinned tinyint not null default 0;
alter table search_preset add column pinned_at timestamp;

create unique index uk_search_preset_owner_name on search_preset(owner_id, name);
create index idx_search_preset_owner_last_used on search_preset(owner_id, last_used_at, usage_count);
create index idx_search_preset_owner_pin on search_preset(owner_id, is_pinned, pinned_at);

create table if not exists search_history (
    id bigint primary key,
    owner_id bigint not null,
    keyword varchar(512) not null,
    usage_count int not null default 0,
    last_used_at timestamp not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_search_history_owner_keyword on search_history(owner_id, keyword);
create index idx_search_history_owner_last_used on search_history(owner_id, last_used_at, usage_count);

create table if not exists audit_event (
    id bigint primary key,
    org_id bigint,
    actor_id bigint not null,
    event_type varchar(64) not null,
    ip_address varchar(64),
    detail text,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

alter table audit_event add column org_id bigint;
alter table audit_event modify column actor_id bigint null;
create index idx_audit_actor_created on audit_event(actor_id, created_at);
create index idx_audit_org_created on audit_event(org_id, created_at);

create table if not exists user_session (
    id bigint primary key,
    owner_id bigint not null,
    refresh_token_hash varchar(128) not null,
    expires_at timestamp not null,
    revoked tinyint not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_user_session_refresh on user_session(refresh_token_hash);
create index idx_user_session_owner_expires on user_session(owner_id, expires_at);

create table if not exists user_preference (
    id bigint primary key,
    owner_id bigint not null,
    signature text,
    timezone varchar(64) not null,
    preferred_locale varchar(16) not null default 'en',
    mail_address_mode varchar(32) not null default 'PROTON_ADDRESS',
    auto_save_seconds int not null,
    undo_send_seconds int not null,
    drive_version_retention_count int not null default 50,
    drive_version_retention_days int not null default 365,
    authenticator_sync_enabled tinyint not null default 1,
    authenticator_encrypted_backup_enabled tinyint not null default 0,
    authenticator_pin_protection_enabled tinyint not null default 0,
    authenticator_pin_hash varchar(255),
    authenticator_lock_timeout_seconds int not null default 300,
    authenticator_last_synced_at timestamp,
    authenticator_last_backup_at timestamp,
    vpn_netshield_mode varchar(32) not null default 'OFF',
    vpn_kill_switch_enabled tinyint not null default 0,
    vpn_default_connection_mode varchar(32) not null default 'FASTEST',
    vpn_default_profile_id bigint,
    mail_e2ee_enabled tinyint not null default 0,
    mail_e2ee_key_fingerprint varchar(64),
    mail_e2ee_public_key_armored longtext,
    mail_e2ee_private_key_encrypted longtext,
    mail_e2ee_key_algorithm varchar(64),
    mail_e2ee_key_created_at timestamp,
    mail_e2ee_recovery_private_key_encrypted longtext,
    mail_e2ee_recovery_updated_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

alter table user_preference add column preferred_locale varchar(16) not null default 'en';
alter table user_preference add column if not exists mail_address_mode varchar(32) not null default 'PROTON_ADDRESS';
alter table user_preference add column drive_version_retention_count int not null default 50;
alter table user_preference add column drive_version_retention_days int not null default 365;
alter table user_preference add column if not exists authenticator_sync_enabled tinyint not null default 1;
alter table user_preference add column if not exists authenticator_encrypted_backup_enabled tinyint not null default 0;
alter table user_preference add column if not exists authenticator_pin_protection_enabled tinyint not null default 0;
alter table user_preference add column if not exists authenticator_pin_hash varchar(255);
alter table user_preference add column if not exists authenticator_lock_timeout_seconds int not null default 300;
alter table user_preference add column if not exists authenticator_last_synced_at timestamp;
alter table user_preference add column if not exists authenticator_last_backup_at timestamp;
alter table user_preference add column if not exists vpn_netshield_mode varchar(32) not null default 'OFF';
alter table user_preference add column if not exists vpn_kill_switch_enabled tinyint not null default 0;
alter table user_preference add column if not exists vpn_default_connection_mode varchar(32) not null default 'FASTEST';
alter table user_preference add column if not exists vpn_default_profile_id bigint;
alter table user_preference add column if not exists mail_e2ee_enabled tinyint not null default 0;
alter table user_preference add column if not exists mail_e2ee_key_fingerprint varchar(64);
alter table user_preference add column if not exists mail_e2ee_public_key_armored longtext;
alter table user_preference add column if not exists mail_e2ee_private_key_encrypted longtext;
alter table user_preference add column if not exists mail_e2ee_key_algorithm varchar(64);
alter table user_preference add column if not exists mail_e2ee_key_created_at timestamp;
alter table user_preference add column if not exists mail_e2ee_recovery_private_key_encrypted longtext;
alter table user_preference add column if not exists mail_e2ee_recovery_updated_at timestamp;

create unique index uk_user_preference_owner on user_preference(owner_id);

create table if not exists mail_easy_switch_session (
    id bigint primary key,
    owner_id bigint not null,
    provider varchar(32) not null,
    source_email varchar(254) not null,
    import_contacts tinyint not null default 0,
    merge_contact_duplicates tinyint not null default 1,
    import_calendar tinyint not null default 0,
    import_mail tinyint not null default 0,
    imported_mail_folder varchar(16) not null default 'ARCHIVE',
    status varchar(32) not null,
    contacts_created int not null default 0,
    contacts_updated int not null default 0,
    contacts_skipped int not null default 0,
    contacts_invalid int not null default 0,
    calendar_imported int not null default 0,
    calendar_invalid int not null default 0,
    mail_imported int not null default 0,
    mail_skipped int not null default 0,
    mail_invalid int not null default 0,
    error_message varchar(512),
    created_at timestamp not null,
    updated_at timestamp not null,
    completed_at timestamp,
    deleted tinyint not null default 0
);

create index idx_mail_easy_switch_owner_created on mail_easy_switch_session(owner_id, created_at);

create table if not exists suite_subscription (
    id bigint primary key,
    owner_id bigint not null,
    plan_code varchar(32) not null,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_suite_subscription_owner on suite_subscription(owner_id);
create index idx_suite_subscription_plan on suite_subscription(plan_code, updated_at);

create table if not exists suite_checkout_draft (
    id bigint primary key,
    owner_id bigint not null,
    offer_code varchar(32) not null,
    offer_name varchar(128) not null,
    quote_status varchar(32) not null,
    checkout_mode varchar(32) not null,
    currency_code varchar(8),
    billing_cycle varchar(16) not null,
    seat_count int not null,
    organization_name varchar(80),
    domain_name varchar(120),
    marketing_badge varchar(32),
    invoice_summary_json text,
    entitlement_summary_json text not null,
    onboarding_summary_json text not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_suite_checkout_draft_owner on suite_checkout_draft(owner_id);
create index idx_suite_checkout_draft_offer on suite_checkout_draft(offer_code, updated_at);

create table if not exists suite_billing_payment_method (
    id bigint primary key,
    owner_id bigint not null,
    method_type varchar(24) not null,
    display_label varchar(80) not null,
    brand varchar(24),
    last_four varchar(4),
    expires_at varchar(7),
    is_default tinyint not null default 0,
    status varchar(24) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_suite_billing_payment_method_owner on suite_billing_payment_method(owner_id, updated_at);
create index idx_suite_billing_payment_method_default on suite_billing_payment_method(owner_id, is_default, updated_at);

create table if not exists suite_billing_invoice (
    id bigint primary key,
    owner_id bigint not null,
    invoice_number varchar(40) not null,
    offer_code varchar(32) not null,
    offer_name varchar(128) not null,
    invoice_status varchar(24) not null,
    currency_code varchar(8) not null,
    total_cents bigint not null,
    billing_cycle varchar(16),
    seat_count int not null,
    invoice_summary_json text not null,
    issued_at timestamp not null,
    due_at timestamp,
    download_code varchar(48) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_suite_billing_invoice_number on suite_billing_invoice(invoice_number);
create index idx_suite_billing_invoice_owner on suite_billing_invoice(owner_id, issued_at);

create table if not exists suite_billing_subscription_state (
    id bigint primary key,
    owner_id bigint not null,
    billing_cycle varchar(16),
    seat_count int not null,
    currency_code varchar(8),
    auto_renew tinyint not null default 0,
    current_period_ends_at timestamp,
    default_payment_method_id bigint,
    pending_action_code varchar(40),
    pending_offer_code varchar(32),
    pending_offer_name varchar(128),
    pending_billing_cycle varchar(16),
    pending_seat_count int,
    pending_effective_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_suite_billing_subscription_state_owner on suite_billing_subscription_state(owner_id);

create table if not exists suite_governance_request (
    id bigint primary key,
    owner_id bigint not null,
    org_id bigint,
    request_id varchar(64) not null,
    template_code varchar(64) not null,
    template_name varchar(128) not null,
    status varchar(64) not null,
    reason varchar(300) not null,
    require_dual_review tinyint not null default 0,
    first_review_note varchar(300),
    first_reviewed_by_user_id bigint,
    first_reviewed_by_session_id bigint,
    second_reviewer_user_id bigint,
    review_note varchar(300),
    reviewed_by_user_id bigint,
    reviewed_by_session_id bigint,
    approval_note varchar(300),
    executed_by_user_id bigint,
    executed_by_session_id bigint,
    rollback_reason varchar(300),
    requested_at timestamp not null,
    review_due_at timestamp,
    first_reviewed_at timestamp,
    reviewed_at timestamp,
    approved_at timestamp,
    executed_at timestamp,
    rolled_back_at timestamp,
    action_codes_json text not null,
    rollback_action_codes_json text not null,
    execution_results_json text not null,
    rollback_results_json text not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

alter table suite_governance_request add column reviewed_by_user_id bigint;
alter table suite_governance_request add column reviewed_by_session_id bigint;
alter table suite_governance_request add column executed_by_user_id bigint;
alter table suite_governance_request add column executed_by_session_id bigint;
alter table suite_governance_request add column org_id bigint;
alter table suite_governance_request add column require_dual_review tinyint not null default 0;
alter table suite_governance_request add column first_review_note varchar(300);
alter table suite_governance_request add column first_reviewed_by_user_id bigint;
alter table suite_governance_request add column first_reviewed_by_session_id bigint;
alter table suite_governance_request add column second_reviewer_user_id bigint;
alter table suite_governance_request add column review_due_at timestamp;
alter table suite_governance_request add column first_reviewed_at timestamp;

create unique index uk_suite_governance_request_owner_request on suite_governance_request(owner_id, request_id);
create index idx_suite_governance_request_owner_status_requested on suite_governance_request(owner_id, status, requested_at);
create index idx_suite_governance_request_owner_updated on suite_governance_request(owner_id, updated_at);
create index idx_suite_governance_request_org_status_requested on suite_governance_request(org_id, status, requested_at);

create table if not exists calendar_event (
    id bigint primary key,
    owner_id bigint not null,
    title varchar(128) not null,
    description varchar(2000),
    location varchar(256),
    start_at timestamp not null,
    end_at timestamp not null,
    all_day tinyint not null default 0,
    timezone varchar(64) not null,
    reminder_minutes int,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_calendar_event_owner_start on calendar_event(owner_id, start_at);
create index idx_calendar_event_owner_updated on calendar_event(owner_id, updated_at);

create table if not exists calendar_event_attendee (
    id bigint primary key,
    owner_id bigint not null,
    event_id bigint not null,
    email varchar(254) not null,
    display_name varchar(64),
    response_status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

drop index uk_calendar_attendee_event_email on calendar_event_attendee;
create unique index uk_calendar_attendee_event_email on calendar_event_attendee(event_id, email);
create index idx_calendar_attendee_owner_event on calendar_event_attendee(owner_id, event_id);

create table if not exists calendar_event_share (
    id bigint primary key,
    owner_id bigint not null,
    event_id bigint not null,
    target_user_id bigint not null,
    target_email varchar(254) not null,
    permission varchar(16) not null,
    response_status varchar(32) not null,
    source varchar(24) not null default 'MANUAL',
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_calendar_share_event_target on calendar_event_share(event_id, target_user_id);
create index idx_calendar_share_target_status on calendar_event_share(target_user_id, response_status, updated_at);
create index idx_calendar_share_owner_event on calendar_event_share(owner_id, event_id, updated_at);
create index idx_calendar_share_owner_event_source on calendar_event_share(owner_id, event_id, source, updated_at);

create table if not exists org_workspace (
    id bigint primary key,
    owner_id bigint not null,
    name varchar(128) not null,
    slug varchar(128) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_org_workspace_slug on org_workspace(slug);
create index idx_org_workspace_owner on org_workspace(owner_id, updated_at);

create table if not exists org_member (
    id bigint primary key,
    org_id bigint not null,
    user_id bigint,
    user_email varchar(254) not null,
    role varchar(16) not null,
    status varchar(16) not null,
    invited_by bigint not null,
    joined_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_org_member_org_email on org_member(org_id, user_email);
create index idx_org_member_user_status on org_member(user_id, status, updated_at);
create index idx_org_member_org_status on org_member(org_id, status, updated_at);

create table if not exists org_policy (
    id bigint primary key,
    org_id bigint not null,
    policy_key varchar(64) not null,
    policy_value varchar(1024) not null,
    updated_by bigint not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_org_policy_org_key on org_policy(org_id, policy_key);
create index idx_org_policy_org_updated on org_policy(org_id, updated_at);

create table if not exists org_custom_domain (
    id bigint primary key,
    org_id bigint not null,
    domain varchar(255) not null,
    verification_token varchar(64) not null,
    status varchar(32) not null,
    is_default tinyint not null default 0,
    created_by bigint not null,
    verified_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_org_custom_domain_org_domain on org_custom_domain(org_id, domain);
create index idx_org_custom_domain_org_default on org_custom_domain(org_id, is_default, updated_at);

create table if not exists org_mail_identity (
    id bigint primary key,
    org_id bigint not null,
    member_id bigint not null,
    custom_domain_id bigint not null,
    local_part varchar(64) not null,
    email_address varchar(254) not null,
    display_name varchar(64),
    status varchar(16) not null,
    is_default tinyint not null default 0,
    created_by bigint,
    updated_by bigint,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_org_mail_identity_org_email on org_mail_identity(org_id, email_address);
create index idx_org_mail_identity_member_status on org_mail_identity(member_id, status, updated_at);
create index idx_org_mail_identity_org_default on org_mail_identity(org_id, is_default, updated_at);

create table if not exists org_product_access (
    id bigint primary key,
    org_id bigint not null,
    member_id bigint not null,
    product_key varchar(32) not null,
    access_state varchar(16) not null,
    updated_by bigint not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_org_product_access_member_product on org_product_access(member_id, product_key);
create index idx_org_product_access_org_member on org_product_access(org_id, member_id, updated_at);

create table if not exists org_team_space (
    id bigint primary key,
    org_id bigint not null,
    name varchar(128) not null,
    slug varchar(128) not null,
    description varchar(256),
    root_item_id bigint not null,
    storage_limit_mb int not null default 10240,
    created_by bigint not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_org_team_space_org_slug on org_team_space(org_id, slug);
create unique index uk_org_team_space_root_item on org_team_space(root_item_id);
create index idx_org_team_space_org_updated on org_team_space(org_id, updated_at);

create table if not exists org_team_space_member (
    id bigint primary key,
    org_id bigint not null,
    team_space_id bigint not null,
    user_id bigint not null,
    user_email varchar(255) not null,
    role varchar(32) not null,
    created_by bigint not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    deleted tinyint not null default 0
);
create unique index uk_org_team_space_member_space_user on org_team_space_member(team_space_id, user_id, deleted);
create index idx_org_team_space_member_org_space_role on org_team_space_member(org_id, team_space_id, role, updated_at);

create table if not exists drive_item (
    id bigint primary key,
    owner_id bigint not null,
    parent_id bigint,
    item_type varchar(16) not null,
    name varchar(128) not null,
    mime_type varchar(128),
    size_bytes bigint not null default 0,
    storage_path varchar(512),
    checksum varchar(128),
    e2ee_enabled tinyint not null default 0,
    e2ee_algorithm varchar(64),
    e2ee_fingerprints_json text,
    trashed_at timestamp,
    purge_after_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

alter table drive_item add column trashed_at timestamp;
alter table drive_item add column purge_after_at timestamp;

alter table drive_item add column team_space_id bigint;
alter table drive_item add column if not exists e2ee_enabled tinyint not null default 0;
alter table drive_item add column if not exists e2ee_algorithm varchar(64);
alter table drive_item add column if not exists e2ee_fingerprints_json text;

create unique index uk_drive_item_owner_parent_type_name on drive_item(owner_id, parent_id, item_type, name);
create index idx_drive_item_owner_parent on drive_item(owner_id, parent_id, updated_at);
create index idx_drive_item_owner_type on drive_item(owner_id, item_type, updated_at);
create index idx_drive_item_owner_deleted_trashed on drive_item(owner_id, deleted, trashed_at);

create index idx_drive_item_team_space_parent on drive_item(team_space_id, parent_id, updated_at);
create index idx_drive_item_team_space_type on drive_item(team_space_id, item_type, updated_at);

create table if not exists drive_file_version (
    id bigint primary key,
    owner_id bigint not null,
    item_id bigint not null,
    version_no int not null,
    mime_type varchar(128),
    size_bytes bigint not null default 0,
    storage_path varchar(512) not null,
    checksum varchar(128),
    e2ee_enabled tinyint not null default 0,
    e2ee_algorithm varchar(64),
    e2ee_fingerprints_json text,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

alter table drive_file_version add column if not exists e2ee_enabled tinyint not null default 0;
alter table drive_file_version add column if not exists e2ee_algorithm varchar(64);
alter table drive_file_version add column if not exists e2ee_fingerprints_json text;

create unique index uk_drive_file_version_item_version on drive_file_version(item_id, version_no);
create index idx_drive_file_version_item_created on drive_file_version(item_id, created_at);
create index idx_drive_file_version_owner_item on drive_file_version(owner_id, item_id, version_no);

create table if not exists drive_share_link (
    id bigint primary key,
    owner_id bigint not null,
    item_id bigint not null,
    token varchar(80) not null,
    permission varchar(16) not null,
    expires_at timestamp,
    password_hash varchar(255),
    readable_e2ee_enabled tinyint not null default 0,
    readable_e2ee_algorithm varchar(64),
    readable_e2ee_storage_path varchar(512),
    readable_e2ee_checksum varchar(128),
    status varchar(16) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_drive_share_link_token on drive_share_link(token);
create index idx_drive_share_link_owner_item on drive_share_link(owner_id, item_id, updated_at);

create table if not exists drive_saved_share (
    id bigint primary key,
    recipient_user_id bigint not null,
    share_id bigint not null,
    owner_id bigint not null,
    owner_email varchar(255) not null,
    owner_display_name varchar(128),
    item_id bigint not null,
    item_type varchar(16) not null,
    item_name varchar(255) not null,
    token varchar(80) not null,
    permission varchar(16) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_drive_saved_share_recipient_share on drive_saved_share(recipient_user_id, share_id, deleted);
create index idx_drive_saved_share_recipient_updated on drive_saved_share(recipient_user_id, updated_at);
create index idx_drive_saved_share_share on drive_saved_share(share_id, updated_at);

create table if not exists drive_collaborator_share (
    id bigint primary key,
    item_id bigint not null,
    owner_id bigint not null,
    collaborator_user_id bigint not null,
    permission varchar(16) not null,
    response_status varchar(24) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_drive_collaborator_share_owner_item on drive_collaborator_share(owner_id, item_id, updated_at);
create index idx_drive_collaborator_share_collaborator_status on drive_collaborator_share(collaborator_user_id, response_status, updated_at);
create unique index uk_drive_collaborator_share_item_collaborator on drive_collaborator_share(item_id, collaborator_user_id, deleted);

create table if not exists drive_share_access_log (
    id bigint primary key,
    owner_id bigint,
    share_id bigint,
    item_id bigint,
    token_hash varchar(128) not null,
    action varchar(24) not null,
    access_status varchar(32) not null,
    ip_address varchar(64),
    user_agent varchar(512),
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_drive_share_access_log_share_created on drive_share_access_log(share_id, created_at);
create index idx_drive_share_access_log_token_created on drive_share_access_log(token_hash, created_at);

create table if not exists docs_note (
    id bigint primary key,
    owner_id bigint not null,
    workspace_type varchar(32) not null default 'DOCS',
    title varchar(128) not null,
    content text,
    current_version int not null default 1,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_docs_note_owner_updated on docs_note(owner_id, updated_at);
create unique index uk_docs_note_owner_title on docs_note(owner_id, title, deleted);
alter table docs_note add column current_version int not null default 1;
alter table docs_note add column workspace_type varchar(32) not null default 'DOCS';
drop index uk_docs_note_owner_title on docs_note;
create unique index uk_docs_note_owner_title_workspace on docs_note(owner_id, workspace_type, title, deleted);

create table if not exists standard_note_profile (
    note_id bigint primary key,
    owner_id bigint not null,
    note_type varchar(32) not null,
    tags_json text,
    folder_id bigint null,
    pinned tinyint not null default 0,
    archived tinyint not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_standard_note_profile_owner_updated on standard_note_profile(owner_id, updated_at);
create index idx_standard_note_profile_owner_archived on standard_note_profile(owner_id, archived, updated_at);
create index idx_standard_note_profile_owner_folder on standard_note_profile(owner_id, folder_id, updated_at);

create table if not exists standard_note_folder (
    id bigint primary key,
    owner_id bigint not null,
    name varchar(64) not null,
    color varchar(7) not null,
    description varchar(160),
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_standard_note_folder_owner_name on standard_note_folder(owner_id, name, deleted);
create index idx_standard_note_folder_owner_updated on standard_note_folder(owner_id, updated_at);

create table if not exists docs_note_share (
    id bigint primary key,
    note_id bigint not null,
    owner_id bigint not null,
    collaborator_user_id bigint not null,
    permission varchar(16) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_docs_note_share_note on docs_note_share(note_id, updated_at);
create index idx_docs_note_share_collaborator on docs_note_share(collaborator_user_id, updated_at);
create unique index uk_docs_note_share_note_collaborator on docs_note_share(note_id, collaborator_user_id, deleted);

create table if not exists docs_note_comment (
    id bigint primary key,
    note_id bigint not null,
    author_user_id bigint not null,
    excerpt varchar(512),
    content varchar(2000) not null,
    resolved tinyint not null default 0,
    resolved_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_docs_note_comment_note on docs_note_comment(note_id, created_at);
create index idx_docs_note_comment_author on docs_note_comment(author_user_id, created_at);

create table if not exists docs_note_suggestion (
    id bigint primary key,
    note_id bigint not null,
    author_user_id bigint not null,
    selection_start int not null,
    selection_end int not null,
    original_text text not null,
    replacement_text text not null,
    base_version int not null,
    status varchar(16) not null default 'PENDING',
    resolved_by_user_id bigint,
    resolved_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_docs_note_suggestion_note on docs_note_suggestion(note_id, created_at);
create index idx_docs_note_suggestion_author on docs_note_suggestion(author_user_id, created_at);
create index idx_docs_note_suggestion_status on docs_note_suggestion(note_id, status, updated_at);

create table if not exists docs_note_presence (
    id bigint primary key,
    note_id bigint not null,
    user_id bigint not null,
    session_id bigint not null,
    active_mode varchar(16) not null,
    last_heartbeat_at timestamp not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_docs_note_presence_note on docs_note_presence(note_id, last_heartbeat_at);
create unique index uk_docs_note_presence_note_session on docs_note_presence(note_id, user_id, session_id, deleted);

create table if not exists pass_vault_item (
    id bigint primary key,
    owner_id bigint not null,
    org_id bigint,
    shared_vault_id bigint,
    scope_type varchar(16) not null default 'PERSONAL',
    item_type varchar(16) not null default 'LOGIN',
    monitor_excluded tinyint not null default 0,
    title varchar(128) not null,
    website varchar(255),
    username varchar(254),
    secret_ciphertext varchar(512),
    two_factor_issuer varchar(128),
    two_factor_account_name varchar(254),
    two_factor_secret_ciphertext varchar(512),
    two_factor_algorithm varchar(16),
    two_factor_digits int,
    two_factor_period_seconds int,
    note varchar(2000),
    favorite tinyint not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_pass_item_owner_updated on pass_vault_item(owner_id, updated_at);
create index idx_pass_item_owner_favorite on pass_vault_item(owner_id, favorite, updated_at);
create index idx_pass_item_owner_title on pass_vault_item(owner_id, title);
create index idx_pass_item_scope_org_updated on pass_vault_item(scope_type, org_id, updated_at);
create index idx_pass_item_vault_updated on pass_vault_item(shared_vault_id, updated_at);

create table if not exists pass_shared_vault (
    id bigint primary key,
    org_id bigint not null,
    name varchar(128) not null,
    description varchar(500),
    created_by bigint not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_pass_shared_vault_org_updated on pass_shared_vault(org_id, updated_at);

create table if not exists pass_shared_vault_member (
    id bigint primary key,
    org_id bigint not null,
    vault_id bigint not null,
    user_id bigint not null,
    user_email varchar(254) not null,
    role varchar(16) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_pass_shared_vault_member_user on pass_shared_vault_member(vault_id, user_id, deleted);
create index idx_pass_shared_vault_member_org_user on pass_shared_vault_member(org_id, user_id, updated_at);

create table if not exists pass_secure_link (
    id bigint primary key,
    org_id bigint not null,
    item_id bigint not null,
    shared_vault_id bigint not null,
    token varchar(64) not null,
    max_views int not null default 1,
    current_views int not null default 0,
    expires_at timestamp,
    revoked_at timestamp,
    created_by bigint not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_pass_secure_link_token on pass_secure_link(token);
create index idx_pass_secure_link_item on pass_secure_link(item_id, revoked_at, expires_at);

create table if not exists pass_item_share (
    id bigint primary key,
    org_id bigint not null,
    item_id bigint not null,
    shared_vault_id bigint not null,
    owner_id bigint not null,
    collaborator_user_id bigint not null,
    collaborator_email varchar(254) not null,
    created_by bigint not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_pass_item_share_item_user on pass_item_share(item_id, collaborator_user_id, deleted);
create index idx_pass_item_share_org_collaborator on pass_item_share(org_id, collaborator_user_id, updated_at);
create index idx_pass_item_share_item_updated on pass_item_share(item_id, updated_at);

alter table pass_vault_item add column if not exists org_id bigint null;
alter table pass_vault_item add column if not exists shared_vault_id bigint null;
alter table pass_vault_item add column if not exists scope_type varchar(16) not null default 'PERSONAL';
alter table pass_vault_item add column if not exists item_type varchar(16) not null default 'LOGIN';
alter table pass_vault_item add column if not exists monitor_excluded tinyint not null default 0;
alter table pass_vault_item add column if not exists two_factor_issuer varchar(128) null;
alter table pass_vault_item add column if not exists two_factor_account_name varchar(254) null;
alter table pass_vault_item add column if not exists two_factor_secret_ciphertext varchar(512) null;
alter table pass_vault_item add column if not exists two_factor_algorithm varchar(16) null;
alter table pass_vault_item add column if not exists two_factor_digits int null;
alter table pass_vault_item add column if not exists two_factor_period_seconds int null;
alter table pass_vault_item modify column secret_ciphertext varchar(512) null;
update pass_vault_item set scope_type = 'PERSONAL' where scope_type is null or scope_type = '';
update pass_vault_item set item_type = 'LOGIN' where item_type is null or item_type = '';
update pass_vault_item set monitor_excluded = 0 where monitor_excluded is null;

create table if not exists authenticator_entry (
    id bigint primary key,
    owner_id bigint not null,
    issuer varchar(128) not null,
    account_name varchar(254) not null,
    secret_ciphertext varchar(512) not null,
    algorithm varchar(16) not null,
    digits int not null default 6,
    period_seconds int not null default 30,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_auth_entry_owner_updated on authenticator_entry(owner_id, updated_at);
create index idx_auth_entry_owner_issuer on authenticator_entry(owner_id, issuer);
create index idx_auth_entry_owner_account on authenticator_entry(owner_id, account_name);

create table if not exists vpn_connection_session (
    id bigint primary key,
    owner_id bigint not null,
    server_id varchar(64) not null,
    server_country varchar(64) not null,
    server_city varchar(64) not null,
    server_tier varchar(32) not null,
    protocol varchar(32) not null,
    status varchar(32) not null,
    profile_id bigint,
    profile_name varchar(128),
    netshield_mode varchar(32) not null default 'OFF',
    kill_switch_enabled tinyint not null default 0,
    connection_source varchar(32) not null default 'MANUAL',
    connected_at timestamp not null,
    disconnected_at timestamp,
    duration_seconds bigint not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

alter table vpn_connection_session add column if not exists profile_id bigint;
alter table vpn_connection_session add column if not exists profile_name varchar(128);
alter table vpn_connection_session add column if not exists netshield_mode varchar(32) not null default 'OFF';
alter table vpn_connection_session add column if not exists kill_switch_enabled tinyint not null default 0;
alter table vpn_connection_session add column if not exists connection_source varchar(32) not null default 'MANUAL';

create index idx_vpn_session_owner_status on vpn_connection_session(owner_id, status, connected_at);
create index idx_vpn_session_owner_connected on vpn_connection_session(owner_id, connected_at);

create table if not exists vpn_connection_profile (
    id bigint primary key,
    owner_id bigint not null,
    name varchar(128) not null,
    protocol varchar(32) not null,
    routing_mode varchar(32) not null,
    target_server_id varchar(64),
    target_country varchar(64),
    secure_core_enabled tinyint not null default 0,
    netshield_mode varchar(32) not null default 'OFF',
    kill_switch_enabled tinyint not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_vpn_profile_owner_name on vpn_connection_profile(owner_id, name, deleted);
create index idx_vpn_profile_owner_updated on vpn_connection_profile(owner_id, updated_at);

create table if not exists meet_room_session (
    id bigint primary key,
    owner_id bigint not null,
    room_code varchar(32) not null,
    topic varchar(128) not null,
    access_level varchar(16) not null,
    max_participants int not null,
    host_user_id bigint not null,
    join_code varchar(32) not null,
    status varchar(16) not null,
    started_at timestamp not null,
    ended_at timestamp,
    duration_seconds bigint not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_meet_room_owner_status on meet_room_session(owner_id, status, started_at);
create index idx_meet_room_owner_started on meet_room_session(owner_id, started_at);
create index idx_meet_room_owner_room_code on meet_room_session(owner_id, room_code);

create table if not exists meet_room_participant (
    id bigint primary key,
    room_id bigint not null,
    owner_id bigint not null,
    user_id bigint not null,
    display_name varchar(64) not null,
    role varchar(16) not null,
    status varchar(16) not null,
    audio_enabled tinyint not null default 1,
    video_enabled tinyint not null default 1,
    screen_sharing tinyint not null default 0,
    joined_at timestamp not null,
    left_at timestamp,
    last_heartbeat_at timestamp not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_meet_participant_room_status on meet_room_participant(room_id, owner_id, status);
create index idx_meet_participant_room_user on meet_room_participant(room_id, owner_id, user_id, status);

alter table meet_room_participant add column audio_enabled tinyint not null default 1;
alter table meet_room_participant add column video_enabled tinyint not null default 1;
alter table meet_room_participant add column screen_sharing tinyint not null default 0;

create table if not exists meet_signal_event (
    id bigint primary key,
    room_id bigint not null,
    owner_id bigint not null,
    event_seq bigint not null,
    signal_type varchar(16) not null,
    from_participant_id bigint not null,
    to_participant_id bigint,
    payload text not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_meet_signal_room_seq on meet_signal_event(room_id, owner_id, event_seq);
create index idx_meet_signal_room_created on meet_signal_event(room_id, owner_id, created_at);

create table if not exists meet_access_enrollment (
    id bigint primary key,
    owner_id bigint not null,
    plan_code_snapshot varchar(32) not null,
    waitlist_requested tinyint not null default 0,
    access_granted tinyint not null default 0,
    sales_contact_requested tinyint not null default 0,
    company_name varchar(128),
    requested_seats int,
    request_note varchar(512),
    waitlist_requested_at timestamp,
    access_granted_at timestamp,
    sales_contact_requested_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_meet_access_owner on meet_access_enrollment(owner_id, deleted);
create index idx_meet_access_plan on meet_access_enrollment(plan_code_snapshot, access_granted, deleted);

create table if not exists meet_guest_request (
    id bigint primary key,
    room_id bigint not null,
    owner_id bigint not null,
    join_code_snapshot varchar(32) not null,
    request_token varchar(32) not null,
    guest_session_token varchar(32),
    participant_id bigint,
    display_name varchar(64) not null,
    audio_enabled tinyint not null default 1,
    video_enabled tinyint not null default 1,
    status varchar(16) not null,
    requested_at timestamp not null,
    approved_at timestamp,
    rejected_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_meet_guest_request_token on meet_guest_request(request_token, deleted);
create unique index uk_meet_guest_session_token on meet_guest_request(guest_session_token, deleted);
create index idx_meet_guest_room_status on meet_guest_request(room_id, owner_id, status, requested_at);

create table if not exists wallet_account (
    id bigint primary key,
    owner_id bigint not null,
    wallet_name varchar(64) not null,
    asset_symbol varchar(16) not null,
    address varchar(128) not null,
    balance_minor bigint not null default 0,
    address_type varchar(32) not null default 'NATIVE_SEGWIT',
    account_index int not null default 0,
    imported tinyint not null default 0,
    wallet_source_fingerprint varchar(64),
    wallet_passphrase_protected tinyint not null default 0,
    imported_at timestamp null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_wallet_account_owner_address on wallet_account(owner_id, address);
create index idx_wallet_account_owner_updated on wallet_account(owner_id, updated_at);

alter table wallet_account add column if not exists address_type varchar(32) not null default 'NATIVE_SEGWIT';
alter table wallet_account add column if not exists account_index int not null default 0;
alter table wallet_account add column if not exists imported tinyint not null default 0;
alter table wallet_account add column if not exists wallet_source_fingerprint varchar(64);
alter table wallet_account add column if not exists wallet_passphrase_protected tinyint not null default 0;
alter table wallet_account add column if not exists imported_at timestamp null;

create table if not exists wallet_transaction (
    id bigint primary key,
    owner_id bigint not null,
    account_id bigint not null,
    tx_type varchar(16) not null,
    counterparty_address varchar(128) not null,
    amount_minor bigint not null,
    asset_symbol varchar(16) not null,
    memo varchar(512),
    status varchar(16) not null,
    confirmations int not null default 0,
    signature_hash varchar(128),
    network_tx_hash varchar(128),
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_wallet_tx_owner_account_created on wallet_transaction(owner_id, account_id, created_at);
create index idx_wallet_tx_owner_created on wallet_transaction(owner_id, created_at);

alter table wallet_transaction add column confirmations int not null default 0;
alter table wallet_transaction add column signature_hash varchar(128);
alter table wallet_transaction add column network_tx_hash varchar(128);

create table if not exists wallet_account_profile (
    id bigint primary key,
    account_id bigint not null,
    owner_id bigint not null,
    bitcoin_via_email_enabled tinyint not null default 0,
    alias_email varchar(254),
    balance_masked tinyint not null default 0,
    address_privacy_enabled tinyint not null default 1,
    address_pool_size int not null default 3,
    recovery_phrase varchar(512) not null,
    recovery_fingerprint varchar(64) not null,
    passphrase_hint varchar(128),
    last_recovery_viewed_at timestamp null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_wallet_account_profile_account on wallet_account_profile(account_id, deleted);

create table if not exists wallet_receive_address (
    id bigint primary key,
    account_id bigint not null,
    owner_id bigint not null,
    address varchar(128) not null,
    label varchar(64) not null,
    source_type varchar(16) not null,
    address_kind varchar(16) not null default 'RECEIVE',
    address_index int not null default 0,
    address_status varchar(16) not null default 'UNUSED',
    value_minor bigint not null default 0,
    reserved_for varchar(64),
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_wallet_receive_address_account_value on wallet_receive_address(account_id, address, deleted);
create unique index uk_wallet_receive_address_kind_index on wallet_receive_address(account_id, address_kind, address_index, deleted);
create index idx_wallet_receive_address_owner_account on wallet_receive_address(owner_id, account_id, updated_at);
create index idx_wallet_receive_address_owner_kind on wallet_receive_address(owner_id, account_id, address_kind, address_index);

alter table wallet_receive_address add column if not exists address_kind varchar(16) not null default 'RECEIVE';
alter table wallet_receive_address add column if not exists address_index int not null default 0;
alter table wallet_receive_address add column if not exists address_status varchar(16) not null default 'UNUSED';
alter table wallet_receive_address add column if not exists value_minor bigint not null default 0;
alter table wallet_receive_address add column if not exists reserved_for varchar(64);

create table if not exists wallet_email_transfer (
    id bigint primary key,
    transaction_id bigint not null,
    account_id bigint not null,
    owner_id bigint not null,
    recipient_email varchar(254) not null,
    delivery_message varchar(256),
    claim_code varchar(64) not null,
    status varchar(32) not null,
    invite_required tinyint not null default 0,
    amount_minor bigint not null,
    asset_symbol varchar(16) not null,
    claimed_at timestamp null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_wallet_email_transfer_transaction on wallet_email_transfer(transaction_id, deleted);
create index idx_wallet_email_transfer_owner_account on wallet_email_transfer(owner_id, account_id, updated_at);

create table if not exists lumo_project (
    id bigint primary key,
    owner_id bigint not null,
    name varchar(64) not null,
    description varchar(256),
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_lumo_project_owner_name on lumo_project(owner_id, name);
create index idx_lumo_project_owner_updated on lumo_project(owner_id, updated_at);

create table if not exists lumo_conversation (
    id bigint primary key,
    owner_id bigint not null,
    project_id bigint,
    title varchar(128) not null,
    pinned tinyint not null default 0,
    model_code varchar(32) not null default 'LUMO-BASE',
    archived tinyint not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_lumo_conversation_owner_updated on lumo_conversation(owner_id, updated_at);

alter table lumo_conversation add column model_code varchar(32) not null default 'LUMO-BASE';
alter table lumo_conversation add column archived tinyint not null default 0;
alter table lumo_conversation add column project_id bigint;
create index idx_lumo_conversation_owner_project_updated on lumo_conversation(owner_id, project_id, updated_at);

create table if not exists lumo_message (
    id bigint primary key,
    owner_id bigint not null,
    conversation_id bigint not null,
    role varchar(16) not null,
    content text not null,
    capability_payload_json text,
    token_count int not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_lumo_message_owner_conversation_created on lumo_message(owner_id, conversation_id, created_at);

create table if not exists lumo_project_knowledge (
    id bigint primary key,
    owner_id bigint not null,
    project_id bigint not null,
    title varchar(128) not null,
    content text not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_lumo_knowledge_owner_project_updated on lumo_project_knowledge(owner_id, project_id, updated_at);

create table if not exists meet_quality_snapshot (
    id bigint primary key,
    room_id bigint not null,
    owner_id bigint not null,
    participant_id bigint not null,
    jitter_ms int not null,
    packet_loss_percent int not null,
    round_trip_ms int not null,
    quality_score int not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_meet_quality_room_created on meet_quality_snapshot(room_id, owner_id, created_at);

create table if not exists suite_notification_state (
    id bigint primary key,
    owner_id bigint not null,
    notification_id varchar(64) not null,
    first_seen_at timestamp not null,
    last_seen_at timestamp not null,
    read_at timestamp,
    workflow_status varchar(16) not null default 'ACTIVE',
    snoozed_until timestamp,
    assigned_to_user_id bigint,
    assigned_to_display_name varchar(64),
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

alter table suite_notification_state add column workflow_status varchar(16) not null default 'ACTIVE';
alter table suite_notification_state add column snoozed_until timestamp;
alter table suite_notification_state add column assigned_to_user_id bigint;
alter table suite_notification_state add column assigned_to_display_name varchar(64);

create unique index uk_suite_notification_state_owner_notification
    on suite_notification_state(owner_id, notification_id);
create index idx_suite_notification_state_owner_last_seen
    on suite_notification_state(owner_id, last_seen_at);
create index idx_suite_notification_state_owner_workflow
    on suite_notification_state(owner_id, workflow_status, snoozed_until);

create table if not exists web_push_subscription (
    id bigint primary key,
    owner_id bigint not null,
    endpoint_hash varchar(64) not null,
    endpoint varchar(1024) not null,
    p256dh_key varchar(255) not null,
    auth_key varchar(255) not null,
    content_encoding varchar(32) not null,
    user_agent varchar(255),
    last_success_at timestamp,
    last_failure_at timestamp,
    last_error_message varchar(255),
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_web_push_subscription_owner_endpoint_hash
    on web_push_subscription(owner_id, endpoint_hash);
create index idx_web_push_subscription_owner_updated
    on web_push_subscription(owner_id, updated_at);

create table if not exists suite_notification_operation_log (
    id bigint primary key,
    owner_id bigint not null,
    operation_id varchar(64) not null,
    notification_id varchar(64) not null,
    previous_workflow_status varchar(16) not null default 'ACTIVE',
    previous_snoozed_until timestamp,
    previous_assigned_to_user_id bigint,
    previous_assigned_to_display_name varchar(64),
    undone tinyint not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

alter table suite_notification_operation_log add column previous_workflow_status varchar(16) not null default 'ACTIVE';
alter table suite_notification_operation_log add column previous_snoozed_until timestamp;
alter table suite_notification_operation_log add column previous_assigned_to_user_id bigint;
alter table suite_notification_operation_log add column previous_assigned_to_display_name varchar(64);
alter table suite_notification_operation_log add column undone tinyint not null default 0;

create unique index uk_suite_notification_op_log_owner_operation_notification
    on suite_notification_operation_log(owner_id, operation_id, notification_id);
create index idx_suite_notification_op_log_owner_operation
    on suite_notification_operation_log(owner_id, operation_id, created_at);
create index idx_suite_notification_op_log_owner_created
    on suite_notification_operation_log(owner_id, created_at);
create index idx_suite_notification_op_log_owner_operation_undone
    on suite_notification_operation_log(owner_id, operation_id, undone);

create table if not exists pass_mail_alias (
    id bigint primary key,
    owner_id bigint not null,
    alias_email varchar(254) not null,
    title varchar(128) not null,
    note varchar(2000) null,
    forward_to_email varchar(254) not null,
    status varchar(16) not null,
    created_at datetime not null,
    updated_at datetime not null,
    deleted tinyint not null default 0
);
create unique index uk_pass_mail_alias_email on pass_mail_alias(alias_email);
create index idx_pass_mail_alias_owner_status on pass_mail_alias(owner_id, status, updated_at);

create table if not exists pass_mailbox (
    id bigint primary key,
    owner_id bigint not null,
    mailbox_user_id bigint not null,
    mailbox_email varchar(254) not null,
    status varchar(16) not null,
    verification_code varchar(32) null,
    verification_sent_at datetime not null,
    verified_at datetime null,
    is_default tinyint not null default 0,
    created_at datetime not null,
    updated_at datetime not null,
    deleted tinyint not null default 0
);
create unique index uk_pass_mailbox_owner_email on pass_mailbox(owner_id, mailbox_email);
create index idx_pass_mailbox_owner_status on pass_mailbox(owner_id, status, updated_at);
create index idx_pass_mailbox_owner_default on pass_mailbox(owner_id, is_default, updated_at);


create table if not exists pass_alias_mailbox_route (
    id bigint primary key,
    alias_id bigint not null,
    owner_id bigint not null,
    mailbox_id bigint not null,
    mailbox_email varchar(254) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create unique index uk_pass_alias_route_alias_mailbox on pass_alias_mailbox_route(alias_id, mailbox_id, deleted);
create index idx_pass_alias_route_owner_alias on pass_alias_mailbox_route(owner_id, alias_id, updated_at);
create index idx_pass_alias_route_owner_mailbox on pass_alias_mailbox_route(owner_id, mailbox_email, updated_at);

create table if not exists pass_alias_contact (
    id bigint primary key,
    alias_id bigint not null,
    owner_id bigint not null,
    target_user_id bigint not null,
    target_email varchar(254) not null,
    display_name varchar(128) not null,
    note varchar(2000) null,
    reverse_alias_email varchar(254) not null,
    created_at datetime not null,
    updated_at datetime not null,
    deleted tinyint not null default 0
);
create unique index uk_pass_alias_contact_reverse on pass_alias_contact(reverse_alias_email);
create unique index uk_pass_alias_contact_alias_target on pass_alias_contact(alias_id, target_email);
create index idx_pass_alias_contact_owner_alias on pass_alias_contact(owner_id, alias_id, updated_at);

create table if not exists sheets_workbook (
    id bigint primary key,
    owner_id bigint not null,
    title varchar(255) not null,
    row_count int not null,
    col_count int not null,
    grid_json text not null,
    current_version int not null,
    last_opened_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_sheets_workbook_owner_updated on sheets_workbook(owner_id, updated_at);
create index idx_sheets_workbook_owner_last_opened on sheets_workbook(owner_id, last_opened_at);
alter table sheets_workbook add column sheets_json text;
alter table sheets_workbook add column active_sheet_id varchar(64);

create table if not exists sheets_workbook_share (
    id bigint primary key,
    workbook_id bigint not null,
    owner_id bigint not null,
    collaborator_user_id bigint not null,
    permission varchar(32) not null,
    response_status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_sheets_workbook_share_owner_workbook on sheets_workbook_share(owner_id, workbook_id, updated_at);
create index idx_sheets_workbook_share_collaborator_status on sheets_workbook_share(collaborator_user_id, response_status, updated_at);

create table if not exists sheets_workbook_version (
    id bigint primary key,
    workbook_id bigint not null,
    version_no int not null,
    title varchar(255) not null,
    row_count int not null,
    col_count int not null,
    grid_json text not null,
    sheets_json text,
    active_sheet_id varchar(64),
    created_by_user_id bigint not null,
    source_event varchar(64) not null,
    created_at timestamp not null,
    deleted tinyint not null default 0
);

create index idx_sheets_workbook_version_workbook_created on sheets_workbook_version(workbook_id, created_at);
create index idx_sheets_workbook_version_workbook_version on sheets_workbook_version(workbook_id, version_no);
