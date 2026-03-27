export interface AuthenticatorSecurityPreference {
  syncEnabled: boolean
  encryptedBackupEnabled: boolean
  pinProtectionEnabled: boolean
  pinConfigured: boolean
  lockTimeoutSeconds: number
  lastSyncedAt: string | null
  lastBackupAt: string | null
}

export interface UpdateAuthenticatorSecurityRequest {
  syncEnabled: boolean
  encryptedBackupEnabled: boolean
  pinProtectionEnabled: boolean
  lockTimeoutSeconds: number
  pin?: string
}

export interface AuthenticatorSecurityPinVerification {
  verified: boolean
  lockTimeoutSeconds: number
}

export interface AuthenticatorSecurityDraft {
  syncEnabled: boolean
  encryptedBackupEnabled: boolean
  pinProtectionEnabled: boolean
  lockTimeoutSeconds: number
  pin: string
  pinConfirm: string
}
