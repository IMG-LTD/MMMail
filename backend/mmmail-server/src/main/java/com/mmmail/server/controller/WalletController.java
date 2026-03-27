package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.BroadcastWalletTransactionRequest;
import com.mmmail.server.model.dto.BatchAdvanceWalletTransactionsRequest;
import com.mmmail.server.model.dto.BatchReconcileWalletTransactionsRequest;
import com.mmmail.server.model.dto.BatchRemediateWalletTransactionsRequest;
import com.mmmail.server.model.dto.ConfirmWalletTransactionRequest;
import com.mmmail.server.model.dto.CreateWalletAccountRequest;
import com.mmmail.server.model.dto.FailWalletTransactionRequest;
import com.mmmail.server.model.dto.RemediateWalletTransactionRequest;
import com.mmmail.server.model.dto.ReceiveWalletTransactionRequest;
import com.mmmail.server.model.dto.SendWalletTransactionRequest;
import com.mmmail.server.model.dto.SignWalletTransactionRequest;
import com.mmmail.server.model.dto.AdvanceWalletTransactionRequest;
import com.mmmail.server.model.vo.WalletAccountVo;
import com.mmmail.server.model.vo.WalletActionResultVo;
import com.mmmail.server.model.vo.WalletBatchActionResultVo;
import com.mmmail.server.model.vo.WalletExecutionOverviewVo;
import com.mmmail.server.model.vo.WalletExecutionPlanVo;
import com.mmmail.server.model.vo.WalletExecutionTraceVo;
import com.mmmail.server.model.vo.WalletReconciliationOverviewVo;
import com.mmmail.server.model.vo.WalletTransactionVo;
import com.mmmail.server.service.WalletService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/accounts")
    public Result<List<WalletAccountVo>> listAccounts(
            @RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletService.listAccounts(SecurityUtils.currentUserId(), limit, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/accounts")
    public Result<WalletAccountVo> createAccount(
            @Valid @RequestBody CreateWalletAccountRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletService.createAccount(
                SecurityUtils.currentUserId(),
                request.walletName(),
                request.assetSymbol(),
                request.address(),
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/transactions")
    public Result<List<WalletTransactionVo>> listTransactions(
            @RequestParam Long accountId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletService.listTransactions(
                SecurityUtils.currentUserId(),
                accountId,
                status,
                limit,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/execution-overview")
    public Result<WalletExecutionOverviewVo> executionOverview(
            @RequestParam Long accountId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletService.getExecutionOverview(
                SecurityUtils.currentUserId(),
                accountId,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/execution-plan")
    public Result<WalletExecutionPlanVo> executionPlan(
            @RequestParam Long accountId,
            @RequestParam(required = false) Integer maxItems,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletService.getExecutionPlan(
                SecurityUtils.currentUserId(),
                accountId,
                maxItems,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/transactions/{transactionId}/execution-trace")
    public Result<WalletExecutionTraceVo> executionTrace(
            @PathVariable Long transactionId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletService.getExecutionTrace(
                SecurityUtils.currentUserId(),
                transactionId,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/reconciliation-overview")
    public Result<WalletReconciliationOverviewVo> reconciliationOverview(
            @RequestParam Long accountId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletService.getReconciliationOverview(
                SecurityUtils.currentUserId(),
                accountId,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/transactions/receive")
    public Result<WalletTransactionVo> receive(
            @Valid @RequestBody ReceiveWalletTransactionRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletService.receive(
                SecurityUtils.currentUserId(),
                request.accountId(),
                request.amountMinor(),
                request.assetSymbol(),
                request.sourceAddress(),
                request.memo(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/transactions/send")
    public Result<WalletTransactionVo> send(
            @Valid @RequestBody SendWalletTransactionRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletService.send(
                SecurityUtils.currentUserId(),
                request.accountId(),
                request.amountMinor(),
                request.assetSymbol(),
                request.targetAddress(),
                request.memo(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/transactions/{transactionId}/advance")
    public Result<WalletActionResultVo> advance(
            @PathVariable Long transactionId,
            @Valid @RequestBody(required = false) AdvanceWalletTransactionRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletService.advanceTransaction(
                SecurityUtils.currentUserId(),
                transactionId,
                request == null ? null : request.operatorHint(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/transactions/{transactionId}/remediate")
    public Result<WalletActionResultVo> remediate(
            @PathVariable Long transactionId,
            @Valid @RequestBody RemediateWalletTransactionRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletService.remediateTransaction(
                SecurityUtils.currentUserId(),
                transactionId,
                request.strategy(),
                request.reason(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/transactions/batch-advance")
    public Result<WalletBatchActionResultVo> batchAdvance(
            @Valid @RequestBody BatchAdvanceWalletTransactionsRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletService.batchAdvanceTransactions(
                SecurityUtils.currentUserId(),
                request.accountId(),
                request.maxItems(),
                request.operatorHint(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/transactions/batch-remediate")
    public Result<WalletBatchActionResultVo> batchRemediate(
            @Valid @RequestBody BatchRemediateWalletTransactionsRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletService.batchRemediateTransactions(
                SecurityUtils.currentUserId(),
                request.accountId(),
                request.maxItems(),
                request.strategy(),
                request.reason(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/transactions/batch-reconcile")
    public Result<WalletBatchActionResultVo> batchReconcile(
            @Valid @RequestBody BatchReconcileWalletTransactionsRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletService.batchReconcileTransactions(
                SecurityUtils.currentUserId(),
                request.accountId(),
                request.maxItems(),
                request.strategy(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/transactions/{transactionId}/confirm")
    public Result<WalletTransactionVo> confirm(
            @PathVariable Long transactionId,
            @Valid @RequestBody ConfirmWalletTransactionRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletService.confirmTransaction(
                SecurityUtils.currentUserId(),
                transactionId,
                request.confirmations(),
                request.networkTxHash(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/transactions/{transactionId}/sign")
    public Result<WalletTransactionVo> sign(
            @PathVariable Long transactionId,
            @Valid @RequestBody SignWalletTransactionRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletService.signTransaction(
                SecurityUtils.currentUserId(),
                transactionId,
                request.signerHint(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/transactions/{transactionId}/broadcast")
    public Result<WalletTransactionVo> broadcast(
            @PathVariable Long transactionId,
            @Valid @RequestBody BroadcastWalletTransactionRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletService.broadcastTransaction(
                SecurityUtils.currentUserId(),
                transactionId,
                request.networkTxHash(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/transactions/{transactionId}/fail")
    public Result<WalletTransactionVo> fail(
            @PathVariable Long transactionId,
            @Valid @RequestBody FailWalletTransactionRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(walletService.failTransaction(
                SecurityUtils.currentUserId(),
                transactionId,
                request.reason(),
                httpRequest.getRemoteAddr()
        ));
    }
}
