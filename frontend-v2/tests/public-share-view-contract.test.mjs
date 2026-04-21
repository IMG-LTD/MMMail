import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const views = [
  new URL('../src/views/public/PublicMailShareView.vue', import.meta.url),
  new URL('../src/views/public/PublicDriveShareView.vue', import.meta.url),
  new URL('../src/views/public/PublicPassShareView.vue', import.meta.url)
]

test('all public share views use the shared flow composable', async () => {
  const contents = await Promise.all(views.map(file => readFile(file, 'utf8')))

  for (const content of contents) {
    assert.match(content, /usePublicShareFlow/)
    assert.match(content, /shareFlow\.unlock\(\)/)
  }
})
