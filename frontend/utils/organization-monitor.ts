import type { OrganizationTranslate } from '~/utils/organization-admin'
import type { OrgMemberSession, OrganizationSummaryCard } from '~/types/organization-admin'

export interface OrgMemberSessionSummary {
  activeSessions: number
  uniqueMembers: number
  managerSessions: number
  currentSessions: number
}

export function summarizeOrgMemberSessions(sessions: readonly OrgMemberSession[]): OrgMemberSessionSummary {
  const uniqueMembers = new Set(sessions.map(item => item.memberId)).size
  const managerSessions = sessions.filter(item => item.role === 'OWNER' || item.role === 'ADMIN').length
  const currentSessions = sessions.filter(item => item.current).length
  return {
    activeSessions: sessions.length,
    uniqueMembers,
    managerSessions,
    currentSessions
  }
}

export function buildOrganizationSessionSummaryCards(
  sessions: readonly OrgMemberSession[],
  t: OrganizationTranslate
): OrganizationSummaryCard[] {
  const summary = summarizeOrgMemberSessions(sessions)
  return [
    {
      label: t('organizations.monitor.summary.activeSessions.label'),
      value: String(summary.activeSessions),
      hint: t('organizations.monitor.summary.activeSessions.hint')
    },
    {
      label: t('organizations.monitor.summary.uniqueMembers.label'),
      value: String(summary.uniqueMembers),
      hint: t('organizations.monitor.summary.uniqueMembers.hint')
    },
    {
      label: t('organizations.monitor.summary.managerSessions.label'),
      value: String(summary.managerSessions),
      hint: t('organizations.monitor.summary.managerSessions.hint')
    },
    {
      label: t('organizations.monitor.summary.protectedSessions.label'),
      value: String(summary.currentSessions),
      hint: t('organizations.monitor.summary.protectedSessions.hint')
    }
  ]
}
