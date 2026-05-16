import type { SheetsWorkbookDetail, SheetsWorkbookSheet } from "@/service/api/sheets";

export const PROTECTED_RANGE_START_ROW_INDEX = 1;
export const PROTECTED_RANGE_END_ROW_INDEX = 7;
export const PROTECTED_RANGE_START_COL_INDEX = 2;
export const PROTECTED_RANGE_END_COL_INDEX = 3;

export interface SelectedCellPosition {
  rowIndex: number;
  colIndex: number;
}

export interface SheetsCellEdit {
  rowIndex: number;
  colIndex: number;
  value: string;
}

export function createSelectedCell(rowIndex = 0, colIndex = 0): SelectedCellPosition {
  return { rowIndex, colIndex };
}

export function isProtectedRangeCell(rowIndex: number, colIndex: number) {
  return (
    rowIndex >= PROTECTED_RANGE_START_ROW_INDEX &&
    rowIndex <= PROTECTED_RANGE_END_ROW_INDEX &&
    colIndex >= PROTECTED_RANGE_START_COL_INDEX &&
    colIndex <= PROTECTED_RANGE_END_COL_INDEX
  );
}

export function readGridCell(grid: string[][], rowIndex: number, colIndex: number) {
  return grid[rowIndex]?.[colIndex] || "";
}

export function updateGridCell(
  grid: string[][],
  rowIndex: number,
  colIndex: number,
  value: string,
) {
  const nextGrid = grid.map((row) => row.slice());

  while (nextGrid.length <= rowIndex) {
    nextGrid.push([]);
  }

  while ((nextGrid[rowIndex] || []).length <= colIndex) {
    nextGrid[rowIndex].push("");
  }

  nextGrid[rowIndex][colIndex] = value;
  return nextGrid;
}

export function resolveSheetGrid(
  sheet: SheetsWorkbookSheet | null,
  workbook: SheetsWorkbookDetail | null,
) {
  if (sheet?.grid?.length) {
    return sheet.grid;
  }

  if (workbook?.grid?.length) {
    return workbook.grid;
  }

  return [];
}

export function buildPendingEdits(
  sheet: SheetsWorkbookSheet | null,
  workbook: SheetsWorkbookDetail | null,
  grid: string[][],
) {
  if (!sheet) {
    return [];
  }

  const baseGrid = resolveSheetGrid(sheet, workbook);
  const maxRows = Math.max(sheet.rowCount, baseGrid.length, grid.length);
  const maxColumns = Math.max(
    sheet.colCount,
    baseGrid.reduce((max, row) => Math.max(max, row.length), 0),
    grid.reduce((max, row) => Math.max(max, row.length), 0),
  );
  const edits: SheetsCellEdit[] = [];

  for (let rowIndex = 0; rowIndex < maxRows; rowIndex += 1) {
    for (let colIndex = 0; colIndex < maxColumns; colIndex += 1) {
      const nextValue = readGridCell(grid, rowIndex, colIndex);
      const previousValue = baseGrid[rowIndex]?.[colIndex] || "";

      if (nextValue !== previousValue) {
        edits.push({ rowIndex, colIndex, value: nextValue });
      }
    }
  }

  return edits;
}

export function cloneGrid(grid: string[][]) {
  return grid.map((row) => row.map((cell) => cell || ""));
}

export function toColumnLabel(index: number) {
  let value = index + 1;
  let label = "";

  while (value > 0) {
    const remainder = (value - 1) % 26;
    label = String.fromCharCode(65 + remainder) + label;
    value = Math.floor((value - 1) / 26);
  }

  return label || "A";
}

export function joinText(parts: Array<string | null | undefined>) {
  return parts.filter((value): value is string => Boolean(value && value.trim())).join(" · ");
}

export function resolveRuntimeError(error: unknown, fallback: string) {
  if (error instanceof Error && error.message) {
    return error.message;
  }

  return fallback;
}
