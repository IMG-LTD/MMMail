package com.mmmail.server.compliance;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StaticDataInventoryCatalog implements DataInventoryCatalog {

    private static final List<DataInventoryEntry> ENTRIES = List.of(
            anonymize("user_account", "id", "email", "display_name"),
            softDelete("user_preference", "user_id"),
            softDelete("user_session", "user_id"),
            softDelete("mail_message", "owner_id"),
            softDelete("mail_attachment", "owner_id"),
            softDelete("mail_label", "owner_id"),
            softDelete("mail_folder", "owner_id"),
            softDelete("mail_external_account", "owner_id"),
            softDelete("mail_filter", "owner_id"),
            softDelete("contact_entry", "owner_id"),
            softDelete("contact_group", "owner_id"),
            softDelete("contact_group_member", "owner_id"),
            softDelete("calendar_event", "owner_id"),
            softDelete("calendar_event_share", "owner_id"),
            softDelete("drive_item", "owner_id"),
            softDelete("drive_file_version", "owner_id"),
            softDelete("pass_vault_item", "owner_id"),
            softDelete("pass_mail_alias", "owner_id"),
            softDelete("pass_mailbox", "owner_id"),
            softDelete("docs_note", "owner_id"),
            softDelete("sheets_workbook", "owner_id"),
            softDelete("standard_note_profile", "owner_id"),
            softDelete("standard_note_folder", "owner_id"),
            softDelete("authenticator_entry", "owner_id"),
            softDelete("vpn_connection_profile", "owner_id"),
            softDelete("wallet_account", "owner_id"),
            softDelete("search_history", "owner_id"),
            softDelete("web_push_subscription", "user_id"),
            retain("audit_event", "actor_id", "actor_email"),
            retain("platform_job_run", "user_id", null)
    );

    @Override
    public List<DataInventoryEntry> entries() {
        return ENTRIES;
    }

    private static DataInventoryEntry softDelete(String tableName, String subjectColumn) {
        return new DataInventoryEntry(
                tableName,
                subjectColumn,
                DsrSubjectRef.USER_ID,
                DsrExportStrategy.INCLUDE_ROWS,
                DsrDeleteStrategy.SOFT_DELETE,
                List.of()
        );
    }

    private static DataInventoryEntry anonymize(String tableName, String subjectColumn, String... columns) {
        return new DataInventoryEntry(
                tableName,
                subjectColumn,
                DsrSubjectRef.USER_ID,
                DsrExportStrategy.INCLUDE_ROWS,
                DsrDeleteStrategy.ANONYMIZE,
                List.of(columns)
        );
    }

    private static DataInventoryEntry retain(String tableName, String subjectColumn, String anonymizeColumn) {
        return new DataInventoryEntry(
                tableName,
                subjectColumn,
                DsrSubjectRef.USER_ID,
                DsrExportStrategy.INCLUDE_ROWS,
                anonymizeColumn == null ? DsrDeleteStrategy.RETAIN : DsrDeleteStrategy.ANONYMIZE,
                anonymizeColumn == null ? List.of() : List.of(anonymizeColumn)
        );
    }
}
