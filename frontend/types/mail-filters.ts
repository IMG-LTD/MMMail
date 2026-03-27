export type MailFilterTargetFolder = 'INBOX' | 'ARCHIVE' | 'SPAM' | 'TRASH'
export type MailFilterEffectiveFolder = MailFilterTargetFolder | 'CUSTOM'

export interface MailFilter {
  id: string
  name: string
  enabled: boolean
  senderContains: string | null
  subjectContains: string | null
  keywordContains: string | null
  targetFolder: MailFilterTargetFolder | null
  targetCustomFolderId: string | null
  targetCustomFolderName: string | null
  labels: string[]
  markRead: boolean
  createdAt: string
  updatedAt: string
}

export interface MailFilterDraft {
  name: string
  senderContains: string
  subjectContains: string
  keywordContains: string
  targetFolder: MailFilterTargetFolder | ''
  targetCustomFolderId: string
  labels: string[]
  markRead: boolean
  enabled: boolean
}

export interface MailFilterPayload {
  name: string
  senderContains?: string
  subjectContains?: string
  keywordContains?: string
  targetFolder?: MailFilterTargetFolder
  targetCustomFolderId?: string
  labels: string[]
  markRead: boolean
  enabled: boolean
}

export interface MailFilterPreviewRequest {
  senderEmail: string
  subject?: string
  body?: string
}

export interface MailFilterPreview {
  senderEmail: string
  subject: string | null
  baseFolder: MailFilterTargetFolder
  effectiveFolder: MailFilterEffectiveFolder
  effectiveCustomFolderId: string | null
  effectiveCustomFolderName: string | null
  effectiveLabels: string[]
  markRead: boolean
  blockedBySecurityRule: boolean
  securityReason: string
  securityMatchedRule: string | null
  matchedFilterId: string | null
  matchedFilterName: string | null
}
