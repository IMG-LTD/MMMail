import { describe, expect, it } from 'vitest'
import type { SheetsIncomingShare, SheetsWorkbookSummary } from '../types/sheets'
import { countPendingIncomingShares, filterSheetsWorkbooksByScope, resolveSheetsVersionSourceI18nKey } from '../utils/sheets-sharing-version'

describe('sheets sharing/version utils', () => {
  const workbookBase: Omit<SheetsWorkbookSummary, 'scope' | 'permission'> = {
    id: '1',
    title: 'Workbook',
    rowCount: 4,
    colCount: 4,
    filledCellCount: 1,
    formulaCellCount: 0,
    computedErrorCount: 0,
    currentVersion: 1,
    sheetCount: 1,
    activeSheetId: 'sheet-1',
    updatedAt: '2026-03-09T12:00:00',
    lastOpenedAt: null,
    ownerEmail: 'owner@mmmail.local',
    ownerDisplayName: 'Owner',
    collaboratorCount: 1,
    canEdit: true
  }

  it('filters workbooks by scope', () => {
    const workbooks: SheetsWorkbookSummary[] = [
      { ...workbookBase, id: 'owned-1', scope: 'OWNED', permission: 'OWNER' },
      { ...workbookBase, id: 'shared-1', scope: 'SHARED', permission: 'VIEW', canEdit: false }
    ]
    expect(filterSheetsWorkbooksByScope(workbooks, 'ALL')).toHaveLength(2)
    expect(filterSheetsWorkbooksByScope(workbooks, 'OWNED')).toHaveLength(1)
    expect(filterSheetsWorkbooksByScope(workbooks, 'SHARED')[0].id).toBe('shared-1')
  })

  it('counts pending incoming shares and maps version sources', () => {
    const shares: SheetsIncomingShare[] = [
      { shareId: '1', workbookId: 'w1', workbookTitle: 'A', ownerEmail: 'a', ownerDisplayName: 'A', permission: 'VIEW', responseStatus: 'NEEDS_ACTION', updatedAt: '2026-03-09T12:00:00' },
      { shareId: '2', workbookId: 'w2', workbookTitle: 'B', ownerEmail: 'b', ownerDisplayName: 'B', permission: 'EDIT', responseStatus: 'ACCEPTED', updatedAt: '2026-03-09T12:10:00' }
    ]
    expect(countPendingIncomingShares(shares)).toBe(1)
    expect(resolveSheetsVersionSourceI18nKey('SHEETS_WORKBOOK_CREATE')).toBe('sheets.versions.sources.create')
    expect(resolveSheetsVersionSourceI18nKey('SHEETS_WORKBOOK_VERSION_RESTORE')).toBe('sheets.versions.sources.restore')
    expect(resolveSheetsVersionSourceI18nKey('UNKNOWN')).toBe('sheets.versions.sources.unknown')
  })
})
