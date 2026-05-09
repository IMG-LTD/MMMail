import test from 'node:test'
import assert from 'node:assert/strict'
import vm from 'node:vm'
import { createRequire } from 'node:module'
import { readFile } from 'node:fs/promises'

const require = createRequire(import.meta.url)
const ts = require('typescript')

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

function createVueRuntime() {
  return {
    computed(getter) {
      return {
        get value() {
          return getter()
        }
      }
    },
    ref(value) {
      return { value }
    }
  }
}

function createPiniaRuntime() {
  return {
    defineStore(id, setup) {
      return Object.assign(() => setup(), { $id: id })
    }
  }
}

function createLocaleRuntime() {
  return {
    lt(_zhCn, _zhTw, en) {
      return en
    }
  }
}

async function loadTsModule(fileUrl, mocks = {}) {
  const source = await readFile(fileUrl, 'utf8')
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022
    }
  })

  const module = { exports: {} }
  const sandbox = {
    exports: module.exports,
    module,
    require(specifier) {
      if (specifier in mocks) {
        return mocks[specifier]
      }
      throw new Error(`Unexpected import: ${specifier}`)
    }
  }

  vm.runInNewContext(outputText, sandbox, { filename: fileUrl.pathname })
  return module.exports
}

test('v2.1 shell state model exposes behavior needed by shell components', async () => {
  const { useShellStore } = await loadTsModule(files.shellStore, {
    pinia: createPiniaRuntime(),
    vue: createVueRuntime()
  })
  const store = useShellStore()

  assert.equal(useShellStore.$id, 'shell')
  assert.equal(store.activeContextPanel.value, 'activity')
  assert.equal(store.contextPanelOpen.value, true)
  assert.equal(store.commandPaletteOpen.value, false)
  assert.equal(store.quickCreateOpen.value, false)
  assert.equal(store.notificationDrawerOpen.value, false)
  assert.equal(store.mobileMorePanelOpen.value, false)
  assert.equal(store.sideNavCollapsed.value, false)

  store.openCommandPalette()
  store.openQuickCreate()
  store.openMobileMorePanel()
  store.toggleSideNav()
  assert.equal(store.commandPaletteOpen.value, true)
  assert.equal(store.quickCreateOpen.value, true)
  assert.equal(store.mobileMorePanelOpen.value, true)
  assert.equal(store.sideNavCollapsed.value, true)
  assert.deepEqual(Object.fromEntries(Object.entries(store.shellStateClasses.value)), {
    'shell-state--command-open': true,
    'shell-state--context-open': true,
    'shell-state--mobile-more-open': true,
    'shell-state--nav-collapsed': true,
    'shell-state--notifications-open': false,
    'shell-state--quick-create-open': true
  })

  store.closeContextPanel()
  assert.equal(store.contextPanelOpen.value, false)
  store.openContextPanel('risk')
  assert.equal(store.activeContextPanel.value, 'risk')
  assert.equal(store.contextPanelOpen.value, true)

  store.toggleNotificationDrawer()
  assert.equal(store.notificationDrawerOpen.value, true)
  assert.equal(store.activeContextPanel.value, 'notifications')
  assert.equal(store.contextPanelOpen.value, true)

  store.closeCommandPalette()
  store.closeQuickCreate()
  store.closeMobileMorePanel()
  store.setSideNavCollapsed(false)
  assert.equal(store.commandPaletteOpen.value, false)
  assert.equal(store.quickCreateOpen.value, false)
  assert.equal(store.mobileMorePanelOpen.value, false)
  assert.equal(store.sideNavCollapsed.value, false)
})

test('v2.1 navigation model includes structured canonical and fallback product entries', async () => {
  const { findShellSurface, getToneColorVar, isMailRoute, mobilePrimaryTabs, shellNavGroups } = await loadTsModule(files.shellNav, {
    '@/locales': createLocaleRuntime()
  })

  const items = shellNavGroups.flatMap(group => group.items)
  const itemByKey = new Map(items.map(item => [item.key, item]))

  assert.deepEqual(Array.from(itemByKey.keys()), requiredProducts)
  assert.equal(itemByKey.get('workspace').canonicalPath, '/workspace')
  assert.equal(itemByKey.get('workspace').fallbackPath, '/suite')
  assert.deepEqual(Array.from(itemByKey.get('workspace').matchPrefixes), ['/workspace', '/suite'])
  assert.equal(itemByKey.get('mail').canonicalPath, '/mail')
  assert.equal(itemByKey.get('mail').fallbackPath, '/inbox')
  assert.deepEqual(Array.from(itemByKey.get('pass').matchPrefixes), ['/pass', '/pass-monitor'])
  assert.deepEqual(Array.from(itemByKey.get('admin').matchPrefixes), ['/admin', '/organizations', '/business', '/security'])

  assert.equal(findShellSurface('/suite').key, 'workspace')
  assert.equal(findShellSurface('/workspace').key, 'workspace')
  assert.equal(findShellSurface('/inbox').key, 'mail')
  assert.equal(findShellSurface('/mail/inbox').key, 'mail')
  assert.equal(findShellSurface('/admin/users').key, 'admin')
  assert.equal(findShellSurface('/settings/profile').key, 'settings')
  assert.equal(isMailRoute('/mail/inbox'), true)
  assert.equal(isMailRoute('/inbox'), true)
  assert.equal(isMailRoute('/drive'), false)
  assert.equal(getToneColorVar('notifications'), 'var(--mm-product-notifications)')

  assert.equal(mobilePrimaryTabs[0].key, 'workspace')
  assert.deepEqual(Array.from(mobilePrimaryTabs[0].matchPrefixes), ['/workspace', '/suite'])
  assert.equal(mobilePrimaryTabs[4].key, 'more')
  assert.ok(mobilePrimaryTabs[4].matchPrefixes.includes('/pass'))
  assert.ok(mobilePrimaryTabs[4].matchPrefixes.includes('/admin'))
  assert.equal(mobilePrimaryTabs[4].matchPrefixes.includes('/suite'), false)
})
