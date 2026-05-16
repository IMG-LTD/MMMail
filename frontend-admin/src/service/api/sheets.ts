import { request } from '../request';

export function listSheetsWorkbooks(params: Record<string, number | undefined> = {}) {
  return request<Api.Sheets.WorkbookSummary[]>({ url: '/api/v2/sheets', params });
}

export function createSheetsWorkbook(data: { title: string; rowCount: number; colCount: number }) {
  return request<Api.Sheets.WorkbookDetail>({
    url: '/api/v2/sheets',
    method: 'post',
    data
  });
}

export function readSheetsWorkbook(workbookId: string) {
  return request<Api.Sheets.WorkbookDetail>({ url: `/api/v2/sheets/${workbookId}` });
}

export function updateSheetsWorkbookCells(workbookId: string, data: Record<string, unknown>) {
  return request<Api.Sheets.WorkbookDetail>({
    url: `/api/v2/sheets/${workbookId}`,
    method: 'patch',
    data
  });
}

export function evaluateSheetsCells(workbookId: string, data: Api.Sheets.FormulaEvaluationPayload) {
  return request<Api.Sheets.FormulaEvaluation>({
    url: `/api/v1/sheets/${workbookId}/cells/evaluate`,
    method: 'post',
    data
  });
}

export function readSheetsDependencyGraph(workbookId: string) {
  return request<Api.Sheets.DependencyGraph>({ url: `/api/v1/sheets/${workbookId}/dependency-graph` });
}

export function recalculateSheetsWorkbook(workbookId: string) {
  return request<Api.Sheets.WorkbookDetail>({
    url: `/api/v1/sheets/${workbookId}/recalculate`,
    method: 'post'
  });
}
