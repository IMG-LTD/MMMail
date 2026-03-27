package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.model.vo.SuitePlanVo;
import com.mmmail.server.model.vo.SuiteProductStatusVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SuiteCatalogService {

    private static final String PLAN_FREE = "FREE";
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

    private static final String PRICE_MODE_FREE = "FREE";
    private static final String PRICE_MODE_FROM = "FROM";
    private static final String PRICE_MODE_PER_USER = "PER_USER";
    private static final String PRICE_MODE_CONTACT_SALES = "CONTACT_SALES";
    private static final String PRICE_MODE_ADD_ON = "ADD_ON";

    private static final String SEGMENT_CONSUMER = "CONSUMER";
    private static final String SEGMENT_BUSINESS = "BUSINESS";
    private static final String SEGMENT_SECURITY = "SECURITY";

    private final Map<String, PlanDefinition> planDefinitions = buildPlanDefinitions();
    private final Map<String, ProductDefinition> productDefinitions = buildProductDefinitions();

    public List<SuitePlanVo> listPlans() {
        return planDefinitions.values().stream().map(this::toPlanVo).toList();
    }

    public PlanDefinition requirePlan(String rawPlanCode) {
        String planCode = normalizePlanCode(rawPlanCode);
        PlanDefinition plan = planDefinitions.get(planCode);
        if (plan == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported plan code");
        }
        return plan;
    }

    public String defaultPlanCode() {
        return PLAN_FREE;
    }

    public String resolvePlanName(String rawPlanCode) {
        return requirePlan(rawPlanCode).name();
    }

    public boolean eligibleForMeetInstantAccess(String rawPlanCode) {
        return requirePlan(rawPlanCode).meetInstantAccess();
    }

    public List<SuiteProductStatusVo> listProductsForPlan(String rawPlanCode) {
        PlanDefinition plan = requirePlan(rawPlanCode);
        return productDefinitions.values().stream()
                .map(product -> toProductVo(product, plan))
                .toList();
    }

    public SuitePlanVo toPlanVo(PlanDefinition plan) {
        return new SuitePlanVo(
                plan.code(),
                plan.name(),
                plan.description(),
                plan.segment(),
                plan.priceMode(),
                plan.priceValue(),
                plan.recommended(),
                plan.highlights(),
                plan.upgradeTargets(),
                plan.mailDailySendLimit(),
                plan.contactLimit(),
                plan.calendarEventLimit(),
                plan.calendarShareLimit(),
                plan.driveStorageMb(),
                plan.enabledProducts()
        );
    }

    private SuiteProductStatusVo toProductVo(ProductDefinition product, PlanDefinition plan) {
        boolean enabledByPlan = plan.enabledProducts().contains(product.code());
        String status = enabledByPlan ? "ENABLED" : product.rolloutStatus();
        return new SuiteProductStatusVo(
                product.code(),
                product.name(),
                status,
                product.category(),
                product.description(),
                enabledByPlan,
                product.highlights()
        );
    }

    private String normalizePlanCode(String rawPlanCode) {
        if (!StringUtils.hasText(rawPlanCode)) {
            return PLAN_FREE;
        }
        return rawPlanCode.trim().toUpperCase();
    }

    private Map<String, PlanDefinition> buildPlanDefinitions() {
        Map<String, PlanDefinition> plans = new LinkedHashMap<>();

        plans.put(PLAN_FREE, new PlanDefinition(
                PLAN_FREE,
                "Free",
                "Entry plan for private essentials",
                SEGMENT_CONSUMER,
                PRICE_MODE_FREE,
                null,
                false,
                List.of("Mail, Calendar, and Drive essentials", "Starter password and VPN access", "Wallet and Docs preview"),
                List.of(PLAN_MAIL_PLUS, PLAN_UNLIMITED, PLAN_DUO, PLAN_FAMILY, PLAN_VISIONARY),
                120,
                1000,
                3,
                3,
                500,
                List.of("MAIL", "CALENDAR", "DRIVE", "DOCS", "PASS", "VPN", "WALLET"),
                false
        ));

        plans.put(PLAN_MAIL_PLUS, new PlanDefinition(
                PLAN_MAIL_PLUS,
                "Mail Plus",
                "Focused mail plan for individual productivity",
                SEGMENT_CONSUMER,
                PRICE_MODE_FROM,
                "€3.99",
                false,
                List.of("Higher send and contact quota", "Custom folders, labels, and aliases", "Expanded Drive and passkey support"),
                List.of(PLAN_UNLIMITED, PLAN_DUO, PLAN_FAMILY, PLAN_VISIONARY),
                600,
                5000,
                50,
                80,
                15000,
                List.of("MAIL", "CALENDAR", "CONTACTS", "DRIVE", "DOCS", "PASS", "SIMPLELOGIN", "STANDARD_NOTES"),
                false
        ));

        plans.put(PLAN_DUO, new PlanDefinition(
                PLAN_DUO,
                "Proton Duo",
                "Two-account privacy bundle",
                SEGMENT_CONSUMER,
                PRICE_MODE_FROM,
                "€14.99",
                false,
                List.of("Two seats across premium apps", "Lumo, Pass, VPN, and Wallet in one bundle", "Shared household-style privacy baseline"),
                List.of(PLAN_FAMILY, PLAN_VISIONARY),
                1200,
                10000,
                200,
                240,
                100000,
                List.of("MAIL", "CALENDAR", "CONTACTS", "DRIVE", "DOCS", "SHEETS", "PASS", "VPN", "WALLET", "LUMO", "AUTHENTICATOR", "SIMPLELOGIN", "STANDARD_NOTES"),
                true
        ));

        plans.put(PLAN_FAMILY, new PlanDefinition(
                PLAN_FAMILY,
                "Proton Family",
                "Household bundle for up to six members",
                SEGMENT_CONSUMER,
                PRICE_MODE_FROM,
                "€23.99",
                false,
                List.of("Shared family storage posture", "Multi-member access across core privacy apps", "Meet, Wallet, and Lumo ready"),
                List.of(PLAN_VISIONARY),
                2000,
                20000,
                400,
                480,
                300000,
                List.of("MAIL", "CALENDAR", "CONTACTS", "DRIVE", "DOCS", "SHEETS", "PASS", "VPN", "WALLET", "LUMO", "AUTHENTICATOR", "MEET", "SIMPLELOGIN", "STANDARD_NOTES"),
                true
        ));

        plans.put(PLAN_UNLIMITED, new PlanDefinition(
                PLAN_UNLIMITED,
                "Proton Unlimited",
                "Cross-product premium plan for one user",
                SEGMENT_CONSUMER,
                PRICE_MODE_FROM,
                "€9.99",
                true,
                List.of("Mail, VPN, Pass, Drive, and Wallet together", "Sheets, Docs, and Lumo in one seat", "Best-fit premium plan for individual parity"),
                List.of(PLAN_DUO, PLAN_FAMILY, PLAN_VISIONARY),
                2000,
                20000,
                2000,
                5000,
                500000,
                List.of("MAIL", "CALENDAR", "CONTACTS", "DRIVE", "DOCS", "SHEETS", "PASS", "VPN", "WALLET", "LUMO", "AUTHENTICATOR", "MEET", "SIMPLELOGIN", "STANDARD_NOTES"),
                true
        ));

        plans.put(PLAN_VISIONARY, new PlanDefinition(
                PLAN_VISIONARY,
                "Proton Visionary",
                "All-in bundle with legacy premium perks",
                SEGMENT_CONSUMER,
                PRICE_MODE_FROM,
                "€29.99",
                false,
                List.of("Broadest consumer product coverage", "High quota ceilings and premium bundle access", "Closest public tier to full Proton surface"),
                List.of(),
                4000,
                40000,
                4000,
                8000,
                1000000,
                List.of("MAIL", "CALENDAR", "CONTACTS", "DRIVE", "DOCS", "SHEETS", "PASS", "VPN", "WALLET", "LUMO", "AUTHENTICATOR", "MEET", "SIMPLELOGIN", "STANDARD_NOTES"),
                true
        ));

        plans.put(PLAN_MAIL_ESSENTIALS, new PlanDefinition(
                PLAN_MAIL_ESSENTIALS,
                "Mail Essentials",
                "Business mail baseline for teams",
                SEGMENT_BUSINESS,
                PRICE_MODE_PER_USER,
                null,
                false,
                List.of("Custom domains and team calendars", "Per-user business mailbox baseline", "Clean step-up from free or personal mail tiers"),
                List.of(PLAN_MAIL_PROFESSIONAL, PLAN_BUSINESS_SUITE, PLAN_ENTERPRISE),
                1500,
                25000,
                1500,
                1500,
                25000,
                List.of("MAIL", "CALENDAR", "CONTACTS"),
                false
        ));

        plans.put(PLAN_MAIL_PROFESSIONAL, new PlanDefinition(
                PLAN_MAIL_PROFESSIONAL,
                "Mail Professional",
                "Advanced mail stack for custom domains",
                SEGMENT_BUSINESS,
                PRICE_MODE_PER_USER,
                null,
                false,
                List.of("More storage and stronger admin posture", "Drive and Docs ready for business mail teams", "Fits domain-heavy collaboration rollouts"),
                List.of(PLAN_BUSINESS_SUITE, PLAN_ENTERPRISE),
                2500,
                50000,
                3000,
                3000,
                100000,
                List.of("MAIL", "CALENDAR", "CONTACTS", "DRIVE", "DOCS"),
                false
        ));

        plans.put(PLAN_BUSINESS_SUITE, new PlanDefinition(
                PLAN_BUSINESS_SUITE,
                "Proton Business Suite",
                "Recommended business bundle across communication and security",
                SEGMENT_BUSINESS,
                PRICE_MODE_PER_USER,
                null,
                true,
                List.of("Mail, Drive, VPN, Pass, and Lumo together", "Meet, Sheets, and Authenticator ready for rollout", "Closest B2B parity surface in MMMail"),
                List.of(PLAN_ENTERPRISE),
                4000,
                80000,
                6000,
                9000,
                750000,
                List.of("MAIL", "CALENDAR", "CONTACTS", "DRIVE", "DOCS", "SHEETS", "PASS", "VPN", "LUMO", "AUTHENTICATOR", "MEET", "SIMPLELOGIN", "STANDARD_NOTES"),
                true
        ));

        plans.put(PLAN_ENTERPRISE, new PlanDefinition(
                PLAN_ENTERPRISE,
                "Enterprise",
                "Custom-scale deployment with sales onboarding",
                SEGMENT_BUSINESS,
                PRICE_MODE_CONTACT_SALES,
                null,
                false,
                List.of("Custom rollout and procurement path", "Broadest business capability matrix", "Reserved for highest-control deployments"),
                List.of(),
                10000,
                200000,
                20000,
                20000,
                2000000,
                List.of("MAIL", "CALENDAR", "CONTACTS", "DRIVE", "DOCS", "SHEETS", "PASS", "VPN", "WALLET", "LUMO", "AUTHENTICATOR", "MEET", "SIMPLELOGIN", "STANDARD_NOTES"),
                true
        ));

        plans.put(PLAN_VPN_PROFESSIONAL, new PlanDefinition(
                PLAN_VPN_PROFESSIONAL,
                "VPN Professional",
                "Team privacy network controls",
                SEGMENT_SECURITY,
                PRICE_MODE_PER_USER,
                null,
                false,
                List.of("Dedicated privacy network baseline", "Team routing and secure-core posture", "Security-first entry for business admins"),
                List.of(PLAN_VPN_PASS_PROFESSIONAL, PLAN_SENTINEL),
                120,
                500,
                50,
                50,
                1000,
                List.of("VPN", "AUTHENTICATOR"),
                false
        ));

        plans.put(PLAN_VPN_PASS_PROFESSIONAL, new PlanDefinition(
                PLAN_VPN_PASS_PROFESSIONAL,
                "VPN and Pass Professional",
                "Combined access for network and credential security",
                SEGMENT_SECURITY,
                PRICE_MODE_PER_USER,
                null,
                true,
                List.of("VPN and credential hardening in one plan", "Pass sharing, aliases, and network controls", "Recommended add-on security stack"),
                List.of(PLAN_SENTINEL),
                240,
                1000,
                100,
                100,
                5000,
                List.of("VPN", "PASS", "AUTHENTICATOR"),
                false
        ));

        plans.put(PLAN_SENTINEL, new PlanDefinition(
                PLAN_SENTINEL,
                "Proton Sentinel",
                "Maximum protection add-on for higher-risk accounts",
                SEGMENT_SECURITY,
                PRICE_MODE_ADD_ON,
                null,
                false,
                List.of("Advanced account protection posture", "Fits higher-risk operator accounts", "Stacks on top of premium business access"),
                List.of(),
                120,
                500,
                50,
                50,
                1000,
                List.of("MAIL", "VPN", "AUTHENTICATOR"),
                false
        ));

        return Collections.unmodifiableMap(plans);
    }

    private Map<String, ProductDefinition> buildProductDefinitions() {
        Map<String, ProductDefinition> products = new LinkedHashMap<>();

        products.put("MAIL", new ProductDefinition("MAIL", "Mail", "Communication", "Secure mailbox and delivery workflows", "ROADMAP", List.of("Inbox", "Compose", "Conversations")));
        products.put("CALENDAR", new ProductDefinition("CALENDAR", "Calendar", "Scheduling", "Shared planning and event collaboration", "ROADMAP", List.of("Events", "Attendees", "Shares")));
        products.put("CONTACTS", new ProductDefinition("CONTACTS", "Contacts", "Communication", "People directory and contact groups", "ROADMAP", List.of("Favorites", "Groups", "CSV Import")));
        products.put("DRIVE", new ProductDefinition("DRIVE", "Drive", "Storage", "Encrypted file storage workspace", "COMING_SOON", List.of("Storage", "Share Links")));
        products.put("DOCS", new ProductDefinition("DOCS", "Docs", "Collaboration", "Collaborative document workspace", "COMING_SOON", List.of("Docs", "Comments", "Versioning")));
        products.put("SHEETS", new ProductDefinition("SHEETS", "Sheets", "Collaboration", "Collaborative spreadsheet workspace", "PREVIEW", List.of("Tables", "Grid editing", "Filters")));
        products.put("PASS", new ProductDefinition("PASS", "Pass", "Security", "Password, alias, and secure link management", "COMING_SOON", List.of("Vault", "Autofill", "Aliases")));
        products.put("SIMPLELOGIN", new ProductDefinition("SIMPLELOGIN", "SimpleLogin", "Communication", "Alias relay and mailbox routing workspace", "PREVIEW", List.of("Aliases", "Mailboxes", "Domains")));
        products.put("STANDARD_NOTES", new ProductDefinition("STANDARD_NOTES", "Standard Notes", "Productivity", "Private notes workspace with archive flows", "PREVIEW", List.of("Notes", "Tags", "Archive")));
        products.put("AUTHENTICATOR", new ProductDefinition("AUTHENTICATOR", "Authenticator", "Security", "Two-factor identity companion", "ROADMAP", List.of("TOTP", "Recovery", "Portability")));
        products.put("VPN", new ProductDefinition("VPN", "VPN", "Network", "Privacy network access", "ROADMAP", List.of("Servers", "Profiles", "Policies")));
        products.put("WALLET", new ProductDefinition("WALLET", "Wallet", "Finance", "Privacy wallet capability track", "PREVIEW", List.of("Accounts", "Address book", "Transfers")));
        products.put("LUMO", new ProductDefinition("LUMO", "Lumo", "AI", "Privacy-first AI workspace capability", "PREVIEW", List.of("Assist", "Search", "Translate")));
        products.put("MEET", new ProductDefinition("MEET", "Meet", "Communication", "Secure meeting and live collaboration", "PREVIEW", List.of("Video", "Rooms", "Guest access")));

        return Collections.unmodifiableMap(products);
    }

    public record PlanDefinition(
            String code,
            String name,
            String description,
            String segment,
            String priceMode,
            String priceValue,
            boolean recommended,
            List<String> highlights,
            List<String> upgradeTargets,
            int mailDailySendLimit,
            int contactLimit,
            int calendarEventLimit,
            int calendarShareLimit,
            int driveStorageMb,
            List<String> enabledProducts,
            boolean meetInstantAccess
    ) {
    }

    private record ProductDefinition(
            String code,
            String name,
            String category,
            String description,
            String rolloutStatus,
            List<String> highlights
    ) {
    }
}
