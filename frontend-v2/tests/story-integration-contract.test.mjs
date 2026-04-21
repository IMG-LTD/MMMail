import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const storyFile = new URL('../src/views/public/StorySurfaceView.vue', import.meta.url)

test('story surface remains limited to onboarding and failure groups', async () => {
  const content = await readFile(storyFile, 'utf8')

  assert.match(content, /'onboarding' \| 'failure'/)
})
