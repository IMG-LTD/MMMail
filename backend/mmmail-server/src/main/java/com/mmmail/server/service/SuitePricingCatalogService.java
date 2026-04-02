package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.model.vo.SuitePricingOfferVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SuitePricingCatalogService {

    public static final String BILLING_CYCLE_MONTHLY = "MONTHLY";
    public static final String BILLING_CYCLE_ANNUAL = "ANNUAL";
    public static final String CHECKOUT_MODE_SELF_SERVE = "SELF_SERVE";
    public static final String CHECKOUT_MODE_CONTACT_SALES = "CONTACT_SALES";

    private static final String SEGMENT_CONSUMER = "CONSUMER";
    private static final String SEGMENT_BUSINESS = "BUSINESS";
    private static final String SEGMENT_SECURITY = "SECURITY";
    private static final String PRICE_MODE_FROM = "FROM";
    private static final String PRICE_MODE_CONTACT_SALES = "CONTACT_SALES";
    private static final String CURRENCY_EUR = "EUR";
    private static final String CURRENCY_USD = "USD";
    private static final String PASS_PLUS = "PASS_PLUS";
    private static final String DRIVE_PLUS = "DRIVE_PLUS";
    private static final String PLAN_MAIL_PLUS = "MAIL_PLUS";
    private static final String PLAN_DUO = "DUO";
    private static final String PLAN_FAMILY = "FAMILY";
    private static final String PLAN_UNLIMITED = "UNLIMITED";
    private static final String PLAN_VISIONARY = "VISIONARY";
    private static final String PLAN_MAIL_ESSENTIALS = "MAIL_ESSENTIALS";
    private static final String PLAN_MAIL_PROFESSIONAL = "MAIL_PROFESSIONAL";
    private static final String PLAN_BUSINESS_SUITE = "BUSINESS_SUITE";
    private static final String PLAN_ENTERPRISE = "ENTERPRISE";
    private static final String PLAN_VPN_PROFESSIONAL = "VPN_PROFESSIONAL";
    private static final String PLAN_VPN_PASS_PROFESSIONAL = "VPN_PASS_PROFESSIONAL";
    private static final String PLAN_SENTINEL = "SENTINEL";
    private static final int DEFAULT_SINGLE_SEAT = 1;
    private static final int DEFAULT_DUO_SEATS = 2;
    private static final int DEFAULT_FAMILY_SEATS = 6;
    private static final int DEFAULT_TEAM_SEATS = 5;
    private static final int PASS_PLUS_MONTHLY_PRICE_CENTS = 499;
    private static final int PASS_PLUS_ANNUAL_PRICE_CENTS = 299;
    private static final int DRIVE_PLUS_MONTHLY_PRICE_CENTS = 499;
    private static final int DRIVE_PLUS_ANNUAL_PRICE_CENTS = 399;
    private static final int MONTHS_PER_YEAR = 12;

    private final Map<String, OfferDefinition> offerDefinitions;

    public SuitePricingCatalogService(SuiteCatalogService suiteCatalogService) {
        this.offerDefinitions = buildOfferDefinitions(suiteCatalogService);
    }

    public List<SuitePricingOfferVo> listOffers() {
        return offerDefinitions.values().stream()
                .map(this::toOfferVo)
                .toList();
    }

    public List<String> listSelfServeOfferCodes() {
        return offerDefinitions.values().stream()
                .filter(offer -> CHECKOUT_MODE_SELF_SERVE.equals(offer.checkoutMode()))
                .map(OfferDefinition::code)
                .toList();
    }

    public List<String> listContactSalesOfferCodes() {
        return offerDefinitions.values().stream()
                .filter(offer -> CHECKOUT_MODE_CONTACT_SALES.equals(offer.checkoutMode()))
                .map(OfferDefinition::code)
                .toList();
    }

    public OfferDefinition requireOffer(String rawOfferCode) {
        if (!StringUtils.hasText(rawOfferCode)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Offer code is required");
        }
        String offerCode = rawOfferCode.trim().toUpperCase();
        OfferDefinition offer = offerDefinitions.get(offerCode);
        if (offer == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported offer code");
        }
        return offer;
    }

    private Map<String, OfferDefinition> buildOfferDefinitions(SuiteCatalogService suiteCatalogService) {
        Map<String, OfferDefinition> offers = new LinkedHashMap<>();
        addPlanOffer(offers, suiteCatalogService.requirePlan(PLAN_MAIL_PLUS), CHECKOUT_MODE_SELF_SERVE, DEFAULT_SINGLE_SEAT, false);
        addPlanOffer(offers, suiteCatalogService.requirePlan(PLAN_DUO), CHECKOUT_MODE_SELF_SERVE, DEFAULT_DUO_SEATS, false);
        addPlanOffer(offers, suiteCatalogService.requirePlan(PLAN_FAMILY), CHECKOUT_MODE_SELF_SERVE, DEFAULT_FAMILY_SEATS, false);
        addPlanOffer(offers, suiteCatalogService.requirePlan(PLAN_UNLIMITED), CHECKOUT_MODE_SELF_SERVE, DEFAULT_SINGLE_SEAT, false);
        addPlanOffer(offers, suiteCatalogService.requirePlan(PLAN_VISIONARY), CHECKOUT_MODE_SELF_SERVE, DEFAULT_SINGLE_SEAT, false);
        offers.put(PASS_PLUS, createPassPlusOffer());
        offers.put(DRIVE_PLUS, createDrivePlusOffer());
        addPlanOffer(offers, suiteCatalogService.requirePlan(PLAN_MAIL_ESSENTIALS), CHECKOUT_MODE_CONTACT_SALES, DEFAULT_TEAM_SEATS, true);
        addPlanOffer(offers, suiteCatalogService.requirePlan(PLAN_MAIL_PROFESSIONAL), CHECKOUT_MODE_CONTACT_SALES, DEFAULT_TEAM_SEATS, true);
        addPlanOffer(offers, suiteCatalogService.requirePlan(PLAN_BUSINESS_SUITE), CHECKOUT_MODE_CONTACT_SALES, DEFAULT_TEAM_SEATS, true);
        addPlanOffer(offers, suiteCatalogService.requirePlan(PLAN_ENTERPRISE), CHECKOUT_MODE_CONTACT_SALES, DEFAULT_TEAM_SEATS, true);
        addPlanOffer(offers, suiteCatalogService.requirePlan(PLAN_VPN_PROFESSIONAL), CHECKOUT_MODE_CONTACT_SALES, DEFAULT_TEAM_SEATS, true);
        addPlanOffer(offers, suiteCatalogService.requirePlan(PLAN_VPN_PASS_PROFESSIONAL), CHECKOUT_MODE_CONTACT_SALES, DEFAULT_TEAM_SEATS, true);
        addPlanOffer(offers, suiteCatalogService.requirePlan(PLAN_SENTINEL), CHECKOUT_MODE_CONTACT_SALES, DEFAULT_TEAM_SEATS, true);
        return Collections.unmodifiableMap(offers);
    }

    private void addPlanOffer(
            Map<String, OfferDefinition> offers,
            SuiteCatalogService.PlanDefinition plan,
            String checkoutMode,
            int defaultSeatCount,
            boolean organizationRequired
    ) {
        PriceSnapshot priceSnapshot = CHECKOUT_MODE_SELF_SERVE.equals(checkoutMode)
                ? buildDerivedPriceSnapshot(plan.priceValue())
                : null;
        offers.put(plan.code(), new OfferDefinition(
                plan.code(),
                plan.name(),
                plan.description(),
                plan.segment(),
                plan.code(),
                checkoutMode,
                CHECKOUT_MODE_SELF_SERVE.equals(checkoutMode) ? PRICE_MODE_FROM : PRICE_MODE_CONTACT_SALES,
                List.of(BILLING_CYCLE_MONTHLY, BILLING_CYCLE_ANNUAL),
                BILLING_CYCLE_ANNUAL,
                defaultSeatCount,
                false,
                organizationRequired,
                plan.recommended(),
                null,
                plan.highlights(),
                plan.enabledProducts(),
                priceSnapshot
        ));
    }

    private OfferDefinition createPassPlusOffer() {
        return new OfferDefinition(
                PASS_PLUS,
                "Pass Plus",
                "Single-product password manager path aligned to Proton public checkout",
                SEGMENT_CONSUMER,
                PASS_PLUS,
                CHECKOUT_MODE_SELF_SERVE,
                PRICE_MODE_FROM,
                List.of(BILLING_CYCLE_MONTHLY, BILLING_CYCLE_ANNUAL),
                BILLING_CYCLE_ANNUAL,
                DEFAULT_SINGLE_SEAT,
                false,
                false,
                false,
                "40% OFF",
                List.of("Unlimited vaults and aliases", "Dark web monitoring posture", "Secure link sharing baseline"),
                List.of("PASS", "SIMPLELOGIN", "AUTHENTICATOR"),
                new PriceSnapshot(
                        CURRENCY_USD,
                        PASS_PLUS_MONTHLY_PRICE_CENTS,
                        PASS_PLUS_ANNUAL_PRICE_CENTS,
                        "$2.99",
                        "$4.99",
                        "Billed at $35.88 every 12 months"
                )
        );
    }

    private OfferDefinition createDrivePlusOffer() {
        return new OfferDefinition(
                DRIVE_PLUS,
                "Drive Plus",
                "Single-product Drive add-on aligned to Proton-style public checkout",
                SEGMENT_CONSUMER,
                DRIVE_PLUS,
                CHECKOUT_MODE_SELF_SERVE,
                PRICE_MODE_FROM,
                List.of(BILLING_CYCLE_MONTHLY, BILLING_CYCLE_ANNUAL),
                BILLING_CYCLE_ANNUAL,
                DEFAULT_SINGLE_SEAT,
                false,
                false,
                false,
                "20% OFF",
                List.of("200 GB storage quota", "Version history and share-link baseline", "Browser-first access and export posture"),
                List.of("DRIVE", "DOCS"),
                new PriceSnapshot(
                        CURRENCY_USD,
                        DRIVE_PLUS_MONTHLY_PRICE_CENTS,
                        DRIVE_PLUS_ANNUAL_PRICE_CENTS,
                        "$3.99",
                        "$4.99",
                        "Billed at $47.88 every 12 months"
                )
        );
    }

    private PriceSnapshot buildDerivedPriceSnapshot(String priceValue) {
        if (!StringUtils.hasText(priceValue)) {
            return null;
        }
        String trimmedValue = priceValue.trim();
        String currencyCode = trimmedValue.startsWith("€") ? CURRENCY_EUR : CURRENCY_USD;
        int annualUnitPriceCents = parseCurrencyCents(trimmedValue);
        return new PriceSnapshot(
                currencyCode,
                annualUnitPriceCents,
                annualUnitPriceCents,
                trimmedValue,
                null,
                buildAnnualNote(trimmedValue, annualUnitPriceCents)
        );
    }

    private String buildAnnualNote(String priceValue, int annualUnitPriceCents) {
        BigDecimal total = BigDecimal.valueOf((long) annualUnitPriceCents * MONTHS_PER_YEAR, 2);
        return "Billed at " + currencySymbol(priceValue) + total.setScale(2, RoundingMode.HALF_UP) + " every 12 months";
    }

    private String currencySymbol(String priceValue) {
        return priceValue.startsWith("€") ? "€" : "$";
    }

    private int parseCurrencyCents(String priceValue) {
        String normalized = priceValue.replace("€", "").replace("$", "").trim();
        BigDecimal value = new BigDecimal(normalized).movePointRight(2);
        return value.setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    private SuitePricingOfferVo toOfferVo(OfferDefinition offer) {
        PriceSnapshot priceSnapshot = offer.priceSnapshot();
        return new SuitePricingOfferVo(
                offer.code(),
                offer.name(),
                offer.description(),
                offer.segment(),
                offer.linkedPlanCode(),
                offer.checkoutMode(),
                offer.priceMode(),
                priceSnapshot == null ? null : priceSnapshot.currencyCode(),
                priceSnapshot == null ? null : priceSnapshot.displayPriceValue(),
                priceSnapshot == null ? null : priceSnapshot.originalPriceValue(),
                priceSnapshot == null ? null : priceSnapshot.billingNote(),
                offer.defaultBillingCycle(),
                offer.billingCycles(),
                offer.defaultSeatCount(),
                offer.seatEditable(),
                offer.organizationRequired(),
                offer.recommended(),
                offer.marketingBadge(),
                offer.highlights(),
                offer.enabledProducts()
        );
    }

    public record OfferDefinition(
            String code,
            String name,
            String description,
            String segment,
            String linkedPlanCode,
            String checkoutMode,
            String priceMode,
            List<String> billingCycles,
            String defaultBillingCycle,
            int defaultSeatCount,
            boolean seatEditable,
            boolean organizationRequired,
            boolean recommended,
            String marketingBadge,
            List<String> highlights,
            List<String> enabledProducts,
            PriceSnapshot priceSnapshot
    ) {
    }

    public record PriceSnapshot(
            String currencyCode,
            int monthlyUnitPriceCents,
            int annualUnitPriceCents,
            String displayPriceValue,
            String originalPriceValue,
            String billingNote
    ) {
    }
}
