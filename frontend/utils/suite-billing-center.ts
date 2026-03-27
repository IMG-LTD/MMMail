import type {
  SuiteBillingInvoice,
  SuiteBillingPaymentMethod,
  SuiteBillingSubscriptionAction
} from '../types/suite-lumo'

export function pickDefaultSuitePaymentMethod(
  paymentMethods: readonly SuiteBillingPaymentMethod[]
): SuiteBillingPaymentMethod | null {
  return paymentMethods.find(method => method.defaultMethod) ?? paymentMethods[0] ?? null
}

export function countPendingSuiteInvoices(invoices: readonly SuiteBillingInvoice[]): number {
  return invoices.filter(invoice => invoice.invoiceStatus === 'PENDING').length
}

export function isSuiteBillingActionRunnable(action: SuiteBillingSubscriptionAction): boolean {
  return action.enabled && action.actionStatus !== 'SCHEDULED'
}
