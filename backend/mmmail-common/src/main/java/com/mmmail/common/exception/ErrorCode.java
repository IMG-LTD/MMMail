package com.mmmail.common.exception;

/**
 * 错误码段位规范（spec docs/v212-migration-spec.md §22.1）：
 *
 *   ┌─────────────────┬─────────────────────────────────────────────────────┐
 *   │ 编码格式         │ {HTTP 状态前 3 位} × 100 + {模块码 21..} 或 {序号}    │
 *   │                 │ 例：40021 = HTTP 400 + 模块 21（社区）+ 子号            │
 *   ├─────────────────┼─────────────────────────────────────────────────────┤
 *   │ 10000–19999     │ 通用 / 网关 / 限流（本身就是 HTTP 4xx 映射）          │
 *   │ 20000–29999     │ 认证 / 用户 / 会话                                    │
 *   │ 30000–39999     │ 业务资源 NotFound / 冲突（HTTP 4xx 默认）              │
 *   │ 40000–40299     │ HTTP 400 业务错误（按 4xxYY 编码：YY 模块×10 + 序号）  │
 *   │ 40300–40399     │ HTTP 403 业务错误                                     │
 *   │ 40900–40999     │ HTTP 409 冲突类业务错误                                │
 *   │ 42000–42399     │ HTTP 422 业务校验错误                                 │
 *   │ 42900           │ HTTP 429 限流                                         │
 *   │ 50000–59999     │ HTTP 5xx 后端 / 第三方 / 网关错误                     │
 *   │ 90000           │ INTERNAL_ERROR 兜底                                   │
 *   └─────────────────┴─────────────────────────────────────────────────────┘
 *
 * 模块码（22.1 段位表）：
 *   01 钱包 / 02 VPN / 03 会议 / 04 联系人 / 05 备忘录 / 06 Drive E2EE / 07 TOTP
 *   08 IMAP/SMTP 外账户 / 09 公式 / 10 RATE_LIMITED / 21 社区 / 22 搜索
 *
 * 新增枚举要求（CI 校验，scripts/check-error-code-ranges.sh）：
 *   1. 每个枚举必须落在上表某一段；不在表内段位需先扩 spec §22.1。
 *   2. 同一段内编码递增，不允许重号。
 *   3. 与前端 i18n key `errors.{code}.title|message` 一一对应。
 */
public enum ErrorCode {

    // === 10000–19999 通用 / 限流 ===
    INVALID_ARGUMENT(10001, "Invalid argument"),
    UNAUTHORIZED(10002, "Unauthorized"),
    FORBIDDEN(10003, "Forbidden"),
    RATE_LIMITED(10004, "Rate limited"),

    // === 20000–29999 认证 / 用户 / 会话 ===
    USER_ALREADY_EXISTS(20001, "Email already exists"),
    INVALID_CREDENTIALS(20002, "Invalid email or password"),
    USER_NOT_FOUND(20003, "User not found"),
    SESSION_INVALID(20004, "Session is invalid"),

    // === 30000–39999 业务资源 NotFound / 冲突（HTTP 4xx 默认） ===
    // 30001–30010 Mail / Contact / Calendar
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

    // 30011–30020 Org / Quota / Docs
    QUOTA_EXCEEDED(30011, "Quota exceeded"),
    ORG_NOT_FOUND(30012, "Organization not found"),
    ORG_FORBIDDEN(30013, "Organization access forbidden"),
    ORG_MEMBER_NOT_FOUND(30014, "Organization member not found"),
    ORG_INVITE_CONFLICT(30015, "Organization invite conflict"),
    ORG_INVITE_INVALID(30016, "Organization invite invalid"),
    DOCS_NOTE_NOT_FOUND(30017, "Docs note not found"),
    DOCS_NOTE_VERSION_CONFLICT(30018, "Docs note version conflict"),
    DOCS_NOTE_SHARE_CONFLICT(30019, "Docs note share conflict"),
    DOCS_NOTE_COMMENT_NOT_FOUND(30020, "Docs note comment not found"),

