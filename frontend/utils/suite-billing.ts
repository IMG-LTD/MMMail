import type {
  SuiteBillingOverview,
  SuiteBillingQuote,
  SuiteCheckoutDraft,
  SuiteOfferCode,
  SuitePlanSegment,
  SuitePricingOffer
} from '../types/suite-lumo'

const SEGMENT_ORDER: readonly SuitePlanSegment[] = ['CONSUMER', 'BUSINESS', 'SECURITY'] as const
const STANDALONE_OFFER_PRIORITY: readonly SuiteOfferCode[] = ['PASS_PLUS', 'DRIVE_PLUS'] as const

export interface SuitePricingSection {
  segment: SuitePlanSegment
  offers: SuitePricingOffer[]
}

function findOfferCode(offers: readonly SuitePricingOffer[], offerCode: SuiteOfferCode): SuiteOfferCode | null {
  return offers.some(offer => offer.code === offerCode) ? offerCode : null
}

export function buildSuitePricingSections(offers: readonly SuitePricingOffer[]): SuitePricingSection[] {
  return SEGMENT_ORDER.map(segment => ({
    segment,
    offers: offers.filter(offer => offer.segment === segment)
  })).filter(section => section.offers.length > 0)
}

export function pickDefaultSuiteOfferCode(
  offers: readonly SuitePricingOffer[],
  overview: SuiteBillingOverview | null
): SuiteOfferCode | null {
  const draftOfferCode = overview?.latestDraft?.offerCode
  if (draftOfferCode && findOfferCode(offers, draftOfferCode)) {
    return draftOfferCode
  }
  for (const offerCode of STANDALONE_OFFER_PRIORITY) {
    const prioritizedCode = findOfferCode(offers, offerCode)
    if (prioritizedCode) {
      return prioritizedCode
    }
  }
  const firstSelfServeOffer = offers.find(offer => offer.checkoutMode === 'SELF_SERVE')
  if (firstSelfServeOffer) {
    return firstSelfServeOffer.code
  }
  return offers[0]?.code ?? null
}

export function buildSuiteQuoteFromDraft(draft: SuiteCheckoutDraft | null): SuiteBillingQuote | null {
  if (!draft) {
    return null
  }
  return {
    offerCode: draft.offerCode,
    offerName: draft.offerName,
    quoteStatus: draft.quoteStatus,
    checkoutMode: draft.checkoutMode,
    currencyCode: draft.currencyCode,
    billingCycle: draft.billingCycle,
    seatCount: draft.seatCount,
    marketingBadge: draft.marketingBadge,
    invoiceSummary: draft.invoiceSummary,
    entitlementSummary: draft.entitlementSummary,
    onboardingSummary: draft.onboardingSummary
  }
}
