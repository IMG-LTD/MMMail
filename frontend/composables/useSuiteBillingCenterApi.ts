import type { ApiResponse } from '~/types/api'
import type {
  CreateSuiteBillingPaymentMethodRequest,
  ExecuteSuiteBillingSubscriptionActionRequest,
  SetDefaultSuiteBillingPaymentMethodRequest,
  SuiteBillingCenter
} from '~/types/suite-lumo'

export function useSuiteBillingCenterApi() {
  const { $apiClient } = useNuxtApp()

  async function getBillingCenter(): Promise<SuiteBillingCenter> {
    const response = await $apiClient.get<ApiResponse<SuiteBillingCenter>>('/api/v1/suite/billing/center')
    return response.data.data
  }

  async function addPaymentMethod(payload: CreateSuiteBillingPaymentMethodRequest): Promise<SuiteBillingCenter> {
    const response = await $apiClient.post<ApiResponse<SuiteBillingCenter>>('/api/v1/suite/billing/payment-methods', payload)
    return response.data.data
  }

  async function setDefaultPaymentMethod(payload: SetDefaultSuiteBillingPaymentMethodRequest): Promise<SuiteBillingCenter> {
    const response = await $apiClient.post<ApiResponse<SuiteBillingCenter>>('/api/v1/suite/billing/payment-methods/default', payload)
    return response.data.data
  }

  async function executeSubscriptionAction(payload: ExecuteSuiteBillingSubscriptionActionRequest): Promise<SuiteBillingCenter> {
    const response = await $apiClient.post<ApiResponse<SuiteBillingCenter>>('/api/v1/suite/billing/subscription-actions', payload)
    return response.data.data
  }

  return {
    getBillingCenter,
    addPaymentMethod,
    setDefaultPaymentMethod,
    executeSubscriptionAction
  }
}
