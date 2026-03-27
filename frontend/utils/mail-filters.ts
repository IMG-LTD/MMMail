import type {
  MailFilter,
  MailFilterDraft,
  MailFilterPayload,
  MailFilterTargetFolder
} from '../types/mail-filters'

type TranslateFn = (key: string, params?: Record<string, string | number>) => string

export const MAIL_FILTER_TARGET_FOLDERS: MailFilterTargetFolder[] = ['INBOX', 'ARCHIVE', 'SPAM', 'TRASH']

export function createMailFilterDraft(): MailFilterDraft {
  return {
    name: '',
    senderContains: '',
    subjectContains: '',
    keywordContains: '',
    targetFolder: '',
    targetCustomFolderId: '',
    labels: [],
    markRead: false,
    enabled: true
  }
}

export function buildMailFilterPayload(draft: MailFilterDraft): MailFilterPayload {
  const name = draft.name.trim()
  const senderContains = normalizeOptional(draft.senderContains)
  const subjectContains = normalizeOptional(draft.subjectContains)
  const keywordContains = normalizeOptional(draft.keywordContains)
  const targetCustomFolderId = normalizeOptional(draft.targetCustomFolderId)
  const labels = draft.labels.filter(Boolean)

  if (!name) {
    throw new Error('name')
  }
  if (!senderContains && !subjectContains && !keywordContains) {
    throw new Error('conditions')
  }
  if (draft.targetFolder && targetCustomFolderId) {
    throw new Error('target')
  }
  if (!draft.targetFolder && !targetCustomFolderId && labels.length === 0 && !draft.markRead) {
    throw new Error('actions')
  }

  return {
    name,
    senderContains,
    subjectContains,
    keywordContains,
    targetFolder: draft.targetFolder || undefined,
    targetCustomFolderId,
    labels,
    markRead: draft.markRead,
    enabled: draft.enabled
  }
}

export function describeMailFilterConditions(filter: MailFilter, t: TranslateFn): string {
  const parts = [
    filter.senderContains ? t('settings.mailFilters.summary.sender', { value: filter.senderContains }) : '',
    filter.subjectContains ? t('settings.mailFilters.summary.subject', { value: filter.subjectContains }) : '',
    filter.keywordContains ? t('settings.mailFilters.summary.keyword', { value: filter.keywordContains }) : ''
  ].filter(Boolean)
  return parts.join(' · ') || t('common.none')
}

export function describeMailFilterActions(filter: MailFilter, t: TranslateFn): string {
  const parts = [
    filter.targetCustomFolderName
      ? t('settings.mailFilters.summary.move', { value: filter.targetCustomFolderName })
      : '',
    filter.targetFolder
      ? t('settings.mailFilters.summary.move', { value: t(resolveFolderKey(filter.targetFolder)) })
      : '',
    filter.labels.length
      ? t('settings.mailFilters.summary.label', { value: filter.labels.join(', ') })
      : '',
    filter.markRead ? t('settings.mailFilters.summary.read') : ''
  ].filter(Boolean)
  return parts.join(' · ') || t('common.none')
}

export function resolveFolderKey(folder: MailFilterTargetFolder): string {
  return `settings.mailFilters.folders.${folder.toLowerCase()}`
}

function normalizeOptional(value: string): string | undefined {
  const normalized = value.trim()
  return normalized ? normalized : undefined
}
