import type { NDateLocale, NLocale } from 'naive-ui'
import { dateEnUS, dateZhCN, dateZhTW, enUS, zhCN, zhTW } from 'naive-ui'
import { computed } from 'vue'
import { useAppStore } from '@/store/modules/app'

export type AppLocale = 'zh-CN' | 'zh-TW' | 'en'

export interface LocalizedText {
  'zh-CN': string
  'zh-TW': string
  en: string
}

export type TextLike = LocalizedText | string

export const APP_LOCALES: AppLocale[] = ['zh-CN', 'zh-TW', 'en']

const localeBadges: Record<AppLocale, string> = {
  'zh-CN': '简',
  'zh-TW': '繁',
  en: 'EN'
}

const localeNames: Record<AppLocale, LocalizedText> = {
  'zh-CN': { 'zh-CN': '简体中文', 'zh-TW': '簡體中文', en: 'Simplified Chinese' },
  'zh-TW': { 'zh-CN': '繁体中文', 'zh-TW': '繁體中文', en: 'Traditional Chinese' },
  en: { 'zh-CN': '英语', 'zh-TW': '英語', en: 'English' }
}

const naiveUiLocales: Record<AppLocale, NLocale> = {
  'zh-CN': zhCN,
  'zh-TW': zhTW,
  en: enUS
}

const naiveUiDateLocales: Record<AppLocale, NDateLocale> = {
  'zh-CN': dateZhCN,
  'zh-TW': dateZhTW,
  en: dateEnUS
}

export function lt(zhCNText: string, zhTWText: string, enText: string): LocalizedText {
  return {
    'zh-CN': zhCNText,
    'zh-TW': zhTWText,
    en: enText
  }
}

export function resolveText(text: TextLike, locale: AppLocale): string {
  if (typeof text === 'string') {
    return text
  }

  return text[locale] ?? text['zh-CN'] ?? text.en
}

export function normalizeAppLocale(input?: string | null): AppLocale | null {
  if (!input) {
    return null
  }

  const normalized = input.toLowerCase()

  if (normalized.startsWith('zh-tw') || normalized.startsWith('zh-hk') || normalized.startsWith('zh-mo')) {
    return 'zh-TW'
  }

  if (normalized.startsWith('zh')) {
    return 'zh-CN'
  }

  if (normalized.startsWith('en')) {
    return 'en'
  }

  return null
}

export function detectAppLocale(): AppLocale {
  if (typeof navigator === 'undefined') {
    return 'en'
  }

  const candidates = [...navigator.languages, navigator.language]

  for (const candidate of candidates) {
    const locale = normalizeAppLocale(candidate)

    if (locale) {
      return locale
    }
  }

  return 'en'
}

export function getNextLocale(locale: AppLocale): AppLocale {
  const index = APP_LOCALES.indexOf(locale)
  const nextIndex = index === -1 ? 0 : (index + 1) % APP_LOCALES.length
  return APP_LOCALES[nextIndex]
}

export function getLocaleBadge(locale: AppLocale): string {
  return localeBadges[locale]
}

export function getLocaleName(locale: AppLocale, currentLocale: AppLocale): string {
  return resolveText(localeNames[locale], currentLocale)
}

export function getNaiveUiLocale(locale: AppLocale): NLocale {
  return naiveUiLocales[locale]
}

export function getNaiveUiDateLocale(locale: AppLocale): NDateLocale {
  return naiveUiDateLocales[locale]
}

export function useLocaleText() {
  const appStore = useAppStore()
  const locale = computed(() => appStore.locale)

  const options = computed(() => {
    return APP_LOCALES.map(item => ({
      value: item,
      badge: getLocaleBadge(item),
      label: getLocaleName(item, locale.value)
    }))
  })

  function tr(text: TextLike) {
    return resolveText(text, locale.value)
  }

  return {
    locale,
    options,
    tr
  }
}
