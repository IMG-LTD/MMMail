import test from 'node:test'
import assert from 'node:assert/strict'
import vm from 'node:vm'
import { createRequire } from 'node:module'
import { readFile } from 'node:fs/promises'

const require = createRequire(import.meta.url)
const ts = require('typescript')

const commandCenterViewFile = new URL('../src/views/app/CommandCenterView.vue', import.meta.url)
const notificationsViewFile = new URL('../src/views/app/NotificationsView.vue', import.meta.url)
const automationRunbookFile = new URL('../src/shared/composables/useAutomationRunbook.ts', import.meta.url)

async function loadTsModule(fileUrl, { mocks = {}, globals = {} } = {}) {
  const source = await readFile(fileUrl, 'utf8')
  return transpileModule(source, fileUrl.pathname, { mocks, globals })
}

async function loadVueScriptModule(fileUrl, { mocks = {}, globals = {} } = {}) {
  const source = await readFile(fileUrl, 'utf8')
  const scriptMatch = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)

  assert.ok(scriptMatch, 'expected a <script setup lang="ts"> block')

  return transpileModule(scriptMatch[1], fileUrl.pathname, { mocks, globals })
}

function transpileModule(source, filename, { mocks = {}, globals = {} } = {}) {
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

  vm.runInNewContext(outputText, sandbox, { filename })
  return module.exports
}

function createVueRuntime() {
  const refs = []
  const watchers = []

  return {
    refs,
    watchers,
    runtime: {
      computed(getter) {
        return {
          get value() {
            return getter()
          }
        }
      },
      ref(value) {
        const state = { value }
        refs.push(state)
        return state
      },
      watch(source, callback, options = {}) {
        watchers.push({ callback, options, source })

        if (options.immediate) {
          callback(source.value)
        }
      }
    }
  }
}

function createRouterRuntime(initialQuery = {}) {
  const route = {
    path: '/command-center',
    query: { ...initialQuery }
  }
  const replaceCalls = []

  return {
    replaceCalls,
    route,
    runtime: {
      useRoute() {
        return route
      },
      useRouter() {
        return {
          replace(payload) {
            replaceCalls.push(payload)
            route.path = payload.path
            route.query = { ...payload.query }
            return Promise.resolve()
          }
        }
      }
    }
  }
}

async function flushAsyncWork() {
  await new Promise(resolve => setTimeout(resolve, 0))
}

test('automation runbook synchronizes the frozen view contract through route query', async () => {
  const vue = createVueRuntime()
  const router = createRouterRuntime({ filter: 'recent', view: 'automation' })
  const { useAutomationRunbook } = await loadTsModule(automationRunbookFile, {
    mocks: {
      vue: vue.runtime,
      'vue-router': router.runtime
    }
  })

  const runbook = useAutomationRunbook()

  assert.equal(runbook.currentView.value, 'automation')

  runbook.setView('runs')
  await flushAsyncWork()

  assert.equal(router.replaceCalls[0].path, '/command-center')
  assert.equal(router.replaceCalls[0].query.filter, 'recent')
  assert.equal(router.replaceCalls[0].query.view, 'runs')
  assert.equal(runbook.currentView.value, 'runs')

  router.route.query = { view: 'invalid' }
  assert.equal(runbook.currentView.value, 'overview')
})

test('command center view consumes the query-backed automation runbook state', async () => {
  const view = await readFile(commandCenterViewFile, 'utf8')

  assert.match(view, /useAutomationRunbook/)
  assert.match(view, /currentView === 'overview'/)
  assert.match(view, /currentView === 'automation'/)
  assert.match(view, /currentView === 'runs'/)
})

test('notifications aggregation stays scope-aware and refreshes when headers change', async () => {
  const vue = createVueRuntime()
  const requestHeaders = {
    value: {
      'X-MMMAIL-ORG-ID': 'org-a',
      'X-MMMAIL-SCOPE-ID': 'scope-a'
    }
  }
  const httpCalls = []
  const pendingResponses = []

  await loadVueScriptModule(notificationsViewFile, {
    mocks: {
      vue: vue.runtime,
      '@/shared/components/CompactPageHeader.vue': {},
      '@/locales': {
        lt: (...values) => values,
        useLocaleText: () => ({
          tr(value) {
            return Array.isArray(value) ? value[0] : value
          }
        })
      },
      '@/shared/composables/useScopeGuard': {
        useScopeGuard: () => ({ requestHeaders })
      },
      '@/service/request/http': {
        httpClient: {
          get(url, options) {
            httpCalls.push({ options, url })
            return new Promise(resolve => {
              pendingResponses.push({ resolve, url })
            })
          }
        }
      }
    }
  })

  await flushAsyncWork()

  assert.equal(vue.watchers.length, 1)
  assert.equal(vue.watchers[0].options.immediate, true)
  assert.equal(httpCalls[0].url, '/api/v2/workspace/aggregation')
  assert.equal(httpCalls[0].options.scopeHeaders['X-MMMAIL-ORG-ID'], 'org-a')
  assert.equal(httpCalls[0].options.scopeHeaders['X-MMMAIL-SCOPE-ID'], 'scope-a')

  requestHeaders.value = {
    'X-MMMAIL-ORG-ID': 'org-b',
    'X-MMMAIL-SCOPE-ID': 'scope-b'
  }

  vue.watchers[0].callback(requestHeaders.value)
  await flushAsyncWork()

  assert.equal(httpCalls[1].url, '/api/v2/workspace/aggregation')
  assert.equal(httpCalls[1].options.scopeHeaders['X-MMMAIL-ORG-ID'], 'org-b')
  assert.equal(httpCalls[1].options.scopeHeaders['X-MMMAIL-SCOPE-ID'], 'scope-b')

  const aggregationSurfaces = vue.refs[0]

  pendingResponses[1].resolve({ data: { surfaces: ['org-b-surface'] } })
  await flushAsyncWork()
  assert.equal(aggregationSurfaces.value[0], 'org-b-surface')

  pendingResponses[0].resolve({ data: { surfaces: ['org-a-stale-surface'] } })
  await flushAsyncWork()
  assert.equal(aggregationSurfaces.value[0], 'org-b-surface')
})
