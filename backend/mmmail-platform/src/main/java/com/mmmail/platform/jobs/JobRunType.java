package com.mmmail.platform.jobs;

public enum JobRunType {
    NOTIFICATION_DELIVERY("notification.delivery", "notifications", true, true, true, true),
    MAIL_DELIVERY("mail.delivery", "mail", true, true, true, true),
    FILE_PREVIEW("file.preview", "drive", true, true, true, true),
    COMMAND_RUN("command.run", "command-center", true, true, true, true),
    AI_LABS("ai.labs", "labs", true, true, true, true),
    BILLING_ENTITLEMENT_SYNC("billing.entitlement_sync", "billing", true, true, true, true),
    AUDIT_EXPORT("audit.export", "admin-governance", true, true, true, true),
    DSR_EXPORT("dsr.export", "privacy-compliance", true, true, true, true),
    DSR_ERASURE("dsr.erasure", "privacy-compliance", true, true, true, true);

    private final String jobName;
    private final String ownerModule;
    private final boolean tenantRequired;
    private final boolean userRequired;
    private final boolean retrySupported;
    private final boolean hostedExtractionCandidate;

    JobRunType(
            String jobName,
            String ownerModule,
            boolean tenantRequired,
            boolean userRequired,
            boolean retrySupported,
            boolean hostedExtractionCandidate
    ) {
        this.jobName = jobName;
        this.ownerModule = ownerModule;
        this.tenantRequired = tenantRequired;
        this.userRequired = userRequired;
        this.retrySupported = retrySupported;
        this.hostedExtractionCandidate = hostedExtractionCandidate;
    }

    public String jobName() {
        return jobName;
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

    public boolean retrySupported() {
        return retrySupported;
    }

    public boolean hostedExtractionCandidate() {
        return hostedExtractionCandidate;
    }

    public static JobRunType fromJobName(String jobName) {
        for (JobRunType type : values()) {
            if (type.jobName.equals(jobName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown job run type: " + jobName);
    }
}
