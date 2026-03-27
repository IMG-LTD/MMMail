import { describe, expect, it } from 'vitest'
import type {
  SuiteCommandCenter,
  SuiteCommandFeed
} from '../types/api'
import type { OrgProductKey } from '../types/organization-admin'
import {
  filterSuiteCommandCenterByAccess,
  filterSuiteCommandFeedByAccess
} from '../utils/org-product-surface-filter'

function buildResolver(enabledProducts: OrgProductKey[]) {
  const enabledSet = new Set(enabledProducts)
  return (productKey: OrgProductKey) => enabledSet.has(productKey)
}

describe('org command surfaces', () => {
  it('filters command center routes, presets, actions, and mail keywords by active org scope', () => {
    const commandCenter: SuiteCommandCenter = {
      generatedAt: '2026-03-10T10:00:00',
      quickRoutes: [
        {
          commandType: 'ROUTE',
          label: 'Mail Inbox',
          description: 'Open inbox',
          routePath: '/inbox',
          actionCode: null,
          productCode: 'MAIL',
          priority: null
        },
        {
          commandType: 'ROUTE',
          label: 'Docs Home',
          description: 'Open docs',
          routePath: '/docs',
          actionCode: null,
          productCode: 'DOCS',
          priority: null
        }
      ],
      pinnedSearches: [
        {
          commandType: 'PRESET',
          label: 'Pinned search',
          description: 'mail preset',
          routePath: '/search?keyword=scope',
          actionCode: null,
          productCode: null,
          priority: null
        }
      ],
      recentKeywords: ['scope keyword'],
      recommendedActions: [
        { priority: 'P0', productCode: 'MAIL', action: 'Mail baseline', actionCode: 'MAIL_BASELINE' },
        { priority: 'P1', productCode: 'PASS', action: 'Pass baseline', actionCode: 'PASS_BASELINE' }
      ],
      pendingGovernanceCount: 2,
      securityAlertCount: 3
    }

    const visible = filterSuiteCommandCenterByAccess(commandCenter, buildResolver(['DOCS', 'PASS']))

    expect(visible?.quickRoutes.map((item) => item.routePath)).toEqual(['/docs'])
    expect(visible?.pinnedSearches).toEqual([])
    expect(visible?.recentKeywords).toEqual([])
    expect(visible?.recommendedActions.map((item) => item.productCode)).toEqual(['PASS'])
  })

  it('filters command feed by productCode and route-derived mail surfaces', () => {
    const commandFeed: SuiteCommandFeed = {
      generatedAt: '2026-03-10T10:05:00',
      limit: 10,
      total: 3,
      items: [
        {
          eventId: 1,
          eventType: 'MAIL_SEARCH',
          category: 'MAIL',
          title: 'Mail search',
          detail: 'mail event',
          productCode: null,
          routePath: '/mail/thread-1',
          ipAddress: '127.0.0.1',
          createdAt: '2026-03-10T10:04:00'
        },
        {
          eventId: 2,
          eventType: 'DOCS_SEARCH',
          category: 'DOCS',
          title: 'Docs search',
          detail: 'docs event',
          productCode: 'DOCS',
          routePath: '/docs',
          ipAddress: '127.0.0.1',
          createdAt: '2026-03-10T10:03:00'
        },
        {
          eventId: 3,
          eventType: 'PASS_ACTION',
          category: 'PASS',
          title: 'Pass action',
          detail: 'pass event',
          productCode: 'PASS',
          routePath: '/pass',
          ipAddress: '127.0.0.1',
          createdAt: '2026-03-10T10:02:00'
        }
      ]
    }

    const visible = filterSuiteCommandFeedByAccess(commandFeed, buildResolver(['DOCS', 'PASS']))

    expect(visible?.total).toBe(2)
    expect(visible?.items.map((item) => item.eventId)).toEqual([2, 3])
  })

  it('drops aggregate suite feed events without product affinity in restricted org scope', () => {
    const commandFeed: SuiteCommandFeed = {
      generatedAt: '2026-03-10T10:06:00',
      limit: 10,
      total: 3,
      items: [
        {
          eventId: 1,
          eventType: 'SUITE_PRODUCT_LIST',
          category: 'SUITE',
          title: 'Product list viewed',
          detail: 'count=14',
          productCode: null,
          routePath: '/command-center',
          ipAddress: '127.0.0.1',
          createdAt: '2026-03-10T10:05:00'
        },
        {
          eventId: 2,
          eventType: 'SUITE_SUBSCRIPTION_QUERY',
          category: 'SUITE',
          title: 'Subscription viewed',
          detail: 'plan=FREE',
          productCode: null,
          routePath: '/command-center',
          ipAddress: '127.0.0.1',
          createdAt: '2026-03-10T10:04:00'
        },
        {
          eventId: 3,
          eventType: 'SUITE_COLLABORATION_CENTER_QUERY',
          category: 'SUITE',
          title: 'Collaboration center viewed',
          detail: 'count=0,limit=60',
          productCode: null,
          routePath: '/command-center',
          ipAddress: '127.0.0.1',
          createdAt: '2026-03-10T10:03:30'
        },
        {
          eventId: 4,
          eventType: 'PASS_ACTION',
          category: 'PASS',
          title: 'Pass action',
          detail: 'pass event',
          productCode: 'PASS',
          routePath: '/pass',
          ipAddress: '127.0.0.1',
          createdAt: '2026-03-10T10:03:00'
        }
      ]
    }

    const visible = filterSuiteCommandFeedByAccess(commandFeed, buildResolver(['PASS']))

    expect(visible?.total).toBe(1)
    expect(visible?.items.map((item) => item.eventType)).toEqual(['PASS_ACTION'])
  })
})
