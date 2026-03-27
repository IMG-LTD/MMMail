package com.mmmail.common.exception;

public enum ErrorCode {
    INVALID_ARGUMENT(10001, "Invalid argument"),
    UNAUTHORIZED(10002, "Unauthorized"),
    FORBIDDEN(10003, "Forbidden"),
    RATE_LIMITED(10004, "Rate limited"),
    USER_ALREADY_EXISTS(20001, "Email already exists"),
    INVALID_CREDENTIALS(20002, "Invalid email or password"),
    USER_NOT_FOUND(20003, "User not found"),
    SESSION_INVALID(20004, "Session is invalid"),
    MAIL_NOT_FOUND(30001, "Mail not found"),
    MAIL_IDEMPOTENCY_CONFLICT(30002, "Mail idempotency conflict"),
    LABEL_ALREADY_EXISTS(30003, "Label already exists"),
    MAIL_ACTION_INVALID(30004, "Mail action is invalid"),
    CONTACT_ALREADY_EXISTS(30005, "Contact already exists"),
    CONTACT_NOT_FOUND(30006, "Contact not found"),
    CONTACT_GROUP_ALREADY_EXISTS(30007, "Contact group already exists"),
    CONTACT_GROUP_NOT_FOUND(30008, "Contact group not found"),
    CALENDAR_EVENT_NOT_FOUND(30009, "Calendar event not found"),
    CALENDAR_SHARE_NOT_FOUND(30010, "Calendar share not found"),
    QUOTA_EXCEEDED(30011, "Quota exceeded"),
    ORG_NOT_FOUND(30012, "Organization not found"),
    ORG_FORBIDDEN(30013, "Organization access forbidden"),
    ORG_MEMBER_NOT_FOUND(30014, "Organization member not found"),
    ORG_CUSTOM_DOMAIN_NOT_FOUND(30021, "Custom domain not found"),
    ORG_MAIL_IDENTITY_NOT_FOUND(30022, "Org mail identity not found"),
    PASS_ALIAS_NOT_FOUND(30023, "Pass alias not found"),
    PASS_ALIAS_CONTACT_NOT_FOUND(30024, "Pass alias contact not found"),
    PASS_ALIAS_CONTACT_ALREADY_EXISTS(30025, "Pass alias contact already exists"),
    PASS_MAILBOX_NOT_FOUND(30026, "Pass mailbox not found"),
    PASS_MAILBOX_ALREADY_EXISTS(30027, "Pass mailbox already exists"),
    PASS_MAILBOX_NOT_VERIFIED(30028, "Pass mailbox is not verified"),
    PASS_MAILBOX_VERIFICATION_INVALID(30029, "Pass mailbox verification is invalid"),
    PASS_MAILBOX_IN_USE(30030, "Pass mailbox is in use"),
    SHEETS_WORKBOOK_NOT_FOUND(30031, "Sheets workbook not found"),
    SHEETS_WORKBOOK_VERSION_CONFLICT(30032, "Sheets workbook version conflict"),
    STANDARD_NOTE_NOT_FOUND(30033, "Standard note not found"),
    STANDARD_NOTE_VERSION_CONFLICT(30034, "Standard note version conflict"),
    STANDARD_NOTE_FOLDER_NOT_FOUND(30035, "Standard note folder not found"),
    STANDARD_NOTE_FOLDER_CONFLICT(30036, "Standard note folder conflict"),
    SHEETS_WORKBOOK_IMPORT_INVALID(30037, "Sheets workbook import is invalid"),
    SHEETS_WORKBOOK_EXPORT_INVALID(30038, "Sheets workbook export is invalid"),
    ORG_INVITE_CONFLICT(30015, "Organization invite conflict"),
    ORG_INVITE_INVALID(30016, "Organization invite invalid"),
    DOCS_NOTE_NOT_FOUND(30017, "Docs note not found"),
    DOCS_NOTE_VERSION_CONFLICT(30018, "Docs note version conflict"),
    DOCS_NOTE_SHARE_CONFLICT(30019, "Docs note share conflict"),
    DOCS_NOTE_COMMENT_NOT_FOUND(30020, "Docs note comment not found"),
    DOCS_NOTE_SHARE_NOT_FOUND(30039, "Docs note share not found"),
    DOCS_NOTE_SUGGESTION_NOT_FOUND(30040, "Docs note suggestion not found"),
    DOCS_NOTE_SUGGESTION_CONFLICT(30041, "Docs note suggestion conflict"),
    SHEETS_WORKBOOK_SHARE_NOT_FOUND(30042, "Sheets workbook share not found"),
    SHEETS_WORKBOOK_VERSION_NOT_FOUND(30043, "Sheets workbook version not found"),
    DRIVE_COLLABORATOR_SHARE_NOT_FOUND(30044, "Drive collaborator share not found"),
    ORG_PRODUCT_ACCESS_DENIED(30045, "Organization product access denied"),
    ORG_TWO_FACTOR_REQUIRED(30046, "Organization two-factor authentication required"),
    ACCOUNT_MAIL_ADDRESS_REQUIRED(30047, "Proton Mail address required"),
    AUTHENTICATOR_IMPORT_INVALID(30048, "Authenticator import is invalid"),
    AUTHENTICATOR_EXPORT_INVALID(30049, "Authenticator export is invalid"),
    AUTHENTICATOR_BACKUP_INVALID(30050, "Authenticator backup is invalid"),
    INTERNAL_ERROR(90000, "Internal server error");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
