import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const files = {
  authApi: new URL('../src/service/api/auth.ts', import.meta.url),
  publicShareApi: new URL('../src/service/api/public-share.ts', import.meta.url),
  systemApi: new URL('../src/service/api/system-health.ts', import.meta.url),
  publicFrame: new URL('../src/views/public/PublicSurfaceFrame.vue', import.meta.url),
  boundaryView: new URL('../src/views/public/BoundaryView.vue', import.meta.url),
  productBlockedView: new URL('../src/views/public/ProductAccessBlockedView.vue', import.meta.url),
  systemStateCard: new URL('../src/shared/components/SystemStateCard.vue', import.meta.url)
}

test('v2.1 public API boundaries use section 14.13 endpoints', async () => {
  const [authApi, publicShareApi, systemApi] = await Promise.all([
    readFile(files.authApi, 'utf8'),
    readFile(files.publicShareApi, 'utf8'),
    readFile(files.systemApi, 'utf8')
  ])

  assert.match(authApi, /'\/api\/v2\/auth\/login'/)
  assert.match(authApi, /'\/api\/v2\/auth\/register'/)
  assert.doesNotMatch(authApi, /'\/api\/v1\/auth\/login'/)
  assert.doesNotMatch(authApi, /'\/api\/v1\/auth\/register'/)

  assert.match(publicShareApi, /`\/api\/v2\/share\/mail\/\$\{token\}`/)
  assert.match(publicShareApi, /`\/api\/v2\/share\/drive\/\$\{token\}`/)
  assert.match(publicShareApi, /`\/api\/v2\/share\/pass\/\$\{token\}`/)
  assert.doesNotMatch(publicShareApi, /\/api\/v1\/public/)

  assert.match(systemApi, /export interface PublicSystemStatus/)
  assert.match(systemApi, /readPublicSystemStatus\(\)/)
  assert.match(systemApi, /'\/api\/v2\/system\/status'/)
})

test('v2.1 public boundary pages expose visible entitlement and hosted states', async () => {
  const [publicFrame, boundaryView, productBlockedView, systemStateCard] = await Promise.all([
    readFile(files.publicFrame, 'utf8'),
    readFile(files.boundaryView, 'utf8'),
    readFile(files.productBlockedView, 'utf8'),
    readFile(files.systemStateCard, 'utf8')
  ])

  assert.match(publicFrame, /public-surface-frame/)
  assert.match(publicFrame, /MMMail/)
  assert.match(publicFrame, /<slot/)
  assert.match(publicFrame, /to="\/workspace"/)
  assert.match(publicFrame, /to="\/boundary"/)

  assert.match(boundaryView, /PublicSurfaceFrame/)
  assert.match(boundaryView, /MaturityBadge/)
  assert.match(boundaryView, /PremiumBadge/)
  assert.match(boundaryView, /HostedBadge/)
  assert.match(boundaryView, /Community/)

  assert.match(productBlockedView, /ProductAccessGate/)
  assert.match(productBlockedView, /PremiumGate/)
  assert.match(productBlockedView, /HostedBadge/)
  assert.match(productBlockedView, /requestAccess/)

  assert.match(systemStateCard, /RouterLink/)
  assert.match(systemStateCard, /to="\/workspace"/)
  assert.doesNotMatch(systemStateCard, /href="\/suite"/)
})
