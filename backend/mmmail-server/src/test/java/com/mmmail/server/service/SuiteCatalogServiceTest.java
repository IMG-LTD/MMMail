package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.server.model.vo.SuitePlanVo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SuiteCatalogServiceTest {

    private final SuiteCatalogService suiteCatalogService = new SuiteCatalogService();

    @Test
    void listPlansExposesExpandedParityCatalog() {
        List<SuitePlanVo> plans = suiteCatalogService.listPlans();

        assertThat(plans).hasSize(13);
        assertThat(plans).extracting(SuitePlanVo::code).contains(
                "DUO",
                "FAMILY",
                "VISIONARY",
                "MAIL_ESSENTIALS",
                "BUSINESS_SUITE",
                "VPN_PASS_PROFESSIONAL",
                "SENTINEL"
        );
        assertThat(plans).filteredOn(SuitePlanVo::recommended)
                .extracting(SuitePlanVo::code)
                .containsExactly("UNLIMITED", "BUSINESS_SUITE", "VPN_PASS_PROFESSIONAL");
    }

    @Test
    void resolvesMeetEligibilityAndRejectsUnknownPlans() {
        assertThat(suiteCatalogService.eligibleForMeetInstantAccess("DUO")).isTrue();
        assertThat(suiteCatalogService.eligibleForMeetInstantAccess("MAIL_ESSENTIALS")).isFalse();

        assertThatThrownBy(() -> suiteCatalogService.requirePlan("UNKNOWN"))
                .isInstanceOf(BizException.class)
                .hasMessage("Unsupported plan code");
    }
}
