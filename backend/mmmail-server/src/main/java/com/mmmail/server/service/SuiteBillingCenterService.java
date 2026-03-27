package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.SuiteBillingInvoiceMapper;
import com.mmmail.server.mapper.SuiteBillingPaymentMethodMapper;
import com.mmmail.server.mapper.SuiteBillingSubscriptionStateMapper;
import com.mmmail.server.mapper.SuiteCheckoutDraftMapper;
import com.mmmail.server.mapper.SuiteSubscriptionMapper;
import com.mmmail.server.model.dto.CreateSuiteBillingPaymentMethodRequest;
import com.mmmail.server.model.dto.ExecuteSuiteBillingSubscriptionActionRequest;
import com.mmmail.server.model.dto.SetDefaultSuiteBillingPaymentMethodRequest;
import com.mmmail.server.model.entity.SuiteBillingInvoice;
import com.mmmail.server.model.entity.SuiteBillingPaymentMethod;
import com.mmmail.server.model.entity.SuiteBillingSubscriptionState;
import com.mmmail.server.model.entity.SuiteCheckoutDraft;
import com.mmmail.server.model.entity.SuiteSubscription;
import com.mmmail.server.model.vo.SuiteBillingCenterVo;
import com.mmmail.server.model.vo.SuiteBillingInvoiceVo;
import com.mmmail.server.model.vo.SuiteBillingPaymentMethodVo;
import com.mmmail.server.model.vo.SuiteBillingSubscriptionActionVo;
import com.mmmail.server.model.vo.SuiteBillingSubscriptionSummaryVo;
import com.mmmail.server.model.vo.SuiteInvoiceSummaryVo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class SuiteBillingCenterService {

    static final String ACTION_APPLY_LATEST_DRAFT = "APPLY_LATEST_DRAFT";
    static final String ACTION_CANCEL_AUTO_RENEW = "CANCEL_AUTO_RENEW";
    static final String ACTION_RESUME_AUTO_RENEW = "RESUME_AUTO_RENEW";
    private static final String METHOD_STATUS_ACTIVE = "ACTIVE";
    private static final String INVOICE_STATUS_PENDING = "PENDING";
    private static final String ACTION_STATUS_AVAILABLE = "AVAILABLE";
    private static final String ACTION_STATUS_LOCKED = "LOCKED";
    private static final String ACTION_STATUS_SCHEDULED = "SCHEDULED";
    private static final String PLAN_CODE_FREE = "FREE";
    private static final int DEFAULT_SEAT_COUNT = 1;
    private static final int CURRENT_PERIOD_YEARS = 1;
    private static final int DRAFT_ACTIVATION_MINUTES = 5;
    private static final int INVOICE_DUE_DAYS = 1;

    private final SuiteBillingPaymentMethodMapper paymentMethodMapper;
    private final SuiteBillingInvoiceMapper invoiceMapper;
    private final SuiteBillingSubscriptionStateMapper subscriptionStateMapper;
    private final SuiteCheckoutDraftMapper checkoutDraftMapper;
    private final SuiteSubscriptionMapper suiteSubscriptionMapper;
    private final SuiteCatalogService suiteCatalogService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public SuiteBillingCenterService(
            SuiteBillingPaymentMethodMapper paymentMethodMapper,
            SuiteBillingInvoiceMapper invoiceMapper,
            SuiteBillingSubscriptionStateMapper subscriptionStateMapper,
            SuiteCheckoutDraftMapper checkoutDraftMapper,
            SuiteSubscriptionMapper suiteSubscriptionMapper,
            SuiteCatalogService suiteCatalogService,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.paymentMethodMapper = paymentMethodMapper;
        this.invoiceMapper = invoiceMapper;
        this.subscriptionStateMapper = subscriptionStateMapper;
        this.checkoutDraftMapper = checkoutDraftMapper;
        this.suiteSubscriptionMapper = suiteSubscriptionMapper;
        this.suiteCatalogService = suiteCatalogService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    public SuiteBillingCenterVo getBillingCenter(Long userId, String ipAddress) {
        SuiteSubscription subscription = ensureSubscription(userId);
        SuiteBillingSubscriptionState state = ensureSubscriptionState(userId, subscription.getPlanCode());
        SuiteBillingCenterVo center = buildCenter(userId, subscription, state);
        auditService.record(userId, "SUITE_BILLING_CENTER_VIEW", "plan=" + subscription.getPlanCode(), ipAddress);
        return center;
    }

    @Transactional
    public SuiteBillingCenterVo addPaymentMethod(
            Long userId,
            CreateSuiteBillingPaymentMethodRequest request,
            String ipAddress
    ) {
        validatePaymentMethodRequest(request);
        List<SuiteBillingPaymentMethod> currentMethods = listPaymentMethodEntities(userId);
        SuiteBillingPaymentMethod paymentMethod = buildPaymentMethod(userId, request, currentMethods.isEmpty());
        if (isDefaultRequest(request.makeDefault(), currentMethods.isEmpty())) {
            clearDefaultFlags(userId);
        }
        paymentMethodMapper.insert(paymentMethod);
        syncDefaultPaymentMethod(userId, paymentMethod.getId(), paymentMethod.getIsDefault() == 1);
        auditService.record(userId, "SUITE_BILLING_PAYMENT_METHOD_ADD", paymentMethod.getMethodType(), ipAddress);
        return getBillingCenter(userId, ipAddress);
    }

    @Transactional
    public SuiteBillingCenterVo setDefaultPaymentMethod(
            Long userId,
            SetDefaultSuiteBillingPaymentMethodRequest request,
            String ipAddress
    ) {
        SuiteBillingPaymentMethod target = requirePaymentMethod(userId, request.paymentMethodId());
        clearDefaultFlags(userId);
        target.setIsDefault(1);
        target.setUpdatedAt(LocalDateTime.now());
        paymentMethodMapper.updateById(target);
        syncDefaultPaymentMethod(userId, target.getId(), true);
        auditService.record(userId, "SUITE_BILLING_PAYMENT_METHOD_DEFAULT", String.valueOf(target.getId()), ipAddress);
        return getBillingCenter(userId, ipAddress);
    }

    @Transactional
    public SuiteBillingCenterVo executeSubscriptionAction(
            Long userId,
            ExecuteSuiteBillingSubscriptionActionRequest request,
            String ipAddress
    ) {
        String actionCode = request.actionCode().trim().toUpperCase();
        SuiteSubscription subscription = ensureSubscription(userId);
        SuiteBillingSubscriptionState state = ensureSubscriptionState(userId, subscription.getPlanCode());
        switch (actionCode) {
            case ACTION_APPLY_LATEST_DRAFT -> applyLatestDraft(userId, subscription, state);
            case ACTION_CANCEL_AUTO_RENEW -> setAutoRenew(state, false);
            case ACTION_RESUME_AUTO_RENEW -> setAutoRenew(state, true);
            default -> throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported subscription action");
        }
        state.setUpdatedAt(LocalDateTime.now());
        subscriptionStateMapper.updateById(state);
        auditService.record(userId, "SUITE_BILLING_SUBSCRIPTION_ACTION", actionCode, ipAddress);
        return getBillingCenter(userId, ipAddress);
    }

    private SuiteBillingCenterVo buildCenter(Long userId, SuiteSubscription subscription, SuiteBillingSubscriptionState state) {
        List<SuiteBillingPaymentMethod> paymentMethods = listPaymentMethodEntities(userId);
        List<SuiteBillingInvoice> invoices = listInvoiceEntities(userId);
        SuiteCheckoutDraft latestDraft = findDraft(userId);
        SuiteBillingSubscriptionSummaryVo summary = buildSubscriptionSummary(subscription, state, paymentMethods);
        List<SuiteBillingSubscriptionActionVo> actions = buildActions(subscription, state, latestDraft, paymentMethods);
        return new SuiteBillingCenterVo(
                summary,
                paymentMethods.stream().map(this::toPaymentMethodVo).toList(),
                invoices.stream().map(this::toInvoiceVo).toList(),
                actions
        );
    }

    private SuiteBillingSubscriptionSummaryVo buildSubscriptionSummary(
            SuiteSubscription subscription,
            SuiteBillingSubscriptionState state,
            List<SuiteBillingPaymentMethod> paymentMethods
    ) {
        String defaultLabel = paymentMethods.stream()
                .filter(method -> method.getId().equals(state.getDefaultPaymentMethodId()))
                .map(SuiteBillingPaymentMethod::getDisplayLabel)
                .findFirst()
                .orElse(null);
        return new SuiteBillingSubscriptionSummaryVo(
                subscription.getPlanCode(),
                suiteCatalogService.resolvePlanName(subscription.getPlanCode()),
                state.getBillingCycle(),
                safeSeatCount(state.getSeatCount()),
                state.getAutoRenew() == 1,
                state.getCurrentPeriodEndsAt(),
                state.getDefaultPaymentMethodId(),
                defaultLabel,
                state.getPendingActionCode(),
                state.getPendingOfferCode(),
                state.getPendingOfferName(),
                state.getPendingEffectiveAt()
        );
    }

    private List<SuiteBillingSubscriptionActionVo> buildActions(
            SuiteSubscription subscription,
            SuiteBillingSubscriptionState state,
            SuiteCheckoutDraft latestDraft,
            List<SuiteBillingPaymentMethod> paymentMethods
    ) {
        boolean hasPaymentMethod = state.getDefaultPaymentMethodId() != null && !paymentMethods.isEmpty();
        return List.of(
                buildApplyDraftAction(subscription, state, latestDraft, hasPaymentMethod),
                buildAutoRenewAction(subscription, state, ACTION_CANCEL_AUTO_RENEW, state.getAutoRenew() == 1),
                buildAutoRenewAction(subscription, state, ACTION_RESUME_AUTO_RENEW, state.getAutoRenew() != 1)
        );
    }

    private SuiteBillingSubscriptionActionVo buildApplyDraftAction(
            SuiteSubscription subscription,
            SuiteBillingSubscriptionState state,
            SuiteCheckoutDraft latestDraft,
            boolean hasPaymentMethod
    ) {
        if (ACTION_APPLY_LATEST_DRAFT.equals(state.getPendingActionCode())) {
            return new SuiteBillingSubscriptionActionVo(
                    ACTION_APPLY_LATEST_DRAFT,
                    ACTION_STATUS_SCHEDULED,
                    true,
                    state.getPendingOfferCode(),
                    state.getPendingOfferName(),
                    state.getPendingEffectiveAt(),
                    null
            );
        }
        if (latestDraft == null) {
            return lockedAction(ACTION_APPLY_LATEST_DRAFT, "DRAFT_REQUIRED");
        }
        if (!"READY".equals(latestDraft.getQuoteStatus())) {
            return lockedAction(ACTION_APPLY_LATEST_DRAFT, "CONTACT_SALES_ONLY");
        }
        if (!hasPaymentMethod) {
            return lockedAction(ACTION_APPLY_LATEST_DRAFT, "PAYMENT_METHOD_REQUIRED");
        }
        return new SuiteBillingSubscriptionActionVo(
                ACTION_APPLY_LATEST_DRAFT,
                ACTION_STATUS_AVAILABLE,
                true,
                latestDraft.getOfferCode(),
                latestDraft.getOfferName(),
                resolvePendingEffectiveAt(subscription, state),
                null
        );
    }

    private SuiteBillingSubscriptionActionVo buildAutoRenewAction(
            SuiteSubscription subscription,
            SuiteBillingSubscriptionState state,
            String actionCode,
            boolean enabled
    ) {
        if (PLAN_CODE_FREE.equals(subscription.getPlanCode())) {
            return lockedAction(actionCode, "PAID_PLAN_REQUIRED");
        }
        if (enabled) {
            return new SuiteBillingSubscriptionActionVo(
                    actionCode,
                    ACTION_STATUS_AVAILABLE,
                    true,
                    null,
                    null,
                    state.getCurrentPeriodEndsAt(),
                    null
            );
        }
        return lockedAction(actionCode, actionCode.equals(ACTION_CANCEL_AUTO_RENEW) ? "AUTO_RENEW_DISABLED" : "AUTO_RENEW_ENABLED");
    }

    private SuiteBillingSubscriptionActionVo lockedAction(String actionCode, String reasonCode) {
        return new SuiteBillingSubscriptionActionVo(
                actionCode,
                ACTION_STATUS_LOCKED,
                false,
                null,
                null,
                null,
                reasonCode
        );
    }

    private SuiteBillingPaymentMethod buildPaymentMethod(
            Long userId,
            CreateSuiteBillingPaymentMethodRequest request,
            boolean firstMethod
    ) {
        LocalDateTime now = LocalDateTime.now();
        SuiteBillingPaymentMethod paymentMethod = new SuiteBillingPaymentMethod();
        paymentMethod.setOwnerId(userId);
        paymentMethod.setMethodType(request.methodType().trim().toUpperCase());
        paymentMethod.setDisplayLabel(request.displayLabel().trim());
        paymentMethod.setBrand(trimToNull(request.brand()));
        paymentMethod.setLastFour(trimToNull(request.lastFour()));
        paymentMethod.setExpiresAt(trimToNull(request.expiresAt()));
        paymentMethod.setIsDefault(isDefaultRequest(request.makeDefault(), firstMethod) ? 1 : 0);
        paymentMethod.setStatus(METHOD_STATUS_ACTIVE);
        paymentMethod.setCreatedAt(now);
        paymentMethod.setUpdatedAt(now);
        paymentMethod.setDeleted(0);
        return paymentMethod;
    }

    private void validatePaymentMethodRequest(CreateSuiteBillingPaymentMethodRequest request) {
        String methodType = request.methodType().trim().toUpperCase();
        if (!"CARD".equals(methodType)) {
            return;
        }
        if (!StringUtils.hasText(request.lastFour())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Card payment method requires lastFour");
        }
        if (!StringUtils.hasText(request.expiresAt())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Card payment method requires expiresAt");
        }
    }

    private boolean isDefaultRequest(Boolean makeDefault, boolean firstMethod) {
        return Boolean.TRUE.equals(makeDefault) || firstMethod;
    }

    private void applyLatestDraft(
            Long userId,
            SuiteSubscription subscription,
            SuiteBillingSubscriptionState state
    ) {
        SuiteCheckoutDraft latestDraft = findRequiredDraft(userId);
        requireDefaultPaymentMethod(userId, state);
        if (!"READY".equals(latestDraft.getQuoteStatus())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Latest draft requires sales-assisted onboarding");
        }
        SuiteInvoiceSummaryVo invoiceSummary = readJson(latestDraft.getInvoiceSummaryJson(), SuiteInvoiceSummaryVo.class);
        if (invoiceSummary == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Latest draft has no invoice summary");
        }
        LocalDateTime effectiveAt = resolvePendingEffectiveAt(subscription, state);
        state.setPendingActionCode(ACTION_APPLY_LATEST_DRAFT);
        state.setPendingOfferCode(latestDraft.getOfferCode());
        state.setPendingOfferName(latestDraft.getOfferName());
        state.setPendingBillingCycle(latestDraft.getBillingCycle());
        state.setPendingSeatCount(latestDraft.getSeatCount());
        state.setPendingEffectiveAt(effectiveAt);
        createInvoiceRecord(userId, latestDraft, invoiceSummary);
    }

    private void setAutoRenew(SuiteBillingSubscriptionState state, boolean enabled) {
        state.setAutoRenew(enabled ? 1 : 0);
    }

    private void createInvoiceRecord(Long userId, SuiteCheckoutDraft latestDraft, SuiteInvoiceSummaryVo invoiceSummary) {
        LocalDateTime now = LocalDateTime.now();
        SuiteBillingInvoice invoice = new SuiteBillingInvoice();
        invoice.setOwnerId(userId);
        invoice.setInvoiceNumber(buildInvoiceNumber(userId, now));
        invoice.setOfferCode(latestDraft.getOfferCode());
        invoice.setOfferName(latestDraft.getOfferName());
        invoice.setInvoiceStatus(INVOICE_STATUS_PENDING);
        invoice.setCurrencyCode(invoiceSummary.currencyCode());
        invoice.setTotalCents(invoiceSummary.totalCents());
        invoice.setBillingCycle(invoiceSummary.billingCycle());
        invoice.setSeatCount(invoiceSummary.seatCount());
        invoice.setInvoiceSummaryJson(latestDraft.getInvoiceSummaryJson());
        invoice.setIssuedAt(now);
        invoice.setDueAt(now.plusDays(INVOICE_DUE_DAYS));
        invoice.setDownloadCode(buildDownloadCode(userId, now));
        invoice.setCreatedAt(now);
        invoice.setUpdatedAt(now);
        invoice.setDeleted(0);
        invoiceMapper.insert(invoice);
    }

    private String buildInvoiceNumber(Long userId, LocalDateTime issuedAt) {
        return "INV-" + userId + "-" + issuedAt.withNano(0).toString().replace(":", "").replace("-", "");
    }

    private String buildDownloadCode(Long userId, LocalDateTime issuedAt) {
        return "DL-" + userId + "-" + issuedAt.getHour() + issuedAt.getMinute() + issuedAt.getSecond();
    }

    private LocalDateTime resolvePendingEffectiveAt(
            SuiteSubscription subscription,
            SuiteBillingSubscriptionState state
    ) {
        if (PLAN_CODE_FREE.equals(subscription.getPlanCode())) {
            return LocalDateTime.now().plusMinutes(DRAFT_ACTIVATION_MINUTES);
        }
        if (state.getCurrentPeriodEndsAt() != null) {
            return state.getCurrentPeriodEndsAt();
        }
        return LocalDateTime.now().plusYears(CURRENT_PERIOD_YEARS);
    }

    private void requireDefaultPaymentMethod(Long userId, SuiteBillingSubscriptionState state) {
        if (state.getDefaultPaymentMethodId() != null) {
            return;
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, "Default payment method is required");
    }

    private void syncDefaultPaymentMethod(Long userId, Long paymentMethodId, boolean isDefault) {
        if (!isDefault) {
            return;
        }
        SuiteBillingSubscriptionState state = ensureSubscriptionState(userId, ensureSubscription(userId).getPlanCode());
        state.setDefaultPaymentMethodId(paymentMethodId);
        state.setUpdatedAt(LocalDateTime.now());
        subscriptionStateMapper.updateById(state);
    }

    private void clearDefaultFlags(Long userId) {
        paymentMethodMapper.update(
                null,
                new LambdaUpdateWrapper<SuiteBillingPaymentMethod>()
                        .eq(SuiteBillingPaymentMethod::getOwnerId, userId)
                        .set(SuiteBillingPaymentMethod::getIsDefault, 0)
                        .set(SuiteBillingPaymentMethod::getUpdatedAt, LocalDateTime.now())
        );
    }

    private SuiteBillingPaymentMethod requirePaymentMethod(Long userId, Long paymentMethodId) {
        SuiteBillingPaymentMethod paymentMethod = paymentMethodMapper.selectOne(new LambdaQueryWrapper<SuiteBillingPaymentMethod>()
                .eq(SuiteBillingPaymentMethod::getOwnerId, userId)
                .eq(SuiteBillingPaymentMethod::getId, paymentMethodId));
        if (paymentMethod == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Payment method is not found");
        }
        return paymentMethod;
    }

    private List<SuiteBillingPaymentMethod> listPaymentMethodEntities(Long userId) {
        return paymentMethodMapper.selectList(new LambdaQueryWrapper<SuiteBillingPaymentMethod>()
                .eq(SuiteBillingPaymentMethod::getOwnerId, userId)
                .orderByDesc(SuiteBillingPaymentMethod::getIsDefault)
                .orderByDesc(SuiteBillingPaymentMethod::getUpdatedAt));
    }

    private List<SuiteBillingInvoice> listInvoiceEntities(Long userId) {
        return invoiceMapper.selectList(new LambdaQueryWrapper<SuiteBillingInvoice>()
                .eq(SuiteBillingInvoice::getOwnerId, userId)
                .orderByDesc(SuiteBillingInvoice::getIssuedAt));
    }

    private SuiteBillingPaymentMethodVo toPaymentMethodVo(SuiteBillingPaymentMethod paymentMethod) {
        return new SuiteBillingPaymentMethodVo(
                paymentMethod.getId(),
                paymentMethod.getMethodType(),
                paymentMethod.getDisplayLabel(),
                paymentMethod.getBrand(),
                paymentMethod.getLastFour(),
                paymentMethod.getExpiresAt(),
                paymentMethod.getIsDefault() == 1,
                paymentMethod.getStatus()
        );
    }

    private SuiteBillingInvoiceVo toInvoiceVo(SuiteBillingInvoice invoice) {
        return new SuiteBillingInvoiceVo(
                invoice.getInvoiceNumber(),
                invoice.getOfferCode(),
                invoice.getOfferName(),
                invoice.getInvoiceStatus(),
                invoice.getCurrencyCode(),
                invoice.getTotalCents(),
                invoice.getBillingCycle(),
                safeSeatCount(invoice.getSeatCount()),
                invoice.getIssuedAt(),
                invoice.getDueAt(),
                invoice.getDownloadCode()
        );
    }

    private SuiteBillingSubscriptionState ensureSubscriptionState(Long userId, String activePlanCode) {
        SuiteBillingSubscriptionState existing = subscriptionStateMapper.selectOne(new LambdaQueryWrapper<SuiteBillingSubscriptionState>()
                .eq(SuiteBillingSubscriptionState::getOwnerId, userId));
        if (existing != null) {
            return alignState(existing, activePlanCode);
        }
        SuiteBillingSubscriptionState created = buildSubscriptionState(userId, activePlanCode);
        try {
            subscriptionStateMapper.insert(created);
            return created;
        } catch (DuplicateKeyException ignored) {
            SuiteBillingSubscriptionState current = subscriptionStateMapper.selectOne(new LambdaQueryWrapper<SuiteBillingSubscriptionState>()
                    .eq(SuiteBillingSubscriptionState::getOwnerId, userId));
            return alignState(current, activePlanCode);
        }
    }

    private SuiteBillingSubscriptionState alignState(SuiteBillingSubscriptionState state, String activePlanCode) {
        boolean paidPlan = !PLAN_CODE_FREE.equals(activePlanCode);
        boolean changed = false;
        if (state.getSeatCount() == null || state.getSeatCount() < 1) {
            state.setSeatCount(DEFAULT_SEAT_COUNT);
            changed = true;
        }
        if (!paidPlan && (state.getAutoRenew() == null || state.getAutoRenew() != 0 || state.getBillingCycle() != null)) {
            state.setAutoRenew(0);
            state.setBillingCycle(null);
            state.setCurrentPeriodEndsAt(null);
            changed = true;
        }
        if (paidPlan && (state.getBillingCycle() == null || state.getCurrentPeriodEndsAt() == null)) {
            state.setBillingCycle("ANNUAL");
            state.setAutoRenew(1);
            state.setCurrentPeriodEndsAt(LocalDateTime.now().plusYears(CURRENT_PERIOD_YEARS));
            changed = true;
        }
        if (!changed) {
            return state;
        }
        state.setUpdatedAt(LocalDateTime.now());
        subscriptionStateMapper.updateById(state);
        return state;
    }

    private SuiteBillingSubscriptionState buildSubscriptionState(Long userId, String activePlanCode) {
        LocalDateTime now = LocalDateTime.now();
        boolean paidPlan = !PLAN_CODE_FREE.equals(activePlanCode);
        SuiteBillingSubscriptionState state = new SuiteBillingSubscriptionState();
        state.setOwnerId(userId);
        state.setBillingCycle(paidPlan ? "ANNUAL" : null);
        state.setSeatCount(DEFAULT_SEAT_COUNT);
        state.setCurrencyCode(paidPlan ? "EUR" : null);
        state.setAutoRenew(paidPlan ? 1 : 0);
        state.setCurrentPeriodEndsAt(paidPlan ? now.plusYears(CURRENT_PERIOD_YEARS) : null);
        state.setCreatedAt(now);
        state.setUpdatedAt(now);
        state.setDeleted(0);
        return state;
    }

    private SuiteCheckoutDraft findRequiredDraft(Long userId) {
        SuiteCheckoutDraft draft = findDraft(userId);
        if (draft == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Latest draft is required");
        }
        return draft;
    }

    private SuiteCheckoutDraft findDraft(Long userId) {
        return checkoutDraftMapper.selectOne(new LambdaQueryWrapper<SuiteCheckoutDraft>()
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
        created.setStatus("ACTIVE");
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

    private <T> T readJson(String json, Class<T> type) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to parse suite billing center snapshot");
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private int safeSeatCount(Integer seatCount) {
        return seatCount == null || seatCount < 1 ? DEFAULT_SEAT_COUNT : seatCount;
    }
}
