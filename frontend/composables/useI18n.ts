import { computed } from 'vue'
import {
  LOCALE_COOKIE_KEY,
  DEFAULT_LOCALE,
  LOCALE_SELECTION_KEY,
  LOCALE_STORAGE_KEY,
  type SupportedLocale
} from '~/constants/i18n'
import { messages } from '~/locales'
import { normalizeLocale, resolveLocale, translate } from '~/utils/i18n'

function canUseLocalStorage(): boolean {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'
}

function readStoredLocale(): SupportedLocale | null {
  if (!canUseLocalStorage()) {
    return null
  }
  return normalizeLocale(window.localStorage.getItem(LOCALE_STORAGE_KEY))
}

function readSelectedLocale(): SupportedLocale | null {
  if (!canUseLocalStorage()) {
    return null
  }
  return normalizeLocale(window.localStorage.getItem(LOCALE_SELECTION_KEY))
}

export function useI18n() {
  const localeCookie = useCookie<string | null>(LOCALE_COOKIE_KEY, {
    default: () => DEFAULT_LOCALE,
    sameSite: 'lax'
  })
  const localeState = useState<SupportedLocale>('mmmail.locale', () => DEFAULT_LOCALE)
  const initializedState = useState<boolean>('mmmail.locale.initialized', () => false)

  function readCookieLocale(): SupportedLocale | null {
    return normalizeLocale(localeCookie.value)
  }

  function persistLocale(locale: SupportedLocale): void {
    localeCookie.value = locale
    if (!canUseLocalStorage()) {
      return
    }
    window.localStorage.setItem(LOCALE_STORAGE_KEY, locale)
  }

  function rememberLocaleSelection(locale: SupportedLocale): SupportedLocale {
    if (canUseLocalStorage()) {
      window.localStorage.setItem(LOCALE_SELECTION_KEY, locale)
    }
    return locale
  }

  function runtimeLocales(): string[] {
    if (import.meta.server) {
      const headers = useRequestHeaders(['accept-language'])
      return headers['accept-language']
        ?.split(',')
        .map(item => item.split(';')[0]?.trim() || '')
        .filter(Boolean) || []
    }
    if (typeof navigator === 'undefined') {
      return []
    }
    if (Array.isArray(navigator.languages) && navigator.languages.length > 0) {
      return navigator.languages
    }
    return navigator.language ? [navigator.language] : []
  }

  function initializeLocale(preferredLocale?: string | null): SupportedLocale {
    const resolved = resolveLocale({
      preferredLocale,
      cookieLocale: readCookieLocale(),
      storedLocale: readStoredLocale(),
      browserLocales: runtimeLocales()
    })
    localeState.value = resolved
    initializedState.value = true
    persistLocale(resolved)
    return resolved
  }

  function setLocale(locale: SupportedLocale): SupportedLocale {
    localeState.value = locale
    initializedState.value = true
    persistLocale(locale)
    return locale
  }

  function applyPreferredLocale(preferredLocale?: string | null): SupportedLocale {
    const resolved = normalizeLocale(preferredLocale) || DEFAULT_LOCALE
    return setLocale(resolved)
  }

  function applyProfileLocale(preferredLocale?: string | null): SupportedLocale {
    const selectedLocale = readSelectedLocale()
    if (selectedLocale) {
      return setLocale(selectedLocale)
    }
    return applyPreferredLocale(preferredLocale)
  }

  function t(key: string, params?: Record<string, string | number>): string {
    return translate(messages, localeState.value, key, params)
  }

  if (!initializedState.value) {
    initializeLocale()
  }

  return {
    locale: computed(() => localeState.value),
    initialized: computed(() => initializedState.value),
    initializeLocale,
    setLocale,
    rememberLocaleSelection,
    applyPreferredLocale,
    applyProfileLocale,
    t
  }
}
