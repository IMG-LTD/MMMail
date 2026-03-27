import { describe, expect, it } from 'vitest'
import type {
  SuiteCollaborationCenter,
  SuiteCollaborationSync,
  SuiteNotificationCenter,
  SuiteProductItem,
  SuiteReadinessReport,
  SuiteSecurityPosture,
  SuiteUnifiedSearchResult
} from '../types/api'
import type { OrgProductKey } from '../types/organization-admin'
import {
  filterSuiteCollaborationCenterByAccess,
  filterSuiteCollaborationSyncByAccess,
  filterSuiteNotificationCenterByAccess,
  filterSuiteProductsByAccess,
  filterSuiteReadinessByAccess,
  filterSuiteSecurityPostureByAccess,
  filterSuiteUnifiedSearchByAccess
} from '../utils/org-product-surface-filter'

function buildResolver(enabledProducts: OrgProductKey[]) {
  const enabledSet = new Set(enabledProducts)
  return (productKey: OrgProductKey) => enabledSet.has(productKey)
}

describe('org suite surfaces', () => {
  it('filters suite products, readiness, posture, and unified search by org access', () => {
    const products: SuiteProductItem[] = [
      {
        code: 'MAIL',
        name: 'Mail',
        category: 'COMMUNICATION',
        status: 'ENABLED',
        enabledByPlan: true,
        description: 'mail',
        highlights: ['inbox']
      },
      {
        code: 'DOCS',
        name: 'Docs',
        category: 'COLLABORATION',
        status: 'ENABLED',
        enabledByPlan: true,
        description: 'docs',
        highlights: ['notes']
      },
      {
        code: 'WALLET',
        name: 'Wallet',
        category: 'FINANCE',
        status: 'ENABLED',
        enabledByPlan: true,
        description: 'wallet',
        highlights: ['sign']
      }
    ]

    const readiness: SuiteReadinessReport = {
      generatedAt: '2026-03-10T10:00:00',
      overallScore: 52,
      overallRiskLevel: 'HIGH',
      highRiskProductCount: 1,
      criticalRiskProductCount: 1,
      items: [
        {
          productCode: 'MAIL',
          productName: 'Mail',
          category: 'COMMUNICATION',
          enabledByPlan: true,
          score: 40,
          riskLevel: 'CRITICAL',
          signals: [{ key: 'blocked_domain_count', value: 3, note: 'mail domains' }],
          blockers: ['mail blocker'],
          actions: [{ priority: 'P0', productCode: 'MAIL', action: 'Fix mail', actionCode: 'MAIL_FIX' }]
        },
        {
          productCode: 'DOCS',
          productName: 'Docs',
          category: 'COLLABORATION',
          enabledByPlan: true,
          score: 61,
          riskLevel: 'HIGH',
          signals: [{ key: 'share_count', value: 2, note: 'docs shares' }],
          blockers: ['docs blocker'],
          actions: [{ priority: 'P1', productCode: 'DOCS', action: 'Fix docs', actionCode: 'DOCS_FIX' }]
        },
        {
          productCode: 'WALLET',
          productName: 'Wallet',
          category: 'FINANCE',
          enabledByPlan: true,
          score: 82,
          riskLevel: 'LOW',
          signals: [{ key: 'signed_tx_count', value: 1, note: 'wallet signed' }],
          blockers: [],
          actions: [{ priority: 'P2', productCode: 'WALLET', action: 'Review wallet', actionCode: 'WALLET_REVIEW' }]
        }
      ]
    }

    const posture: SuiteSecurityPosture = {
      generatedAt: '2026-03-10T10:01:00',
      securityScore: 58,
      overallRiskLevel: 'HIGH',
      activeSessionCount: 4,
      blockedSenderCount: 5,
      trustedSenderCount: 6,
      blockedDomainCount: 2,
      trustedDomainCount: 7,
      highRiskProductCount: 1,
      criticalRiskProductCount: 1,
      alerts: ['MAIL alert', 'DOCS alert'],
      recommendedActions: [
        { priority: 'P0', productCode: 'MAIL', action: 'Mail baseline', actionCode: 'MAIL_BASELINE' },
        { priority: 'P1', productCode: 'WALLET', action: 'Wallet baseline', actionCode: 'WALLET_BASELINE' }
      ]
    }

    const searchResult: SuiteUnifiedSearchResult = {
      generatedAt: '2026-03-10T10:02:00',
      keyword: 'scope',
      limit: 20,
      total: 3,
      items: [
        {
          productCode: 'MAIL',
          itemType: 'THREAD',
          entityId: 'mail-1',
          title: 'Mail thread',
          summary: 'mail',
          routePath: '/search?keyword=scope',
          updatedAt: '2026-03-10T09:59:00'
        },
        {
          productCode: 'DOCS',
          itemType: 'NOTE',
          entityId: 'docs-1',
          title: 'Docs note',
          summary: 'docs',
          routePath: '/docs',
          updatedAt: '2026-03-10T09:58:00'
        },
        {
          productCode: 'WALLET',
          itemType: 'TRANSACTION',
          entityId: 'wallet-1',
          title: 'Wallet tx',
          summary: 'wallet',
          routePath: '/wallet',
          updatedAt: '2026-03-10T09:57:00'
        }
      ]
    }

    const resolver = buildResolver(['WALLET'])

    expect(filterSuiteProductsByAccess(products, resolver).map((item) => item.code)).toEqual(['WALLET'])

    const visibleReadiness = filterSuiteReadinessByAccess(readiness, resolver)
    expect(visibleReadiness?.overallScore).toBe(82)
    expect(visibleReadiness?.overallRiskLevel).toBe('LOW')
    expect(visibleReadiness?.highRiskProductCount).toBe(0)
    expect(visibleReadiness?.criticalRiskProductCount).toBe(0)
    expect(visibleReadiness?.items.map((item) => item.productCode)).toEqual(['WALLET'])

    const visiblePosture = filterSuiteSecurityPostureByAccess(posture, resolver)
    expect(visiblePosture?.recommendedActions.map((item) => item.productCode)).toEqual(['WALLET'])

    const visibleSearch = filterSuiteUnifiedSearchByAccess(searchResult, resolver)
    expect(visibleSearch?.total).toBe(1)
    expect(visibleSearch?.items.map((item) => item.productCode)).toEqual(['WALLET'])
  })

  it('filters notifications and collaboration aggregates and recomputes counts', () => {
    const notificationCenter: SuiteNotificationCenter = {
      generatedAt: '2026-03-10T10:03:00',
      limit: 20,
      total: 3,
      criticalCount: 2,
      unreadCount: 2,
      syncCursor: 33,
      syncVersion: 'NTF-33',
      items: [
        {
          notificationId: 'n1',
          channel: 'SECURITY',
          severity: 'CRITICAL',
          title: 'Mail alert',
          message: 'mail',
          routePath: '/search?keyword=scope',
          actionCode: 'MAIL_FIX',
          productCode: 'MAIL',
          createdAt: '2026-03-10T10:00:00',
          read: false,
          readAt: null,
          workflowStatus: 'ACTIVE',
          snoozedUntil: null,
          assignedToUserId: null,
          assignedToDisplayName: null
        },
        {
          notificationId: 'n2',
          channel: 'READINESS',
          severity: 'HIGH',
          title: 'Docs alert',
          message: 'docs',
          routePath: '/docs',
          actionCode: null,
          productCode: 'DOCS',
          createdAt: '2026-03-10T09:59:00',
          read: true,
          readAt: '2026-03-10T10:01:00',
          workflowStatus: 'ARCHIVED',
          snoozedUntil: null,
          assignedToUserId: null,
          assignedToDisplayName: null
        },
        {
          notificationId: 'n3',
          channel: 'ACTION',
          severity: 'LOW',
          title: 'Wallet alert',
          message: 'wallet',
          routePath: '/wallet',
          actionCode: 'WALLET_FIX',
          productCode: 'WALLET',
          createdAt: '2026-03-10T09:58:00',
          read: false,
          readAt: null,
          workflowStatus: 'ACTIVE',
          snoozedUntil: null,
          assignedToUserId: null,
          assignedToDisplayName: null
        }
      ]
    }

    const collaborationCenter: SuiteCollaborationCenter = {
      generatedAt: '2026-03-10T10:04:00',
      limit: 20,
      total: 3,
      productCounts: { ALL: 3, DOCS: 1, DRIVE: 1, MEET: 1 },
      syncCursor: 88,
      syncVersion: 'COLLAB-88',
      items: [
        {
          eventId: 1,
          productCode: 'DOCS',
          eventType: 'DOCS_NOTE_COMMENT_ADD',
          title: 'Docs comment',
          summary: 'docs',
          routePath: '/docs',
          actorEmail: 'docs@mmmail.local',
          sessionId: 's1',
          createdAt: '2026-03-10T10:00:00'
        },
        {
          eventId: 2,
          productCode: 'DRIVE',
          eventType: 'DRIVE_SHARE_CREATE',
          title: 'Drive share',
          summary: 'drive',
          routePath: '/drive',
          actorEmail: 'drive@mmmail.local',
          sessionId: 's2',
          createdAt: '2026-03-10T09:59:00'
        },
        {
          eventId: 3,
          productCode: 'MEET',
          eventType: 'MEET_ROOM_END',
          title: 'Meet ended',
          summary: 'meet',
          routePath: '/meet',
          actorEmail: 'meet@mmmail.local',
          sessionId: 's3',
          createdAt: '2026-03-10T09:58:00'
        }
      ]
    }

    const collaborationSync: SuiteCollaborationSync = {
      kind: 'UPDATE',
      generatedAt: '2026-03-10T10:05:00',
      syncCursor: 89,
      syncVersion: 'COLLAB-89',
      hasUpdates: true,
      total: 3,
      items: collaborationCenter.items
    }

    const resolver = buildResolver(['DRIVE', 'WALLET'])

    const visibleNotifications = filterSuiteNotificationCenterByAccess(notificationCenter, resolver)
    expect(visibleNotifications?.total).toBe(1)
    expect(visibleNotifications?.criticalCount).toBe(0)
    expect(visibleNotifications?.unreadCount).toBe(1)
    expect(visibleNotifications?.items.map((item) => item.productCode)).toEqual(['WALLET'])

    const visibleCollaboration = filterSuiteCollaborationCenterByAccess(collaborationCenter, resolver)
    expect(visibleCollaboration?.total).toBe(1)
    expect(visibleCollaboration?.productCounts).toEqual({
      ALL: 1,
      DOCS: 0,
      DRIVE: 1,
      MEET: 0,
      SHEETS: 0
    })
    expect(visibleCollaboration?.items.map((item) => item.productCode)).toEqual(['DRIVE'])

    const visibleSync = filterSuiteCollaborationSyncByAccess(collaborationSync, resolver)
    expect(visibleSync.hasUpdates).toBe(true)
    expect(visibleSync.total).toBe(1)
    expect(visibleSync.items.map((item) => item.productCode)).toEqual(['DRIVE'])
  })
})
