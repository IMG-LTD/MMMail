import { httpClient } from "@/service/request/http";
import type { ApiResponse } from "@/shared/types/api";

export type PassWorkspaceMode = "PERSONAL" | "SHARED";
export type PassItemType = "LOGIN" | "PASSWORD" | "NOTE" | "CARD" | "ALIAS" | "PASSKEY";
export type PassMailboxStatus = "PENDING" | "VERIFIED";

export interface PassVault {
  id: string;
  name: string;
  scopeType: PassWorkspaceMode;
  ownerEmail: string | null;
  itemCount: number;
  updatedAt: string;
}

export interface PassWorkspaceItemSummary {
  id: string;
  title: string;
  website: string | null;
  username: string | null;
  favorite: boolean;
  updatedAt: string;
  scopeType: PassWorkspaceMode;
  itemType: PassItemType;
  sharedVaultId: string | null;
  secureLinkCount: number;
}

export interface PassMailbox {
  id: string;
  mailboxEmail: string;
  status: PassMailboxStatus;
  defaultMailbox: boolean;
  primaryMailbox: boolean;
  createdAt: string;
  updatedAt: string;
  verifiedAt: string | null;
}

export interface PassMonitorItem {
  id: string;
  title: string;
  website: string | null;
  username: string | null;
  itemType: PassItemType;
  scopeType: PassWorkspaceMode;
  orgId: string | null;
  sharedVaultId: string | null;
  sharedVaultName: string | null;
  excluded: boolean;
  weakPassword: boolean;
  reusedPassword: boolean;
  inactiveTwoFactor: boolean;
  reusedGroupSize: number;
  updatedAt: string;
}

export interface PassMonitorOverview {
  scopeType?: PassWorkspaceMode;
  orgId?: string | null;
  currentRole?: string | null;
  totalItemCount: number;
  trackedItemCount: number;
  weakPasswordCount: number;
  reusedPasswordCount: number;
  inactiveTwoFactorCount: number;
  excludedItemCount: number;
  generatedAt?: string;
  weakPasswords: PassMonitorItem[];
  reusedPasswords: PassMonitorItem[];
  inactiveTwoFactorItems: PassMonitorItem[];
  excludedItems: PassMonitorItem[];
}

export interface PassSecureLink {
  id: string;
  itemId: string;
  itemTitle: string;
  expiresAt: string | null;
  accessCount: number;
  createdAt: string;
}

export interface PassAlias {
  id: string;
  aliasEmail: string;
  mailboxEmail: string;
  status: PassMailboxStatus;
  defaultMailbox?: boolean;
  primaryMailbox?: boolean;
  createdAt: string;
  updatedAt: string;
  verifiedAt?: string | null;
}

export function listPassItems(token: string, query: Record<string, string | undefined> = {}) {
  return httpClient.get<ApiResponse<PassWorkspaceItemSummary[]>>("/api/v2/pass/items", {
    token,
    query,
  });
}

export function listPassVaults(token: string) {
  return httpClient.get<ApiResponse<PassVault[]>>("/api/v2/pass/vaults", { token });
}

export function createPassItem(body: Record<string, unknown>, token: string) {
  return httpClient.post<ApiResponse<PassWorkspaceItemSummary>>("/api/v2/pass/items", {
    body,
    token,
  });
}

export function patchPassItem(itemId: string, body: Record<string, unknown>, token: string) {
  return httpClient.patch<ApiResponse<PassWorkspaceItemSummary>>(`/api/v2/pass/items/${itemId}`, {
    body,
    token,
  });
}

export function sharePassItem(body: Record<string, unknown>, token: string) {
  return httpClient.post<ApiResponse<PassSecureLink>>("/api/v2/pass/share", { body, token });
}

export function listPassSecureLinks(token: string) {
  return httpClient.get<ApiResponse<PassSecureLink[]>>("/api/v2/pass/secure-links", { token });
}

export function createPassSecureLink(body: Record<string, unknown>, token: string) {
  return httpClient.post<ApiResponse<PassSecureLink>>("/api/v2/pass/secure-links", { body, token });
}

export function deletePassSecureLink(linkId: string, token: string) {
  return httpClient.delete<ApiResponse<void>>(`/api/v2/pass/secure-links/${linkId}`, { token });
}

export function listPassAliases(token: string) {
  return httpClient.get<ApiResponse<PassAlias[]>>("/api/v2/pass/aliases", { token });
}

export function patchPassAlias(aliasId: string, body: Record<string, unknown>, token: string) {
  return httpClient.patch<ApiResponse<PassAlias>>(`/api/v2/pass/aliases/${aliasId}`, {
    body,
    token,
  });
}

export function listPassMailboxes(token: string) {
  return httpClient.get<ApiResponse<PassMailbox[]>>("/api/v2/pass/aliases", { token });
}

export function readPassMonitor(token: string) {
  return httpClient.get<ApiResponse<PassMonitorOverview>>("/api/v2/pass/monitor", { token });
}
