import { describe, expect, it } from 'vitest'
import { messages } from '../locales'
import type {
  SuiteBillingOverview,
  SuiteCheckoutDraft,
  SuitePricingOffer
} from '../types/suite-lumo'
import { translate } from '../utils/i18n'
import {
  buildSuitePricingSections,
  buildSuiteQuoteFromDraft,
  pickDefaultSuiteOfferCode
} from '../utils/suite-billing'

const offers: SuitePricingOffer[] = [
  {
    code: 'BUSINESS_SUITE',
    name: 'Business Suite',
    description: 'sales-led',
    segment: 'BUSINESS',
    linkedPlanCode: 'BUSINESS_SUITE',
    checkoutMode: 'CONTACT_SALES',
    priceMode: 'CONTACT_SALES',
    currencyCode: null,
    priceValue: null,
    originalPriceValue: null,
    priceNote: null,
    defaultBillingCycle: 'ANNUAL',
    billingCycles: ['MONTHLY', 'ANNUAL'],
    defaultSeatCount: 5,
    seatEditable: false,
    organizationRequired: true,
    recommended: true,
    marketingBadge: null,
    highlights: ['Managed rollout'],
    enabledProducts: ['MAIL', 'DRIVE']
  },
  {
    code: 'PASS_PLUS',
    name: 'Pass Plus',
    description: 'single-product',
    segment: 'CONSUMER',
    linkedPlanCode: 'PASS_PLUS',
    checkoutMode: 'SELF_SERVE',
    priceMode: 'FROM',
    currencyCode: 'USD',
    priceValue: '$2.99',
    originalPriceValue: '$4.99',
    priceNote: 'Billed at $35.88 every 12 months',
    defaultBillingCycle: 'ANNUAL',
    billingCycles: ['MONTHLY', 'ANNUAL'],
    defaultSeatCount: 1,
    seatEditable: false,
    organizationRequired: false,
    recommended: false,
    marketingBadge: '40% OFF',
    highlights: ['Vaults'],
    enabledProducts: ['PASS']
  },
  {
    code: 'DRIVE_PLUS',
    name: 'Drive Plus',
    description: 'single-product',
    segment: 'CONSUMER',
    linkedPlanCode: 'DRIVE_PLUS',
    checkoutMode: 'SELF_SERVE',
    priceMode: 'FROM',
    currencyCode: 'USD',
    priceValue: '$3.99',
    originalPriceValue: '$4.99',
    priceNote: 'Billed at $47.88 every 12 months',
    defaultBillingCycle: 'ANNUAL',
    billingCycles: ['MONTHLY', 'ANNUAL'],
    defaultSeatCount: 1,
    seatEditable: false,
    organizationRequired: false,
    recommended: false,
    marketingBadge: '20% OFF',
    highlights: ['200 GB'],
    enabledProducts: ['DRIVE', 'DOCS']
  }
]

const latestDraft: SuiteCheckoutDraft = {
  offerCode: 'DRIVE_PLUS',
  offerName: 'Drive Plus',
  quoteStatus: 'READY',
  checkoutMode: 'SELF_SERVE',
  currencyCode: 'USD',
  billingCycle: 'ANNUAL',
  seatCount: 1,
  marketingBadge: '20% OFF',
  organizationName: null,
  domainName: null,
  invoiceSummary: {
    currencyCode: 'USD',
    billingCycle: 'ANNUAL',
    seatCount: 1,
    billingMonths: 12,
    subtotalCents: 5988,
    discountCents: 1200,
    totalCents: 4788,
    lineItems: [
      {
        lineCode: 'PRIMARY_OFFER',
        quantity: 12,
        unitPriceCents: 399,
        totalPriceCents: 4788
      }
    ]
  },
  entitlementSummary: {
    offerCode: 'DRIVE_PLUS',
    linkedPlanCode: 'DRIVE_PLUS',
    primaryProductCode: 'DRIVE',
    supportTier: 'STANDARD',
    workspaceMode: 'PERSONAL',
    seatCount: 1,
    prioritySupport: false,
    unlockedProducts: ['DRIVE', 'DOCS'],
    highlights: ['200 GB']
  },
  onboardingSummary: {
    onboardingMode: 'SELF_SERVE',
    nextAction: 'START_CHECKOUT',
    organizationRequired: false,
    checklistCodes: ['VERIFY_ACCOUNT', 'UPLOAD_FILES']
  },
  updatedAt: '2026-03-13T08:45:00'
}

describe('suite billing helpers', () => {
  it('groups pricing offers by segment order', () => {
    expect(buildSuitePricingSections(offers)).toEqual([
      { segment: 'CONSUMER', offers: [offers[1], offers[2]] },
      { segment: 'BUSINESS', offers: [offers[0]] }
    ])
  })

  it('prefers latest draft and standalone fallbacks for default selection', () => {
    const overview: SuiteBillingOverview = {
      activePlanCode: 'FREE',
      activePlanName: 'Free',
      latestDraft,
      selfServeOfferCodes: ['PASS_PLUS', 'DRIVE_PLUS'],
      contactSalesOfferCodes: ['BUSINESS_SUITE']
    }
    expect(pickDefaultSuiteOfferCode(offers, overview)).toBe('DRIVE_PLUS')
    expect(pickDefaultSuiteOfferCode(offers, {
      ...overview,
      latestDraft: null
    })).toBe('PASS_PLUS')
  })

  it('rebuilds a quote snapshot from the latest draft', () => {
    const quote = buildSuiteQuoteFromDraft(latestDraft)

    expect(quote?.offerCode).toBe('DRIVE_PLUS')
    expect(quote?.invoiceSummary?.totalCents).toBe(4788)
    expect(quote?.entitlementSummary.unlockedProducts).toEqual(['DRIVE', 'DOCS'])
  })

  it('registers billing translations in all supported locales', () => {
    expect(translate(messages, 'en', 'suite.billing.compare.title'))
      .toBe('Compare public offers and single-product entry points')
    expect(translate(messages, 'zh-CN', 'suite.billing.checkout.actions.saveDraft')).toBe('保存结算草案')
    expect(translate(messages, 'zh-TW', 'suite.billing.quoteStatus.CONTACT_SALES')).toBe('聯絡銷售')
  })
})
