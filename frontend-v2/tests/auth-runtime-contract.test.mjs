import test from 'node:test'
import assert from 'node:assert/strict'
import vm from 'node:vm'
import { createRequire } from 'node:module'
import { readFile } from 'node:fs/promises'

const require = createRequire(import.meta.url)
const ts = require('typescript')

const authGuardFile = new URL('../src/app/router/auth-guard.ts', import.meta.url)
const routerFile = new URL('../src/app/router/index.ts', import.meta.url)
const httpFile = new URL('../src/service/request/http.ts', import.meta.url)

async function loadTsModule(fileUrl) {
  const source = await readFile(fileUrl, 'utf8')
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022
    }
  })

  const module = { exports: {} }
  vm.runInNewContext(outputText, { exports: module.exports, module }, { filename: fileUrl.pathname })
  return module.exports
}

test('auth guard redirects protected routes and leaves public shares open', async () => {
  const { resolveAuthRedirect } = await loadTsModule(authGuardFile)

  assert.equal(
    resolveAuthRedirect({ fullPath: '/mail/inbox?label=team', meta: { auth: 'required' }, path: '/mail/inbox' }, false),
    '/login?redirect=%2Fmail%2Finbox%3Flabel%3Dteam'
  )
  assert.equal(resolveAuthRedirect({ fullPath: '/inbox', meta: { layout: 'base' }, path: '/inbox' }, false), '/login?redirect=%2Finbox')
  assert.equal(resolveAuthRedirect({ fullPath: '/share/mail/t1', meta: { layout: 'blank' }, path: '/share/mail/t1' }, false), null)
  assert.equal(resolveAuthRedirect({ fullPath: '/mail/inbox', meta: { auth: 'required' }, path: '/mail/inbox' }, true), null)
})

test('token-bound HTTP 401 responses clear session through the router bridge', async () => {
  const [routerContent, httpContent] = await Promise.all([
    readFile(routerFile, 'utf8'),
    readFile(httpFile, 'utf8')
  ])

  assert.match(httpContent, /export function registerUnauthorizedHandler/)
  assert.match(httpContent, /response\.status === 401 && options\.token/)
  assert.match(routerContent, /registerUnauthorizedHandler/)
  assert.match(routerContent, /authStore\.clearSession\(\)/)
  assert.match(routerContent, /router\.replace/)
})
