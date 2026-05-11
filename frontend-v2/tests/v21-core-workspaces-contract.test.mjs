import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const files = {
  calendarApi: new URL('../src/service/api/calendar.ts', import.meta.url),
  driveApi: new URL('../src/service/api/drive.ts', import.meta.url),
  mailApi: new URL('../src/service/api/mail.ts', import.meta.url),
  settingsApi: new URL('../src/service/api/settings.ts', import.meta.url),
  suiteSectionView: new URL('../src/views/app/SuiteSectionView.vue', import.meta.url),
  workspaceApi: new URL('../src/service/api/workspace.ts', import.meta.url)
}

test('v2.1 core workspace API boundaries use section 14 endpoints', async () => {
  const [workspaceApi, mailApi, calendarApi, driveApi, settingsApi] = await Promise.all([
    readFile(files.workspaceApi, 'utf8'),
    readFile(files.mailApi, 'utf8'),
    readFile(files.calendarApi, 'utf8'),
    readFile(files.driveApi, 'utf8'),
    readFile(files.settingsApi, 'utf8')
  ])

  for (const endpoint of ['/api/v2/workspace/summary', '/api/v2/workspace/activity', '/api/v2/workspace/tasks']) {
    assert.match(workspaceApi, new RegExp(endpoint.replaceAll('/', '\\/')))
  }
  assert.match(workspaceApi, /`\/api\/v2\/workspace\/tasks\/\$\{taskId\}`/)
  assert.match(workspaceApi, /readWorkspaceSummary/)
  assert.match(workspaceApi, /listWorkspaceActivity/)
  assert.match(workspaceApi, /listWorkspaceTasks/)
  assert.match(workspaceApi, /patchWorkspaceTask/)

  for (const endpoint of ['/api/v2/mail/folders', '/api/v2/mail/messages', '/api/v2/mail/drafts', '/api/v2/mail/send', '/api/v2/mail/messages/bulk-action', '/api/v2/mail/contacts', '/api/v2/mail/rules']) {
    assert.match(mailApi, new RegExp(endpoint.replaceAll('/', '\\/')))
  }
  assert.match(mailApi, /`\/api\/v2\/mail\/threads\/\$\{threadId\}`/)
  assert.doesNotMatch(mailApi, /\/api\/v1\/mails/)

  for (const endpoint of ['/api/v2/calendar/events', '/api/v2/calendar/availability', '/api/v2/calendar/resources', '/api/v2/calendar/bookings', '/api/v2/calendar/settings']) {
    assert.match(calendarApi, new RegExp(endpoint.replaceAll('/', '\\/')))
  }
  assert.doesNotMatch(calendarApi, /\/api\/v1\/calendar/)

  for (const endpoint of ['/api/v2/drive/folders', '/api/v2/drive/files', '/api/v2/drive/uploads', '/api/v2/drive/storage/summary']) {
    assert.match(driveApi, new RegExp(endpoint.replaceAll('/', '\\/')))
  }
  assert.match(driveApi, /`\/api\/v2\/drive\/files\/\$\{fileId\}`/)
  assert.match(driveApi, /`\/api\/v2\/drive\/files\/\$\{fileId\}\/share`/)
  assert.match(driveApi, /`\/api\/v2\/drive\/files\/\$\{fileId\}\/versions`/)
  assert.doesNotMatch(driveApi, /\/api\/v1\/drive/)

  for (const endpoint of ['/api/v2/settings/profile', '/api/v2/settings/security', '/api/v2/settings/devices', '/api/v2/settings/notifications', '/api/v2/settings/integrations', '/api/v2/settings/audit']) {
    assert.match(settingsApi, new RegExp(endpoint.replaceAll('/', '\\/')))
  }
  assert.match(settingsApi, /`\/api\/v2\/settings\/devices\/\$\{deviceId\}`/)
  assert.match(settingsApi, /`\/api\/v2\/settings\/integrations\/\$\{integrationId\}`/)
  assert.doesNotMatch(settingsApi, /\/api\/v1\/settings/)
})

test('v2.1 workspace shell consumes the shared workspace client and visible boundary cues', async () => {
  const suiteSectionView = await readFile(files.suiteSectionView, 'utf8')

  assert.match(suiteSectionView, /readWorkspaceSummary/)
  assert.match(suiteSectionView, /listWorkspaceActivity/)
  assert.match(suiteSectionView, /listWorkspaceTasks/)
  assert.match(suiteSectionView, /patchWorkspaceTask/)
  assert.match(suiteSectionView, /useAuthStore/)
  assert.match(suiteSectionView, /latestWorkspaceRequest/)
  assert.match(suiteSectionView, /PremiumBadge/)
  assert.match(suiteSectionView, /HostedBadge/)
  assert.match(suiteSectionView, /Community/)
  assert.doesNotMatch(suiteSectionView, /const cards = \[/)
})
