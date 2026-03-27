import { describe, expect, it } from 'vitest'
import { messages } from '../locales'
import type { DriveCollaboratorSharedItem, DriveIncomingCollaboratorShare } from '../types/api'
import {
  canOpenDriveCollaboratorShare,
  countPendingDriveIncomingShares,
  getDriveCollaboratorStatusI18nKey
} from '../utils/drive-collaboration'
import { translate } from '../utils/i18n'

const incomingShares: DriveIncomingCollaboratorShare[] = [
  {
    shareId: 'incoming-1',
    itemId: 'folder-1',
    itemName: 'Design handoff',
    itemType: 'FOLDER',
    permission: 'EDIT',
    responseStatus: 'NEEDS_ACTION',
    ownerEmail: 'owner@mmmail.local',
    ownerDisplayName: 'Owner',
    updatedAt: '2026-03-09T20:00:00'
  },
  {
    shareId: 'incoming-2',
    itemId: 'file-1',
    itemName: 'Quarterly plan',
    itemType: 'FILE',
    permission: 'VIEW',
    responseStatus: 'ACCEPTED',
    ownerEmail: 'owner@mmmail.local',
    ownerDisplayName: 'Owner',
    updatedAt: '2026-03-09T20:03:00'
  },
  {
    shareId: 'incoming-3',
    itemId: 'file-2',
    itemName: 'Expired invite',
    itemType: 'FILE',
    permission: 'VIEW',
    responseStatus: 'DECLINED',
    ownerEmail: 'owner@mmmail.local',
    ownerDisplayName: 'Owner',
    updatedAt: '2026-03-09T20:05:00'
  }
]

const sharedItems: DriveCollaboratorSharedItem[] = [
  {
    shareId: 'shared-1',
    itemId: 'folder-1',
    itemName: 'Product workspace',
    itemType: 'FOLDER',
    permission: 'EDIT',
    status: 'ACCEPTED',
    available: true,
    ownerEmail: 'owner@mmmail.local',
    ownerDisplayName: 'Owner',
    updatedAt: '2026-03-09T20:00:00'
  },
  {
    shareId: 'shared-2',
    itemId: 'file-1',
    itemName: 'Revoked file',
    itemType: 'FILE',
    permission: 'VIEW',
    status: 'REVOKED',
    available: false,
    ownerEmail: 'owner@mmmail.local',
    ownerDisplayName: 'Owner',
    updatedAt: '2026-03-09T20:02:00'
  }
]

describe('drive collaborator sharing', () => {
  it('counts pending incoming invites only', () => {
    expect(countPendingDriveIncomingShares(incomingShares)).toBe(1)
  })

  it('opens only accepted and available shared items', () => {
    expect(canOpenDriveCollaboratorShare(sharedItems[0])).toBe(true)
    expect(canOpenDriveCollaboratorShare(sharedItems[1])).toBe(false)
    expect(canOpenDriveCollaboratorShare({ status: 'ACCEPTED', available: false })).toBe(false)
  })

  it('maps collaborator states to localized copy', () => {
    expect(getDriveCollaboratorStatusI18nKey('NEEDS_ACTION')).toBe('drive.collaboration.status.needsAction')
    expect(translate(messages, 'en', getDriveCollaboratorStatusI18nKey('REVOKED'))).toBe('Revoked')
    expect(translate(messages, 'zh-CN', 'drive.collaboration.owner.title')).toBe('通过邮箱邀请')
    expect(translate(messages, 'zh-TW', 'drive.collaboration.shared.actions.opened')).toBe('已開啟')
    expect(translate(messages, 'en', 'drive.collaboration.workspace.createFolder')).toBe('Create shared folder')
  })
})
