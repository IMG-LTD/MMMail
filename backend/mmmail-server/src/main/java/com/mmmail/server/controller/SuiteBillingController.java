package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateSuiteBillingPaymentMethodRequest;
import com.mmmail.server.model.dto.CreateSuiteBillingQuoteRequest;
import com.mmmail.server.model.dto.CreateSuiteCheckoutDraftRequest;
import com.mmmail.server.model.dto.ExecuteSuiteBillingSubscriptionActionRequest;
import com.mmmail.server.model.dto.SetDefaultSuiteBillingPaymentMethodRequest;
import com.mmmail.server.model.vo.SuiteBillingCenterVo;
import com.mmmail.server.model.vo.SuiteBillingOverviewVo;
import com.mmmail.server.model.vo.SuiteBillingQuoteVo;
import com.mmmail.server.model.vo.SuiteCheckoutDraftVo;
import com.mmmail.server.model.vo.SuitePricingOfferVo;
import com.mmmail.server.service.SuiteBillingCenterService;
import com.mmmail.server.service.SuiteBillingService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suite")
public class SuiteBillingController {

    private final SuiteBillingService suiteBillingService;
    private final SuiteBillingCenterService suiteBillingCenterService;

    public SuiteBillingController(
            SuiteBillingService suiteBillingService,
            SuiteBillingCenterService suiteBillingCenterService
    ) {
        this.suiteBillingService = suiteBillingService;
        this.suiteBillingCenterService = suiteBillingCenterService;
    }

    @GetMapping("/pricing/offers")
    public Result<List<SuitePricingOfferVo>> listPricingOffers(HttpServletRequest httpRequest) {
        return Result.success(suiteBillingService.listPricingOffers(
                SecurityUtils.currentUserId(),
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/billing/overview")
    public Result<SuiteBillingOverviewVo> getBillingOverview(HttpServletRequest httpRequest) {
        return Result.success(suiteBillingService.getBillingOverview(
                SecurityUtils.currentUserId(),
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/billing/center")
    public Result<SuiteBillingCenterVo> getBillingCenter(HttpServletRequest httpRequest) {
        return Result.success(suiteBillingCenterService.getBillingCenter(
                SecurityUtils.currentUserId(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/billing/quote")
    public Result<SuiteBillingQuoteVo> createQuote(
            @Valid @RequestBody CreateSuiteBillingQuoteRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(suiteBillingService.createQuote(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/billing/checkout-draft")
    public Result<SuiteCheckoutDraftVo> saveCheckoutDraft(
            @Valid @RequestBody CreateSuiteCheckoutDraftRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(suiteBillingService.saveCheckoutDraft(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/billing/payment-methods")
    public Result<SuiteBillingCenterVo> addPaymentMethod(
            @Valid @RequestBody CreateSuiteBillingPaymentMethodRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(suiteBillingCenterService.addPaymentMethod(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/billing/payment-methods/default")
    public Result<SuiteBillingCenterVo> setDefaultPaymentMethod(
            @Valid @RequestBody SetDefaultSuiteBillingPaymentMethodRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(suiteBillingCenterService.setDefaultPaymentMethod(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/billing/subscription-actions")
    public Result<SuiteBillingCenterVo> executeSubscriptionAction(
            @Valid @RequestBody ExecuteSuiteBillingSubscriptionActionRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(suiteBillingCenterService.executeSubscriptionAction(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }
}
