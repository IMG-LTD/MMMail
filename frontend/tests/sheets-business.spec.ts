import { describe, expect, it } from 'vitest'
import type { SheetsWorkbookDetail } from '../types/sheets'
import { findSheetsSearchMatches } from '../utils/sheets-search'
import {
  applyCellValue,
  buildFormulaHint,
  buildSheetsHealthChips,
  cellLabel,
  collectCellEdits,
  columnLabel,
  countFilledCells,
  normalizeSheetsGrid,
  resolveCellPresentation,
  resolveImportFormat,
  sortSheetsWorkbooks,
  summarizeSupportedFormats
} from '../utils/sheets'

describe('sheets utils', () => {
  it('normalizes sparse grids into a fixed rectangle', () => {
    expect(normalizeSheetsGrid([['A1'], ['B1', 'B2']], 3, 3)).toEqual([
      ['A1', '', ''],
      ['B1', 'B2', ''],
      ['', '', '']
    ])
  })

  it('finds matches across raw and computed sheet values', () => {
    const rawGrid = normalizeSheetsGrid([['Alpha', '=SUM(B1:B1)'], ['Beta', '']], 2, 2)
    const computedGrid = normalizeSheetsGrid([['Alpha', '42'], ['Beta', '']], 2, 2)
    expect(findSheetsSearchMatches({ rawGrid, computedGrid, query: 'alp' })).toEqual([
      { rowIndex: 0, colIndex: 0, key: '0:0' }
    ])
    expect(findSheetsSearchMatches({ rawGrid, computedGrid, query: '42' })).toEqual([
      { rowIndex: 0, colIndex: 1, key: '0:1' }
    ])
  })

  it('builds spreadsheet labels', () => {
    expect(columnLabel(0)).toBe('A')
    expect(columnLabel(25)).toBe('Z')
    expect(columnLabel(26)).toBe('AA')
    expect(columnLabel(51)).toBe('AZ')
    expect(cellLabel(4, 2)).toBe('C5')
  })

  it('counts filled cells and applies immutable cell edits', () => {
    const grid = normalizeSheetsGrid([['Roadmap']], 2, 2)
    const next = applyCellValue(grid, 1, 1, 'Ready')
    expect(grid[1][1]).toBe('')
    expect(next[1][1]).toBe('Ready')
    expect(countFilledCells(next)).toBe(2)
  })

  it('sorts workbooks by recent open time first', () => {
    const items = sortSheetsWorkbooks([
      { id: '1', updatedAt: '2026-03-07T10:00:00', lastOpenedAt: null },
      { id: '2', updatedAt: '2026-03-07T09:00:00', lastOpenedAt: '2026-03-07T11:00:00' },
      { id: '3', updatedAt: '2026-03-07T12:00:00', lastOpenedAt: null }
    ])
    expect(items.map((item) => item.id)).toEqual(['3', '2', '1'])
  })

  it('collects dirty edits in row-major order', () => {
    const edits = new Map([
      ['1:2', { rowIndex: 1, colIndex: 2, value: 'B2' }],
      ['0:1', { rowIndex: 0, colIndex: 1, value: 'A2' }]
    ])
    expect(collectCellEdits(edits).map((item) => `${item.rowIndex}:${item.colIndex}`)).toEqual(['0:1', '1:2'])
  })

  it('resolves formula presentation and pending preview state', () => {
    const rawGrid = normalizeSheetsGrid([['10', '=SUM(A1:A1)']], 1, 2)
    const savedGrid = normalizeSheetsGrid([['10', '=SUM(A1:A1)']], 1, 2)
    const computedGrid = normalizeSheetsGrid([['10', '10']], 1, 2)
    const savedFormula = resolveCellPresentation({ rawGrid, savedGrid, computedGrid, rowIndex: 0, colIndex: 1 })
    expect(savedFormula.displayValue).toBe('10')
    expect(buildFormulaHint(savedFormula)).toContain('saved workbook state')

    const dirtyGrid = normalizeSheetsGrid([['10', '=SUM(A1:A2)']], 2, 2)
    const pendingFormula = resolveCellPresentation({ rawGrid: dirtyGrid, savedGrid, computedGrid, rowIndex: 0, colIndex: 1 })
    expect(pendingFormula.displayValue).toBe('Pending…')
    expect(buildFormulaHint(pendingFormula)).toContain('Unsaved formula')
  })

  it('builds workspace health chips and format helpers', () => {
    const workbook: SheetsWorkbookDetail = {
      id: '1',
      title: 'Ops',
      rowCount: 2,
      colCount: 2,
      filledCellCount: 2,
      formulaCellCount: 1,
      computedErrorCount: 0,
      currentVersion: 2,
      sheetCount: 2,
      activeSheetId: 'sheet-1',
      updatedAt: '2026-03-08T12:00:00',
      lastOpenedAt: '2026-03-08T12:05:00',
      permission: 'OWNER',
      scope: 'OWNED',
      ownerEmail: 'owner@mmmail.local',
      ownerDisplayName: 'Owner',
      collaboratorCount: 1,
      canEdit: true,
      sheets: [
        {
          id: 'sheet-1',
          name: 'Summary',
          rowCount: 2,
          colCount: 2,
          frozenRowCount: 0,
          frozenColCount: 0,
          filledCellCount: 2,
          formulaCellCount: 1,
          computedErrorCount: 0,
          grid: [['10', '=SUM(A1:A1)']],
          computedGrid: [['10', '10']]
        },
        {
          id: 'sheet-2',
          name: 'Archive',
          rowCount: 2,
          colCount: 2,
          frozenRowCount: 0,
          frozenColCount: 0,
          filledCellCount: 0,
          formulaCellCount: 0,
          computedErrorCount: 0,
          grid: [['', '']],
          computedGrid: [['', '']]
        }
      ],
      grid: [['10', '=SUM(A1:A1)']],
      computedGrid: [['10', '10']],
      supportedImportFormats: ['CSV', 'TSV', 'XLSX'],
      supportedExportFormats: ['CSV', 'TSV', 'JSON'],
      createdAt: '2026-03-08T11:59:00',
      canManageShares: true,
      canRestoreVersions: true
    }
    expect(buildSheetsHealthChips({ workbookCount: 1, workbook, dirtyCount: 0 })).toContain('1 formulas')
    expect(summarizeSupportedFormats(workbook.supportedExportFormats)).toBe('CSV / TSV / JSON')
    expect(resolveImportFormat('report.xlsx')).toBe('XLSX')
    expect(resolveImportFormat('report.unknown')).toBe('UNKNOWN')
  })
})
