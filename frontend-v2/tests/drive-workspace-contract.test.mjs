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

  assert.match(api, /\/api\/v2\/drive\/files/)
  assert.match(api, /\/api\/v2\/drive\/storage\/summary/)
  assert.match(api, /\/api\/v2\/drive\/files\/\$\{fileId\}\/share/)
  assert.match(api, /\/api\/v2\/drive\/uploads\/\$\{uploadId\}/)
  assert.match(api, /updateDriveFile/)
  assert.match(api, /deleteDriveFile/)
  assert.match(api, /createDriveShare/)
  assert.match(api, /listDriveFileVersions/)
  assert.doesNotMatch(api, /\/api\/v1\/drive/)
  assert.doesNotMatch(api, /readDriveFile/)
  assert.match(view, /useAuthStore/)
  assert.match(view, /listDriveItems/)
  assert.match(view, /readDriveUsage/)
  assert.match(view, /listDriveShares/)
  assert.match(view, /watch\(\s*\(\) => \[route\.fullPath, authStore\.accessToken\]/)
  assert.match(view, /latestDriveRequest/)
  assert.match(view, /latestShareRequest/)
  assert.doesNotMatch(view, /const files = \[/)
})
