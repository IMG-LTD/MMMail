import type { OrgMonitorStatus, OrganizationSummaryCard } from '../types/organization-admin'
import type { OrganizationTranslate } from './organization-admin'
import { formatOrganizationAuditType } from './organization-admin'

export function buildOrganizationMonitorSemanticCards(
  status: OrgMonitorStatus | null,
  t: OrganizationTranslate
): OrganizationSummaryCard[] {
  return [
    {
      label: t('organizations.monitor.status.semantics.alwaysOn.label'),
      value: status?.alwaysOn
        ? t('organizations.monitor.status.semantics.alwaysOn.value')
        : t('organizations.monitor.status.metrics.empty'),
      hint: t('organizations.monitor.status.semantics.alwaysOn.hint')
    },
    {
      label: t('organizations.monitor.status.semantics.retention.label'),
      value: t(`organizations.monitor.status.retention.${status?.retentionMode || 'PERMANENT'}`),
      hint: t('organizations.monitor.status.semantics.retention.hint')
    },
    {
      label: t('organizations.monitor.status.semantics.visibility.label'),
      value: t(`organizations.monitor.status.visibility.${status?.visibilityScope || 'ALL_ADMINS'}`),
      hint: t('organizations.monitor.status.semantics.visibility.hint')
    },
    {
      label: t('organizations.monitor.status.semantics.immutability.label'),
      value: t('organizations.monitor.status.semantics.immutability.value'),
      hint: t('organizations.monitor.status.semantics.immutability.hint')
    }
  ]
}

export function buildOrganizationMonitorMetricCards(
  status: OrgMonitorStatus | null,
  t: OrganizationTranslate
): OrganizationSummaryCard[] {
  return [
    {
      label: t('organizations.monitor.status.metrics.totalEvents.label'),
      value: String(status?.totalEvents ?? 0),
      hint: t('organizations.monitor.status.metrics.totalEvents.hint')
    },
    {
      label: t('organizations.monitor.status.metrics.coveredEventTypes.label'),
      value: String(status?.coveredEventTypes ?? 0),
      hint: t('organizations.monitor.status.metrics.coveredEventTypes.hint')
    },
    {
      label: t('organizations.monitor.status.metrics.latestEvent.label'),
      value: resolveLatestEventValue(status, t),
      hint: status?.latestEvent?.createdAt || t('organizations.monitor.status.metrics.latestEvent.empty')
    },
    {
      label: t('organizations.monitor.status.metrics.exportLimit.label'),
      value: String(status?.maximumExportSize ?? 0),
      hint: t('organizations.monitor.status.metrics.exportLimit.hint')
    }
  ]
}

function resolveLatestEventValue(
  status: OrgMonitorStatus | null,
  t: OrganizationTranslate
): string {
  if (!status?.latestEvent) {
    return t('organizations.monitor.status.metrics.empty')
  }
  return formatOrganizationAuditType(status.latestEvent.eventType, t)
}
