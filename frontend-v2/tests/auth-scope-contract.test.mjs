import test from 'node:test'
import assert from 'node:assert/strict'
import vm from 'node:vm'
import { createRequire } from 'node:module'
import { readFile } from 'node:fs/promises'

const require = createRequire(import.meta.url)
const ts = require('typescript')

const scopeGuardFile = new URL('../src/shared/composables/useScopeGuard.ts', import.meta.url)
const orgAccessFile = new URL('../src/store/modules/org-access.ts', import.meta.url)
const httpFile = new URL('../src/service/request/http.ts', import.meta.url)

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
    defineStore(_id, setup) {
      return () => setup()
    }
  }
}

function createLocalStorage(initial = {}) {
  const store = new Map(Object.entries(initial))

  return {
    getItem(key) {
      return store.has(key) ? store.get(key) : null
    },
    removeItem(key) {
      store.delete(key)
    },
    setItem(key, value) {
      store.set(key, String(value))
    }
  }
}

async function loadTsModule(fileUrl, { mocks = {}, globals = {} } = {}) {
  const source = await readFile(fileUrl, 'utf8')
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022
    }
  })

  const module = { exports: {} }
  const sandbox = {
    ...globals,
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

test('scope guard and HTTP client share org/scope headers', async () => {
  const [scopeGuard, orgAccess, http] = await Promise.all([
    readFile(scopeGuardFile, 'utf8'),
    readFile(orgAccessFile, 'utf8'),
    readFile(httpFile, 'utf8')
  ])

  assert.match(scopeGuard, /X-MMMAIL-ORG-ID/)
  assert.match(scopeGuard, /X-MMMAIL-SCOPE-ID/)
  assert.match(orgAccess, /activeScopeId/)
  assert.match(http, /scopeHeaders/)
})

test('org access store clears persisted scope when hydrated org changes', async () => {
  const localStorage = createLocalStorage({
    'mmmail.active-org-scope.v1': 'org-a',
    'mmmail.active-scope.v2': 'scope-a'
  })

  const { useOrgAccessStore } = await loadTsModule(orgAccessFile, {
    globals: { window: { localStorage } },
    mocks: {
      pinia: createPiniaRuntime(),
      vue: createVueRuntime()
    }
  })

  const store = useOrgAccessStore()

  store.applyScopes([
    {
      enabledProductCount: 1,
      orgId: 'org-b',
      orgName: 'Org B',
      orgSlug: 'org-b',
      products: [{ accessState: 'ENABLED', productKey: 'MAIL' }],
      role: 'OWNER'
    }
  ])

  assert.equal(store.activeOrgId.value, 'org-b')
  assert.equal(store.activeScopeId.value, '')
  assert.equal(localStorage.getItem('mmmail.active-scope.v2'), null)
})
