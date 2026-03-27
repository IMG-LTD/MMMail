import { describe, expect, it } from 'vitest'
import type { SuiteCollaborationEvent } from '../types/api'
import { messages } from '../locales'
import { buildCollaborationCounts, filterCollaborationEvents } from '../utils/collaboration'
import { translate } from '../utils/i18n'
import { extractWorkbookIdFromRoute, filterSheetsCollaborationEvents } from '../utils/sheets-collaboration'

const collaborationItems: SuiteCollaborationEvent[] = [
  {
    eventId: 11,
    productCode: 'DOCS',
    eventType: 'DOCS_NOTE_CREATE',
    title: 'Document created',
    summary: 'Created document',
    routePath: '/docs?noteId=88',
    actorEmail: 'docs@mmmail.local',
    sessionId: 's1',
    createdAt: '2026-03-08T18:00:00'
  },
  {
    eventId: 12,
    productCode: 'SHEETS',
    eventType: 'SHEETS_WORKBOOK_CREATE',
    title: 'Workbook created',
    summary: 'Created workbook',
    routePath: '/sheets?workbookId=42',
    actorEmail: 'sheets@mmmail.local',
    sessionId: 's2',
    createdAt: '2026-03-08T18:01:00'
  },
  {
    eventId: 13,
    productCode: 'SHEETS',
    eventType: 'SHEETS_WORKBOOK_EXPORT',
    title: 'Workbook exported',
    summary: 'Exported workbook',
    routePath: '/sheets?workbookId=77',
    actorEmail: 'sheets@mmmail.local',
    sessionId: 's3',
    createdAt: '2026-03-08T18:02:00'
  },
  {
    eventId: 14,
    productCode: 'DRIVE',
    eventType: 'DRIVE_ITEM_CREATE',
    title: 'Drive item created',
    summary: 'Created file',
    routePath: '/drive?itemId=9',
    actorEmail: 'drive@mmmail.local',
    sessionId: 's4',
    createdAt: '2026-03-08T18:03:00'
  }
]

describe('Drive / Docs / Sheets v86 regression', () => {
  it('counts and filters collaboration events including SHEETS', () => {
    expect(filterCollaborationEvents(collaborationItems, 'SHEETS')).toEqual([
      collaborationItems[1],
      collaborationItems[2]
    ])
    expect(buildCollaborationCounts(collaborationItems)).toEqual({
      ALL: 4,
      DOCS: 1,
      DRIVE: 1,
      SHEETS: 2,
      MEET: 0
    })
  })

  it('extracts workbook ids and narrows sheets events to the active workbook', () => {
    expect(extractWorkbookIdFromRoute('/sheets?workbookId=42')).toBe('42')
    expect(extractWorkbookIdFromRoute('/sheets')).toBeNull()
    expect(filterSheetsCollaborationEvents(collaborationItems, '42')).toEqual([collaborationItems[1]])
    expect(filterSheetsCollaborationEvents(collaborationItems, null)).toEqual([
      collaborationItems[1],
      collaborationItems[2]
    ])
  })

  it('resolves newly added localized copy for drive docs and sheets', () => {
    expect(translate(messages, 'zh-CN', 'drive.launcher.newDoc')).toBe('新建 Doc')
    expect(translate(messages, 'zh-CN', 'docs.share.revoke')).toBe('撤销')
    expect(translate(messages, 'zh-TW', 'docs.review.suggest')).toBe('建議')
    expect(translate(messages, 'zh-TW', 'drive.trash.title')).toBe('回收筒')
    expect(translate(messages, 'en', 'drive.shareDrawer.title')).toBe('Share Links')
    expect(translate(messages, 'en', 'drive.shareDrawer.actions.manage')).toBe('Manage')
    expect(translate(messages, 'en', 'drive.shareDrawer.protection.protected')).toBe('Protected')
    expect(translate(messages, 'zh-CN', 'drive.accessLog.status.denyPasswordRequired')).toBe('需要密码')
    expect(translate(messages, 'zh-TW', 'drive.publicShare.title')).toBe('安全共享檔案')
    expect(translate(messages, 'en', 'drive.publicShare.folder.title')).toBe('Shared folder')
    expect(translate(messages, 'zh-CN', 'drive.publicShare.errors.uploadForbidden')).toBe('当前链接不允许上传')
    expect(translate(messages, 'en', 'drive.accessLog.actions.list')).toBe('Browse folder')
    expect(translate(messages, 'en', 'drive.accessLog.actions.save')).toBe('Save for later')
    expect(translate(messages, 'zh-CN', 'drive.views.sharedWithMe')).toBe('共享给我')
    expect(translate(messages, 'zh-TW', 'drive.sharedWithMe.actions.save')).toBe('稍後儲存')
    expect(translate(messages, 'en', 'drive.publicShare.messages.savedForLater')).toBe('Saved to Shared with me')
    expect(translate(messages, 'en', 'sheets.collaboration.title')).toBe('Templates + recent workbook signals')
    expect(translate(messages, 'en', 'sheets.dataTools.title')).toBe('Sort, find, and freeze')
  })
})
