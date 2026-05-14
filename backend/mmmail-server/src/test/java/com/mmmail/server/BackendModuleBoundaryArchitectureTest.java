package com.mmmail.server;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

@AnalyzeClasses(packages = "com.mmmail", importOptions = ImportOption.DoNotIncludeTests.class)
class BackendModuleBoundaryArchitectureTest {

    private static final String SERVER_PACKAGE = "com.mmmail.server..";
    private static final String[] FEATURE_MODULE_PACKAGES = {
            "com.mmmail.billing..",
            "com.mmmail.common..",
            "com.mmmail.drive..",
            "com.mmmail.foundation..",
            "com.mmmail.identity..",
            "com.mmmail.labs..",
            "com.mmmail.mail..",
            "com.mmmail.orggovernance..",
            "com.mmmail.pass..",
            "com.mmmail.platform..",
            "com.mmmail.workspace.."
    };
    private static final Set<String> EXPECTED_SCAN_ROOTS = Set.of(
            "com.mmmail.billing",
            "com.mmmail.common",
            "com.mmmail.drive",
            "com.mmmail.foundation",
            "com.mmmail.identity",
            "com.mmmail.labs",
            "com.mmmail.mail",
            "com.mmmail.orggovernance",
            "com.mmmail.pass",
            "com.mmmail.platform",
            "com.mmmail.server",
            "com.mmmail.workspace"
    );

    @ArchTest
    static final ArchRule featureModulesMustNotDependOnServerRuntime = noClasses()
            .that()
            .resideInAnyPackage(FEATURE_MODULE_PACKAGES)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(SERVER_PACKAGE);

    @Test
    void springBootApplicationShouldUseExplicitModuleScanRoots() {
        SpringBootApplication annotation = MmmailServerApplication.class.getAnnotation(SpringBootApplication.class);

        assertThat(annotation.scanBasePackages()).isEmpty();
        assertThat(scanRootPackages(annotation)).isEqualTo(EXPECTED_SCAN_ROOTS);
    }

    private Set<String> scanRootPackages(SpringBootApplication annotation) {
        return Arrays.stream(annotation.scanBasePackageClasses())
                .map(Class::getPackageName)
                .collect(Collectors.toUnmodifiableSet());
    }
}