    // 30021–30030 Domain / Pass
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

    // 30031–30050 Sheets / Standard Notes / Authenticator
    SHEETS_WORKBOOK_NOT_FOUND(30031, "Sheets workbook not found"),
    SHEETS_WORKBOOK_VERSION_CONFLICT(30032, "Sheets workbook version conflict"),
    STANDARD_NOTE_NOT_FOUND(30033, "Standard note not found"),
    STANDARD_NOTE_VERSION_CONFLICT(30034, "Standard note version conflict"),
    STANDARD_NOTE_FOLDER_NOT_FOUND(30035, "Standard note folder not found"),
    STANDARD_NOTE_FOLDER_CONFLICT(30036, "Standard note folder conflict"),
    SHEETS_WORKBOOK_IMPORT_INVALID(30037, "Sheets workbook import is invalid"),
    SHEETS_WORKBOOK_EXPORT_INVALID(30038, "Sheets workbook export is invalid"),
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

    // 30051–30099 v2 API / 公开分享 / CalDAV / 外账户
    V2_API_CONTRACT_NOT_FOUND(30051, "Unknown v2 API contract"),
    V2_ENTITLEMENT_REQUIRED(30052, "Required entitlement is not enabled"),
    V2_PERMISSION_DENIED(30053, "Required permission is not granted"),
    PUBLIC_SHARE_NOT_FOUND(30054, "Public share not found"),
    CALENDAR_SUBSCRIPTION_NOT_FOUND(30055, "Calendar subscription not found"),
    MAIL_EXTERNAL_ACCOUNT_NOT_FOUND(30056, "External mail account not found"),

    // === 40000–40299 HTTP 400 业务错误（4xxYY = 400+模块×10+序号） ===
    // 40021–40029 模块 02：社区
    COMMUNITY_TITLE_REQUIRED(40021, "Community post title is required"),
    COMMUNITY_TOPIC_NOT_FOUND(40022, "Community topic not found"),
    COMMUNITY_POST_NOT_FOUND(40023, "Community post not found"),
    COMMUNITY_COMMENT_NOT_FOUND(40024, "Community comment not found"),
    COMMUNITY_REPORT_NOT_FOUND(40025, "Community report not found"),

    // 40026–40029 模块 03：搜索 / 外账户配置
    SEARCH_QUERY_TOO_SHORT(40026, "Search query is too short"),
    SEARCH_REINDEX_JOB_NOT_FOUND(40027, "Search reindex job not found"),
    SEARCH_MODULE_UNSUPPORTED(40028, "Search module is unsupported"),
    MAIL_EXTERNAL_ACCOUNT_CONFIG(40029, "External mail account configuration is invalid"),

    // 40121 模块 12：外账户认证
    MAIL_EXTERNAL_AUTH_INVALID(40121, "External mail authentication is invalid"),

    // === 40300–40399 HTTP 403 业务错误 ===
    COMMUNITY_NOT_AUTHOR(40321, "Only the author can mutate this community resource"),
    COMMUNITY_ADMIN_REQUIRED(40322, "Community admin action requires administrator role"),

    // === 40900–40999 HTTP 409 冲突 ===
    COMMUNITY_POST_LOCKED(40921, "Community post is locked"),
    COMMUNITY_TOPIC_NOT_EMPTY(40922, "Community topic still has active posts"),

    // === 42000–42399 HTTP 422 校验错误 ===
    SHEETS_CIRCULAR_REF(42221, "Sheets formula has a circular reference"),

    // === 50000–59999 HTTP 5xx 后端 / 第三方 ===
    MAIL_EXTERNAL_TIMEOUT(50421, "External mail server timed out"),
    MAIL_EXTERNAL_RATE_LIMITED(50521, "External mail server is rate limited"),

    // === 90000 INTERNAL_ERROR 兜底 ===
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
