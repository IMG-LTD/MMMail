import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const composableFile = new URL('../src/shared/composables/usePublicShareFlow.ts', import.meta.url)

test('public share flow keeps the frozen state set', async () => {
  const content = await readFile(composableFile, 'utf8')

  assert.match(content, /'token-valid' \| 'password-required' \| 'unlocked' \| 'expired' \| 'revoked' \| 'locked' \| 'download-blocked'/)
  assert.match(content, /readPublicShareCapabilities/)
})
