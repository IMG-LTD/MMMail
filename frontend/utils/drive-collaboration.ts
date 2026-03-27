import type { DriveCollaboratorSharedItem, DriveIncomingCollaboratorShare } from '~/types/api'

const COLLABORATOR_STATUS_KEY_MAP: Record<string, string> = {
  NEEDS_ACTION: 'drive.collaboration.status.needsAction',
  ACCEPTED: 'drive.collaboration.status.accepted',
  DECLINED: 'drive.collaboration.status.declined',
  REVOKED: 'drive.collaboration.status.revoked'
}

export function getDriveCollaboratorStatusI18nKey(status: string): string {
  return COLLABORATOR_STATUS_KEY_MAP[status] || 'common.none'
}

export function countPendingDriveIncomingShares(items: DriveIncomingCollaboratorShare[]): number {
  return items.filter((item) => item.responseStatus === 'NEEDS_ACTION').length
}

export function canOpenDriveCollaboratorShare(item: Pick<DriveCollaboratorSharedItem, 'status' | 'available'>): boolean {
  return item.status === 'ACCEPTED' && item.available
}
