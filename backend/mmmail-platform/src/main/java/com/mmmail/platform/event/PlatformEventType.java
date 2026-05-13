package com.mmmail.platform.event;

public enum PlatformEventType {
    IDENTITY_USER_CREATED("identity.user.created", "identity", true, true, true),
    IDENTITY_SESSION_REVOKED("identity.session.revoked", "identity", true, false, true),
    WORKSPACE_ACTIVITY_RECORDED("workspace.activity.recorded", "workspace", true, true, true),
    MAIL_MESSAGE_CREATED("mail.message.created", "mail", true, true, true),
    MAIL_MESSAGE_SENT("mail.message.sent", "mail", true, true, true),
    MAIL_RULE_MATCHED("mail.rule.matched", "mail", true, true, true),
    CALENDAR_EVENT_CREATED("calendar.event.created", "calendar", true, true, true),
    CALENDAR_BOOKING_CREATED("calendar.booking.created", "calendar", true, true, true),
    DRIVE_FILE_UPLOADED("drive.file.uploaded", "drive", true, true, true),
    DRIVE_FILE_SHARED("drive.file.shared", "drive", true, true, true),
    DOCS_DOCUMENT_UPDATED("docs.document.updated", "docs", true, true, true),
    DOCS_VERSION_CREATED("docs.version.created", "docs", true, true, true),
    SHEETS_WORKBOOK_IMPORTED("sheets.workbook.imported", "sheets", true, true, true),
    PASS_ITEM_UPDATED("pass.item.updated", "pass", true, true, true),
    PASS_SECURE_LINK_CREATED("pass.secure_link.created", "pass", true, true, true),
    COLLABORATION_PROJECT_CREATED("collaboration.project.created.v1", "collaboration", true, true, true),
    COLLABORATION_TASK_CREATED("collaboration.task.created.v1", "collaboration", true, true, true),
    COLLABORATION_TASK_UPDATED_V1("collaboration.task.updated.v1", "collaboration", true, true, true),
    COLLABORATION_COMMENT_CREATED("collaboration.comment.created.v1", "collaboration", true, true, true),
    COLLABORATION_TASK_UPDATED("collaboration.task.updated", "collaboration", true, true, true),
    COMMAND_RUN_REQUESTED("command.run.requested", "command-center", true, true, true),
    COMMAND_RUN_COMPLETED("command.run.completed", "command-center", true, true, true),
    NOTIFICATION_DELIVERY_REQUESTED("notification.delivery.requested", "notifications", true, true, true),
    ADMIN_AUDIT_RECORDED("admin.audit.recorded", "admin-governance", true, true, true),
    BILLING_ENTITLEMENT_CHANGED("billing.entitlement.changed", "billing", true, true, true),
    LABS_AI_JOB_REQUESTED("labs.ai_job.requested", "labs", true, true, true);

    private final String eventName;
    private final String ownerModule;
    private final boolean tenantRequired;
    private final boolean userRequired;
    private final boolean replayable;

    PlatformEventType(String eventName, String ownerModule, boolean tenantRequired, boolean userRequired, boolean replayable) {
        this.eventName = eventName;
        this.ownerModule = ownerModule;
        this.tenantRequired = tenantRequired;
        this.userRequired = userRequired;
        this.replayable = replayable;
    }

    public String eventName() {
        return eventName;
    }

    public String ownerModule() {
        return ownerModule;
    }

    public boolean tenantRequired() {
        return tenantRequired;
    }

    public boolean userRequired() {
        return userRequired;
    }

    public boolean replayable() {
        return replayable;
    }

    public static PlatformEventType fromEventName(String eventName) {
        for (PlatformEventType type : values()) {
            if (type.eventName.equals(eventName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown platform event type: " + eventName);
    }
}
