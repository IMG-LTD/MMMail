import { DEFAULT_LOCALE, SUPPORTED_LOCALE_SET, type SupportedLocale } from '../constants/i18n'
import type { TranslationCatalog } from '../locales'

interface ResolveLocaleOptions {
  preferredLocale?: string | null
  cookieLocale?: string | null
  storedLocale?: string | null
  browserLocales?: readonly string[]
}

export function normalizeLocale(input?: string | null): SupportedLocale | null {
  if (!input) {
    return null
  }
  if (SUPPORTED_LOCALE_SET.has(input as SupportedLocale)) {
    return input as SupportedLocale
  }
  const value = input.toLowerCase()
  if (value === 'en' || value.startsWith('en-')) {
    return 'en'
  }
  if (value === 'zh-cn' || value === 'zh-hans' || value.startsWith('zh-cn') || value.startsWith('zh-hans')) {
    return 'zh-CN'
  }
  if (value === 'zh-tw' || value === 'zh-hant' || value.startsWith('zh-tw') || value.startsWith('zh-hant')) {
    return 'zh-TW'
  }
  return null
}

export function resolveLocale(options: ResolveLocaleOptions): SupportedLocale {
  const preferredLocale = normalizeLocale(options.preferredLocale)
  if (preferredLocale) {
    return preferredLocale
  }
  const cookieLocale = normalizeLocale(options.cookieLocale)
  if (cookieLocale) {
    return cookieLocale
  }
  const storedLocale = normalizeLocale(options.storedLocale)
  if (storedLocale) {
    return storedLocale
  }
  const browserLocales = options.browserLocales || []
  for (const candidate of browserLocales) {
    const normalized = normalizeLocale(candidate)
    if (normalized) {
      return normalized
    }
  }
  return DEFAULT_LOCALE
}

export function translate(
  catalog: TranslationCatalog,
  locale: SupportedLocale,
  key: string,
  params?: Record<string, string | number>
): string {
  const template = catalog[locale]?.[key] || catalog[DEFAULT_LOCALE]?.[key]
  if (!template) {
    return key
  }
  if (!params) {
    return template
  }
  return Object.entries(params).reduce((message, [paramKey, value]) => {
    return message.replaceAll(`{${paramKey}}`, String(value))
  }, template)
}
