import type {
  LumoConversation as BaseLumoConversation,
  LumoMessage as BaseLumoMessage,
  LumoProject,
  LumoProjectKnowledge,
  SuiteProductItem,
  SuiteSubscription as BaseSuiteSubscription
} from './api'

export type SuitePlanCode =
  | 'FREE'
  | 'MAIL_PLUS'
  | 'DUO'
  | 'FAMILY'
  | 'UNLIMITED'
  | 'VISIONARY'
  | 'MAIL_ESSENTIALS'
  | 'MAIL_PROFESSIONAL'
  | 'BUSINESS_SUITE'
  | 'ENTERPRISE'
  | 'VPN_PROFESSIONAL'
  | 'VPN_PASS_PROFESSIONAL'
  | 'SENTINEL'

export type SuiteOfferCode = SuitePlanCode | 'PASS_PLUS' | 'DRIVE_PLUS'

export type SuitePlanSegment = 'CONSUMER' | 'BUSINESS' | 'SECURITY'
export type SuitePlanPriceMode = 'FREE' | 'FROM' | 'PER_USER' | 'CONTACT_SALES' | 'ADD_ON'
export type SuiteBillingCycle = 'MONTHLY' | 'ANNUAL'
export type SuiteCheckoutMode = 'SELF_SERVE' | 'CONTACT_SALES'
export type SuiteBillingPaymentMethodType = 'CARD' | 'PAYPAL' | 'BITCOIN' | 'CASH'
export type SuiteBillingInvoiceStatus = 'PENDING'
export type SuiteBillingActionCode = 'APPLY_LATEST_DRAFT' | 'CANCEL_AUTO_RENEW' | 'RESUME_AUTO_RENEW'
export type SuiteBillingActionStatus = 'AVAILABLE' | 'LOCKED' | 'SCHEDULED'

export interface SuitePlan {
  code: SuitePlanCode
  name: string
  description: string
  segment: SuitePlanSegment
  priceMode: SuitePlanPriceMode
  priceValue: string | null
  recommended: boolean
  highlights: string[]
  upgradeTargets: SuitePlanCode[]
  mailDailySendLimit: number
  contactLimit: number
  calendarEventLimit: number
  calendarShareLimit: number
  driveStorageMb: number
  enabledProducts: string[]
}

export interface SuiteSubscription extends Omit<BaseSuiteSubscription, 'planCode' | 'plan'> {
  planCode: SuitePlanCode
  plan: SuitePlan
}

export interface ChangeSuitePlanRequest {
  planCode: SuitePlanCode
}

export interface SuitePricingOffer {
  code: SuiteOfferCode
  name: string
  description: string
  segment: SuitePlanSegment
  linkedPlanCode: SuiteOfferCode
  checkoutMode: SuiteCheckoutMode
  priceMode: 'FROM' | 'CONTACT_SALES'
  currencyCode: string | null
  priceValue: string | null
  originalPriceValue: string | null
  priceNote: string | null
  defaultBillingCycle: SuiteBillingCycle
  billingCycles: SuiteBillingCycle[]
  defaultSeatCount: number
  seatEditable: boolean
  organizationRequired: boolean
  recommended: boolean
  marketingBadge: string | null
  highlights: string[]
  enabledProducts: string[]
}

export interface SuiteInvoiceLine {
  lineCode: string
  quantity: number
  unitPriceCents: number
  totalPriceCents: number
}

export interface SuiteInvoiceSummary {
  currencyCode: string
  billingCycle: SuiteBillingCycle
  seatCount: number
  billingMonths: number
  subtotalCents: number
  discountCents: number
  totalCents: number
  lineItems: SuiteInvoiceLine[]
}

export interface SuiteEntitlementSummary {
  offerCode: SuiteOfferCode
  linkedPlanCode: SuiteOfferCode
  primaryProductCode: string | null
  supportTier: string
  workspaceMode: string
  seatCount: number
  prioritySupport: boolean
  unlockedProducts: string[]
  highlights: string[]
}

export interface SuiteOnboardingSummary {
  onboardingMode: 'SELF_SERVE' | 'CONTACT_SALES'
  nextAction: string
  organizationRequired: boolean
  checklistCodes: string[]
}

export interface SuiteBillingQuote {
  offerCode: SuiteOfferCode
  offerName: string
  quoteStatus: 'READY' | 'CONTACT_SALES'
  checkoutMode: SuiteCheckoutMode
  currencyCode: string | null
  billingCycle: SuiteBillingCycle
  seatCount: number
  marketingBadge: string | null
  invoiceSummary: SuiteInvoiceSummary | null
  entitlementSummary: SuiteEntitlementSummary
  onboardingSummary: SuiteOnboardingSummary
}

