import type {
  AuthenticatorSecurityDraft,
  AuthenticatorSecurityPreference
} from '~/types/authenticator-security'

const DEFAULT_LOCK_TIMEOUT_SECONDS = 300
const MIN_LOCK_TIMEOUT_SECONDS = 60
const MAX_LOCK_TIMEOUT_SECONDS = 3600
const AUTHENTICATOR_UNLOCK_STORAGE_PREFIX = 'mmmail.authenticator.unlock.v1'

export const AUTHENTICATOR_LOCK_TIMEOUT_OPTIONS = Object.freeze([60, 300, 600, 1800, 3600] as const)
export const AUTHENTICATOR_PIN_PATTERN = /^\d{4,12}$/

export function createAuthenticatorSecurityDraft(
  preference?: AuthenticatorSecurityPreference | null
): AuthenticatorSecurityDraft {
  return {
    syncEnabled: preference?.syncEnabled ?? true,
    encryptedBackupEnabled: preference?.encryptedBackupEnabled ?? false,
    pinProtectionEnabled: preference?.pinProtectionEnabled ?? false,
    lockTimeoutSeconds: normalizeAuthenticatorLockTimeout(preference?.lockTimeoutSeconds),
    pin: '',
    pinConfirm: ''
  }
}

export function normalizeAuthenticatorLockTimeout(value?: number | null): number {
  if (value == null || !Number.isFinite(value)) {
    return DEFAULT_LOCK_TIMEOUT_SECONDS
  }
  const normalized = Math.round(value)
  if (normalized < MIN_LOCK_TIMEOUT_SECONDS || normalized > MAX_LOCK_TIMEOUT_SECONDS) {
    return DEFAULT_LOCK_TIMEOUT_SECONDS
  }
  return normalized
}

export function isAuthenticatorPinValid(pin: string): boolean {
  return AUTHENTICATOR_PIN_PATTERN.test(pin.trim())
}

export function buildAuthenticatorUnlockStorageKey(userId?: string | null): string {
  return `${AUTHENTICATOR_UNLOCK_STORAGE_PREFIX}.${userId || 'anonymous'}`
}

export function buildAuthenticatorUnlockDeadline(
  lockTimeoutSeconds: number,
  now = Date.now()
): number {
  return now + normalizeAuthenticatorLockTimeout(lockTimeoutSeconds) * 1000
}

export function isAuthenticatorUnlockDeadlineActive(
  deadline: number | null,
  now = Date.now()
): boolean {
  return typeof deadline === 'number' && deadline > now
}

export function readAuthenticatorUnlockDeadline(storage: Storage, key: string): number | null {
  const raw = storage.getItem(key)
  if (!raw) {
    return null
  }
  const value = Number.parseInt(raw, 10)
  return Number.isFinite(value) ? value : null
}

export function writeAuthenticatorUnlockDeadline(
  storage: Storage,
  key: string,
  deadline: number
): void {
  storage.setItem(key, String(deadline))
}

export function clearAuthenticatorUnlockDeadline(storage: Storage, key: string): void {
  storage.removeItem(key)
}
