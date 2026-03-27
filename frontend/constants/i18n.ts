export type SupportedLocale = 'en' | 'zh-CN' | 'zh-TW'

export const DEFAULT_LOCALE: SupportedLocale = 'en'
export const LOCALE_STORAGE_KEY = 'mmmail.locale.v1'
export const LOCALE_COOKIE_KEY = 'mmmail-locale'
export const LOCALE_SELECTION_KEY = 'mmmail.locale.selection.v1'
export const SUPPORTED_LOCALES: SupportedLocale[] = ['en', 'zh-CN', 'zh-TW']
export const SUPPORTED_LOCALE_SET = new Set<SupportedLocale>(SUPPORTED_LOCALES)
export const LOCALE_OPTIONS: Array<{ label: string; value: SupportedLocale }> = [
  { label: 'English', value: 'en' },
  { label: '简体中文', value: 'zh-CN' },
  { label: '繁體中文', value: 'zh-TW' }
]
