import { httpClient } from "@/service/request/http";
import type { ApiResponse } from "@/shared/types/api";

export interface AdminRequestOptions {
  scopeHeaders?: Record<string, string>;
  token: string;
}

export interface AdminSummary {
  userCount: number;
  activeUserCount: number;
  storageUsedTb: number;
  dailyMailCount: number;
  securityScore: number;
}

export interface AdminUser {
  id: string;
  email: string;
  displayName: string;
  role: string;
  status: string;
  lastActiveAt: string | null;
}

export interface AdminRole {
  id: string;
  name: string;
  userCount: number;
  permissionCount: number;
}

export interface AdminDomain {
  id: string;
  domain: string;
  status: string;
  verifiedAt: string | null;
}

export interface AdminPolicy {
  id: string;
  name: string;
  enabled: boolean;
  updatedAt: string;
}

export interface AdminAuditEntry {
  id: string;
  action: string;
  actorEmail: string;
  target: string;
  createdAt: string;
}

export interface AdminAlert {
  id: string;
  title: string;
  severity: string;
  product: string;
  createdAt: string;
}

export interface AdminSystemStatus {
  services: Array<{ name: string; status: string }>;
  cpuPercent: number;
  memoryPercent: number;
  storagePercent: number;
}

export interface AdminRiskOverview {
  riskScore: number;
  openRiskCount: number;
  criticalRiskCount: number;
}

function unwrapResponse<T>(response: ApiResponse<T>) {
  return response.data;
}

export async function readAdminSummary(options: AdminRequestOptions) {
  const response = await httpClient.get<ApiResponse<AdminSummary>>(
    "/api/v2/admin/summary",
    options,
  );
  return unwrapResponse(response);
}

export async function listAdminUsers(options: AdminRequestOptions) {
  const response = await httpClient.get<ApiResponse<AdminUser[]>>("/api/v2/admin/users", options);
  return unwrapResponse(response);
}

export function createAdminUser(body: Record<string, unknown>, options: AdminRequestOptions) {
  return httpClient.post<ApiResponse<AdminUser>>("/api/v2/admin/users", { ...options, body });
}

export function patchAdminUser(
  userId: string,
  body: Record<string, unknown>,
  options: AdminRequestOptions,
) {
  return httpClient.patch<ApiResponse<AdminUser>>(`/api/v2/admin/users/${userId}`, {
    ...options,
    body,
  });
}

export async function listAdminRoles(options: AdminRequestOptions) {
  const response = await httpClient.get<ApiResponse<AdminRole[]>>("/api/v2/admin/roles", options);
  return unwrapResponse(response);
}

export async function listAdminDomains(options: AdminRequestOptions) {
  const response = await httpClient.get<ApiResponse<AdminDomain[]>>(
    "/api/v2/admin/domains",
    options,
  );
  return unwrapResponse(response);
}

export async function listAdminPolicies(options: AdminRequestOptions) {
  const response = await httpClient.get<ApiResponse<AdminPolicy[]>>(
    "/api/v2/admin/policies",
    options,
  );
  return unwrapResponse(response);
}

export function patchAdminPolicy(
  policyId: string,
  body: Record<string, unknown>,
  options: AdminRequestOptions,
) {
  return httpClient.patch<ApiResponse<AdminPolicy>>(`/api/v2/admin/policies/${policyId}`, {
    ...options,
    body,
  });
}

export async function listAdminAudit(options: AdminRequestOptions) {
  const response = await httpClient.get<ApiResponse<AdminAuditEntry[]>>(
    "/api/v2/admin/audit",
    options,
  );
  return unwrapResponse(response);
}

export async function listAdminAlerts(options: AdminRequestOptions) {
  const response = await httpClient.get<ApiResponse<AdminAlert[]>>("/api/v2/admin/alerts", options);
  return unwrapResponse(response);
}

export async function readAdminSystem(options: AdminRequestOptions) {
  const response = await httpClient.get<ApiResponse<AdminSystemStatus>>(
    "/api/v2/admin/system",
    options,
  );
  return unwrapResponse(response);
}

export async function readAdminRisk(options: AdminRequestOptions) {
  const response = await httpClient.get<ApiResponse<AdminRiskOverview>>(
    "/api/v2/admin/risk",
    options,
  );
  return unwrapResponse(response);
}
