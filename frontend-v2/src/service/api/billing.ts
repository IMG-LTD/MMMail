import { httpClient } from "@/service/request/http";
import type { ApiResponse } from "@/shared/types/api";

export interface BillingRequestOptions {
  scopeHeaders?: Record<string, string>;
  token: string;
}

export interface BillingSummary {
  planName: string;
  status: string;
  seatCount: number;
  quotaUsedPercent: number;
  nextInvoiceAt: string | null;
}

export interface BillingPlan {
  id: string;
  name: string;
  entitlement: string;
  hostedRequired: boolean;
}

export interface BillingInvoice {
  id: string;
  amountLabel: string;
  status: string;
  issuedAt: string;
}

export interface BillingUsage {
  storageUsedTb: number;
  mailCount: number;
  userCount: number;
}

function unwrapResponse<T>(response: ApiResponse<T>) {
  return response.data;
}

export async function readBillingSummary(options: BillingRequestOptions) {
  const response = await httpClient.get<ApiResponse<BillingSummary>>(
    "/api/v2/billing/summary",
    options,
  );
  return unwrapResponse(response);
}

export async function listBillingPlans(options: BillingRequestOptions) {
  const response = await httpClient.get<ApiResponse<BillingPlan[]>>(
    "/api/v2/billing/plans",
    options,
  );
  return unwrapResponse(response);
}

export async function listBillingInvoices(options: BillingRequestOptions) {
  const response = await httpClient.get<ApiResponse<BillingInvoice[]>>(
    "/api/v2/billing/invoices",
    options,
  );
  return unwrapResponse(response);
}

export async function readBillingUsage(options: BillingRequestOptions) {
  const response = await httpClient.get<ApiResponse<BillingUsage>>(
    "/api/v2/billing/usage",
    options,
  );
  return unwrapResponse(response);
}
