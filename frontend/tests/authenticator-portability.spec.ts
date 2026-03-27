import { describe, expect, it } from 'vitest'
import {
  buildAuthenticatorDownloadMimeType,
  isBackupPassphraseConfirmed,
  normalizeAuthenticatorImportFormat,
  normalizeAuthenticatorPortabilityContent,
  readAuthenticatorPortabilityFile,
  readAuthenticatorQrImageFile
} from '../utils/authenticator-portability'
import {
  buildAuthenticatorUnlockDeadline,
  buildAuthenticatorUnlockStorageKey,
  clearAuthenticatorUnlockDeadline,
  createAuthenticatorSecurityDraft,
  isAuthenticatorPinValid,
  isAuthenticatorUnlockDeadlineActive,
  normalizeAuthenticatorLockTimeout,
  readAuthenticatorUnlockDeadline,
  writeAuthenticatorUnlockDeadline
} from '../utils/authenticator-security'

describe('authenticator portability utils', () => {
  it('normalizes portability content and CRLF input', () => {
    expect(normalizeAuthenticatorPortabilityContent('\r\notpauth://line\r\n')).toBe('otpauth://line')
  })

  it('normalizes supported import formats', () => {
    expect(normalizeAuthenticatorImportFormat('OTPAUTH_URI')).toBe('OTPAUTH_URI')
    expect(normalizeAuthenticatorImportFormat('MMMAIL_JSON')).toBe('MMMAIL_JSON')
    expect(normalizeAuthenticatorImportFormat('UNKNOWN')).toBe('AUTO')
  })

  it('validates backup passphrase confirmation', () => {
    expect(isBackupPassphraseConfirmed('Backup@123', 'Backup@123')).toBe(true)
    expect(isBackupPassphraseConfirmed('short', 'short')).toBe(false)
    expect(isBackupPassphraseConfirmed('Backup@123', 'Mismatch@123')).toBe(false)
  })

  it('reads file text and preserves normalized payload', async () => {
    const file = new File(['{\r\n  "format": "MMMAIL_AUTHENTICATOR_EXPORT"\r\n}'], 'auth.json', {
      type: buildAuthenticatorDownloadMimeType('export')
    })
    await expect(readAuthenticatorPortabilityFile(file)).resolves.toBe('{\n  "format": "MMMAIL_AUTHENTICATOR_EXPORT"\n}')
  })

  it('reads QR image file as data url', async () => {
    const file = new File(['qr-image'], 'auth.png', { type: 'image/png' })
    await expect(readAuthenticatorQrImageFile(file)).resolves.toMatch(/^data:image\/png;base64,/)
  })
})

describe('authenticator security utils', () => {
  it('creates a draft from server preference', () => {
    expect(createAuthenticatorSecurityDraft({
      syncEnabled: false,
      encryptedBackupEnabled: true,
      pinProtectionEnabled: true,
      pinConfigured: true,
      lockTimeoutSeconds: 600,
      lastSyncedAt: null,
      lastBackupAt: null
    })).toEqual({
      syncEnabled: false,
      encryptedBackupEnabled: true,
      pinProtectionEnabled: true,
      lockTimeoutSeconds: 600,
      pin: '',
      pinConfirm: ''
    })
  })

  it('validates pin and normalizes lock timeout', () => {
    expect(isAuthenticatorPinValid('246824')).toBe(true)
    expect(isAuthenticatorPinValid('24ab')).toBe(false)
    expect(normalizeAuthenticatorLockTimeout(600)).toBe(600)
    expect(normalizeAuthenticatorLockTimeout(10)).toBe(300)
  })

  it('persists unlock deadlines in session storage', () => {
    const storage = window.sessionStorage
    const key = buildAuthenticatorUnlockStorageKey('user-1')
    const deadline = buildAuthenticatorUnlockDeadline(300, 1000)
    writeAuthenticatorUnlockDeadline(storage, key, deadline)
    expect(readAuthenticatorUnlockDeadline(storage, key)).toBe(deadline)
    expect(isAuthenticatorUnlockDeadlineActive(deadline, 1100)).toBe(true)
    clearAuthenticatorUnlockDeadline(storage, key)
    expect(readAuthenticatorUnlockDeadline(storage, key)).toBeNull()
  })
})
