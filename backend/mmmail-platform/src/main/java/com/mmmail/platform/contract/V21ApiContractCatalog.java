package com.mmmail.platform.contract;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public record V21ApiContractCatalog(String version, List<V21ApiContract> contracts) {

    private static final int METHOD = 0;
    private static final int PATH = 1;
    private static final int RESPONSE = 2;
    private static final int ENTITLEMENT = 3;
    private static final int PERMISSIONS = 4;
    private static final String COMMUNITY = "community";
    private static final String PREMIUM = "premium";
    private static final String HOSTED = "hosted";
    private static final String GOVERNANCE = "enterprise-governance";

    public V21ApiContractCatalog {
        contracts = List.copyOf(contracts);
    }

    public static V21ApiContractCatalog defaultCatalog() {
        List<V21ApiContract> contracts = Stream.of(
                workspaceContracts(), mailContracts(), calendarContracts(), driveContracts(),
                docsContracts(), sheetsContracts(), labsContracts(), passContracts(),
                collaborationContracts(), commandCenterContracts(), notificationContracts(),
                adminContracts(), billingContracts(), entitlementsContracts(),
                platformContracts(), settingsContracts(), publicAuthShareSystemContracts()
        ).flatMap(List::stream).toList();
        return new V21ApiContractCatalog("v2.1", contracts);
    }

    public int moduleCount() {
        return (int) contracts.stream().map(V21ApiContract::ownerModule).distinct().count();
    }

    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("version", version);
        payload.put("moduleCount", moduleCount());
        payload.put("contracts", contracts.stream().map(V21ApiContract::toPayload).toList());
        return payload;
    }

    private static List<V21ApiContract> workspaceContracts() {
        return module("workspace", "docs/MMMail/UI/首页", new String[][]{
                {"GET", "/api/v2/workspace/summary", "WorkspaceSummary", COMMUNITY, "workspace:read"},
                {"GET", "/api/v2/workspace/activity", "WorkspaceActivityItem[]", COMMUNITY, "workspace:read"},
                {"GET", "/api/v2/workspace/tasks", "WorkspaceTask[]", COMMUNITY, "workspace:read"},
                {"PATCH", "/api/v2/workspace/tasks/:id", "WorkspaceTask", COMMUNITY, "workspace:write"}
        });
    }

    private static List<V21ApiContract> mailContracts() {
        return module("mail", "docs/MMMail/UI/邮件", new String[][]{
                {"GET", "/api/v2/mail/folders", "MailFolder[]", COMMUNITY, "mail:read"},
                {"GET", "/api/v2/mail/messages", "MailFolderPagePayload", COMMUNITY, "mail:read"},
                {"GET", "/api/v2/mail/threads/:id", "MailDetail", COMMUNITY, "mail:read"},
                {"POST", "/api/v2/mail/drafts", "MailDetail", COMMUNITY, "mail:write"},
                {"PATCH", "/api/v2/mail/drafts/:id", "MailDetail", COMMUNITY, "mail:write"},
                {"POST", "/api/v2/mail/send", "MailSendResult", COMMUNITY, "mail:send"},
                {"POST", "/api/v2/mail/messages/bulk-action", "MailSummary[]", COMMUNITY, "mail:write"},
                {"GET", "/api/v2/mail/contacts", "MailSenderIdentity[]", COMMUNITY, "mail:read"},
                {"GET", "/api/v2/mail/rules", "MailRule[]", PREMIUM, "mail:rules:read"},
                {"POST", "/api/v2/mail/rules", "MailRule", PREMIUM, "mail:rules:write"}
        });
    }

    private static List<V21ApiContract> calendarContracts() {
        return module("calendar", "docs/MMMail/UI/日历", new String[][]{
                {"GET", "/api/v2/calendar/events", "CalendarEvent[]", COMMUNITY, "calendar:read"},
                {"POST", "/api/v2/calendar/events", "CalendarEvent", COMMUNITY, "calendar:write"},
                {"PATCH", "/api/v2/calendar/events/:id", "CalendarEvent", COMMUNITY, "calendar:write"},
                {"DELETE", "/api/v2/calendar/events/:id", "Void", COMMUNITY, "calendar:write"},
                {"GET", "/api/v2/calendar/availability", "CalendarAvailability", PREMIUM, "calendar:read"},
                {"GET", "/api/v2/calendar/resources", "CalendarResource[]", PREMIUM, "calendar:resources:read"},
                {"POST", "/api/v2/calendar/bookings", "CalendarEvent", PREMIUM, "calendar:resources:book"},
                {"GET", "/api/v2/calendar/settings", "CalendarSettings", COMMUNITY, "calendar:read"},
                {"PATCH", "/api/v2/calendar/settings", "CalendarSettings", COMMUNITY, "calendar:write"}
        });
    }

    private static List<V21ApiContract> driveContracts() {
        return module("drive", "docs/MMMail/UI/云盘", new String[][]{
                {"GET", "/api/v2/drive/folders", "DriveItem[]", COMMUNITY, "drive:read"},
                {"GET", "/api/v2/drive/files", "DriveItem[]", COMMUNITY, "drive:read"},
                {"POST", "/api/v2/drive/uploads", "DriveItem", COMMUNITY, "drive:write"},
                {"GET", "/api/v2/drive/uploads/:id", "UploadQueueItem", COMMUNITY, "drive:read"},
                {"PATCH", "/api/v2/drive/files/:id", "DriveItem", COMMUNITY, "drive:write"},
                {"DELETE", "/api/v2/drive/files/:id", "Void", COMMUNITY, "drive:write"},
                {"POST", "/api/v2/drive/files/:id/share", "DriveShareLink", COMMUNITY, "drive:share"},
                {"GET", "/api/v2/drive/files/:id/versions", "DriveFileVersion[]", PREMIUM, "drive:read"},
                {"GET", "/api/v2/drive/storage/summary", "DriveUsage", COMMUNITY, "drive:read"}
        });
    }

    private static List<V21ApiContract> docsContracts() {
        return module("docs", "docs/MMMail/UI/文档", new String[][]{
                {"GET", "/api/v2/docs", "DocsNoteSummary[]", COMMUNITY, "docs:read"},
                {"POST", "/api/v2/docs", "DocsNoteDetail", COMMUNITY, "docs:write"},
                {"GET", "/api/v2/docs/:id", "DocsNoteDetail", COMMUNITY, "docs:read"},
                {"PATCH", "/api/v2/docs/:id", "DocsNoteDetail", COMMUNITY, "docs:write"},
                {"GET", "/api/v2/docs/:id/comments", "DocsComment[]", COMMUNITY, "docs:read"},
                {"POST", "/api/v2/docs/:id/comments", "DocsComment", COMMUNITY, "docs:write"},
                {"GET", "/api/v2/docs/:id/versions", "DocsVersion[]", PREMIUM, "docs:read"},
                {"POST", "/api/v2/docs/:id/share", "DocsNoteDetail", COMMUNITY, "docs:share"}
        });
    }

    private static List<V21ApiContract> sheetsContracts() {
        return module("sheets", "docs/MMMail/UI/Sheets和labs", new String[][]{
                {"GET", "/api/v2/sheets", "SheetsWorkbookSummary[]", COMMUNITY, "sheets:read"},
                {"POST", "/api/v2/sheets", "SheetsWorkbookDetail", COMMUNITY, "sheets:write"},
                {"GET", "/api/v2/sheets/:id", "SheetsWorkbookDetail", COMMUNITY, "sheets:read"},
                {"PATCH", "/api/v2/sheets/:id", "SheetsWorkbookDetail", COMMUNITY, "sheets:write"},
                {"POST", "/api/v2/sheets/:id/imports", "SheetsWorkbookDetail", COMMUNITY, "sheets:write"},
                {"POST", "/api/v2/sheets/:id/cleaning-rules", "SheetsWorkbookDetail", PREMIUM, "sheets:clean"},
                {"GET", "/api/v2/sheets/:id/insights", "SheetsInsight[]", PREMIUM, "sheets:ai:read"}
        });
    }

    private static List<V21ApiContract> labsContracts() {
        return module("labs", "docs/MMMail/UI/Sheets和labs", new String[][]{
                {"GET", "/api/v2/labs/modules", "LabsModule[]", COMMUNITY, "labs:read"},
                {"GET", "/api/v2/labs/modules/:key", "LabsModule", COMMUNITY, "labs:read"},
                {"PATCH", "/api/v2/labs/modules/:key/settings", "LabsModuleSettings", PREMIUM, "labs:write"}
        });
    }

    private static List<V21ApiContract> passContracts() {
        return module("pass", "docs/MMMail/UI/Pass", new String[][]{
                {"GET", "/api/v2/pass/vaults", "PassVault[]", COMMUNITY, "pass:read"},
                {"GET", "/api/v2/pass/items", "PassWorkspaceItemSummary[]", COMMUNITY, "pass:read"},
                {"POST", "/api/v2/pass/items", "PassWorkspaceItemSummary", COMMUNITY, "pass:write"},
                {"PATCH", "/api/v2/pass/items/:id", "PassWorkspaceItemSummary", COMMUNITY, "pass:write"},
                {"POST", "/api/v2/pass/share", "PassSecureLink", PREMIUM, "pass:share"},
                {"GET", "/api/v2/pass/secure-links", "PassSecureLink[]", PREMIUM, "pass:share"},
                {"POST", "/api/v2/pass/secure-links", "PassSecureLink", PREMIUM, "pass:share"},
                {"DELETE", "/api/v2/pass/secure-links/:id", "Void", PREMIUM, "pass:share"},
                {"GET", "/api/v2/pass/aliases", "PassAlias[]", PREMIUM, "pass:aliases:read"},
                {"PATCH", "/api/v2/pass/aliases/:id", "PassAlias", PREMIUM, "pass:aliases:write"},
                {"GET", "/api/v2/pass/monitor", "PassMonitorOverview", PREMIUM, "pass:monitor:read"}
        });
    }

    private static List<V21ApiContract> collaborationContracts() {
        return module("collaboration", "docs/MMMail/UI/Collaboration", new String[][]{
                {"GET", "/api/v2/collaboration/projects", "CollaborationProject[]", COMMUNITY, "collaboration:read"},
                {"POST", "/api/v2/collaboration/projects", "CollaborationProject", COMMUNITY, "collaboration:write"},
                {"GET", "/api/v2/collaboration/projects/:id", "CollaborationProject", COMMUNITY, "collaboration:read"},
                {"GET", "/api/v2/collaboration/tasks", "CollaborationTask[]", COMMUNITY, "collaboration:read"},
                {"POST", "/api/v2/collaboration/tasks", "CollaborationTask", COMMUNITY, "collaboration:write"},
                {"PATCH", "/api/v2/collaboration/tasks/:id", "CollaborationTask", COMMUNITY, "collaboration:write"},
                {"POST", "/api/v2/collaboration/tasks/:id/comments", "CollaborationTask", COMMUNITY, "collaboration:write"},
                {"GET", "/api/v2/collaboration/activity", "CollaborationActivity[]", COMMUNITY, "collaboration:read"}
        });
    }

    private static List<V21ApiContract> commandCenterContracts() {
        return module("command-center", "docs/MMMail/UI/CommandCenter", new String[][]{
                {"GET", "/api/v2/command-center/commands", "CommandCenterCommand[]", COMMUNITY, "command:center:read"},
                {"GET", "/api/v2/command-center/commands/:id", "CommandCenterCommand", COMMUNITY, "command:center:read"},
                {"POST", "/api/v2/command-center/runs", "CommandCenterRun", PREMIUM, "command:center:run"},
                {"GET", "/api/v2/command-center/runs/:id", "CommandCenterRun", PREMIUM, "command:center:read"},
                {"POST", "/api/v2/command-center/runs/:id/cancel", "CommandCenterRun", PREMIUM, "command:center:run"},
                {"POST", "/api/v2/command-center/runs/:id/retry", "CommandCenterRun", PREMIUM, "command:center:run"},
                {"GET", "/api/v2/command-center/workflows", "CommandCenterWorkflow[]", PREMIUM, "command:center:read"},
                {"POST", "/api/v2/command-center/workflows", "CommandCenterWorkflow", PREMIUM, "command:center:write"},
                {"GET", "/api/v2/command-center/audit", "CommandCenterAuditEntry[]", PREMIUM, "command:center:audit"}
        });
    }

    private static List<V21ApiContract> notificationContracts() {
        return module("notifications", "docs/MMMail/UI/Notifications", new String[][]{
                {"GET", "/api/v2/notifications", "NotificationItem[]", COMMUNITY, "notifications:read"},
                {"PATCH", "/api/v2/notifications/:id", "NotificationItem", COMMUNITY, "notifications:write"},
                {"GET", "/api/v2/notifications/rules", "NotificationRule[]", PREMIUM, "notifications:rules:read"},
                {"POST", "/api/v2/notifications/rules", "NotificationRule", PREMIUM, "notifications:rules:write"},
                {"GET", "/api/v2/notifications/subscriptions", "NotificationSubscription[]", COMMUNITY, "notifications:read"},
                {"PATCH", "/api/v2/notifications/subscriptions/:id", "NotificationSubscription", COMMUNITY, "notifications:write"},
                {"GET", "/api/v2/notifications/templates", "NotificationTemplate[]", PREMIUM, "notifications:templates:read"},
                {"POST", "/api/v2/notifications/send", "NotificationItem", PREMIUM, "notifications:send"},
                {"GET", "/api/v2/notifications/analytics", "NotificationAnalytics", PREMIUM, "notifications:analytics:read"}
        });
    }

    private static List<V21ApiContract> adminContracts() {
        return module("admin-governance", "docs/MMMail/UI/Admin", new String[][]{
                {"GET", "/api/v2/admin/summary", "AdminSummary", GOVERNANCE, "admin:read"},
                {"GET", "/api/v2/admin/users", "AdminUser[]", GOVERNANCE, "admin:users:read"},
                {"POST", "/api/v2/admin/users", "AdminUser", GOVERNANCE, "admin:users:manage"},
                {"PATCH", "/api/v2/admin/users/:id", "AdminUser", GOVERNANCE, "admin:users:manage"},
                {"GET", "/api/v2/admin/roles", "AdminRole[]", GOVERNANCE, "admin:roles:read"},
                {"GET", "/api/v2/admin/domains", "AdminDomain[]", GOVERNANCE, "admin:domains:read"},
                {"GET", "/api/v2/admin/policies", "AdminPolicy[]", GOVERNANCE, "admin:policies:read"},
                {"PATCH", "/api/v2/admin/policies/:id", "AdminPolicy", GOVERNANCE, "admin:policies:manage"},
                {"GET", "/api/v2/admin/audit", "AdminAuditEntry[]", GOVERNANCE, "admin:audit:read"},
                {"GET", "/api/v2/admin/alerts", "AdminAlert[]", GOVERNANCE, "admin:alerts:read"},
                {"GET", "/api/v2/admin/system", "AdminSystemStatus", GOVERNANCE, "admin:system:read"},
                {"GET", "/api/v2/admin/risk", "AdminRiskOverview", GOVERNANCE, "admin:risk:read"}
        });
    }

    private static List<V21ApiContract> billingContracts() {
        return module("billing", "docs/MMMail/UI/Admin", new String[][]{
                {"GET", "/api/v2/billing/summary", "BillingSummary", HOSTED, "billing:read"},
                {"GET", "/api/v2/billing/plans", "BillingPlan[]", HOSTED, "billing:plans:read"},
                {"GET", "/api/v2/billing/invoices", "BillingInvoice[]", HOSTED, "billing:invoices:read"},
                {"GET", "/api/v2/billing/usage", "BillingUsage", HOSTED, "billing:usage:read"}
        });
    }

    private static List<V21ApiContract> entitlementsContracts() {
        return module("entitlements", "docs/MMMail/UI/Admin", new String[][]{
                {"GET", "/api/v2/entitlements", "EntitlementState[]", COMMUNITY, "entitlements:read"},
                {"GET", "/api/v2/entitlements/matrix", "EntitlementMatrix", COMMUNITY, "entitlements:matrix:read"}
        });
    }

    private static List<V21ApiContract> platformContracts() {
        return module("platform", "docs/MMMail/UI/Admin", new String[][]{
                {"GET", "/api/v2/platform/contracts", "V21ApiContractCatalog", COMMUNITY, "platform:contracts:read"},
                {"GET", "/api/v2/platform/capabilities", "PlatformCapabilities", COMMUNITY, "platform:capabilities:read"}
        });
    }

    private static List<V21ApiContract> settingsContracts() {
        return module("settings", "docs/MMMail/UI/Setting", new String[][]{
                {"GET", "/api/v2/settings/profile", "UserPreference", COMMUNITY, "settings:read"},
                {"PATCH", "/api/v2/settings/profile", "UserPreference", COMMUNITY, "settings:write"},
                {"GET", "/api/v2/settings/security", "SecuritySettings", COMMUNITY, "settings:security:read"},
                {"PATCH", "/api/v2/settings/security", "SecuritySettings", COMMUNITY, "settings:security:write"},
                {"GET", "/api/v2/settings/devices", "DeviceSession[]", COMMUNITY, "settings:devices:read"},
                {"DELETE", "/api/v2/settings/devices/:id", "Void", COMMUNITY, "settings:devices:write"},
                {"GET", "/api/v2/settings/notifications", "NotificationSettings", COMMUNITY, "settings:notifications:read"},
                {"PATCH", "/api/v2/settings/notifications", "NotificationSettings", COMMUNITY, "settings:notifications:write"},
                {"GET", "/api/v2/settings/integrations", "IntegrationSetting[]", PREMIUM, "settings:integrations:read"},
                {"PATCH", "/api/v2/settings/integrations/:id", "IntegrationSetting", PREMIUM, "settings:integrations:write"},
                {"GET", "/api/v2/settings/audit", "AuditEvent[]", PREMIUM, "settings:audit:read"}
        });
    }

    private static List<V21ApiContract> publicAuthShareSystemContracts() {
        return Stream.of(
                module("identity", "docs/MMMail/UI/首页", new String[][]{
                        {"POST", "/api/v2/auth/login", "AuthPayload", COMMUNITY, "auth:public"},
                        {"POST", "/api/v2/auth/register", "AuthPayload", COMMUNITY, "auth:public"}
                }),
                module("public-share", "docs/MMMail/UI/首页", new String[][]{
                        {"GET", "/api/v2/share/capabilities", "PublicShareCapabilities", COMMUNITY, "share:public"},
                        {"GET", "/api/v2/public-share/capabilities", "PublicShareCapabilities", COMMUNITY, "share:public"},
                        {"GET", "/api/v2/share/mail/:token", "PublicMailShare", COMMUNITY, "share:public"},
                        {"GET", "/api/v2/share/drive/:token", "PublicDriveShareMetadata", COMMUNITY, "share:public"},
                        {"GET", "/api/v2/share/pass/:token", "PublicPassShare", COMMUNITY, "share:public"}
                }),
                module("system", "docs/MMMail/UI/首页", new String[][]{
                        {"GET", "/api/v2/system/status", "PublicSystemStatus", COMMUNITY, "system:public"}
                })
        ).flatMap(List::stream).toList();
    }

    private static List<V21ApiContract> module(String owner, String designSource, String[][] definitions) {
        return Arrays.stream(definitions)
                .map(definition -> contract(owner, designSource, definition))
                .toList();
    }

    private static V21ApiContract contract(String owner, String designSource, String[] definition) {
        if (definition.length != 5) {
            throw new IllegalArgumentException("v2.1 API contract definitions require five columns");
        }
        V21ApiOwner apiOwner = new V21ApiOwner(owner, designSource);
        V21ApiSchema schema = new V21ApiSchema(definition[RESPONSE], requestModel(definition[METHOD]));
        V21ApiAccess access = new V21ApiAccess(List.of(definition[PERMISSIONS].split("\\|")), definition[ENTITLEMENT]);
        return new V21ApiContract(definition[METHOD], definition[PATH], new V21ApiContractMetadata(apiOwner, schema, access));
    }

    private static String requestModel(String method) {
        return switch (method) {
            case "GET", "DELETE" -> "None";
            default -> "RequestPayload";
        };
    }
}
