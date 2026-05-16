declare namespace Api {
  namespace Wallet {
    type TransactionStatus = 'PENDING' | 'SIGNED' | 'BROADCASTED' | 'CONFIRMED' | 'FAILED';

    interface AccountParams {
      limit?: number;
    }

    interface AccountPayload {
      walletName: string;
      assetSymbol: string;
      address: string;
    }

    interface TransactionParams {
      accountId: string;
      status?: TransactionStatus | string;
      limit?: number;
    }

    interface ExecutionPlanParams {
      accountId: string;
      maxItems?: number;
    }

    interface StageCounts {
      pendingCount: number;
      signedCount: number;
      broadcastedCount: number;
      confirmedCount: number;
      failedCount: number;
    }

    interface PriorityTransaction {
      transactionId: string;
      status: string;
      ageMinutes: number;
      reason: string;
      recommendedActions: string[];
    }

    interface ExecutionOverview {
      stageCounts: StageCounts;
      priorityTransactions: PriorityTransaction[];
    }

    interface ExecutionPlanItem {
      transactionId: string;
      status: string;
      reason: string;
      recommendedOperation: string;
      priority: number;
    }

    interface ExecutionPlan {
      accountId: string;
      generatedAt: string;
      recommendedAdvanceCount: number;
      recommendedRemediationCount: number;
      estimatedRiskDelta: number;
      items: ExecutionPlanItem[];
    }

    interface TraceStageEvent {
      stage: string;
      at: string;
      source: string;
      message: string;
    }

    interface ExecutionTrace {
      transactionId: string;
      currentStatus: string;
      integrityScore: number;
      warnings: string[];
      stageEvents: TraceStageEvent[];
    }

    interface ReconciliationOverview {
      accountId: string;
      generatedAt: string;
      integrityScore: number;
      riskLevel: string;
      mismatchCount: number;
      blockedCount: number;
      failedCount: number;
      recommendedActions: string[];
    }

    interface ReceivePayload {
      accountId: string;
      amountMinor: number;
      assetSymbol: string;
      sourceAddress: string;
      memo?: string;
    }

    interface SendPayload {
      accountId: string;
      amountMinor: number;
      assetSymbol: string;
      targetAddress: string;
      memo?: string;
    }

    interface AdvancePayload {
      operatorHint?: string;
    }

    interface RemediatePayload {
      strategy: string;
      reason?: string;
    }

    interface ConfirmPayload {
      confirmations: number;
      networkTxHash: string;
    }

    interface SignPayload {
      signerHint: string;
    }

    interface BroadcastPayload {
      networkTxHash: string;
    }

    interface FailPayload {
      reason?: string;
    }

    interface BatchAdvancePayload extends AdvancePayload {
      accountId: string;
      maxItems: number;
    }

    interface BatchRemediatePayload extends RemediatePayload {
      accountId: string;
      maxItems: number;
    }

    interface BatchReconcilePayload {
      accountId: string;
      maxItems: number;
      strategy?: string;
    }

    interface ActionResult {
      transaction: Transaction;
      fromStatus: string;
      toStatus: string;
      operation: string;
      message: string;
    }

    interface BatchActionResult {
      accountId: string;
      operation: string;
      requestedCount: number;
      processedCount: number;
      successCount: number;
      failedCount: number;
      skippedCount: number;
      results: ActionResult[];
    }
  }
}
