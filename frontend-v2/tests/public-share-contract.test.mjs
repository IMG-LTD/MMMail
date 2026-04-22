import test from 'node:test'
import assert from 'node:assert/strict'
import vm from 'node:vm'
import { createRequire } from 'node:module'
import { readFile } from 'node:fs/promises'

const require = createRequire(import.meta.url)
const ts = require('typescript')

const composableFile = new URL('../src/shared/composables/usePublicShareFlow.ts', import.meta.url)
const apiFile = new URL('../src/service/api/public-share.ts', import.meta.url)

async function loadTsModule(fileUrl, { mocks = {} } = {}) {
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

test('public share API keeps the frozen capability state contract', async () => {
  const content = await readFile(apiFile, 'utf8')

  assert.match(content, /export type PublicShareCapabilityState = 'token-valid' \| 'password-required' \| 'unlocked' \| 'expired' \| 'revoked' \| 'locked' \| 'download-blocked'/)
  assert.match(content, /states: PublicShareCapabilityState\[\]/)
})

test('public share flow hydrates capability payloads from backend data', async () => {
  const { usePublicShareFlow } = await loadTsModule(composableFile, {
    mocks: {
      vue: createVueRuntime(),
      '@/service/api/public-share': {
        readPublicShareCapabilities: () => Promise.resolve({
          data: {
            auditedActions: ['preview', 'download'],
            passwordHeader: 'X-Drive-Share-Password',
            states: ['token-valid', 'locked']
          }
        })
      }
    }
  })

  const flow = usePublicShareFlow()

  await flow.loadCapabilities()

  assert.deepEqual(Array.from(flow.auditedActions.value), ['preview', 'download'])
  assert.deepEqual(Array.from(flow.capabilityStates.value), ['token-valid', 'locked'])
  assert.equal(flow.passwordHeader.value, 'X-Drive-Share-Password')
  assert.equal(flow.loading.value, false)
})

test('public share flow clears capability payloads when capability reads fail', async () => {
  const { usePublicShareFlow } = await loadTsModule(composableFile, {
    mocks: {
      vue: createVueRuntime(),
      '@/service/api/public-share': {
        readPublicShareCapabilities: (() => {
          let callCount = 0
          return () => {
            callCount += 1
            if (callCount === 1) {
              return Promise.resolve({
                data: {
                  auditedActions: ['preview'],
                  passwordHeader: 'X-Drive-Share-Password',
                  states: ['token-valid']
                }
              })
            }

            return Promise.reject(new Error('boom'))
          }
        })()
      }
    }
  })

  const flow = usePublicShareFlow()

  await flow.loadCapabilities()
  assert.deepEqual(Array.from(flow.auditedActions.value), ['preview'])
  assert.deepEqual(Array.from(flow.capabilityStates.value), ['token-valid'])
  assert.equal(flow.passwordHeader.value, 'X-Drive-Share-Password')

  await flow.loadCapabilities()

  assert.deepEqual(Array.from(flow.auditedActions.value), [])
  assert.deepEqual(Array.from(flow.capabilityStates.value), [])
  assert.equal(flow.passwordHeader.value, '')
  assert.equal(flow.loading.value, false)
})
