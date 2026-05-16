import { request } from '../request';

export function listSuitePricingOffers() {
  return request<Api.Billing.PricingOffer[]>({ url: '/api/v1/suite/pricing/offers' });
}

export function readSuiteBillingOverview() {
  return request<Api.Billing.Overview>({ url: '/api/v1/suite/billing/overview' });
}

export function readSuiteBillingCenter() {
  return request<Api.Billing.Center>({ url: '/api/v1/suite/billing/center' });
}

export function createSuiteBillingQuote(data: Api.Billing.QuotePayload) {
  return request<Api.Billing.Quote>({
    url: '/api/v1/suite/billing/quote',
    method: 'post',
    data
  });
}

export function createSuiteCheckoutDraft(data: Api.Billing.CheckoutDraftPayload) {
  return request<Api.Billing.CheckoutDraft>({
    url: '/api/v1/suite/billing/checkout-draft',
    method: 'post',
    data
  });
}

export function addSuiteBillingPaymentMethod(data: Api.Billing.PaymentMethodPayload) {
  return request<Api.Billing.Center>({
    url: '/api/v1/suite/billing/payment-methods',
    method: 'post',
    data
  });
}

export function setSuiteBillingDefaultPaymentMethod(data: Api.Billing.DefaultPaymentMethodPayload) {
  return request<Api.Billing.Center>({
    url: '/api/v1/suite/billing/payment-methods/default',
    method: 'post',
    data
  });
}

export function executeSuiteBillingSubscriptionAction(data: Api.Billing.SubscriptionActionPayload) {
  return request<Api.Billing.Center>({
    url: '/api/v1/suite/billing/subscription-actions',
    method: 'post',
    data
  });
}
