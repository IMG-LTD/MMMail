import type { ApiResponse } from '~/types/api'
import type {
  CreateSuiteBillingQuoteRequest,
  CreateSuiteCheckoutDraftRequest,
  SuiteBillingOverview,
  SuiteBillingQuote,
  SuiteCheckoutDraft,
  SuitePricingOffer
} from '~/types/suite-lumo'

export function useSuiteBillingApi() {
  const { $apiClient } = useNuxtApp()

  async function listPricingOffers(): Promise<SuitePricingOffer[]> {
    const response = await $apiClient.get<ApiResponse<SuitePricingOffer[]>>('/api/v1/suite/pricing/offers')
    return response.data.data
  }

  async function getBillingOverview(): Promise<SuiteBillingOverview> {
    const response = await $apiClient.get<ApiResponse<SuiteBillingOverview>>('/api/v1/suite/billing/overview')
    return response.data.data
  }

  async function createBillingQuote(payload: CreateSuiteBillingQuoteRequest): Promise<SuiteBillingQuote> {
    const response = await $apiClient.post<ApiResponse<SuiteBillingQuote>>('/api/v1/suite/billing/quote', payload)
    return response.data.data
  }

  async function saveCheckoutDraft(payload: CreateSuiteCheckoutDraftRequest): Promise<SuiteCheckoutDraft> {
    const response = await $apiClient.post<ApiResponse<SuiteCheckoutDraft>>('/api/v1/suite/billing/checkout-draft', payload)
    return response.data.data
  }

  return {
    listPricingOffers,
    getBillingOverview,
    createBillingQuote,
    saveCheckoutDraft
  }
}
