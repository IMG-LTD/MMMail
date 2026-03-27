const SHARE_STATUS_KEY_MAP: Record<string, string> = {
  ACTIVE: 'drive.shareDrawer.status.active',
  REVOKED: 'drive.shareDrawer.status.revoked'
}

const ACCESS_ACTION_KEY_MAP: Record<string, string> = {
  METADATA: 'drive.accessLog.actions.metadata',
  LIST: 'drive.accessLog.actions.list',
  DOWNLOAD: 'drive.accessLog.actions.download',
  PREVIEW: 'drive.accessLog.actions.preview',
  UPLOAD: 'drive.accessLog.actions.upload',
  SAVE: 'drive.accessLog.actions.save'
}

const SAVED_SHARE_STATUS_KEY_MAP: Record<string, string> = {
  ACTIVE: 'drive.sharedWithMe.status.active',
  REVOKED: 'drive.sharedWithMe.status.revoked',
  EXPIRED: 'drive.sharedWithMe.status.expired',
  UNAVAILABLE: 'drive.sharedWithMe.status.unavailable'
}

const ACCESS_STATUS_KEY_MAP: Record<string, string> = {
  ALLOW: 'drive.accessLog.status.allow',
  DENY_RATE_LIMIT: 'drive.accessLog.status.denyRateLimit',
  DENY_INVALID_TOKEN: 'drive.accessLog.status.denyInvalidToken',
  DENY_REVOKED: 'drive.accessLog.status.denyRevoked',
  DENY_EXPIRED: 'drive.accessLog.status.denyExpired',
  DENY_FILE_MISSING: 'drive.accessLog.status.denyFileMissing',
  DENY_PERMISSION: 'drive.accessLog.status.denyPermission',
  DENY_PASSWORD_REQUIRED: 'drive.accessLog.status.denyPasswordRequired',
  DENY_PASSWORD_INVALID: 'drive.accessLog.status.denyPasswordInvalid',
  DENY_UNSUPPORTED_PREVIEW: 'drive.accessLog.status.denyUnsupportedPreview'
}

const PUBLIC_SHARE_ERROR_KEY_MAP: Record<string, string> = {
  'Share link is unavailable': 'drive.publicShare.errors.linkUnavailable',
  'Drive share password is required': 'drive.publicShare.errors.passwordRequired',
  'Drive share password is invalid': 'drive.publicShare.errors.passwordInvalid',
  'Drive preview is unavailable for current file type': 'drive.publicShare.errors.previewUnavailable',
  'Public share access is temporarily rate limited': 'drive.publicShare.errors.rateLimited',
  'Drive share does not allow uploads': 'drive.publicShare.errors.uploadForbidden',
  'Shared folder is unavailable': 'drive.publicShare.errors.folderUnavailable',
  'Shared file is unavailable': 'drive.publicShare.errors.fileUnavailable',
  'Only view-only shared links can be saved': 'drive.sharedWithMe.errors.onlyViewLinks',
  'Drive share is already saved': 'drive.sharedWithMe.errors.alreadySaved',
  'Shared with me item is not found': 'drive.sharedWithMe.errors.notFound'
}

export const DRIVE_SHARE_ACCESS_ACTION_OPTIONS = ['METADATA', 'LIST', 'DOWNLOAD', 'PREVIEW', 'UPLOAD', 'SAVE'] as const

export const DRIVE_SHARE_ACCESS_STATUS_OPTIONS = [
  'ALLOW',
  'DENY_RATE_LIMIT',
  'DENY_INVALID_TOKEN',
  'DENY_REVOKED',
  'DENY_EXPIRED',
  'DENY_FILE_MISSING',
  'DENY_PERMISSION',
  'DENY_PASSWORD_REQUIRED',
  'DENY_PASSWORD_INVALID',
  'DENY_UNSUPPORTED_PREVIEW'
] as const

export function getDriveShareStatusI18nKey(status: string): string {
  return SHARE_STATUS_KEY_MAP[status] || 'common.none'
}

export function getDriveShareProtectionI18nKey(passwordProtected: boolean): string {
  return passwordProtected
    ? 'drive.shareDrawer.protection.protected'
    : 'drive.shareDrawer.protection.open'
}

export function getDriveShareAccessActionI18nKey(action: string): string {
  return ACCESS_ACTION_KEY_MAP[action] || 'common.none'
}

export function getDriveShareAccessStatusI18nKey(status: string): string {
  return ACCESS_STATUS_KEY_MAP[status] || 'common.none'
}

export function getDriveSavedShareStatusI18nKey(status: string): string {
  return SAVED_SHARE_STATUS_KEY_MAP[status] || 'common.none'
}

export function resolvePublicDriveShareErrorKey(message: string): string | null {
  return PUBLIC_SHARE_ERROR_KEY_MAP[message] || null
}
