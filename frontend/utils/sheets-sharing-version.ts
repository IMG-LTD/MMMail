import type {
  SheetsIncomingShare,
  SheetsScopeFilter,
  SheetsWorkbookSummary
} from '~/types/sheets'

export function filterSheetsWorkbooksByScope(
  workbooks: SheetsWorkbookSummary[],
  scopeFilter: SheetsScopeFilter
): SheetsWorkbookSummary[] {
  if (scopeFilter === 'ALL') {
    return [...workbooks]
  }
  return workbooks.filter((item) => item.scope === scopeFilter)
}

export function countPendingIncomingShares(items: SheetsIncomingShare[]): number {
  return items.filter((item) => item.responseStatus === 'NEEDS_ACTION').length
}

export function resolveSheetsVersionSourceI18nKey(sourceEvent: string): string {
  switch (sourceEvent) {
    case 'SHEETS_WORKBOOK_CREATE':
      return 'sheets.versions.sources.create'
    case 'SHEETS_WORKBOOK_IMPORT':
      return 'sheets.versions.sources.import'
    case 'SHEETS_WORKBOOK_UPDATE_CELLS':
      return 'sheets.versions.sources.update'
    case 'SHEETS_WORKBOOK_VERSION_RESTORE':
      return 'sheets.versions.sources.restore'
    default:
      return 'sheets.versions.sources.unknown'
  }
}
