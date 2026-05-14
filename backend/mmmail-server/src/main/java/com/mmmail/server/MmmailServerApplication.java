package com.mmmail.server;

import com.mmmail.billing.BillingModuleMarker;
import com.mmmail.common.CommonModuleMarker;
import com.mmmail.drive.DriveModuleMarker;
import com.mmmail.foundation.FoundationModuleMarker;
import com.mmmail.identity.IdentityModuleMarker;
import com.mmmail.labs.LabsModuleMarker;
import com.mmmail.mail.MailModuleMarker;
import com.mmmail.orggovernance.OrgGovernanceModuleMarker;
import com.mmmail.pass.PassModuleMarker;
import com.mmmail.platform.PlatformModuleMarker;
import com.mmmail.workspace.WorkspaceModuleMarker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackageClasses = {
        BillingModuleMarker.class,
        CommonModuleMarker.class,
        DriveModuleMarker.class,
        FoundationModuleMarker.class,
        IdentityModuleMarker.class,
        LabsModuleMarker.class,
        MailModuleMarker.class,
        MmmailServerApplication.class,
        OrgGovernanceModuleMarker.class,
        PassModuleMarker.class,
        PlatformModuleMarker.class,
        WorkspaceModuleMarker.class
})
public class MmmailServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MmmailServerApplication.class, args);
    }
}