export interface SuiteCheckoutDraft {
  offerCode: SuiteOfferCode
  offerName: string
  quoteStatus: 'READY' | 'CONTACT_SALES'
  checkoutMode: SuiteCheckoutMode
  currencyCode: string | null
  billingCycle: SuiteBillingCycle
  seatCount: number
  marketingBadge: string | null
  organizationName: string | null
  domainName: string | null
  invoiceSummary: SuiteInvoiceSummary | null
  entitlementSummary: SuiteEntitlementSummary
  onboardingSummary: SuiteOnboardingSummary
  updatedAt: string
}

export interface SuiteBillingOverview {
  activePlanCode: SuitePlanCode
  activePlanName: string
  latestDraft: SuiteCheckoutDraft | null
  selfServeOfferCodes: SuiteOfferCode[]
  contactSalesOfferCodes: SuiteOfferCode[]
}

export interface SuiteBillingPaymentMethod {
  id: number
  methodType: SuiteBillingPaymentMethodType
  displayLabel: string
  brand: string | null
  lastFour: string | null
  expiresAt: string | null
  defaultMethod: boolean
  status: string
}

export interface SuiteBillingInvoice {
  invoiceNumber: string
  offerCode: SuiteOfferCode
  offerName: string
  invoiceStatus: SuiteBillingInvoiceStatus
  currencyCode: string
  totalCents: number
  billingCycle: SuiteBillingCycle | null
  seatCount: number
  issuedAt: string
  dueAt: string | null
  downloadCode: string
}

export interface SuiteBillingSubscriptionSummary {
  activePlanCode: SuitePlanCode
  activePlanName: string
  billingCycle: SuiteBillingCycle | null
  seatCount: number
  autoRenew: boolean
  currentPeriodEndsAt: string | null
  defaultPaymentMethodId: number | null
  defaultPaymentMethodLabel: string | null
  pendingActionCode: SuiteBillingActionCode | null
  pendingOfferCode: SuiteOfferCode | null
  pendingOfferName: string | null
  pendingEffectiveAt: string | null
}

export interface SuiteBillingSubscriptionAction {
  actionCode: SuiteBillingActionCode
  actionStatus: SuiteBillingActionStatus
  enabled: boolean
  targetOfferCode: SuiteOfferCode | null
  targetOfferName: string | null
  effectiveAt: string | null
  reasonCode: string | null
}

export interface SuiteBillingCenter {
  subscriptionSummary: SuiteBillingSubscriptionSummary
  paymentMethods: SuiteBillingPaymentMethod[]
  invoices: SuiteBillingInvoice[]
  availableActions: SuiteBillingSubscriptionAction[]
}

export interface CreateSuiteBillingQuoteRequest {
  offerCode: SuiteOfferCode
  billingCycle: SuiteBillingCycle
  seatCount: number
}

export interface CreateSuiteCheckoutDraftRequest extends CreateSuiteBillingQuoteRequest {
  organizationName?: string
  domainName?: string
}

export interface CreateSuiteBillingPaymentMethodRequest {
  methodType: SuiteBillingPaymentMethodType
  displayLabel: string
  brand?: string
  lastFour?: string
  expiresAt?: string
  makeDefault?: boolean
}

export interface SetDefaultSuiteBillingPaymentMethodRequest {
  paymentMethodId: number
}

export interface ExecuteSuiteBillingSubscriptionActionRequest {
  actionCode: SuiteBillingActionCode
}

export type SuiteParityProduct = SuiteProductItem

export type LumoConversation = BaseLumoConversation
export type LumoProjectParity = LumoProject
export type LumoProjectKnowledgeParity = LumoProjectKnowledge

export type LumoMessageRole = BaseLumoMessage['role']
export type LumoCapabilityMode = 'STANDARD' | 'SEARCH' | 'SEARCH_TRANSLATE'
export type LumoTranslateLocale = 'SYSTEM' | 'en' | 'zh-CN' | 'zh-TW'
export type LumoCitationSourceType = 'PUBLIC_SOURCE' | 'PROJECT_KNOWLEDGE'

export interface LumoCitation {
  title: string
  url: string
  note: string
  sourceType: LumoCitationSourceType
}

export interface LumoMessage extends Omit<BaseLumoMessage, 'role'> {
  role: LumoMessageRole
  capabilityMode: LumoCapabilityMode
  responseLocale: Exclude<LumoTranslateLocale, 'SYSTEM'> | null
  webSearchEnabled: boolean
  citationsEnabled: boolean
  citations: LumoCitation[]
}

export interface SendLumoMessageRequest {
  content: string
  knowledgeIds?: string[]
  webSearchEnabled?: boolean
  citationsEnabled?: boolean
  translateToLocale?: LumoTranslateLocale
}
