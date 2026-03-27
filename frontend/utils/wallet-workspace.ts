import type {
  WalletExecutionOverview,
  WalletRemediationStrategy,
  WalletTransaction,
  WalletTransactionStatus,
  WalletTransactionType
} from '~/types/api'

type TagType = 'success' | 'warning' | 'danger' | 'info'

export function formatWalletWorkspaceAmount(amountMinor: number, assetSymbol: string): string {
  return `${amountMinor.toLocaleString()} ${assetSymbol}`
}

export function walletStatusTagType(status: WalletTransactionStatus): TagType {
  if (status === 'CONFIRMED') {
    return 'success'
  }
  if (status === 'BROADCASTED') {
    return 'info'
  }
  if (status === 'FAILED') {
    return 'danger'
  }
  return 'warning'
}

export function walletRiskTagType(risk: WalletExecutionOverview['riskLevel']): 'success' | 'warning' | 'danger' {
  if (risk === 'LOW') {
    return 'success'
  }
  if (risk === 'MEDIUM') {
    return 'warning'
  }
  return 'danger'
}

export function canSignWalletTransaction(tx: WalletTransaction): boolean {
  return tx.status === 'PENDING'
}

export function canBroadcastWalletTransaction(tx: WalletTransaction): boolean {
  return tx.status === 'PENDING' || tx.status === 'SIGNED'
}

export function canFinalizeWalletTransaction(tx: WalletTransaction): boolean {
  return tx.status === 'PENDING' || tx.status === 'SIGNED' || tx.status === 'BROADCASTED'
}

export function canAdvanceWalletTransaction(tx: WalletTransaction): boolean {
  return tx.status === 'PENDING' || tx.status === 'SIGNED' || tx.status === 'BROADCASTED'
}

export function canRemediateWalletTransaction(tx: WalletTransaction): boolean {
  return tx.status === 'PENDING'
    || tx.status === 'SIGNED'
    || tx.status === 'BROADCASTED'
    || tx.status === 'FAILED'
}

export function defaultWalletRemediationStrategy(status: WalletTransactionStatus): WalletRemediationStrategy {
  if (status === 'FAILED') {
    return 'RETRY_BROADCAST'
  }
  if (status === 'PENDING') {
    return 'RETRY_SIGN'
  }
  if (status === 'SIGNED' || status === 'BROADCASTED') {
    return 'RETRY_BROADCAST'
  }
  return 'ROLLBACK_FAIL'
}

export function buildManualWalletTxHash(transactionId: string): string {
  return `manual-${Date.now()}-${transactionId.slice(-8)}`
}

export function walletStatusKey(status: WalletTransactionStatus): string {
  return `wallet.workspace.status.${status}`
}

export function walletRiskKey(risk: WalletExecutionOverview['riskLevel']): string {
  return `wallet.workspace.risk.${risk}`
}

export function walletTransactionTypeKey(type: WalletTransactionType): string {
  return `wallet.workspace.txType.${type}`
}

export function walletOperationKey(operation: 'ADVANCE' | WalletRemediationStrategy): string {
  return `wallet.workspace.operation.${operation}`
}
