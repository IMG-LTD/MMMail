export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export type MailAddressMode = 'PROTON_ADDRESS' | 'EXTERNAL_ACCOUNT'
export type OrgRole = 'USER' | 'ADMIN' | 'OWNER' | 'MEMBER'

export interface UserProfile {
  id: string
  email: string
  displayName: string
  role: OrgRole
  mailAddressMode: MailAddressMode
}

export interface AuthPayload {
  accessToken: string
  refreshToken: string
  user: UserProfile
}

export interface UserSession {
  sessionId: string
  deviceName: string
  location?: string
  ipAddress?: string
  current: boolean
  lastActiveAt: string
}

export interface UserPreference {
  displayName: string
  signature: string
  timezone: string
  preferredLocale: string
  mailAddressMode: MailAddressMode
  autoSaveSeconds: number
  undoSendSeconds: number
  driveVersionRetentionCount: number
  driveVersionRetentionDays: number
}
