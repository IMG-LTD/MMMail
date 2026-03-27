import type { SheetsGrid } from '~/types/sheets'
import { buildCellEditKey, getCellValue } from './sheets'

export interface SheetsSearchMatch {
  rowIndex: number
  colIndex: number
  key: string
}

interface FindSheetsSearchMatchesOptions {
  rawGrid: SheetsGrid
  computedGrid: SheetsGrid
  query: string
}

export function findSheetsSearchMatches(options: FindSheetsSearchMatchesOptions): SheetsSearchMatch[] {
  const normalizedQuery = options.query.trim().toLowerCase()
  if (!normalizedQuery) {
    return []
  }

  const rowCount = Math.max(options.rawGrid.length, options.computedGrid.length)
  const matches: SheetsSearchMatch[] = []
  for (let rowIndex = 0; rowIndex < rowCount; rowIndex += 1) {
    const colCount = Math.max(options.rawGrid[rowIndex]?.length ?? 0, options.computedGrid[rowIndex]?.length ?? 0)
    for (let colIndex = 0; colIndex < colCount; colIndex += 1) {
      if (isSearchMatch(options.rawGrid, options.computedGrid, rowIndex, colIndex, normalizedQuery)) {
        matches.push({ rowIndex, colIndex, key: buildCellEditKey(rowIndex, colIndex) })
      }
    }
  }
  return matches
}

function isSearchMatch(
  rawGrid: SheetsGrid,
  computedGrid: SheetsGrid,
  rowIndex: number,
  colIndex: number,
  query: string
): boolean {
  const rawValue = getCellValue(rawGrid, rowIndex, colIndex).toLowerCase()
  const computedValue = getCellValue(computedGrid, rowIndex, colIndex).toLowerCase()
  return rawValue.includes(query) || computedValue.includes(query)
}
