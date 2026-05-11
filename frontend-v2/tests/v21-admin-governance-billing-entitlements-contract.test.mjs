import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const files = {
  adminApi: new URL('../src/service/api/admin.ts', import.meta.url),
  adminView: new URL('../src/views/app/AdminSectionView.vue', import.meta.url),
  billingApi: new URL('../src/service/api/billing.ts', import.meta.url),
  entitlementsApi: new URL('../src/service/api/entitlements.ts', import.meta.url),
  routes: new URL('../src/app/router/routes.ts', import.meta.url)
}

const adminRoutes = [
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
  '/admin/risk'
]

async function readRequiredFile(fileUrl, label) {
  try {
    return await readFile(fileUrl, 'utf8')
  } catch (error) {
    assert.fail(`expected ${label} to exist: ${error.message}`)
  }
}

function assertContainsAll(source, patterns) {
  for (const pattern of patterns) {
    assert.match(source, pattern)
  }
}

test('v2.1 admin API boundaries use section 14.11 endpoints', async () => {
  const adminApi = await readRequiredFile(files.adminApi, 'Admin API client')

  assertContainsAll(adminApi, [
    /\/api\/v2\/admin\/summary/,
    /\/api\/v2\/admin\/users/,
    /`\/api\/v2\/admin\/users\/\$\{userId\}`/,
    /\/api\/v2\/admin\/roles/,
    /\/api\/v2\/admin\/domains/,
    /\/api\/v2\/admin\/policies/,
    /`\/api\/v2\/admin\/policies\/\$\{policyId\}`/,
    /\/api\/v2\/admin\/audit/,
    /\/api\/v2\/admin\/alerts/,
    /\/api\/v2\/admin\/system/,
    /\/api\/v2\/admin\/risk/,
    /readAdminSummary/,
    /listAdminUsers/,
    /createAdminUser/,
    /patchAdminUser/,
    /listAdminRoles/,
    /listAdminDomains/,
    /listAdminPolicies/,
    /patchAdminPolicy/,
    /listAdminAudit/,
    /listAdminAlerts/,
    /readAdminSystem/,
    /readAdminRisk/
  ])
  assert.doesNotMatch(adminApi, /\/api\/v1\/admin/)
})

test('v2.1 billing and entitlement boundaries use v2 namespaces', async () => {
  const [billingApi, entitlementsApi] = await Promise.all([
    readRequiredFile(files.billingApi, 'Billing API client'),
    readRequiredFile(files.entitlementsApi, 'Entitlements API client')
  ])

  assertContainsAll(billingApi, [
    /\/api\/v2\/billing\/summary/,
    /\/api\/v2\/billing\/plans/,
    /\/api\/v2\/billing\/invoices/,
    /\/api\/v2\/billing\/usage/,
    /readBillingSummary/,
    /listBillingPlans/,
    /listBillingInvoices/,
    /readBillingUsage/
  ])
  assert.doesNotMatch(billingApi, /\/api\/v1\/billing/)

  assertContainsAll(entitlementsApi, [
    /\/api\/v2\/entitlements/,
    /\/api\/v2\/entitlements\/matrix/,
    /listEntitlements/,
    /readEntitlementMatrix/
  ])
  assert.doesNotMatch(entitlementsApi, /\/api\/v1\/entitlements/)
})

test('v2.1 admin routes and runtime surface are wired', async () => {
  const [routes, adminView] = await Promise.all([
    readRequiredFile(files.routes, 'router routes'),
    readRequiredFile(files.adminView, 'Admin section view')
  ])

  assert.match(routes, /AdminSectionView/)
  for (const routePath of adminRoutes) {
    assert.match(routes, new RegExp(`path: '${routePath.replaceAll('/', '\\/')}'`))
    assert.match(routes, new RegExp(`buildRouteMeta\\('${routePath.replaceAll('/', '\\/')}'`))
  }

  assertContainsAll(adminView, [
    /useAuthStore/,
    /useScopeGuard/,
    /readAdminSummary/,
    /listAdminUsers/,
    /listAdminRoles/,
    /listAdminDomains/,
    /listAdminPolicies/,
    /listAdminAudit/,
    /listAdminAlerts/,
    /readAdminSystem/,
    /readAdminRisk/,
    /readBillingSummary/,
    /listEntitlements/,
    /latestAdminRequest/,
    /watch\(/,
    /adminKpiCards/,
    /serviceStatusCards/,
    /alertRows/,
    /entitlementRows/
  ])
  assert.doesNotMatch(adminView, /const rows = \[/)
  assert.doesNotMatch(adminView, /const metrics = \[/)
  assert.doesNotMatch(adminView, /\/api\/v1\//)
})
