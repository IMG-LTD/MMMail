import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const apiFile = new URL('../src/service/api/drive.ts', import.meta.url)
const viewFile = new URL('../src/views/app/DriveSectionView.vue', import.meta.url)

test('drive workspace reads items, shares, and usage from APIs', async () => {
  const [api, view] = await Promise.all([
    readFile(apiFile, 'utf8'),
    readFile(viewFile, 'utf8')
  ])

  assert.match(api, /\/api\/v1\/drive\/items/)
  assert.match(api, /\/api\/v1\/drive\/usage/)
  assert.match(api, /\/api\/v1\/drive\/items\/\$\{itemId\}\/shares/)
  assert.match(view, /useAuthStore/)
  assert.match(view, /listDriveItems/)
  assert.match(view, /readDriveUsage/)
  assert.match(view, /listDriveShares/)
  assert.match(view, /watch\(\s*\(\) => \[route\.fullPath, authStore\.accessToken\]/)
  assert.match(view, /latestDriveRequest/)
  assert.match(view, /latestShareRequest/)
  assert.doesNotMatch(view, /const files = \[/)
})
