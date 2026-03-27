package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.WalletAccountMapper;
import com.mmmail.server.mapper.WalletTransactionMapper;
import com.mmmail.server.model.entity.WalletAccount;
import com.mmmail.server.model.entity.WalletTransaction;
import com.mmmail.server.model.vo.WalletAccountVo;
import com.mmmail.server.model.vo.WalletActionResultVo;
import com.mmmail.server.model.vo.WalletBatchActionResultVo;
import com.mmmail.server.model.vo.WalletExecutionOverviewVo;
import com.mmmail.server.model.vo.WalletExecutionPlanItemVo;
import com.mmmail.server.model.vo.WalletExecutionPlanVo;
import com.mmmail.server.model.vo.WalletExecutionTraceStageEventVo;
import com.mmmail.server.model.vo.WalletExecutionTraceVo;
import com.mmmail.server.model.vo.WalletPriorityTransactionVo;
import com.mmmail.server.model.vo.WalletReconciliationOverviewVo;
import com.mmmail.server.model.vo.WalletStageCountsVo;
import com.mmmail.server.model.vo.WalletTransactionVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class WalletService {

    private static final String TX_TYPE_SEND = "SEND";
    private static final String TX_TYPE_RECEIVE = "RECEIVE";
    private static final String TX_STATUS_PENDING = "PENDING";
    private static final String TX_STATUS_SIGNED = "SIGNED";
    private static final String TX_STATUS_BROADCASTED = "BROADCASTED";
    private static final String TX_STATUS_CONFIRMED = "CONFIRMED";
    private static final String TX_STATUS_FAILED = "FAILED";
    private static final String REMEDIATE_RETRY_SIGN = "RETRY_SIGN";
    private static final String REMEDIATE_RETRY_BROADCAST = "RETRY_BROADCAST";
    private static final String REMEDIATE_ROLLBACK_FAIL = "ROLLBACK_FAIL";
    private static final String RECONCILE_AUTO = "AUTO";
    private static final String RISK_LOW = "LOW";
    private static final String RISK_MEDIUM = "MEDIUM";
    private static final String RISK_HIGH = "HIGH";
    private static final String RISK_CRITICAL = "CRITICAL";
    private static final int DEFAULT_ACCOUNT_LIMIT = 20;
    private static final int DEFAULT_TX_LIMIT = 50;
    private static final int DEFAULT_BATCH_ITEMS = 10;
    private static final int MAX_LIMIT = 200;
    private static final int MAX_BATCH_ITEMS = 20;
    private static final int MAX_OVERVIEW_TX = 500;
    private static final int BLOCKED_THRESHOLD_MINUTES = 30;

    private final WalletAccountMapper walletAccountMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final AuditService auditService;

    public WalletService(
            WalletAccountMapper walletAccountMapper,
            WalletTransactionMapper walletTransactionMapper,
            AuditService auditService
    ) {
        this.walletAccountMapper = walletAccountMapper;
        this.walletTransactionMapper = walletTransactionMapper;
        this.auditService = auditService;
    }

    public List<WalletAccountVo> listAccounts(Long userId, Integer limit, String ipAddress) {
        int safeLimit = safeLimit(limit, DEFAULT_ACCOUNT_LIMIT);
        List<WalletAccountVo> accounts = walletAccountMapper.selectList(new LambdaQueryWrapper<WalletAccount>()
                        .eq(WalletAccount::getOwnerId, userId)
                        .orderByDesc(WalletAccount::getUpdatedAt)
                        .last("limit " + safeLimit))
                .stream()
                .map(this::toAccountVo)
                .toList();
        auditService.record(userId, "WALLET_ACCOUNT_LIST", "count=" + accounts.size(), ipAddress);
        return accounts;
    }

    @Transactional
    public WalletAccountVo createAccount(
            Long userId,
            String walletName,
            String assetSymbol,
            String address,
            String ipAddress
    ) {
        String safeWalletName = requireWalletName(walletName);
        String safeAssetSymbol = normalizeAssetSymbol(assetSymbol);
        String safeAddress = normalizeAddress(address);

        WalletAccount existing = walletAccountMapper.selectOne(new LambdaQueryWrapper<WalletAccount>()
                .eq(WalletAccount::getOwnerId, userId)
                .eq(WalletAccount::getAddress, safeAddress));
        if (existing != null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet account address already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        WalletAccount account = new WalletAccount();
        account.setOwnerId(userId);
        account.setWalletName(safeWalletName);
        account.setAssetSymbol(safeAssetSymbol);
        account.setAddress(safeAddress);
        account.setBalanceMinor(0L);
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        account.setDeleted(0);
        walletAccountMapper.insert(account);

        auditService.record(userId, "WALLET_ACCOUNT_CREATE", "accountId=" + account.getId(), ipAddress);
        return toAccountVo(account);
    }

    public List<WalletTransactionVo> listTransactions(
            Long userId,
            Long accountId,
            String status,
            Integer limit,
            String ipAddress
    ) {
        WalletAccount account = loadAccount(userId, accountId);
        int safeLimit = safeLimit(limit, DEFAULT_TX_LIMIT);
        String safeStatus = normalizeStatus(status);

        LambdaQueryWrapper<WalletTransaction> query = new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getOwnerId, userId)
                .eq(WalletTransaction::getAccountId, account.getId())
                .orderByDesc(WalletTransaction::getCreatedAt)
                .last("limit " + safeLimit);
        if (safeStatus != null) {
            query.eq(WalletTransaction::getStatus, safeStatus);
        }

        List<WalletTransactionVo> transactions = walletTransactionMapper.selectList(query)
                .stream()
                .map(this::toTransactionVo)
                .toList();
        auditService.record(
                userId,
                "WALLET_TX_LIST",
                "accountId=" + accountId + ",status=" + safeStatus + ",count=" + transactions.size(),
                ipAddress
        );
        return transactions;
    }

    public WalletExecutionOverviewVo getExecutionOverview(Long userId, Long accountId, String ipAddress) {
        WalletAccount account = loadAccount(userId, accountId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime blockedThreshold = now.minusMinutes(BLOCKED_THRESHOLD_MINUTES);
        List<WalletTransaction> transactions = walletTransactionMapper.selectList(new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getOwnerId, userId)
                .eq(WalletTransaction::getAccountId, account.getId())
                .orderByDesc(WalletTransaction::getUpdatedAt)
                .last("limit " + MAX_OVERVIEW_TX));

        int pendingCount = 0;
        int signedCount = 0;
        int broadcastedCount = 0;
        int confirmedCount = 0;
        int failedCount = 0;
        int blockedCount = 0;
        List<WalletPriorityTransactionVo> priorityTransactions = new ArrayList<>();

        for (WalletTransaction tx : transactions) {
            String status = tx.getStatus();
            if (TX_STATUS_PENDING.equals(status)) {
                pendingCount += 1;
            } else if (TX_STATUS_SIGNED.equals(status)) {
                signedCount += 1;
            } else if (TX_STATUS_BROADCASTED.equals(status)) {
                broadcastedCount += 1;
            } else if (TX_STATUS_CONFIRMED.equals(status)) {
                confirmedCount += 1;
            } else if (TX_STATUS_FAILED.equals(status)) {
                failedCount += 1;
            }

            boolean blocked = isBlocked(tx, blockedThreshold);
            if (blocked) {
                blockedCount += 1;
            }
            if (blocked || TX_STATUS_FAILED.equals(status)) {
                priorityTransactions.add(toPriorityTransactionVo(tx, blocked, now));
            }
        }

        priorityTransactions = priorityTransactions.stream()
                .sorted((left, right) -> {
                    int byStatus = Integer.compare(statusPriority(left.status()), statusPriority(right.status()));
                    if (byStatus != 0) {
                        return byStatus;
                    }
                    return Integer.compare(right.ageMinutes(), left.ageMinutes());
                })
                .limit(20)
                .toList();

        int score = 100;
        score -= Math.min(40, failedCount * 10);
        score -= Math.min(32, blockedCount * 8);
        if (signedCount + broadcastedCount > 6) {
            score -= 10;
        }
        if (pendingCount > 8) {
            score -= 8;
        }
        if (transactions.isEmpty()) {
            score -= 25;
        }
        int healthScore = clamp(score);

        WalletExecutionOverviewVo overview = new WalletExecutionOverviewVo(
                String.valueOf(account.getId()),
                now,
                healthScore,
                toRiskLevel(healthScore),
                new WalletStageCountsVo(
                        pendingCount,
                        signedCount,
                        broadcastedCount,
                        confirmedCount,
                        failedCount
                ),
                blockedCount,
                priorityTransactions
        );
        auditService.record(
                userId,
                "WALLET_EXECUTION_OVERVIEW_QUERY",
                "accountId=" + account.getId() + ",score=" + overview.executionHealthScore() + ",risk=" + overview.riskLevel(),
                ipAddress
        );
        return overview;
    }

    public WalletExecutionPlanVo getExecutionPlan(
            Long userId,
            Long accountId,
            Integer maxItems,
            String ipAddress
    ) {
        WalletAccount account = loadAccount(userId, accountId);
        int safeMaxItems = safeBatchMaxItems(maxItems);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime blockedThreshold = now.minusMinutes(BLOCKED_THRESHOLD_MINUTES);
        List<WalletTransaction> transactions = walletTransactionMapper.selectList(new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getOwnerId, userId)
                .eq(WalletTransaction::getAccountId, account.getId())
                .orderByDesc(WalletTransaction::getUpdatedAt)
                .last("limit " + MAX_OVERVIEW_TX));

        List<WalletExecutionPlanItemVo> planItems = new ArrayList<>();
        int recommendedAdvanceCount = 0;
        int recommendedRemediationCount = 0;

        for (WalletTransaction tx : transactions) {
            WalletExecutionPlanItemVo item = toExecutionPlanItem(tx, blockedThreshold, now);
            if (item == null) {
                continue;
            }
            planItems.add(item);
            if ("ADVANCE".equals(item.recommendedOperation())) {
                recommendedAdvanceCount += 1;
            } else {
                recommendedRemediationCount += 1;
            }
        }

        List<WalletExecutionPlanItemVo> prioritized = planItems.stream()
                .sorted((left, right) -> {
                    int byPriority = Integer.compare(left.priority(), right.priority());
                    if (byPriority != 0) {
                        return byPriority;
                    }
                    long leftId = Long.parseLong(left.transactionId());
                    long rightId = Long.parseLong(right.transactionId());
                    return Long.compare(rightId, leftId);
                })
                .limit(safeMaxItems)
                .toList();

        int estimatedRiskDelta = Math.min(45, recommendedAdvanceCount * 3 + recommendedRemediationCount * 5);
        WalletExecutionPlanVo plan = new WalletExecutionPlanVo(
                String.valueOf(account.getId()),
                now,
                recommendedAdvanceCount,
                recommendedRemediationCount,
                estimatedRiskDelta,
                prioritized
        );
        auditService.record(
                userId,
                "WALLET_EXECUTION_PLAN_QUERY",
                "accountId=" + account.getId() + ",items=" + prioritized.size(),
                ipAddress
        );
        return plan;
    }

    public WalletExecutionTraceVo getExecutionTrace(
            Long userId,
            Long transactionId,
            String ipAddress
    ) {
        WalletTransaction tx = loadTransaction(userId, transactionId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime blockedThreshold = now.minusMinutes(BLOCKED_THRESHOLD_MINUTES);
        LocalDateTime createdAt = tx.getCreatedAt() == null ? now : tx.getCreatedAt();
        LocalDateTime updatedAt = tx.getUpdatedAt() == null ? now : tx.getUpdatedAt();

        List<String> warnings = new ArrayList<>();
        List<WalletExecutionTraceStageEventVo> stageEvents = new ArrayList<>();

        stageEvents.add(new WalletExecutionTraceStageEventVo(
                "CREATED",
                createdAt,
                "wallet-core",
                "Transaction is created in wallet ledger."
        ));

        if (TX_TYPE_RECEIVE.equals(tx.getTxType())) {
            stageEvents.add(new WalletExecutionTraceStageEventVo(
                    "RECEIVED",
                    createdAt,
                    "wallet-core",
                    "Receive transaction is accepted from source address."
            ));
            if (TX_STATUS_CONFIRMED.equals(tx.getStatus())) {
                stageEvents.add(new WalletExecutionTraceStageEventVo(
                        "SETTLED",
                        updatedAt,
                        "wallet-core",
                        "Receive transaction is confirmed and balance is settled."
                ));
            }
        } else {
            stageEvents.add(new WalletExecutionTraceStageEventVo(
                    "QUEUED",
                    createdAt,
                    "wallet-core",
                    "Send transaction is queued for signing."
            ));
            if (StringUtils.hasText(tx.getSignatureHash())) {
                stageEvents.add(new WalletExecutionTraceStageEventVo(
                        "SIGNED",
                        updatedAt,
                        "wallet-signer",
                        "Transaction signature is generated."
                ));
            }
            if (StringUtils.hasText(tx.getNetworkTxHash())) {
                stageEvents.add(new WalletExecutionTraceStageEventVo(
                        "BROADCASTED",
                        updatedAt,
                        "wallet-network",
                        "Transaction hash is available on broadcast channel."
                ));
            }
        }

        if (TX_STATUS_CONFIRMED.equals(tx.getStatus())) {
            stageEvents.add(new WalletExecutionTraceStageEventVo(
                    "CONFIRMED",
                    updatedAt,
                    "wallet-network",
                    "Transaction reached confirmed status."
            ));
        }
        if (TX_STATUS_FAILED.equals(tx.getStatus())) {
            stageEvents.add(new WalletExecutionTraceStageEventVo(
                    "FAILED",
                    updatedAt,
                    "wallet-reconcile",
                    "Transaction entered failed status and requires remediation."
            ));
        }

        if (TX_TYPE_SEND.equals(tx.getTxType())
                && (TX_STATUS_SIGNED.equals(tx.getStatus())
                || TX_STATUS_BROADCASTED.equals(tx.getStatus())
                || TX_STATUS_CONFIRMED.equals(tx.getStatus()))
                && !StringUtils.hasText(tx.getSignatureHash())) {
            warnings.add("Signature hash is missing for signed/broadcasted send transaction.");
        }
        if ((TX_STATUS_BROADCASTED.equals(tx.getStatus()) || TX_STATUS_CONFIRMED.equals(tx.getStatus()))
                && !StringUtils.hasText(tx.getNetworkTxHash())) {
            warnings.add("Network transaction hash is missing for broadcasted/confirmed transaction.");
        }
        if (TX_STATUS_CONFIRMED.equals(tx.getStatus()) && (tx.getConfirmations() == null || tx.getConfirmations() <= 0)) {
            warnings.add("Confirmations should be greater than zero for confirmed transaction.");
        }
        if (isBlocked(tx, blockedThreshold)) {
            warnings.add("Transaction is blocked in mid-stage execution beyond threshold.");
        }

        int integrityScore = 100;
        integrityScore -= Math.min(60, warnings.size() * 15);
        if (TX_STATUS_FAILED.equals(tx.getStatus())) {
            integrityScore -= 20;
        }
        WalletExecutionTraceVo trace = new WalletExecutionTraceVo(
                String.valueOf(tx.getId()),
                tx.getStatus(),
                clamp(integrityScore),
                warnings,
                stageEvents
        );
        auditService.record(
                userId,
                "WALLET_TX_EXECUTION_TRACE_QUERY",
                "transactionId=" + tx.getId() + ",status=" + tx.getStatus() + ",warnings=" + warnings.size(),
                ipAddress
        );
        return trace;
    }

    public WalletReconciliationOverviewVo getReconciliationOverview(
            Long userId,
            Long accountId,
            String ipAddress
    ) {
        WalletAccount account = loadAccount(userId, accountId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime blockedThreshold = now.minusMinutes(BLOCKED_THRESHOLD_MINUTES);
        List<WalletTransaction> transactions = walletTransactionMapper.selectList(new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getOwnerId, userId)
                .eq(WalletTransaction::getAccountId, account.getId())
                .orderByDesc(WalletTransaction::getUpdatedAt)
                .last("limit " + MAX_OVERVIEW_TX));

        int mismatchCount = 0;
        int blockedCount = 0;
        int failedCount = 0;
        for (WalletTransaction tx : transactions) {
            if (hasReconciliationMismatch(tx)) {
                mismatchCount += 1;
            }
            if (isBlocked(tx, blockedThreshold)) {
                blockedCount += 1;
            }
            if (TX_STATUS_FAILED.equals(tx.getStatus())) {
                failedCount += 1;
            }
        }

        List<String> recommendedActions = new ArrayList<>();
        if (failedCount > 0) {
            recommendedActions.add("Run batch reconcile to recover failed transactions.");
        }
        if (blockedCount > 0) {
            recommendedActions.add("Prioritize blocked mid-stage transactions for reconciliation.");
        }
        if (mismatchCount > 0) {
            recommendedActions.add("Inspect execution trace for missing signature/hash and reconcile.");
        }
        if (recommendedActions.isEmpty()) {
            recommendedActions.add("No immediate reconciliation action required.");
        }

        int integrityScore = 100;
        integrityScore -= Math.min(42, failedCount * 10);
        integrityScore -= Math.min(30, blockedCount * 6);
        integrityScore -= Math.min(20, mismatchCount * 5);
        if (transactions.isEmpty()) {
            integrityScore -= 12;
        }
        WalletReconciliationOverviewVo overview = new WalletReconciliationOverviewVo(
                String.valueOf(account.getId()),
                now,
                clamp(integrityScore),
                toRiskLevel(clamp(integrityScore)),
                mismatchCount,
                blockedCount,
                failedCount,
                recommendedActions
        );
        auditService.record(
                userId,
                "WALLET_RECONCILIATION_OVERVIEW_QUERY",
                "accountId=" + account.getId()
                        + ",score=" + overview.integrityScore()
                        + ",mismatch=" + mismatchCount
                        + ",blocked=" + blockedCount
                        + ",failed=" + failedCount,
                ipAddress
        );
        return overview;
    }

    @Transactional
    public WalletBatchActionResultVo batchReconcileTransactions(
            Long userId,
            Long accountId,
            Integer maxItems,
            String strategy,
            String ipAddress
    ) {
        WalletAccount account = loadAccount(userId, accountId);
        int safeMaxItems = safeBatchMaxItems(maxItems);
        String safeStrategy = normalizeReconcileStrategy(strategy);
        LocalDateTime blockedThreshold = LocalDateTime.now().minusMinutes(BLOCKED_THRESHOLD_MINUTES);

        List<WalletTransaction> candidates = walletTransactionMapper.selectList(new LambdaQueryWrapper<WalletTransaction>()
                        .eq(WalletTransaction::getOwnerId, userId)
                        .eq(WalletTransaction::getAccountId, account.getId())
                        .orderByAsc(WalletTransaction::getUpdatedAt)
                        .last("limit " + MAX_OVERVIEW_TX))
                .stream()
                .sorted((left, right) -> {
                    int byPriority = Integer.compare(
                            reconciliationQueuePriority(left, blockedThreshold),
                            reconciliationQueuePriority(right, blockedThreshold)
                    );
                    if (byPriority != 0) {
                        return byPriority;
                    }
                    LocalDateTime leftTime = left.getUpdatedAt() == null ? LocalDateTime.MIN : left.getUpdatedAt();
                    LocalDateTime rightTime = right.getUpdatedAt() == null ? LocalDateTime.MIN : right.getUpdatedAt();
                    return leftTime.compareTo(rightTime);
                })
                .toList();

        int processedCount = 0;
        int successCount = 0;
        int failedCount = 0;
        int skippedCount = 0;
        List<WalletActionResultVo> results = new ArrayList<>();
        for (WalletTransaction tx : candidates) {
            if (processedCount >= safeMaxItems) {
                break;
            }
            if (!canReconcile(tx, blockedThreshold)) {
                skippedCount += 1;
                continue;
            }
            String effectiveStrategy = resolveReconcileStrategy(tx, safeStrategy);
            if (!StringUtils.hasText(effectiveStrategy)) {
                skippedCount += 1;
                continue;
            }
            processedCount += 1;
            try {
                WalletActionResultVo result = remediateTransaction(
                        userId,
                        tx.getId(),
                        effectiveStrategy,
                        "batch reconcile (" + safeStrategy + ")",
                        ipAddress
                );
                results.add(result);
                successCount += 1;
            } catch (BizException exception) {
                failedCount += 1;
                results.add(toFailedActionResult(tx, effectiveStrategy, exception.getMessage()));
            }
        }

        WalletBatchActionResultVo summary = new WalletBatchActionResultVo(
                String.valueOf(account.getId()),
                "BATCH_RECONCILE",
                safeMaxItems,
                processedCount,
                successCount,
                failedCount,
                skippedCount,
                results
        );
        auditService.record(
                userId,
                "WALLET_TX_BATCH_RECONCILE",
                "accountId=" + account.getId()
                        + ",requested=" + safeMaxItems
                        + ",processed=" + processedCount
                        + ",success=" + successCount
                        + ",failed=" + failedCount
                        + ",skipped=" + skippedCount
                        + ",strategy=" + safeStrategy,
                ipAddress
        );
        return summary;
    }

    @Transactional
    public WalletBatchActionResultVo batchAdvanceTransactions(
            Long userId,
            Long accountId,
            Integer maxItems,
            String operatorHint,
            String ipAddress
    ) {
        WalletAccount account = loadAccount(userId, accountId);
        int safeMaxItems = safeBatchMaxItems(maxItems);
        String safeOperatorHint = normalizeOptionalOperatorHint(operatorHint);
        LocalDateTime blockedThreshold = LocalDateTime.now().minusMinutes(BLOCKED_THRESHOLD_MINUTES);

        List<WalletTransaction> candidates = walletTransactionMapper.selectList(new LambdaQueryWrapper<WalletTransaction>()
                        .eq(WalletTransaction::getOwnerId, userId)
                        .eq(WalletTransaction::getAccountId, account.getId())
                        .orderByAsc(WalletTransaction::getUpdatedAt)
                        .last("limit " + MAX_OVERVIEW_TX))
                .stream()
                .sorted((left, right) -> {
                    int byPriority = Integer.compare(
                            executionQueuePriority(left, blockedThreshold),
                            executionQueuePriority(right, blockedThreshold)
                    );
                    if (byPriority != 0) {
                        return byPriority;
                    }
                    LocalDateTime leftTime = left.getUpdatedAt() == null ? LocalDateTime.MIN : left.getUpdatedAt();
                    LocalDateTime rightTime = right.getUpdatedAt() == null ? LocalDateTime.MIN : right.getUpdatedAt();
                    return leftTime.compareTo(rightTime);
                })
                .toList();

        int processedCount = 0;
        int successCount = 0;
        int failedCount = 0;
        int skippedCount = 0;
        List<WalletActionResultVo> results = new ArrayList<>();

        for (WalletTransaction tx : candidates) {
            if (processedCount >= safeMaxItems) {
                break;
            }
            if (!canAdvance(tx)) {
                skippedCount += 1;
                continue;
            }
            processedCount += 1;
            try {
                WalletActionResultVo result = advanceTransaction(
                        userId,
                        tx.getId(),
                        safeOperatorHint,
                        ipAddress
                );
                results.add(result);
                successCount += 1;
            } catch (BizException exception) {
                failedCount += 1;
                results.add(toFailedActionResult(tx, "ADVANCE", exception.getMessage()));
            }
        }

        WalletBatchActionResultVo summary = new WalletBatchActionResultVo(
                String.valueOf(account.getId()),
                "BATCH_ADVANCE",
                safeMaxItems,
                processedCount,
                successCount,
                failedCount,
                skippedCount,
                results
        );
        auditService.record(
                userId,
                "WALLET_TX_BATCH_ADVANCE",
                "accountId=" + account.getId()
                        + ",requested=" + safeMaxItems
                        + ",processed=" + processedCount
                        + ",success=" + successCount
                        + ",failed=" + failedCount
                        + ",skipped=" + skippedCount,
                ipAddress
        );
        return summary;
    }

    @Transactional
    public WalletBatchActionResultVo batchRemediateTransactions(
            Long userId,
            Long accountId,
            Integer maxItems,
            String strategy,
            String reason,
            String ipAddress
    ) {
        WalletAccount account = loadAccount(userId, accountId);
        int safeMaxItems = safeBatchMaxItems(maxItems);
        String safeStrategy = normalizeRemediationStrategy(strategy);
        String safeReason = normalizeFailReason(reason);
        LocalDateTime blockedThreshold = LocalDateTime.now().minusMinutes(BLOCKED_THRESHOLD_MINUTES);

        List<WalletTransaction> candidates = walletTransactionMapper.selectList(new LambdaQueryWrapper<WalletTransaction>()
                        .eq(WalletTransaction::getOwnerId, userId)
                        .eq(WalletTransaction::getAccountId, account.getId())
                        .orderByAsc(WalletTransaction::getUpdatedAt)
                        .last("limit " + MAX_OVERVIEW_TX))
                .stream()
                .sorted((left, right) -> {
                    int byPriority = Integer.compare(
                            remediationQueuePriority(left, blockedThreshold),
                            remediationQueuePriority(right, blockedThreshold)
                    );
                    if (byPriority != 0) {
                        return byPriority;
                    }
                    LocalDateTime leftTime = left.getUpdatedAt() == null ? LocalDateTime.MIN : left.getUpdatedAt();
                    LocalDateTime rightTime = right.getUpdatedAt() == null ? LocalDateTime.MIN : right.getUpdatedAt();
                    return leftTime.compareTo(rightTime);
                })
                .toList();

        int processedCount = 0;
        int successCount = 0;
        int failedCount = 0;
        int skippedCount = 0;
        List<WalletActionResultVo> results = new ArrayList<>();

        for (WalletTransaction tx : candidates) {
            if (processedCount >= safeMaxItems) {
                break;
            }
            if (!canRemediate(tx, blockedThreshold)) {
                skippedCount += 1;
                continue;
            }
            processedCount += 1;
            try {
                WalletActionResultVo result = remediateTransaction(
                        userId,
                        tx.getId(),
                        safeStrategy,
                        safeReason,
                        ipAddress
                );
                results.add(result);
                successCount += 1;
            } catch (BizException exception) {
                failedCount += 1;
                results.add(toFailedActionResult(tx, safeStrategy, exception.getMessage()));
            }
        }

        WalletBatchActionResultVo summary = new WalletBatchActionResultVo(
                String.valueOf(account.getId()),
                "BATCH_REMEDIATE",
                safeMaxItems,
                processedCount,
                successCount,
                failedCount,
                skippedCount,
                results
        );
        auditService.record(
                userId,
                "WALLET_TX_BATCH_REMEDIATE",
                "accountId=" + account.getId()
                        + ",requested=" + safeMaxItems
                        + ",processed=" + processedCount
                        + ",success=" + successCount
                        + ",failed=" + failedCount
                        + ",skipped=" + skippedCount
                        + ",strategy=" + safeStrategy,
                ipAddress
        );
        return summary;
    }

    @Transactional
    public WalletTransactionVo receive(
            Long userId,
            Long accountId,
            long amountMinor,
            String assetSymbol,
            String sourceAddress,
            String memo,
            String ipAddress
    ) {
        WalletAccount account = loadAccount(userId, accountId);
        long safeAmountMinor = requirePositiveAmount(amountMinor);
        String safeSymbol = normalizeAssetSymbol(assetSymbol);
        if (!safeSymbol.equals(account.getAssetSymbol())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet transaction asset mismatch");
        }
        String safeSourceAddress = normalizeAddress(sourceAddress);
        String safeMemo = normalizeMemo(memo);

        LocalDateTime now = LocalDateTime.now();
        WalletTransaction tx = new WalletTransaction();
        tx.setOwnerId(userId);
        tx.setAccountId(account.getId());
        tx.setTxType(TX_TYPE_RECEIVE);
        tx.setCounterpartyAddress(safeSourceAddress);
        tx.setAmountMinor(safeAmountMinor);
        tx.setAssetSymbol(safeSymbol);
        tx.setMemo(safeMemo);
        tx.setStatus(TX_STATUS_CONFIRMED);
        tx.setConfirmations(1);
        tx.setSignatureHash(null);
        tx.setNetworkTxHash(generateSyntheticHash("RX", safeSourceAddress, now));
        tx.setCreatedAt(now);
        tx.setUpdatedAt(now);
        tx.setDeleted(0);
        walletTransactionMapper.insert(tx);

        account.setBalanceMinor(safeAdd(account.getBalanceMinor(), safeAmountMinor));
        account.setUpdatedAt(now);
        walletAccountMapper.updateById(account);

        auditService.record(
                userId,
                "WALLET_TX_RECEIVE",
                "accountId=" + account.getId() + ",transactionId=" + tx.getId() + ",amountMinor=" + safeAmountMinor,
                ipAddress
        );
        return toTransactionVo(tx);
    }

    @Transactional
    public WalletTransactionVo send(
            Long userId,
            Long accountId,
            long amountMinor,
            String assetSymbol,
            String targetAddress,
            String memo,
            String ipAddress
    ) {
        WalletAccount account = loadAccount(userId, accountId);
        long safeAmountMinor = requirePositiveAmount(amountMinor);
        String safeSymbol = normalizeAssetSymbol(assetSymbol);
        if (!safeSymbol.equals(account.getAssetSymbol())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet transaction asset mismatch");
        }
        String safeTargetAddress = normalizeAddress(targetAddress);
        String safeMemo = normalizeMemo(memo);

        long currentBalance = account.getBalanceMinor() == null ? 0L : account.getBalanceMinor();
        if (currentBalance < safeAmountMinor) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet balance is insufficient");
        }

        LocalDateTime now = LocalDateTime.now();
        WalletTransaction tx = new WalletTransaction();
        tx.setOwnerId(userId);
        tx.setAccountId(account.getId());
        tx.setTxType(TX_TYPE_SEND);
        tx.setCounterpartyAddress(safeTargetAddress);
        tx.setAmountMinor(safeAmountMinor);
        tx.setAssetSymbol(safeSymbol);
        tx.setMemo(safeMemo);
        tx.setStatus(TX_STATUS_PENDING);
        tx.setConfirmations(0);
        tx.setSignatureHash(null);
        tx.setNetworkTxHash(null);
        tx.setCreatedAt(now);
        tx.setUpdatedAt(now);
        tx.setDeleted(0);
        walletTransactionMapper.insert(tx);

        account.setBalanceMinor(currentBalance - safeAmountMinor);
        account.setUpdatedAt(now);
        walletAccountMapper.updateById(account);

        auditService.record(
                userId,
                "WALLET_TX_SEND",
                "accountId=" + account.getId() + ",transactionId=" + tx.getId() + ",amountMinor=" + safeAmountMinor,
                ipAddress
        );
        return toTransactionVo(tx);
    }

    @Transactional
    public WalletActionResultVo advanceTransaction(
            Long userId,
            Long transactionId,
            String operatorHint,
            String ipAddress
    ) {
        WalletTransaction tx = loadTransaction(userId, transactionId);
        String fromStatus = tx.getStatus();
        String safeOperatorHint = normalizeOptionalOperatorHint(operatorHint);
        LocalDateTime now = LocalDateTime.now();
        String operation;
        if (TX_STATUS_PENDING.equals(fromStatus)) {
            tx.setStatus(TX_STATUS_SIGNED);
            tx.setSignatureHash(generateSyntheticHash("SIG", safeOperatorHint + "-" + tx.getId(), now));
            operation = "SIGN";
        } else if (TX_STATUS_SIGNED.equals(fromStatus)) {
            tx.setStatus(TX_STATUS_BROADCASTED);
            tx.setNetworkTxHash(generateSyntheticHash("TX", "advance-" + tx.getId(), now));
            operation = "BROADCAST";
        } else if (TX_STATUS_BROADCASTED.equals(fromStatus)) {
            tx.setStatus(TX_STATUS_CONFIRMED);
            tx.setConfirmations(Math.max(1, tx.getConfirmations() == null ? 0 : tx.getConfirmations()));
            if (!StringUtils.hasText(tx.getNetworkTxHash())) {
                tx.setNetworkTxHash(generateSyntheticHash("TX", "confirm-" + tx.getId(), now));
            }
            operation = "CONFIRM";
        } else {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet transaction is in terminal status");
        }
        tx.setUpdatedAt(now);
        walletTransactionMapper.updateById(tx);
        auditService.record(
                userId,
                "WALLET_TX_ADVANCE",
                "transactionId=" + tx.getId() + ",from=" + fromStatus + ",to=" + tx.getStatus() + ",operator=" + safeOperatorHint,
                ipAddress
        );
        return new WalletActionResultVo(
                toTransactionVo(tx),
                fromStatus,
                tx.getStatus(),
                operation,
                "Wallet transaction advanced successfully"
        );
    }

    @Transactional
    public WalletActionResultVo remediateTransaction(
            Long userId,
            Long transactionId,
            String strategy,
            String reason,
            String ipAddress
    ) {
        WalletTransaction tx = loadTransaction(userId, transactionId);
        String fromStatus = tx.getStatus();
        String safeStrategy = normalizeRemediationStrategy(strategy);
        String safeReason = normalizeFailReason(reason);
        LocalDateTime now = LocalDateTime.now();
        String message;

        if (REMEDIATE_RETRY_SIGN.equals(safeStrategy)) {
            if (TX_STATUS_FAILED.equals(tx.getStatus())) {
                prepareRetryFromFailedSendIfNeeded(userId, tx, now);
            }
            if (!TX_STATUS_PENDING.equals(tx.getStatus())) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "Retry sign requires pending or failed transaction");
            }
            tx.setStatus(TX_STATUS_SIGNED);
            tx.setSignatureHash(generateSyntheticHash("SIG", "retry-sign-" + tx.getId(), now));
            tx.setUpdatedAt(now);
            message = "Retry sign completed";
        } else if (REMEDIATE_RETRY_BROADCAST.equals(safeStrategy)) {
            if (TX_STATUS_FAILED.equals(tx.getStatus())) {
                prepareRetryFromFailedSendIfNeeded(userId, tx, now);
            }
            if (TX_STATUS_PENDING.equals(tx.getStatus())) {
                tx.setStatus(TX_STATUS_SIGNED);
                tx.setSignatureHash(generateSyntheticHash("SIG", "retry-sign-" + tx.getId(), now));
            }
            if (!TX_STATUS_SIGNED.equals(tx.getStatus()) && !TX_STATUS_BROADCASTED.equals(tx.getStatus())) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "Retry broadcast requires signed/broadcasted/failed transaction");
            }
            tx.setStatus(TX_STATUS_BROADCASTED);
            tx.setNetworkTxHash(generateSyntheticHash("TX", "retry-broadcast-" + tx.getId(), now));
            tx.setUpdatedAt(now);
            message = "Retry broadcast completed";
        } else {
            if (TX_STATUS_FAILED.equals(tx.getStatus())) {
                message = "Transaction already failed";
            } else {
                requirePendingSignedOrBroadcastedTransaction(tx);
                tx.setStatus(TX_STATUS_FAILED);
                tx.setUpdatedAt(now);
                if (TX_TYPE_SEND.equals(tx.getTxType())) {
                    WalletAccount account = loadAccount(userId, tx.getAccountId());
                    account.setBalanceMinor(safeAdd(account.getBalanceMinor(), tx.getAmountMinor() == null ? 0L : tx.getAmountMinor()));
                    account.setUpdatedAt(now);
                    walletAccountMapper.updateById(account);
                }
                message = "Rollback to failed completed";
            }
        }

        walletTransactionMapper.updateById(tx);
        auditService.record(
                userId,
                "WALLET_TX_REMEDIATE",
                "transactionId=" + tx.getId()
                        + ",strategy=" + safeStrategy
                        + ",from=" + fromStatus
                        + ",to=" + tx.getStatus()
                        + ",reason=" + safeReason,
                ipAddress
        );
        return new WalletActionResultVo(
                toTransactionVo(tx),
                fromStatus,
                tx.getStatus(),
                safeStrategy,
                message
        );
    }

    @Transactional
    public WalletTransactionVo signTransaction(
            Long userId,
            Long transactionId,
            String signerHint,
            String ipAddress
    ) {
        WalletTransaction tx = loadTransaction(userId, transactionId);
        requirePendingTransaction(tx);
        String safeSignerHint = normalizeSignerHint(signerHint);
        LocalDateTime now = LocalDateTime.now();
        String signatureHash = generateSyntheticHash("SIG", safeSignerHint + "-" + tx.getId(), now);

        tx.setStatus(TX_STATUS_SIGNED);
        tx.setSignatureHash(signatureHash);
        tx.setUpdatedAt(now);
        walletTransactionMapper.updateById(tx);

        auditService.record(
                userId,
                "WALLET_TX_SIGN",
                "transactionId=" + tx.getId() + ",signatureHash=" + signatureHash,
                ipAddress
        );
        return toTransactionVo(tx);
    }

    @Transactional
    public WalletTransactionVo broadcastTransaction(
            Long userId,
            Long transactionId,
            String networkTxHash,
            String ipAddress
    ) {
        WalletTransaction tx = loadTransaction(userId, transactionId);
        requirePendingOrSignedTransaction(tx);

        String safeNetworkTxHash = normalizeNetworkTxHash(networkTxHash);
        LocalDateTime now = LocalDateTime.now();

        tx.setStatus(TX_STATUS_BROADCASTED);
        tx.setNetworkTxHash(safeNetworkTxHash);
        tx.setUpdatedAt(now);
        walletTransactionMapper.updateById(tx);

        auditService.record(
                userId,
                "WALLET_TX_BROADCAST",
                "transactionId=" + tx.getId() + ",networkTxHash=" + safeNetworkTxHash,
                ipAddress
        );
        return toTransactionVo(tx);
    }

    @Transactional
    public WalletTransactionVo confirmTransaction(
            Long userId,
            Long transactionId,
            Integer confirmations,
            String networkTxHash,
            String ipAddress
    ) {
        WalletTransaction tx = loadTransaction(userId, transactionId);
        requirePendingSignedOrBroadcastedTransaction(tx);

        int safeConfirmations = requireConfirmations(confirmations);
        String safeNetworkTxHash = normalizeNetworkTxHash(networkTxHash);
        LocalDateTime now = LocalDateTime.now();

        tx.setStatus(TX_STATUS_CONFIRMED);
        tx.setConfirmations(safeConfirmations);
        tx.setNetworkTxHash(safeNetworkTxHash);
        tx.setUpdatedAt(now);
        walletTransactionMapper.updateById(tx);

        auditService.record(
                userId,
                "WALLET_TX_CONFIRM",
                "transactionId=" + tx.getId() + ",confirmations=" + safeConfirmations,
                ipAddress
        );
        return toTransactionVo(tx);
    }

    @Transactional
    public WalletTransactionVo failTransaction(
            Long userId,
            Long transactionId,
            String reason,
            String ipAddress
    ) {
        WalletTransaction tx = loadTransaction(userId, transactionId);
        requirePendingSignedOrBroadcastedTransaction(tx);
        String safeReason = normalizeFailReason(reason);
        LocalDateTime now = LocalDateTime.now();

        tx.setStatus(TX_STATUS_FAILED);
        tx.setUpdatedAt(now);
        walletTransactionMapper.updateById(tx);

        if (TX_TYPE_SEND.equals(tx.getTxType())) {
            WalletAccount account = loadAccount(userId, tx.getAccountId());
            account.setBalanceMinor(safeAdd(account.getBalanceMinor(), tx.getAmountMinor() == null ? 0L : tx.getAmountMinor()));
            account.setUpdatedAt(now);
            walletAccountMapper.updateById(account);
        }

        auditService.record(
                userId,
                "WALLET_TX_FAIL",
                "transactionId=" + tx.getId() + ",reason=" + safeReason,
                ipAddress
        );
        return toTransactionVo(tx);
    }

    private WalletAccount loadAccount(Long userId, Long accountId) {
        if (accountId == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet accountId is required");
        }
        WalletAccount account = walletAccountMapper.selectOne(new LambdaQueryWrapper<WalletAccount>()
                .eq(WalletAccount::getId, accountId)
                .eq(WalletAccount::getOwnerId, userId));
        if (account == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet account is not found");
        }
        return account;
    }

    private WalletTransaction loadTransaction(Long userId, Long transactionId) {
        if (transactionId == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet transactionId is required");
        }
        WalletTransaction tx = walletTransactionMapper.selectOne(new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getId, transactionId)
                .eq(WalletTransaction::getOwnerId, userId));
        if (tx == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet transaction is not found");
        }
        return tx;
    }

    private void requirePendingTransaction(WalletTransaction tx) {
        if (!TX_STATUS_PENDING.equals(tx.getStatus())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet transaction is not pending");
        }
    }

    private void requirePendingOrBroadcastedTransaction(WalletTransaction tx) {
        if (!TX_STATUS_PENDING.equals(tx.getStatus())
                && !TX_STATUS_SIGNED.equals(tx.getStatus())
                && !TX_STATUS_BROADCASTED.equals(tx.getStatus())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet transaction status cannot be changed");
        }
    }

    private void requirePendingOrSignedTransaction(WalletTransaction tx) {
        if (!TX_STATUS_PENDING.equals(tx.getStatus()) && !TX_STATUS_SIGNED.equals(tx.getStatus())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet transaction status cannot be broadcasted");
        }
    }

    private void requirePendingSignedOrBroadcastedTransaction(WalletTransaction tx) {
        requirePendingOrBroadcastedTransaction(tx);
    }

    private String requireWalletName(String walletName) {
        if (!StringUtils.hasText(walletName)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet name is required");
        }
        String safeWalletName = walletName.trim();
        if (safeWalletName.length() < 2 || safeWalletName.length() > 64) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet name length is invalid");
        }
        return safeWalletName;
    }

    private String normalizeAssetSymbol(String assetSymbol) {
        if (!StringUtils.hasText(assetSymbol)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet asset symbol is required");
        }
        String safeSymbol = assetSymbol.trim().toUpperCase(Locale.ROOT);
        if (safeSymbol.length() < 2 || safeSymbol.length() > 16) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet asset symbol is invalid");
        }
        return safeSymbol;
    }

    private String normalizeAddress(String address) {
        if (!StringUtils.hasText(address)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet address is required");
        }
        String safeAddress = address.trim();
        if (safeAddress.length() < 16 || safeAddress.length() > 128) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet address is invalid");
        }
        return safeAddress;
    }

    private String normalizeMemo(String memo) {
        if (memo == null) {
            return "";
        }
        String safeMemo = memo.trim();
        if (safeMemo.length() > 512) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet memo is too long");
        }
        return safeMemo;
    }

    private String normalizeNetworkTxHash(String hash) {
        if (!StringUtils.hasText(hash)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet network tx hash is required");
        }
        String safeHash = hash.trim();
        if (safeHash.length() < 8 || safeHash.length() > 128) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet network tx hash is invalid");
        }
        return safeHash;
    }

    private String normalizeSignerHint(String signerHint) {
        if (!StringUtils.hasText(signerHint)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet signer hint is required");
        }
        String safeSignerHint = signerHint.trim();
        if (safeSignerHint.length() < 2 || safeSignerHint.length() > 64) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet signer hint is invalid");
        }
        return safeSignerHint;
    }

    private String normalizeFailReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return "manual-fail";
        }
        String safeReason = reason.trim();
        if (safeReason.length() > 256) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet fail reason is too long");
        }
        return safeReason;
    }

    private String normalizeOptionalOperatorHint(String operatorHint) {
        if (!StringUtils.hasText(operatorHint)) {
            return "wallet-auto-operator";
        }
        String safeOperatorHint = operatorHint.trim();
        if (safeOperatorHint.length() > 64) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet operator hint is too long");
        }
        return safeOperatorHint;
    }

    private String normalizeRemediationStrategy(String strategy) {
        if (!StringUtils.hasText(strategy)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet remediation strategy is required");
        }
        String safeStrategy = strategy.trim().toUpperCase(Locale.ROOT);
        if (!REMEDIATE_RETRY_SIGN.equals(safeStrategy)
                && !REMEDIATE_RETRY_BROADCAST.equals(safeStrategy)
                && !REMEDIATE_ROLLBACK_FAIL.equals(safeStrategy)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet remediation strategy is invalid");
        }
        return safeStrategy;
    }

    private String normalizeReconcileStrategy(String strategy) {
        if (!StringUtils.hasText(strategy)) {
            return RECONCILE_AUTO;
        }
        String safeStrategy = strategy.trim().toUpperCase(Locale.ROOT);
        if (!RECONCILE_AUTO.equals(safeStrategy)
                && !REMEDIATE_RETRY_SIGN.equals(safeStrategy)
                && !REMEDIATE_RETRY_BROADCAST.equals(safeStrategy)
                && !REMEDIATE_ROLLBACK_FAIL.equals(safeStrategy)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet reconcile strategy is invalid");
        }
        return safeStrategy;
    }

    private String resolveReconcileStrategy(WalletTransaction tx, String requestedStrategy) {
        if (!RECONCILE_AUTO.equals(requestedStrategy)) {
            return requestedStrategy;
        }
        if (TX_STATUS_FAILED.equals(tx.getStatus())) {
            return REMEDIATE_RETRY_BROADCAST;
        }
        if (TX_STATUS_PENDING.equals(tx.getStatus())) {
            return REMEDIATE_RETRY_SIGN;
        }
        if (TX_STATUS_SIGNED.equals(tx.getStatus()) || TX_STATUS_BROADCASTED.equals(tx.getStatus())) {
            return REMEDIATE_RETRY_BROADCAST;
        }
        return null;
    }

    private void prepareRetryFromFailedSendIfNeeded(Long userId, WalletTransaction tx, LocalDateTime now) {
        if (!TX_STATUS_FAILED.equals(tx.getStatus())) {
            return;
        }
        if (TX_TYPE_SEND.equals(tx.getTxType())) {
            WalletAccount account = loadAccount(userId, tx.getAccountId());
            long amount = tx.getAmountMinor() == null ? 0L : tx.getAmountMinor();
            long currentBalance = account.getBalanceMinor() == null ? 0L : account.getBalanceMinor();
            if (currentBalance < amount) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet balance is insufficient for retry");
            }
            account.setBalanceMinor(currentBalance - amount);
            account.setUpdatedAt(now);
            walletAccountMapper.updateById(account);
        }
        tx.setStatus(TX_STATUS_PENDING);
        tx.setConfirmations(0);
        tx.setSignatureHash(null);
        tx.setNetworkTxHash(null);
        tx.setUpdatedAt(now);
    }

    private int requireConfirmations(Integer confirmations) {
        if (confirmations == null || confirmations < 0 || confirmations > 999999) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet confirmations is invalid");
        }
        return confirmations;
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String safeStatus = status.trim().toUpperCase(Locale.ROOT);
        if (!TX_STATUS_PENDING.equals(safeStatus)
                && !TX_STATUS_SIGNED.equals(safeStatus)
                && !TX_STATUS_BROADCASTED.equals(safeStatus)
                && !TX_STATUS_CONFIRMED.equals(safeStatus)
                && !TX_STATUS_FAILED.equals(safeStatus)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet transaction status is invalid");
        }
        return safeStatus;
    }

    private long requirePositiveAmount(long amountMinor) {
        if (amountMinor <= 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet amount must be positive");
        }
        return amountMinor;
    }

    private String generateSyntheticHash(String prefix, String source, LocalDateTime now) {
        String compact = source.replaceAll("[^A-Za-z0-9]", "");
        String tail = compact.length() > 12 ? compact.substring(compact.length() - 12) : compact;
        String hash = prefix + "-" + now.toString().replaceAll("[^0-9]", "") + "-" + tail;
        if (hash.length() <= 64) {
            return hash;
        }
        return hash.substring(0, 64);
    }

    private long safeAdd(Long current, long delta) {
        long base = current == null ? 0L : current;
        if (Long.MAX_VALUE - base < delta) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Wallet balance overflow");
        }
        return base + delta;
    }

    private int safeLimit(Integer limit, int defaultLimit) {
        if (limit == null) {
            return defaultLimit;
        }
        if (limit < 1) {
            return 1;
        }
        if (limit > MAX_LIMIT) {
            return MAX_LIMIT;
        }
        return limit;
    }

    private int safeBatchMaxItems(Integer maxItems) {
        if (maxItems == null) {
            return DEFAULT_BATCH_ITEMS;
        }
        if (maxItems < 1) {
            return 1;
        }
        if (maxItems > MAX_BATCH_ITEMS) {
            return MAX_BATCH_ITEMS;
        }
        return maxItems;
    }

    private boolean isBlocked(WalletTransaction tx, LocalDateTime threshold) {
        String status = tx.getStatus();
        if (!TX_STATUS_PENDING.equals(status)
                && !TX_STATUS_SIGNED.equals(status)
                && !TX_STATUS_BROADCASTED.equals(status)) {
            return false;
        }
        if (tx.getUpdatedAt() == null) {
            return false;
        }
        return tx.getUpdatedAt().isBefore(threshold);
    }

    private boolean canAdvance(WalletTransaction tx) {
        return TX_STATUS_PENDING.equals(tx.getStatus())
                || TX_STATUS_SIGNED.equals(tx.getStatus())
                || TX_STATUS_BROADCASTED.equals(tx.getStatus());
    }

    private boolean canRemediate(WalletTransaction tx, LocalDateTime blockedThreshold) {
        if (TX_STATUS_FAILED.equals(tx.getStatus())) {
            return true;
        }
        return isBlocked(tx, blockedThreshold);
    }

    private boolean hasReconciliationMismatch(WalletTransaction tx) {
        if (TX_STATUS_SIGNED.equals(tx.getStatus()) && !StringUtils.hasText(tx.getSignatureHash())) {
            return true;
        }
        if ((TX_STATUS_BROADCASTED.equals(tx.getStatus()) || TX_STATUS_CONFIRMED.equals(tx.getStatus()))
                && !StringUtils.hasText(tx.getNetworkTxHash())) {
            return true;
        }
        return TX_STATUS_CONFIRMED.equals(tx.getStatus())
                && (tx.getConfirmations() == null || tx.getConfirmations() <= 0);
    }

    private boolean canReconcile(WalletTransaction tx, LocalDateTime blockedThreshold) {
        if (TX_STATUS_FAILED.equals(tx.getStatus())) {
            return true;
        }
        if (TX_STATUS_BROADCASTED.equals(tx.getStatus())) {
            return isBlocked(tx, blockedThreshold) || !StringUtils.hasText(tx.getNetworkTxHash());
        }
        if (TX_STATUS_SIGNED.equals(tx.getStatus())) {
            return isBlocked(tx, blockedThreshold) || !StringUtils.hasText(tx.getSignatureHash());
        }
        if (TX_STATUS_PENDING.equals(tx.getStatus())) {
            return isBlocked(tx, blockedThreshold);
        }
        return false;
    }

    private int executionQueuePriority(WalletTransaction tx, LocalDateTime blockedThreshold) {
        if (!canAdvance(tx)) {
            return 99;
        }
        if (TX_STATUS_BROADCASTED.equals(tx.getStatus()) && isBlocked(tx, blockedThreshold)) {
            return 0;
        }
        if (TX_STATUS_SIGNED.equals(tx.getStatus()) && isBlocked(tx, blockedThreshold)) {
            return 1;
        }
        if (TX_STATUS_PENDING.equals(tx.getStatus()) && isBlocked(tx, blockedThreshold)) {
            return 2;
        }
        if (TX_STATUS_BROADCASTED.equals(tx.getStatus())) {
            return 3;
        }
        if (TX_STATUS_SIGNED.equals(tx.getStatus())) {
            return 4;
        }
        return 5;
    }

    private int remediationQueuePriority(WalletTransaction tx, LocalDateTime blockedThreshold) {
        if (TX_STATUS_FAILED.equals(tx.getStatus())) {
            return 0;
        }
        if (TX_STATUS_BROADCASTED.equals(tx.getStatus()) && isBlocked(tx, blockedThreshold)) {
            return 1;
        }
        if (TX_STATUS_SIGNED.equals(tx.getStatus()) && isBlocked(tx, blockedThreshold)) {
            return 2;
        }
        if (TX_STATUS_PENDING.equals(tx.getStatus()) && isBlocked(tx, blockedThreshold)) {
            return 3;
        }
        return 99;
    }

    private int reconciliationQueuePriority(WalletTransaction tx, LocalDateTime blockedThreshold) {
        if (TX_STATUS_FAILED.equals(tx.getStatus())) {
            return 0;
        }
        if (TX_STATUS_BROADCASTED.equals(tx.getStatus())
                && (isBlocked(tx, blockedThreshold) || !StringUtils.hasText(tx.getNetworkTxHash()))) {
            return 1;
        }
        if (TX_STATUS_SIGNED.equals(tx.getStatus())
                && (isBlocked(tx, blockedThreshold) || !StringUtils.hasText(tx.getSignatureHash()))) {
            return 2;
        }
        if (TX_STATUS_PENDING.equals(tx.getStatus()) && isBlocked(tx, blockedThreshold)) {
            return 3;
        }
        return 99;
    }

    private WalletExecutionPlanItemVo toExecutionPlanItem(
            WalletTransaction tx,
            LocalDateTime blockedThreshold,
            LocalDateTime now
    ) {
        int ageMinutes = 0;
        if (tx.getUpdatedAt() != null) {
            ageMinutes = (int) Math.max(0, Duration.between(tx.getUpdatedAt(), now).toMinutes());
        }

        if (TX_STATUS_FAILED.equals(tx.getStatus())) {
            return new WalletExecutionPlanItemVo(
                    String.valueOf(tx.getId()),
                    tx.getStatus(),
                    "Failed transaction should be remediated first.",
                    REMEDIATE_RETRY_BROADCAST,
                    0
            );
        }

        boolean blocked = isBlocked(tx, blockedThreshold);
        if (TX_STATUS_BROADCASTED.equals(tx.getStatus()) && blocked) {
            return new WalletExecutionPlanItemVo(
                    String.valueOf(tx.getId()),
                    tx.getStatus(),
                    "Broadcasted transaction is blocked for " + ageMinutes + " minutes.",
                    REMEDIATE_RETRY_BROADCAST,
                    1
            );
        }
        if (TX_STATUS_SIGNED.equals(tx.getStatus()) && blocked) {
            return new WalletExecutionPlanItemVo(
                    String.valueOf(tx.getId()),
                    tx.getStatus(),
                    "Signed transaction is waiting too long before confirmation.",
                    "ADVANCE",
                    2
            );
        }
        if (TX_STATUS_PENDING.equals(tx.getStatus()) && blocked) {
            return new WalletExecutionPlanItemVo(
                    String.valueOf(tx.getId()),
                    tx.getStatus(),
                    "Pending transaction is blocked before signing.",
                    "ADVANCE",
                    3
            );
        }
        if (TX_STATUS_SIGNED.equals(tx.getStatus()) || TX_STATUS_PENDING.equals(tx.getStatus())) {
            return new WalletExecutionPlanItemVo(
                    String.valueOf(tx.getId()),
                    tx.getStatus(),
                    "Transaction can be advanced in current execution cycle.",
                    "ADVANCE",
                    4
            );
        }
        if (TX_STATUS_BROADCASTED.equals(tx.getStatus())) {
            return new WalletExecutionPlanItemVo(
                    String.valueOf(tx.getId()),
                    tx.getStatus(),
                    "Broadcasted transaction can be confirmed by advance action.",
                    "ADVANCE",
                    5
            );
        }
        return null;
    }

    private WalletActionResultVo toFailedActionResult(WalletTransaction tx, String operation, String errorMessage) {
        String fromStatus = tx.getStatus();
        String safeMessage = StringUtils.hasText(errorMessage) ? errorMessage : "Wallet action failed";
        return new WalletActionResultVo(
                toTransactionVo(tx),
                fromStatus,
                fromStatus,
                operation,
                safeMessage
        );
    }

    private WalletPriorityTransactionVo toPriorityTransactionVo(WalletTransaction tx, boolean blocked, LocalDateTime now) {
        int ageMinutes = 0;
        if (tx.getUpdatedAt() != null) {
            ageMinutes = (int) Math.max(0, Duration.between(tx.getUpdatedAt(), now).toMinutes());
        }
        String reason;
        List<String> actions;
        if (TX_STATUS_FAILED.equals(tx.getStatus())) {
            reason = "Transaction is failed and requires recovery or rollback.";
            actions = List.of(REMEDIATE_ROLLBACK_FAIL, REMEDIATE_RETRY_SIGN, REMEDIATE_RETRY_BROADCAST);
        } else if (TX_STATUS_BROADCASTED.equals(tx.getStatus()) && blocked) {
            reason = "Transaction is broadcasted for too long without confirmation.";
            actions = List.of(REMEDIATE_RETRY_BROADCAST, REMEDIATE_ROLLBACK_FAIL);
        } else if (TX_STATUS_SIGNED.equals(tx.getStatus()) && blocked) {
            reason = "Transaction is signed but has not been broadcasted in time.";
            actions = List.of(REMEDIATE_RETRY_BROADCAST, REMEDIATE_ROLLBACK_FAIL);
        } else {
            reason = "Transaction is pending and stuck before signing.";
            actions = List.of(REMEDIATE_RETRY_SIGN, REMEDIATE_ROLLBACK_FAIL);
        }
        return new WalletPriorityTransactionVo(
                String.valueOf(tx.getId()),
                tx.getStatus(),
                ageMinutes,
                reason,
                actions
        );
    }

    private int statusPriority(String status) {
        if (TX_STATUS_FAILED.equals(status)) {
            return 0;
        }
        if (TX_STATUS_BROADCASTED.equals(status)) {
            return 1;
        }
        if (TX_STATUS_SIGNED.equals(status)) {
            return 2;
        }
        if (TX_STATUS_PENDING.equals(status)) {
            return 3;
        }
        return 9;
    }

    private String toRiskLevel(int score) {
        if (score < 45) {
            return RISK_CRITICAL;
        }
        if (score < 65) {
            return RISK_HIGH;
        }
        if (score < 80) {
            return RISK_MEDIUM;
        }
        return RISK_LOW;
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private WalletAccountVo toAccountVo(WalletAccount account) {
        return new WalletAccountVo(
                String.valueOf(account.getId()),
                account.getWalletName(),
                account.getAssetSymbol(),
                account.getAddress(),
                account.getBalanceMinor() == null ? 0L : account.getBalanceMinor(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    private WalletTransactionVo toTransactionVo(WalletTransaction tx) {
        return new WalletTransactionVo(
                String.valueOf(tx.getId()),
                String.valueOf(tx.getAccountId()),
                tx.getTxType(),
                tx.getCounterpartyAddress(),
                tx.getAmountMinor() == null ? 0L : tx.getAmountMinor(),
                tx.getAssetSymbol(),
                tx.getMemo(),
                tx.getStatus(),
                tx.getConfirmations() == null ? 0 : tx.getConfirmations(),
                tx.getSignatureHash(),
                tx.getNetworkTxHash(),
                tx.getCreatedAt()
        );
    }
}
