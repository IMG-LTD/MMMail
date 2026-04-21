import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const registryFile = new URL('../src/app/router/redirect-registry.ts', import.meta.url)

test('redirect registry freezes named compatibility redirects', async () => {
  const content = await readFile(registryFile, 'utf8')

  assert.match(content, /from: '\/pass-monitor', to: '\/pass\/monitor'/)
  assert.match(content, /from: '\/conversations', to: '\/inbox'/)
  assert.match(content, /from: '\/labels', to: '\/inbox'/)
  assert.match(content, /from: '\/settings\/system-health', to: '\/settings\?panel=system-health'/)
})
