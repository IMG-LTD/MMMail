import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const files = {
  shellStore: new URL('../src/store/modules/shell.ts', import.meta.url),
  shellNav: new URL('../src/layouts/modules/shell-nav.ts', import.meta.url)
}

const requiredProducts = [
  'workspace',
  'mail',
  'calendar',
  'drive',
  'docs',
  'sheets',
  'pass',
  'collaboration',
  'command-center',
  'notifications',
  'admin',
  'settings',
  'labs'
]

test('v2.1 shell state model is declared', async () => {
  const shellStore = await readFile(files.shellStore, 'utf8')

  assert.match(shellStore, /defineStore\('shell'/)
  assert.match(shellStore, /contextPanelOpen/)
  assert.match(shellStore, /activeContextPanel/)
  assert.match(shellStore, /quickCreateOpen/)
  assert.match(shellStore, /commandPaletteOpen/)
  assert.match(shellStore, /notificationDrawerOpen/)
  assert.match(shellStore, /sideNavCollapsed/)
  assert.match(shellStore, /mobileMorePanelOpen/)
  assert.match(shellStore, /shellStateClasses/)
  assert.match(shellStore, /openContextPanel/)
  assert.match(shellStore, /toggleNotificationDrawer/)
})

test('v2.1 navigation model includes canonical and fallback product entries', async () => {
  const shellNav = await readFile(files.shellNav, 'utf8')

  for (const product of requiredProducts) {
    assert.match(shellNav, new RegExp(`key: '${product}'`))
  }

  assert.match(shellNav, /canonicalPath: '\/workspace'/)
  assert.match(shellNav, /fallbackPath: '\/suite'/)
  assert.match(shellNav, /canonicalPath: '\/mail'/)
  assert.match(shellNav, /fallbackPath: '\/inbox'/)
  assert.match(shellNav, /matchPrefixes: \['\/mail', \.\.\.mailRoutePrefixes\]/)
  assert.match(shellNav, /matchPrefixes: \['\/pass', '\/pass-monitor'\]/)
  assert.match(shellNav, /key: 'more'/)
  assert.match(shellNav, /'\/admin'/)
  assert.match(shellNav, /getToneColorVar\(tone: NavTone\)/)
  assert.match(shellNav, /var\(--mm-product-notifications\)/)
})
