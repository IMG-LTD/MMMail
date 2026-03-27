import type { SuiteCollaborationEvent } from '~/types/api'

export interface SheetsTemplatePreset {
  code: string
  titleKey: string
  descriptionKey: string
  presetTitleKey: string
}

export const SHEETS_TEMPLATE_PRESETS: SheetsTemplatePreset[] = [
  {
    code: 'launch-readiness',
    titleKey: 'sheets.templates.launchReadiness.title',
    descriptionKey: 'sheets.templates.launchReadiness.description',
    presetTitleKey: 'sheets.templates.launchReadiness.presetTitle'
  },
  {
    code: 'budget-ops',
    titleKey: 'sheets.templates.budgetOps.title',
    descriptionKey: 'sheets.templates.budgetOps.description',
    presetTitleKey: 'sheets.templates.budgetOps.presetTitle'
  },
  {
    code: 'support-handoff',
    titleKey: 'sheets.templates.supportHandoff.title',
    descriptionKey: 'sheets.templates.supportHandoff.description',
    presetTitleKey: 'sheets.templates.supportHandoff.presetTitle'
  }
]

export function extractWorkbookIdFromRoute(routePath: string): string | null {
  if (!routePath) {
    return null
  }
  try {
    const url = new URL(routePath, 'https://mmmail.local')
    return url.searchParams.get('workbookId')
  } catch {
    return null
  }
}

export function filterSheetsCollaborationEvents(
  items: SuiteCollaborationEvent[],
  workbookId: string | null
): SuiteCollaborationEvent[] {
  const sheetsItems = items.filter((item) => item.productCode === 'SHEETS')
  if (!workbookId) {
    return sheetsItems
  }
  return sheetsItems.filter((item) => extractWorkbookIdFromRoute(item.routePath) === workbookId)
}
