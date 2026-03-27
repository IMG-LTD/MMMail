import { describe, expect, it } from 'vitest'
import { messages } from '../locales'
import type {
  SuiteBillingInvoice,
  SuiteBillingPaymentMethod,
  SuiteBillingSubscriptionAction
} from '../types/suite-lumo'
import { translate } from '../utils/i18n'
import {
  countPendingSuiteInvoices,
  isSuiteBillingActionRunnable,
  pickDefaultSuitePaymentMethod
} from '../utils/suite-billing-center'

const paymentMethods: SuiteBillingPaymentMethod[] = [
  {
    id: 11,
    methodType: 'CARD',
    displayLabel: 'Corporate Visa · 4242',
    brand: 'VISA',
    lastFour: '4242',
    expiresAt: '2028-09',
    defaultMethod: false,
    status: 'ACTIVE'
  },
  {
    id: 12,
    methodType: 'PAYPAL',
    displayLabel: 'Finance PayPal',
    brand: null,
    lastFour: null,
    expiresAt: null,
    defaultMethod: true,
    status: 'ACTIVE'
  }
]

const invoices: SuiteBillingInvoice[] = [
  {
    invoiceNumber: 'INV-001',
    offerCode: 'DRIVE_PLUS',
    offerName: 'Drive Plus',
    invoiceStatus: 'PENDING',
    currencyCode: 'USD',
    totalCents: 4788,
    billingCycle: 'ANNUAL',
    seatCount: 1,
    issuedAt: '2026-03-13T09:00:00',
    dueAt: '2026-03-14T09:00:00',
    downloadCode: 'DL-100'
  }
]

describe('suite billing center helpers', () => {
  it('picks the explicit default payment method first', () => {
    expect(pickDefaultSuitePaymentMethod(paymentMethods)?.id).toBe(12)
    expect(pickDefaultSuitePaymentMethod([paymentMethods[0]])?.id).toBe(11)
  })

  it('counts pending invoices only', () => {
    expect(countPendingSuiteInvoices(invoices)).toBe(1)
    expect(countPendingSuiteInvoices([])).toBe(0)
  })

  it('runs only enabled and unscheduled actions', () => {
    const readyAction: SuiteBillingSubscriptionAction = {
      actionCode: 'APPLY_LATEST_DRAFT',
      actionStatus: 'AVAILABLE',
      enabled: true,
      targetOfferCode: 'DRIVE_PLUS',
      targetOfferName: 'Drive Plus',
      effectiveAt: '2026-03-14T09:00:00',
      reasonCode: null
    }
    const scheduledAction: SuiteBillingSubscriptionAction = {
      ...readyAction,
      actionCode: 'CANCEL_AUTO_RENEW',
      actionStatus: 'SCHEDULED'
    }
    const lockedAction: SuiteBillingSubscriptionAction = {
      ...readyAction,
      actionCode: 'RESUME_AUTO_RENEW',
      actionStatus: 'LOCKED',
      enabled: false,
      reasonCode: 'AUTO_RENEW_ENABLED'
    }

    expect(isSuiteBillingActionRunnable(readyAction)).toBe(true)
    expect(isSuiteBillingActionRunnable(scheduledAction)).toBe(false)
    expect(isSuiteBillingActionRunnable(lockedAction)).toBe(false)
  })

  it('registers billing center translations in all supported locales', () => {
    expect(translate(messages, 'en', 'suite.billing.center.title'))
      .toBe('Payment methods, invoices, and subscription controls')
    expect(translate(messages, 'zh-CN', 'suite.billing.center.paymentMethods.actions.add'))
      .toBe('添加付款方式')
    expect(translate(messages, 'zh-TW', 'suite.billing.center.actions.reason.PAYMENT_METHOD_REQUIRED'))
      .toBe('請先新增並設定預設付款方式。')
  })
})
