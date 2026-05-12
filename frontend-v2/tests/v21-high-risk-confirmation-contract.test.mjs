import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const settingsViewUrl = new URL('../src/views/app/SettingsWorkspaceView.vue', import.meta.url)

test('v2.1 settings high-risk delete action requires shared modal confirmation', async () => {
  const source = await readFile(settingsViewUrl, 'utf8')

  assert.match(source, /Modal/)
  assert.match(source, /deleteAccountConfirmationOpen/)
  assert.match(source, /openDeleteAccountConfirmation/)
  assert.match(source, /closeDeleteAccountConfirmation/)
  assert.match(source, /settings-delete-confirmation/)
  assert.match(source, /tone="danger"/)
  assert.match(source, /@click="openDeleteAccountConfirmation/)
})
