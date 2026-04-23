import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const routesFile = new URL('../src/app/router/routes.ts', import.meta.url)
const registryFile = new URL('../src/app/router/redirect-registry.ts', import.meta.url)

const requiredPathSnippets = [
  "path: '/login'",
  "path: '/register'",
  "path: '/boundary'",
  "path: '/product-access-blocked'",
  "path: '/share/mail/:token'",
  "path: '/share/drive/:token'",
  "path: '/onboarding/:storyKey'",
  "path: '/failure-modes/:storyKey'",
  "path: '/inbox'",
  "path: '/compose'",
  "path: '/conversations/:id'",
  "path: '/calendar'",
  "path: '/drive'",
  "path: '/drive/shared'",
  "path: '/pass'",
  "path: '/docs/:id'",
  "path: '/sheets/:id'",
  "path: '/suite/plans'",
  "path: '/organizations/members'",
  "path: '/security'",
  "path: '/settings'",
  "path: '/labs/:moduleKey'"
]

test('critical handoff routes remain declared', async () => {
  const [routesContent, registryContent] = await Promise.all([
    readFile(routesFile, 'utf8'),
    readFile(registryFile, 'utf8')
  ])

  for (const snippet of requiredPathSnippets) {
    assert.match(routesContent, new RegExp(snippet.replaceAll('/', '\\/')))
  }

  assert.match(registryContent, /from: '\/pass-monitor', to: '\/pass\/monitor'/)
  assert.doesNotMatch(routesContent, /redirect:\s*'\/share\/mail\/demo-token'/)
  assert.doesNotMatch(routesContent, /redirect:\s*'\/share\/drive\/demo-token'/)
})
