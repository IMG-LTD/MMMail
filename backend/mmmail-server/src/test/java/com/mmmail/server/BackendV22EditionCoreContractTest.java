package com.mmmail.server;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.server.commercial.Edition;
import com.mmmail.server.commercial.EditionContext;
import com.mmmail.server.commercial.FeatureCode;
import com.mmmail.server.commercial.FeatureGate;
import com.mmmail.server.commercial.BillingProviderType;
import com.mmmail.server.commercial.JdbcEditionContextResolver;
import com.mmmail.server.commercial.LicenseState;
import com.mmmail.server.commercial.LicenseStateRepository;
import com.mmmail.server.commercial.LicenseStatus;
import com.mmmail.server.commercial.RequiresEdition;
import com.mmmail.server.commercial.RequiresFeature;
import com.mmmail.server.commercial.SubscriptionPlan;
import com.mmmail.server.commercial.SubscriptionState;
import com.mmmail.server.commercial.SubscriptionStateRepository;
import com.mmmail.server.commercial.SubscriptionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BackendV22EditionCoreContractTest {

    @Test
    void editionOrderShouldKeepFreeProBusinessBoundaries() {
        assertThat(Edition.FREE.allows(Edition.FREE)).isTrue();
        assertThat(Edition.FREE.allows(Edition.PRO)).isFalse();
        assertThat(Edition.PRO.allows(Edition.FREE)).isTrue();
        assertThat(Edition.PRO.allows(Edition.BUSINESS)).isFalse();
        assertThat(Edition.BUSINESS.allows(Edition.PRO)).isTrue();
    }

    @Test
    void featureRegistryShouldResolveStableCodesAndRequiredEditions() {
        assertThat(FeatureCode.fromCode("license.management")).isEqualTo(FeatureCode.LICENSE_MANAGEMENT);
        assertThat(FeatureCode.fromCode("oidc.sso")).isEqualTo(FeatureCode.OIDC_SSO);
        assertThat(FeatureCode.LICENSE_MANAGEMENT.requiredEdition()).isEqualTo(Edition.PRO);
        assertThat(FeatureCode.OIDC_SSO.requiredEdition()).isEqualTo(Edition.BUSINESS);
    }

    @Test
    void featureGateShouldRejectFeatureBelowRequiredEditionWithExplicitError() {
        FeatureGate gate = new FeatureGate();
        EditionContext context = new EditionContext(7L, Edition.FREE);

        assertThatThrownBy(() -> gate.requireFeature(context, FeatureCode.LICENSE_MANAGEMENT))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode())
                        .isEqualTo(ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode()))
                .hasMessageContaining("requiredEdition=PRO")
                .hasMessageContaining("currentEdition=FREE")
                .hasMessageContaining("upgradeAction=upgrade");
    }

    @Test
    void editionResolverShouldPreferSubscriptionStateThenActiveLicense() {
        LicenseStateRepository businessLicense = licenseRepository(Edition.BUSINESS);

        JdbcEditionContextResolver canceledSubscription = resolver(
                subscriptionRepository(SubscriptionPlan.BUSINESS, SubscriptionStatus.CANCELED),
                businessLicense
        );
        assertThat(canceledSubscription.resolve(42L).edition()).isEqualTo(Edition.FREE);

        JdbcEditionContextResolver noSubscription = resolver(emptySubscriptionRepository(), businessLicense);
        assertThat(noSubscription.resolve(42L).edition()).isEqualTo(Edition.BUSINESS);
    }

    @Test
    void annotationsShouldExposeEditionAndFeatureContracts() throws Exception {
        RequiresEdition edition = EditionFixture.class.getAnnotation(RequiresEdition.class);
        RequiresFeature feature = FeatureFixture.class.getAnnotation(RequiresFeature.class);

        assertThat(edition.value()).isEqualTo(Edition.PRO);
        assertThat(feature.value()).isEqualTo(FeatureCode.OIDC_SSO);
    }

    @Test
    void v36MigrationShouldInitializeEditionCoreWithoutCollidingWithExistingVersions() throws Exception {
        Path migration = resolveRepoRoot()
                .resolve("backend/mmmail-server/src/main/resources/db/migration/V36__edition_init.sql");
        String sql = Files.readString(migration);

        assertThat(sql).contains("alter table org_workspace add column edition");
        assertThat(sql).contains("default 'FREE'");
        assertThat(sql).contains("edition.free");
        assertThat(sql).contains("schema_version = '36'");
    }

    private Path resolveRepoRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null && !Files.isDirectory(current.resolve("scripts"))) {
            current = current.getParent();
        }
        assertThat(current).isNotNull();
        return current;
    }

    private JdbcEditionContextResolver resolver(
            SubscriptionStateRepository subscriptionRepository,
            LicenseStateRepository licenseRepository
    ) {
        return new JdbcEditionContextResolver(
                subscriptionRepository,
                licenseRepository,
                new JdbcTemplate(),
                new ObjectMapper()
        );
    }

    private SubscriptionStateRepository subscriptionRepository(SubscriptionPlan plan, SubscriptionStatus status) {
        SubscriptionState state = new SubscriptionState(
                42L,
                plan,
                status,
                BillingProviderType.WEBHOOK,
                Instant.now()
        );
        return new SubscriptionStateRepository() {
            @Override
            public void save(SubscriptionState ignored) {
            }

            @Override
            public Optional<SubscriptionState> findByOrgId(long orgId) {
                return Optional.of(state);
            }
        };
    }

    private SubscriptionStateRepository emptySubscriptionRepository() {
        return new SubscriptionStateRepository() {
            @Override
            public void save(SubscriptionState ignored) {
            }

            @Override
            public Optional<SubscriptionState> findByOrgId(long orgId) {
                return Optional.empty();
            }
        };
    }

    private LicenseStateRepository licenseRepository(Edition edition) {
        LicenseState state = new LicenseState(
                42L,
                "{\"edition\":\"" + edition.name() + "\"}",
                LicenseStatus.ACTIVE,
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );
        return new LicenseStateRepository() {
            @Override
            public void save(LicenseState ignored) {
            }

            @Override
            public Optional<LicenseState> findByOrgId(long orgId) {
                return Optional.of(state);
            }
        };
    }

    @RequiresEdition(Edition.PRO)
    private static final class EditionFixture {
    }

    @RequiresFeature(FeatureCode.OIDC_SSO)
    private static final class FeatureFixture {
    }
}
