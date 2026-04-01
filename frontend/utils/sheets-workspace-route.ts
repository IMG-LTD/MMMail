import type { SheetsScopeFilter } from '~/types/sheets'
import type { LocationQuery, LocationQueryRaw, LocationQueryValue } from 'vue-router'

export type SheetsWorkspaceView = 'WORKBOOKS' | 'INCOMING_SHARES'

export interface SheetsWorkspaceRouteState {
  workbookId: string | null
  view: SheetsWorkspaceView
  scope: SheetsScopeFilter
}

const DEFAULT_SHEETS_SCOPE: SheetsScopeFilter = 'ALL'
const DEFAULT_SHEETS_VIEW: SheetsWorkspaceView = 'WORKBOOKS'
const SHEETS_SCOPE_FILTERS: readonly SheetsScopeFilter[] = ['ALL', 'OWNED', 'SHARED']
const SHEETS_WORKSPACE_VIEWS: readonly SheetsWorkspaceView[] = ['WORKBOOKS', 'INCOMING_SHARES']

function normalizeRouteQueryValue(value: LocationQueryValue | LocationQueryValue[] | undefined): string | null {
  if (Array.isArray(value)) {
    return normalizeRouteQueryValue(value[0])
  }
  if (typeof value !== 'string') {
    return null
  }
  const trimmed = value.trim()
  return trimmed ? trimmed : null
}

export function extractSheetsWorkbookIdFromRouteQuery(
  value: LocationQueryValue | LocationQueryValue[] | undefined
): string | null {
  return normalizeRouteQueryValue(value)
}

function extractSheetsWorkspaceViewFromRouteQuery(
  value: LocationQueryValue | LocationQueryValue[] | undefined
): SheetsWorkspaceView {
  const normalized = normalizeRouteQueryValue(value)
  if (!normalized) {
    return DEFAULT_SHEETS_VIEW
  }
  return SHEETS_WORKSPACE_VIEWS.includes(normalized as SheetsWorkspaceView)
    ? normalized as SheetsWorkspaceView
    : DEFAULT_SHEETS_VIEW
}

function extractSheetsScopeFromRouteQuery(
  value: LocationQueryValue | LocationQueryValue[] | undefined
): SheetsScopeFilter {
  const normalized = normalizeRouteQueryValue(value)
  if (!normalized) {
    return DEFAULT_SHEETS_SCOPE
  }
  return SHEETS_SCOPE_FILTERS.includes(normalized as SheetsScopeFilter)
    ? normalized as SheetsScopeFilter
    : DEFAULT_SHEETS_SCOPE
}

export function extractSheetsWorkspaceRouteState(query: LocationQuery): SheetsWorkspaceRouteState {
  return {
    workbookId: extractSheetsWorkbookIdFromRouteQuery(query.workbookId),
    view: extractSheetsWorkspaceViewFromRouteQuery(query.view),
    scope: extractSheetsScopeFromRouteQuery(query.scope)
  }
}

export function buildSheetsWorkspaceRouteQuery(
  query: LocationQuery,
  state: SheetsWorkspaceRouteState
): LocationQueryRaw {
  const nextQuery: LocationQueryRaw = { ...query }
  delete nextQuery.workbookId
  delete nextQuery.view
  delete nextQuery.scope
  if (state.workbookId) {
    nextQuery.workbookId = state.workbookId
  }
  if (state.view !== DEFAULT_SHEETS_VIEW) {
    nextQuery.view = state.view
  }
  if (state.scope !== DEFAULT_SHEETS_SCOPE) {
    nextQuery.scope = state.scope
  }
  return nextQuery
}

export function hasSheetsWorkspaceRouteStateChanged(
  query: LocationQuery,
  state: SheetsWorkspaceRouteState
): boolean {
  const current = extractSheetsWorkspaceRouteState(query)
  return current.workbookId !== state.workbookId
    || current.view !== state.view
    || current.scope !== state.scope
}

export function resolvePreferredSheetsWorkbookId(
  query: LocationQuery,
  activeWorkbookId: string | null
): string | null {
  return extractSheetsWorkbookIdFromRouteQuery(query.workbookId) || activeWorkbookId
}
