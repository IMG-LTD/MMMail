import { httpClient } from '@/service/request/http'
import type { ApiResponse } from '@/shared/types/api'

export type PublicShareCapabilityState = 'token-valid' | 'password-required' | 'unlocked' | 'expired' | 'revoked' | 'locked' | 'download-blocked'

export interface PublicShareCapabilities {
  auditedActions: string[]
  passwordHeader: string
  states: PublicShareCapabilityState[]
}

export interface PublicMailShareAttachment {
  id: string
  fileName: string
  fileSize: number
  contentType: string
}

export interface PublicMailShare {
  id: string
  subject: string
  senderEmail: string
  recipientEmail: string
  bodyCiphertext: string
  bodyE2eeAlgorithm: string | null
  passwordHint: string | null
  expiresAt: string | null
  attachments: PublicMailShareAttachment[]
}

export interface PublicDriveShareMetadata {
  itemCount: number
  expiresAt: string | null
  permission: string
  rootItemName: string
}

export interface PublicPassShare {
  currentViews: number
  expiresAt: string | null
  itemType: string
  maxViews: number
  note: string | null
  secretCiphertext: string | null
  sharedVaultName: string
  title: string
  username: string | null
  website: string | null
}

export function readPublicShareCapabilities() {
  return httpClient.get<ApiResponse<PublicShareCapabilities>>('/api/v2/public-share/capabilities')
}

export function readPublicMailShare(token: string) {
  return httpClient.get<ApiResponse<PublicMailShare>>(`/api/v1/public/mail/secure-links/${token}`)
}

export function downloadPublicMailAttachment(token: string, attachmentId: string) {
  return httpClient.getBlob(`/api/v1/public/mail/secure-links/${token}/attachments/${attachmentId}/download`)
}

export function readPublicDriveShareMetadata(token: string) {
  return httpClient.get<ApiResponse<PublicDriveShareMetadata>>(`/api/v1/public/drive/shares/${token}/metadata`)
}

export function listPublicDriveShareItems(token: string, password?: string) {
  return httpClient.get<ApiResponse<unknown[]>>(`/api/v1/public/drive/shares/${token}/items`, {
    headers: password ? { 'X-Drive-Share-Password': password } : undefined
  })
}

export function downloadPublicDriveShareItem(token: string, itemId: string, password?: string) {
  return httpClient.getBlob(`/api/v1/public/drive/shares/${token}/items/${itemId}/download`, {
    headers: password ? { 'X-Drive-Share-Password': password } : undefined
  })
}

export function readPublicPassShare(token: string) {
  return httpClient.get<ApiResponse<PublicPassShare>>(`/api/v1/public/pass/secure-links/${token}`)
}
