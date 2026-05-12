import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const packageJsonUrl = new URL('../package.json', import.meta.url)
const qaScriptUrl = new URL('../scripts/v21-browser-visual-qa.mjs', import.meta.url)

test('v2.1 browser visual QA runner is exposed and covers required surfaces', async () => {
  const [packageJsonRaw, qaScript] = await Promise.all([
    readFile(packageJsonUrl, 'utf8'),
    readFile(qaScriptUrl, 'utf8')
  ])
  const packageJson = JSON.parse(packageJsonRaw)

  assert.equal(packageJson.scripts['visual:qa'], 'node scripts/v21-browser-visual-qa.mjs')
  assert.match(qaScript, /google-chrome/)
  assert.match(qaScript, /Chrome DevTools Protocol/)
  assert.match(qaScript, /Page\.captureScreenshot/)
  assert.match(qaScript, /v21-browser-visual-qa-report\.md/)
  assert.match(qaScript, /desktop/)
  assert.match(qaScript, /tablet/)
  assert.match(qaScript, /mobile/)
  assert.match(qaScript, /1440/)
  assert.match(qaScript, /1024/)
  assert.match(qaScript, /390/)
  assert.match(qaScript, /\/workspace/)
  assert.match(qaScript, /\/command-center/)
  assert.match(qaScript, /\/notifications/)
  assert.match(qaScript, /\/sheets/)
  assert.match(qaScript, /command-palette/)
  assert.match(qaScript, /quick-create/)
  assert.match(qaScript, /theme-drawer/)
})
