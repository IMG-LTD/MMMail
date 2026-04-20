import test from 'node:test'
import assert from 'node:assert/strict'
import vm from 'node:vm'
import { createRequire } from 'node:module'
import { readFile } from 'node:fs/promises'

const require = createRequire(import.meta.url)
const ts = require('typescript')

const asyncFile = new URL('../src/shared/composables/useAsyncActionState.ts', import.meta.url)

function createVueRuntime() {
  return {
    ref(value) {
      return { value }
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

test('async action state uses the frozen three-phase model', async () => {
  const content = await readFile(asyncFile, 'utf8')

  assert.match(content, /'idle' \| 'loading' \| 'success'/)
  assert.match(content, /phase.value = 'loading'/)
  assert.match(content, /phase.value = 'success'/)
})

test('async action state returns to idle when work rejects', async () => {
  const { useAsyncActionState } = await loadTsModule(asyncFile, { vue: createVueRuntime() })
  const state = useAsyncActionState()

  await assert.rejects(
    state.run(async () => {
      throw new Error('boom')
    }),
    /boom/
  )

  assert.equal(state.phase.value, 'idle')
})
