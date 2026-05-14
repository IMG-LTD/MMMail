import test from 'node:test'
import assert from 'node:assert/strict'
import { readdir, readFile } from 'node:fs/promises'

const repoRoot = new URL('../../', import.meta.url)
const backendTestDir = new URL('backend/mmmail-server/src/test/java/com/mmmail/server/', repoRoot)
const validateLocalFile = new URL('scripts/validate-local.sh', repoRoot)
const ciFile = new URL('.github/workflows/ci.yml', repoRoot)
const backendV21TestFile = /^BackendV21.*Test\.java$/

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function parseValidateLocalGroup(content) {
  const match = content.match(/BACKEND_V21_RUNTIME_TESTS="([^"]+)"/)
  assert.ok(match, 'validate-local missing BACKEND_V21_RUNTIME_TESTS')
  return match[1].split(',').map(item => item.trim()).filter(Boolean)
}

test('local and CI release gates include every committed BackendV21 regression', async () => {
  const [backendFiles, validateLocal, ci] = await Promise.all([
    readdir(backendTestDir),
    readFile(validateLocalFile, 'utf8'),
    readFile(ciFile, 'utf8')
  ])

  const backendV21Tests = backendFiles
    .filter(fileName => backendV21TestFile.test(fileName))
    .map(fileName => fileName.replace(/\.java$/, ''))
    .sort()

  assert.ok(backendV21Tests.length > 0, 'expected committed BackendV21 tests')
  assert.deepEqual(parseValidateLocalGroup(validateLocal).sort(), backendV21Tests)
  assert.match(validateLocal, /echo "\[validate-local\] backend v2\.1 runtime regression"/)
  assert.match(validateLocal, /-Dtest="\$BACKEND_V21_RUNTIME_TESTS"/)
  assert.match(validateLocal, /\/tmp\/mmmail-backend-v21-runtime\.log/)
  assert.match(ci, /Backend v2\.1 runtime regression/)

  for (const className of backendV21Tests) {
    assert.match(ci, new RegExp(`\\b${escapeRegExp(className)}\\b`), `CI missing ${className}`)
  }
})
