package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.SuiteCheckoutDraftMapper;
import com.mmmail.server.mapper.SuiteSubscriptionMapper;
import com.mmmail.server.model.dto.CreateSuiteBillingQuoteRequest;
import com.mmmail.server.model.dto.CreateSuiteCheckoutDraftRequest;
import com.mmmail.server.model.entity.SuiteCheckoutDraft;
import com.mmmail.server.model.entity.SuiteSubscription;
import com.mmmail.server.model.vo.SuiteBillingOverviewVo;
import com.mmmail.server.model.vo.SuiteBillingQuoteVo;
import com.mmmail.server.model.vo.SuiteCheckoutDraftVo;
import com.mmmail.server.model.vo.SuiteEntitlementSummaryVo;
import com.mmmail.server.model.vo.SuiteInvoiceLineVo;
import com.mmmail.server.model.vo.SuiteInvoiceSummaryVo;
import com.mmmail.server.model.vo.SuiteOnboardingSummaryVo;
import com.mmmail.server.model.vo.SuitePricingOfferVo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SuiteBillingService {

    private static final String SUBSCRIPTION_STATUS_ACTIVE = "ACTIVE";
    private static final String QUOTE_STATUS_READY = "READY";
    private static final String QUOTE_STATUS_CONTACT_SALES = "CONTACT_SALES";
    private static final int MONTHS_IN_YEAR = 12;
    private static final int MAX_ORGANIZATION_NAME = 80;
    private static final int MAX_DOMAIN_NAME = 120;

    private final SuiteSubscriptionMapper suiteSubscriptionMapper;
    private final SuiteCheckoutDraftMapper suiteCheckoutDraftMapper;
    private final SuiteCatalogService suiteCatalogService;
    private final SuitePricingCatalogService suitePricingCatalogService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public SuiteBillingService(
            SuiteSubscriptionMapper suiteSubscriptionMapper,
            SuiteCheckoutDraftMapper suiteCheckoutDraftMapper,
            SuiteCatalogService suiteCatalogService,
            SuitePricingCatalogService suitePricingCatalogService,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.suiteSubscriptionMapper = suiteSubscriptionMapper;
        this.suiteCheckoutDraftMapper = suiteCheckoutDraftMapper;
        this.suiteCatalogService = suiteCatalogService;
        this.suitePricingCatalogService = suitePricingCatalogService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    public List<SuitePricingOfferVo> listPricingOffers(Long userId, String ipAddress) {
        List<SuitePricingOfferVo> offers = suitePricingCatalogService.listOffers();
        auditService.record(userId, "SUITE_PRICING_OFFER_LIST", "count=" + offers.size(), ipAddress);
        return offers;
    }

    public SuiteBillingOverviewVo getBillingOverview(Long userId, String ipAddress) {
        SuiteSubscription subscription = ensureSubscription(userId);
        SuiteCheckoutDraft draft = findDraft(userId);
        SuiteBillingOverviewVo overview = new SuiteBillingOverviewVo(
                subscription.getPlanCode(),
                suiteCatalogService.resolvePlanName(subscription.getPlanCode()),
                draft == null ? null : toDraftVo(draft),
                suitePricingCatalogService.listSelfServeOfferCodes(),
                suitePricingCatalogService.listContactSalesOfferCodes()
        );
        auditService.record(userId, "SUITE_BILLING_OVERVIEW", "plan=" + overview.activePlanCode(), ipAddress);
        return overview;
    }

    public SuiteBillingQuoteVo createQuote(Long userId, CreateSuiteBillingQuoteRequest request, String ipAddress) {
        SuiteBillingQuoteVo quote = buildQuote(
                suitePricingCatalogService.requireOffer(request.offerCode()),
                request.billingCycle(),
                request.seatCount()
        );
        auditService.record(
                userId,
                "SUITE_BILLING_QUOTE",
                "offer=" + quote.offerCode() + ",cycle=" + quote.billingCycle() + ",seats=" + quote.seatCount(),
                ipAddress
        );
        return quote;
    }

    @Transactional
    public SuiteCheckoutDraftVo saveCheckoutDraft(Long userId, CreateSuiteCheckoutDraftRequest request, String ipAddress) {
        SuiteBillingQuoteVo quote = buildQuote(
                suitePricingCatalogService.requireOffer(request.offerCode()),
                request.billingCycle(),
                request.seatCount()
        );
        LocalDateTime now = LocalDateTime.now();
        SuiteCheckoutDraft draft = upsertDraft(
                userId,
                quote,
                trimToNull(request.organizationName(), MAX_ORGANIZATION_NAME),
                trimToNull(request.domainName(), MAX_DOMAIN_NAME),
                now
        );
        auditService.record(
                userId,
                "SUITE_CHECKOUT_DRAFT_SAVE",
                "offer=" + quote.offerCode() + ",status=" + quote.quoteStatus(),
                ipAddress
        );
        return toDraftVo(draft);
    }

    private SuiteBillingQuoteVo buildQuote(
            SuitePricingCatalogService.OfferDefinition offer,
            String rawBillingCycle,
            Integer rawSeatCount
    ) {
        String billingCycle = normalizeBillingCycle(rawBillingCycle, offer.billingCycles());
        int seatCount = normalizeSeatCount(rawSeatCount, offer);
        SuiteEntitlementSummaryVo entitlementSummary = buildEntitlementSummary(offer, seatCount);
        SuiteOnboardingSummaryVo onboardingSummary = buildOnboardingSummary(offer);
        if (SuitePricingCatalogService.CHECKOUT_MODE_CONTACT_SALES.equals(offer.checkoutMode())) {
            return new SuiteBillingQuoteVo(
                    offer.code(),
                    offer.name(),
                    QUOTE_STATUS_CONTACT_SALES,
                    offer.checkoutMode(),
                    null,
                    billingCycle,
                    seatCount,
                    offer.marketingBadge(),
                    null,
                    entitlementSummary,
                    onboardingSummary
            );
        }
        SuiteInvoiceSummaryVo invoiceSummary = buildInvoiceSummary(offer, billingCycle, seatCount);
        return new SuiteBillingQuoteVo(
                offer.code(),
                offer.name(),
                QUOTE_STATUS_READY,
                offer.checkoutMode(),
                invoiceSummary.currencyCode(),
                billingCycle,
                seatCount,
                offer.marketingBadge(),
                invoiceSummary,
                entitlementSummary,
                onboardingSummary
        );
    }

    private SuiteInvoiceSummaryVo buildInvoiceSummary(
            SuitePricingCatalogService.OfferDefinition offer,
            String billingCycle,
            int seatCount
    ) {
        SuitePricingCatalogService.PriceSnapshot priceSnapshot = offer.priceSnapshot();
        if (priceSnapshot == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Selected offer requires sales-assisted checkout");
        }
        int billingMonths = resolveBillingMonths(billingCycle);
        long unitPriceCents = SuitePricingCatalogService.BILLING_CYCLE_MONTHLY.equals(billingCycle)
                ? priceSnapshot.monthlyUnitPriceCents()
                : priceSnapshot.annualUnitPriceCents();
        long quantity = (long) seatCount * billingMonths;
        long subtotalCents = quantity * unitPriceCents;
        long discountCents = resolveDiscountCents(priceSnapshot, seatCount, billingCycle);
        long totalCents = subtotalCents;
        return new SuiteInvoiceSummaryVo(
                priceSnapshot.currencyCode(),
                billingCycle,
                seatCount,
                billingMonths,
                subtotalCents + discountCents,
                discountCents,
                totalCents,
                List.of(new SuiteInvoiceLineVo("PRIMARY_OFFER", (int) quantity, unitPriceCents, subtotalCents))
        );
    }

    private long resolveDiscountCents(
            SuitePricingCatalogService.PriceSnapshot priceSnapshot,
            int seatCount,
            String billingCycle
    ) {
        if (!SuitePricingCatalogService.BILLING_CYCLE_ANNUAL.equals(billingCycle)) {
            return 0L;
        }
        long monthlyBaseline = (long) priceSnapshot.monthlyUnitPriceCents() * MONTHS_IN_YEAR * seatCount;
        long annualEquivalent = (long) priceSnapshot.annualUnitPriceCents() * MONTHS_IN_YEAR * seatCount;
        return Math.max(0L, monthlyBaseline - annualEquivalent);
    }

    private SuiteEntitlementSummaryVo buildEntitlementSummary(
            SuitePricingCatalogService.OfferDefinition offer,
            int seatCount
    ) {
        return new SuiteEntitlementSummaryVo(
                offer.code(),
                offer.linkedPlanCode(),
                offer.enabledProducts().isEmpty() ? null : offer.enabledProducts().getFirst(),
                resolveSupportTier(offer),
                resolveWorkspaceMode(offer),
                seatCount,
                offer.recommended(),
                offer.enabledProducts(),
                offer.highlights()
        );
    }

    private SuiteOnboardingSummaryVo buildOnboardingSummary(SuitePricingCatalogService.OfferDefinition offer) {
        if (SuitePricingCatalogService.CHECKOUT_MODE_CONTACT_SALES.equals(offer.checkoutMode())) {
            return new SuiteOnboardingSummaryVo(
                    "CONTACT_SALES",
                    "REQUEST_SALES_CONTACT",
                    true,
                    List.of("SUBMIT_ORG_PROFILE", "VERIFY_DOMAIN", "DEFINE_SEAT_PLAN", "ALIGN_PROCUREMENT")
            );
        }
        return new SuiteOnboardingSummaryVo(
                "SELF_SERVE",
                "START_CHECKOUT",
                offer.organizationRequired(),
                resolveSelfServeChecklist(offer)
        );
    }

    private List<String> resolveSelfServeChecklist(SuitePricingCatalogService.OfferDefinition offer) {
        if ("PASS_PLUS".equals(offer.code())) {
            return List.of("VERIFY_ACCOUNT", "IMPORT_PASSWORDS", "SET_ALIAS_POLICY", "ENABLE_2FA");
        }
        if ("DRIVE_PLUS".equals(offer.code())) {
            return List.of("VERIFY_ACCOUNT", "UPLOAD_FILES", "ENABLE_BACKUP", "CONFIGURE_VERSIONING");
        }
        if ("DUO".equals(offer.code())) {
            return List.of("VERIFY_ACCOUNT", "INVITE_SECOND_MEMBER", "ENABLE_SHARED_VAULTS");
        }
        if ("FAMILY".equals(offer.code())) {
            return List.of("VERIFY_ACCOUNT", "INVITE_FAMILY", "SET_STORAGE_POLICY");
        }
        return List.of("VERIFY_ACCOUNT", "CONFIRM_PLAN", "OPEN_WORKSPACE");
    }

    private String resolveSupportTier(SuitePricingCatalogService.OfferDefinition offer) {
        if (SuitePricingCatalogService.CHECKOUT_MODE_CONTACT_SALES.equals(offer.checkoutMode())) {
            return "BUSINESS";
        }
        if ("VISIONARY".equals(offer.code()) || "UNLIMITED".equals(offer.code())) {
            return "PRIORITY";
        }
        return "STANDARD";
    }

    private String resolveWorkspaceMode(SuitePricingCatalogService.OfferDefinition offer) {
        if (offer.organizationRequired()) {
            return "BUSINESS";
        }
        if (offer.defaultSeatCount() > 1) {
            return "GROUP";
        }
        return "PERSONAL";
    }

    private int normalizeSeatCount(Integer rawSeatCount, SuitePricingCatalogService.OfferDefinition offer) {
        if (rawSeatCount == null || rawSeatCount < 1) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Seat count is required");
        }
        if (!offer.seatEditable() && rawSeatCount != offer.defaultSeatCount()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Selected offer has a fixed seat count");
        }
        return rawSeatCount;
    }

    private String normalizeBillingCycle(String rawBillingCycle, List<String> allowedBillingCycles) {
        if (!StringUtils.hasText(rawBillingCycle)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Billing cycle is required");
        }
        String normalized = rawBillingCycle.trim().toUpperCase();
        if (!allowedBillingCycles.contains(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported billing cycle");
        }
        return normalized;
    }

    private int resolveBillingMonths(String billingCycle) {
        return SuitePricingCatalogService.BILLING_CYCLE_ANNUAL.equals(billingCycle) ? MONTHS_IN_YEAR : 1;
    }

    private SuiteCheckoutDraft upsertDraft(
            Long userId,
            SuiteBillingQuoteVo quote,
            String organizationName,
            String domainName,
            LocalDateTime now
    ) {
        SuiteCheckoutDraft existing = findDraft(userId);
        SuiteCheckoutDraft target = existing == null ? new SuiteCheckoutDraft() : existing;
        target.setOwnerId(userId);
        target.setOfferCode(quote.offerCode());
        target.setOfferName(quote.offerName());
        target.setQuoteStatus(quote.quoteStatus());
        target.setCheckoutMode(quote.checkoutMode());
        target.setCurrencyCode(quote.currencyCode());
        target.setBillingCycle(quote.billingCycle());
        target.setSeatCount(quote.seatCount());
        target.setOrganizationName(organizationName);
        target.setDomainName(domainName);
        target.setMarketingBadge(quote.marketingBadge());
        target.setInvoiceSummaryJson(writeJson(quote.invoiceSummary()));
        target.setEntitlementSummaryJson(writeJson(quote.entitlementSummary()));
        target.setOnboardingSummaryJson(writeJson(quote.onboardingSummary()));
        target.setUpdatedAt(now);
        target.setDeleted(0);
        if (existing != null) {
            suiteCheckoutDraftMapper.updateById(target);
            return target;
        }
        target.setCreatedAt(now);
        try {
            suiteCheckoutDraftMapper.insert(target);
            return target;
        } catch (DuplicateKeyException ignored) {
            SuiteCheckoutDraft current = findDraft(userId);
            if (current == null) {
                throw ignored;
            }
            current.setOfferCode(target.getOfferCode());
            current.setOfferName(target.getOfferName());
            current.setQuoteStatus(target.getQuoteStatus());
            current.setCheckoutMode(target.getCheckoutMode());
            current.setCurrencyCode(target.getCurrencyCode());
            current.setBillingCycle(target.getBillingCycle());
            current.setSeatCount(target.getSeatCount());
            current.setOrganizationName(target.getOrganizationName());
            current.setDomainName(target.getDomainName());
            current.setMarketingBadge(target.getMarketingBadge());
            current.setInvoiceSummaryJson(target.getInvoiceSummaryJson());
            current.setEntitlementSummaryJson(target.getEntitlementSummaryJson());
            current.setOnboardingSummaryJson(target.getOnboardingSummaryJson());
            current.setUpdatedAt(now);
            suiteCheckoutDraftMapper.updateById(current);
            return current;
        }
    }

    private SuiteCheckoutDraftVo toDraftVo(SuiteCheckoutDraft draft) {
        return new SuiteCheckoutDraftVo(
                draft.getOfferCode(),
                draft.getOfferName(),
                draft.getQuoteStatus(),
                draft.getCheckoutMode(),
                draft.getCurrencyCode(),
                draft.getBillingCycle(),
                draft.getSeatCount(),
                draft.getMarketingBadge(),
                draft.getOrganizationName(),
                draft.getDomainName(),
                readJson(draft.getInvoiceSummaryJson(), SuiteInvoiceSummaryVo.class),
                readJson(draft.getEntitlementSummaryJson(), SuiteEntitlementSummaryVo.class),
                readJson(draft.getOnboardingSummaryJson(), SuiteOnboardingSummaryVo.class),
                draft.getUpdatedAt()
        );
    }

    private <T> T readJson(String json, Class<T> type) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to parse suite billing snapshot");
        }
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to persist suite billing snapshot");
        }
    }

    private String trimToNull(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private SuiteCheckoutDraft findDraft(Long userId) {
        return suiteCheckoutDraftMapper.selectOne(new LambdaQueryWrapper<SuiteCheckoutDraft>()
                .eq(SuiteCheckoutDraft::getOwnerId, userId));
    }

    private SuiteSubscription ensureSubscription(Long userId) {
        SuiteSubscription existing = suiteSubscriptionMapper.selectOne(new LambdaQueryWrapper<SuiteSubscription>()
                .eq(SuiteSubscription::getOwnerId, userId));
        if (existing != null) {
            return existing;
        }
        LocalDateTime now = LocalDateTime.now();
        SuiteSubscription created = new SuiteSubscription();
        created.setOwnerId(userId);
        created.setPlanCode(suiteCatalogService.defaultPlanCode());
        created.setStatus(SUBSCRIPTION_STATUS_ACTIVE);
        created.setCreatedAt(now);
        created.setUpdatedAt(now);
        created.setDeleted(0);
        try {
            suiteSubscriptionMapper.insert(created);
            return created;
        } catch (DuplicateKeyException ignored) {
            return suiteSubscriptionMapper.selectOne(new LambdaQueryWrapper<SuiteSubscription>()
                    .eq(SuiteSubscription::getOwnerId, userId));
        }
    }
}
