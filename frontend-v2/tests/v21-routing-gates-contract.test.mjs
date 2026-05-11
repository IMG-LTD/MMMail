import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const files = {
  routeMeta: new URL('../src/app/router/v21-route-meta.ts', import.meta.url),
  routes: new URL('../src/app/router/routes.ts', import.meta.url)
}

const plannedRoutes = [
  '/',
  '/login',
  '/register',
  '/boundary',
  '/product-access-blocked',
  '/share/mail/:token',
  '/share/drive/:token',
  '/public/drive/shares/:token',
  '/share/pass/:token',
  '/onboarding/:storyKey',
  '/failure-modes',
  '/failure-modes/:storyKey',
  '/404',
  '/500',
  '/offline',
  '/maintenance',
  '/workspace',
  '/workspace/today',
  '/workspace/activity',
  '/workspace/tasks',
  '/mail',
  '/mail/inbox',
  '/mail/starred',
  '/mail/snoozed',
  '/mail/drafts',
  '/mail/scheduled',
  '/mail/outbox',
  '/mail/sent',
  '/mail/archive',
  '/mail/spam',
  '/mail/trash',
  '/mail/unread',
  '/mail/contacts',
  '/mail/search',
  '/mail/folders/:folderId',
  '/mail/labels/:labelId',
  '/mail/conversations/:threadId',
  '/mail/settings',
  '/mail/compose',
  '/calendar',
  '/calendar/day',
  '/calendar/week',
  '/calendar/month',
  '/calendar/rooms',
  '/calendar/seats',
  '/calendar/resources',
  '/calendar/settings',
  '/drive',
  '/drive/recent',
  '/drive/shared',
  '/drive/starred',
  '/drive/trash',
  '/drive/folders/:folderId',
  '/drive/files/:fileId',
  '/drive/uploads',
  '/drive/storage',
  '/drive/admin',
  '/docs',
  '/docs/templates',
  '/docs/:documentId',
  '/docs/:documentId/versions',
  '/docs/:documentId/share',
  '/sheets',
  '/sheets/import',
  '/sheets/:sheetId',
  '/sheets/:sheetId/data-cleaning',
  '/sheets/:sheetId/insights',
  '/labs',
  '/labs/:moduleKey',
  '/labs/:moduleKey/settings',
  '/pass',
  '/pass/vault',
  '/pass/vaults/:vaultId',
  '/pass/items/:itemId',
  '/pass/shared',
  '/pass/secure-links',
  '/pass/aliases',
  '/pass/mailbox',
  '/pass/policies',
  '/pass/import',
  '/pass/monitor',
  '/collaboration',
  '/collaboration/projects',
  '/collaboration/projects/:projectId',
  '/collaboration/tasks',
  '/collaboration/tasks/:taskId',
  '/collaboration/knowledge',
  '/collaboration/activity',
  '/command-center',
  '/command-center/commands',
  '/command-center/commands/:commandId',
  '/command-center/runs',
  '/command-center/runs/:runId',
  '/command-center/workflows',
  '/command-center/workflows/:workflowId',
  '/command-center/audit',
  '/notifications',
  '/notifications/inbox',
  '/notifications/rules',
  '/notifications/subscriptions',
  '/notifications/templates',
  '/notifications/compose',
  '/notifications/analytics',
  '/admin',
  '/admin/users',
  '/admin/roles',
  '/admin/organizations',
  '/admin/domains',
  '/admin/policies',
  '/admin/audit',
  '/admin/alerts',
  '/admin/integrations',
  '/admin/billing',
  '/admin/system',
  '/admin/risk',
  '/settings',
  '/settings/profile',
  '/settings/security',
  '/settings/devices',
  '/settings/notifications',
  '/settings/privacy',
  '/settings/integrations',
  '/settings/storage',
  '/settings/billing',
  '/settings/audit',
  '/settings/help'
]

const publicRoutes = [
  '/',
  '/login',
  '/register',
  '/boundary',
  '/product-access-blocked',
  '/share/mail/:token',
  '/share/drive/:token',
  '/public/drive/shares/:token',
  '/share/pass/:token',
  '/onboarding/:storyKey',
  '/failure-modes',
  '/failure-modes/:storyKey',
  '/404',
  '/500',
  '/offline',
  '/maintenance'
]

const requiredFields = ['productArea', 'layout', 'maturity', 'auth', 'permission', 'entitlement', 'hosted', 'replacementFor']

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function routeRecord(content, path) {
  const start = content.indexOf(`path: '${path}'`)
  assert.notEqual(start, -1, `missing metadata for ${path}`)
  const next = content.indexOf('\n  {', start + 1)
  return content.slice(start, next === -1 ? content.length : next)
}

test('v2.1 planned route metadata covers every route from the master spec', async () => {
  const content = await readFile(files.routeMeta, 'utf8')

  assert.match(content, /export interface V21RouteMeta/)
  assert.match(content, /export const V21_ROUTE_META/)
  assert.match(content, /export function buildRouteMeta/)

  for (const path of plannedRoutes) {
    const record = routeRecord(content, path)

    for (const field of requiredFields) {
      assert.match(record, new RegExp(`${escapeRegExp(field)}:`), `${path} missing ${field}`)
    }
  }
})

test('v2.1 route metadata marks public boundaries and protected product surfaces explicitly', async () => {
  const content = await readFile(files.routeMeta, 'utf8')

  for (const path of publicRoutes) {
    assert.match(routeRecord(content, path), /auth: 'public'/, `${path} must remain public`)
  }

  for (const path of ['/workspace', '/mail/inbox', '/drive', '/admin', '/settings']) {
    assert.match(routeRecord(content, path), /auth: 'required'/, `${path} must require auth`)
  }

  assert.match(routeRecord(content, '/drive/admin'), /hosted: 'optional'/)
  assert.match(routeRecord(content, '/admin/billing'), /entitlement: 'premium'/)
  assert.match(routeRecord(content, '/command-center/runs'), /entitlement: 'premium'/)
})

test('router consumes v2.1 route metadata helpers without silent legacy root fallback', async () => {
  const content = await readFile(files.routes, 'utf8')

  assert.match(content, /buildRouteMeta/)
  assert.match(content, /redirect: '\/workspace'/)
  assert.doesNotMatch(content, /redirect: '\/suite'/)
  assert.match(content, /buildRouteMeta\('\/workspace'/)
  assert.match(content, /buildRouteMeta\('\/mail\/inbox'/)
  assert.match(content, /buildRouteMeta\('\/drive'/)
})
