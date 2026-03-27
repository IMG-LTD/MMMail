import type { SupportedLocale } from '~/constants/i18n'
import type {
  SearchHistoryItem,
  SearchPreset,
  SystemMailFolder
} from '~/types/api'

export type SearchReadState = 'ALL' | 'UNREAD' | 'READ'
export type SearchStarState = 'ALL' | 'STARRED' | 'UNSTARRED'
export type SearchTranslator = (key: string, params?: Record<string, string | number>) => string

export interface SearchPresetEditorState {
  name: string
  keyword: string
  folder: SystemMailFolder | ''
  unread: SearchReadState
  starred: SearchStarState
  dateRange: string[]
  label: string
}

export interface SearchOption<T extends string> {
  label: string
  value: T
}

const SEARCH_DATE_TIME_OPTIONS: Intl.DateTimeFormatOptions = {
  dateStyle: 'medium',
  timeStyle: 'short'
}

const SEARCH_FOLDER_LABEL_KEYS: Record<SystemMailFolder, string> = {
  INBOX: 'nav.inbox',
  SENT: 'nav.sent',
  DRAFTS: 'nav.drafts',
  OUTBOX: 'nav.outbox',
  SCHEDULED: 'nav.scheduled',
  SNOOZED: 'nav.snoozed',
  ARCHIVE: 'nav.archive',
  SPAM: 'nav.spam',
  TRASH: 'nav.trash'
}

const SEARCH_READ_STATE_KEYS: Record<SearchReadState, string> = {
  ALL: 'search.readState.ALL',
  UNREAD: 'search.readState.UNREAD',
  READ: 'search.readState.READ'
}

const SEARCH_STAR_STATE_KEYS: Record<SearchStarState, string> = {
  ALL: 'search.starState.ALL',
  STARRED: 'search.starState.STARRED',
  UNSTARRED: 'search.starState.UNSTARRED'
}

export function buildSearchFolderOptions(
  t?: SearchTranslator
): SearchOption<SystemMailFolder>[] {
  return Object.entries(SEARCH_FOLDER_LABEL_KEYS).map(([value, key]) => ({
    value: value as SystemMailFolder,
    label: t ? t(key) : key
  }))
}

export function buildSearchReadStateOptions(
  t?: SearchTranslator
): SearchOption<SearchReadState>[] {
  return Object.entries(SEARCH_READ_STATE_KEYS).map(([value, key]) => ({
    value: value as SearchReadState,
    label: t ? t(key) : key
  }))
}

export function buildSearchStarStateOptions(
  t?: SearchTranslator
): SearchOption<SearchStarState>[] {
  return Object.entries(SEARCH_STAR_STATE_KEYS).map(([value, key]) => ({
    value: value as SearchStarState,
    label: t ? t(key) : key
  }))
}

export function formatSearchDateTime(
  value: string | null | undefined,
  locale: SupportedLocale = 'en'
): string {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat(locale, SEARCH_DATE_TIME_OPTIONS).format(date)
}

export function buildSearchUsageText(
  item: Pick<SearchHistoryItem | SearchPreset, 'usageCount' | 'lastUsedAt'>,
  locale: SupportedLocale = 'en',
  t?: SearchTranslator
): string {
  const lastUsedAt = formatSearchDateTime(item.lastUsedAt, locale)
  if (!t) {
    return lastUsedAt
      ? `Used ${item.usageCount} times · Last used ${lastUsedAt}`
      : `Used ${item.usageCount} times`
  }
  if (!lastUsedAt) {
    return t('search.meta.usedOnly', { count: item.usageCount })
  }
  return t('search.meta.usedAndLastUsed', {
    count: item.usageCount,
    time: lastUsedAt
  })
}
