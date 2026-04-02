package com.mmmail.server.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SuitePricingCatalogServiceTest {

    private final SuitePricingCatalogService suitePricingCatalogService = new SuitePricingCatalogService(new SuiteCatalogService());

    @Test
    void drivePlusOfferShouldKeepCapabilityCopyAlignedWithCurrentBoundary() {
        SuitePricingCatalogService.OfferDefinition drivePlus = suitePricingCatalogService.requireOffer("DRIVE_PLUS");

        assertThat(drivePlus.description()).isEqualTo("Single-product Drive add-on aligned to Proton-style public checkout");
        assertThat(drivePlus.highlights()).containsExactly(
                "200 GB storage quota",
                "Version history and share-link baseline",
                "Browser-first access and export posture"
        );
        assertThat(drivePlus.highlights()).noneMatch(item -> item.contains("encrypted") || item.contains("Desktop sync"));
    }
}
