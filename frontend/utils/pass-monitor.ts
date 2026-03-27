import type { PassMonitorItem, PassMonitorOverview } from '~/types/pass-business'

type Translate = (key: string, params?: Record<string, string | number>) => string

export interface PassMonitorMetricCard {
  key: string
  label: string
  value: string
  hint: string
}

export interface PassMonitorSection {
  key: 'weak' | 'reused' | 'inactiveTwoFactor' | 'excluded'
  title: string
  description: string
  emptyText: string
  items: PassMonitorItem[]
}

export function buildPassMonitorMetricCards(
  overview: PassMonitorOverview | null,
  t: Translate
): PassMonitorMetricCard[] {
  return [
    metricCard('total', overview?.totalItemCount, t),
    metricCard('tracked', overview?.trackedItemCount, t),
    metricCard('weak', overview?.weakPasswordCount, t),
    metricCard('reused', overview?.reusedPasswordCount, t),
    metricCard('inactiveTwoFactor', overview?.inactiveTwoFactorCount, t)
  ]
}

export function buildPassMonitorSections(
  overview: PassMonitorOverview | null,
  t: Translate
): PassMonitorSection[] {
  return [
    section('weak', overview?.weakPasswords || [], t),
    section('reused', overview?.reusedPasswords || [], t),
    section('inactiveTwoFactor', overview?.inactiveTwoFactorItems || [], t),
    section('excluded', overview?.excludedItems || [], t)
  ]
}

export function resolveDefaultMonitorOrgId(orgIds: string[], selectedOrgId: string): string {
  if (selectedOrgId && orgIds.includes(selectedOrgId)) {
    return selectedOrgId
  }
  return orgIds[0] || ''
}

function metricCard(key: string, value: number | undefined, t: Translate): PassMonitorMetricCard {
  return {
    key,
    label: t(`pass.monitor.metrics.${key}`),
    value: value === undefined ? '--' : String(value),
    hint: t(`pass.monitor.metrics.${key}Hint`)
  }
}

function section(key: PassMonitorSection['key'], items: PassMonitorItem[], t: Translate): PassMonitorSection {
  return {
    key,
    title: t(`pass.monitor.sections.${key}.title`),
    description: t(`pass.monitor.sections.${key}.description`),
    emptyText: t(`pass.monitor.empty.${key}`),
    items
  }
}
