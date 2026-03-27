import { describe, expect, it } from 'vitest'
import { messages } from '../locales'
import type { OrgMemberSession, OrgMonitorStatus } from '../types/organization-admin'
import { translate } from '../utils/i18n'
import {
  buildOrganizationSessionSummaryCards,
  summarizeOrgMemberSessions
} from '../utils/organization-monitor'
import {
  buildOrganizationMonitorMetricCards,
  buildOrganizationMonitorSemanticCards
} from '../utils/organization-monitor-status'

const tEn = (key: string, params?: Record<string, string | number>) => translate(messages, 'en', key, params)

const sessions: OrgMemberSession[] = [
  {
    sessionId: '101',
    memberId: 'member-1',
    userId: 'user-1',
    memberEmail: 'owner@example.com',
    role: 'OWNER',
    createdAt: '2026-03-10T10:00:00',
    expiresAt: '2026-03-17T10:00:00',
    current: true
  },
  {
    sessionId: '102',
    memberId: 'member-2',
    userId: 'user-2',
    memberEmail: 'admin@example.com',
    role: 'ADMIN',
    createdAt: '2026-03-10T10:10:00',
    expiresAt: '2026-03-17T10:10:00',
    current: false
  },
  {
    sessionId: '103',
    memberId: 'member-3',
    userId: 'user-3',
    memberEmail: 'member@example.com',
    role: 'MEMBER',
    createdAt: '2026-03-10T10:20:00',
    expiresAt: '2026-03-17T10:20:00',
    current: false
  },
  {
    sessionId: '104',
    memberId: 'member-3',
    userId: 'user-3',
    memberEmail: 'member@example.com',
    role: 'MEMBER',
    createdAt: '2026-03-10T10:30:00',
    expiresAt: '2026-03-17T10:30:00',
    current: false
  }
]

const monitorStatus: OrgMonitorStatus = {
  orgId: 'org-1',
  alwaysOn: true,
  canDisable: false,
  canDeleteEvents: false,
  canEditEvents: false,
  visibilityScope: 'ALL_ADMINS',
  retentionMode: 'PERMANENT',
  totalEvents: 42,
  coveredEventTypes: 8,
  activeSessions: 4,
  managerSessions: 2,
  protectedSessions: 1,
  maximumExportSize: 10000,
  oldestEventAt: '2026-03-01T09:00:00',
  latestEvent: {
    id: '88',
    orgId: 'org-1',
    actorId: 'actor-1',
    actorEmail: 'admin@example.com',
    eventType: 'ORG_MEMBER_SESSION_REVOKE',
    ipAddress: '0.0.0.0',
    detail: 'session revoked',
    createdAt: '2026-03-10T15:00:00'
  },
  generatedAt: '2026-03-10T15:01:00'
}

describe('organization monitor utils', () => {
  it('summarizes active sessions for monitor cards', () => {
    expect(summarizeOrgMemberSessions(sessions)).toEqual({
      activeSessions: 4,
      uniqueMembers: 3,
      managerSessions: 2,
      currentSessions: 1
    })
  })

  it('builds translated session summary cards', () => {
    expect(buildOrganizationSessionSummaryCards(sessions, tEn)).toEqual([
      {
        label: 'Active sessions',
        value: '4',
        hint: 'Live refresh-token sessions'
      },
      {
        label: 'Members online',
        value: '3',
        hint: 'Unique accounts represented'
      },
      {
        label: 'Manager sessions',
        value: '2',
        hint: 'Owner/Admin seats online'
      },
      {
        label: 'Protected sessions',
        value: '1',
        hint: 'Current session cannot be revoked'
      }
    ])
  })

  it('builds monitor semantic cards', () => {
    expect(buildOrganizationMonitorSemanticCards(monitorStatus, tEn)).toEqual([
      {
        label: 'Monitor rail',
        value: 'Always on',
        hint: 'Organization monitor cannot be disabled by members or admins.'
      },
      {
        label: 'Retention',
        value: 'Permanent',
        hint: 'Retained organization events stay available as a permanent archive.'
      },
      {
        label: 'Visibility',
        value: 'All admins',
        hint: 'All organization administrators can review the monitor dashboard.'
      },
      {
        label: 'Audit history',
        value: 'Tamper resistant',
        hint: 'Nobody can edit or delete retained monitor events from this product flow.'
      }
    ])
  })

  it('builds monitor metric cards', () => {
    expect(buildOrganizationMonitorMetricCards(monitorStatus, tEn)).toEqual([
      {
        label: 'Events retained',
        value: '42',
        hint: 'All retained organization monitor events.'
      },
      {
        label: 'Event categories',
        value: '8',
        hint: 'Unique audit event types currently captured.'
      },
      {
        label: 'Latest event',
        value: 'Member session revoked',
        hint: '2026-03-10T15:00:00'
      },
      {
        label: 'CSV export cap',
        value: '10000',
        hint: 'Maximum rows available in a single CSV export.'
      }
    ])
  })
})
