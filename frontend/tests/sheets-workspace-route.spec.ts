import { describe, expect, it } from 'vitest'
import {
  buildSheetsWorkspaceRouteQuery,
  extractSheetsWorkbookIdFromRouteQuery,
  extractSheetsWorkspaceRouteState,
  hasSheetsWorkspaceRouteStateChanged,
  resolvePreferredSheetsWorkbookId
} from '../utils/sheets-workspace-route'

describe('sheets workspace route utilities', () => {
  it('extracts workbook id from route query variants', () => {
    expect(extractSheetsWorkbookIdFromRouteQuery('wb-1')).toBe('wb-1')
    expect(extractSheetsWorkbookIdFromRouteQuery(['wb-2', 'wb-3'])).toBe('wb-2')
    expect(extractSheetsWorkbookIdFromRouteQuery('   ')).toBeNull()
    expect(extractSheetsWorkbookIdFromRouteQuery(undefined)).toBeNull()
  })

  it('extracts normalized view and scope state', () => {
    expect(extractSheetsWorkspaceRouteState({
      workbookId: ['wb-2'],
      view: 'INCOMING_SHARES',
      scope: 'SHARED'
    })).toEqual({
      workbookId: 'wb-2',
      view: 'INCOMING_SHARES',
      scope: 'SHARED'
    })
    expect(extractSheetsWorkspaceRouteState({
      workbookId: '   ',
      view: 'INVALID',
      scope: 'INVALID'
    })).toEqual({
      workbookId: null,
      view: 'WORKBOOKS',
      scope: 'ALL'
    })
  })

  it('builds sheets route query while preserving unrelated keys', () => {
    expect(buildSheetsWorkspaceRouteQuery(
      { panel: 'meta', workbookId: 'old', view: 'WORKBOOKS', scope: 'ALL' },
      { workbookId: 'next', view: 'INCOMING_SHARES', scope: 'SHARED' }
    )).toEqual({
      panel: 'meta',
      workbookId: 'next',
      view: 'INCOMING_SHARES',
      scope: 'SHARED'
    })
    expect(buildSheetsWorkspaceRouteQuery(
      { panel: 'meta', workbookId: 'old', view: 'INCOMING_SHARES', scope: 'SHARED' },
      { workbookId: null, view: 'WORKBOOKS', scope: 'ALL' }
    )).toEqual({
      panel: 'meta'
    })
  })

  it('resolves preferred workbook and route diff state', () => {
    expect(resolvePreferredSheetsWorkbookId({ workbookId: 'wb-1' }, 'wb-2')).toBe('wb-1')
    expect(resolvePreferredSheetsWorkbookId({}, 'wb-2')).toBe('wb-2')
    expect(hasSheetsWorkspaceRouteStateChanged(
      { workbookId: 'wb-1', view: 'INCOMING_SHARES', scope: 'SHARED' },
      { workbookId: 'wb-1', view: 'INCOMING_SHARES', scope: 'SHARED' }
    )).toBe(false)
    expect(hasSheetsWorkspaceRouteStateChanged(
      { workbookId: 'wb-1' },
      { workbookId: 'wb-2', view: 'WORKBOOKS', scope: 'ALL' }
    )).toBe(true)
  })
})
