import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const settingsViewFile = new URL('../src/views/app/SettingsWorkspaceView.vue', import.meta.url)

async function readOrEmpty(file) {
  try {
    return await readFile(file, 'utf8')
  } catch {
    return ''
  }
}

test('settings workspace keeps the frozen route-driven panel contract', async () => {
  const content = await readOrEmpty(settingsViewFile)

  assert.match(content, /useRoute/)
  assert.match(content, /useRouter/)
  assert.match(content, /route\.query\.panel/)
  assert.match(content, /privacy-telemetry[\s\S]*system-health[\s\S]*integrations/)
  assert.match(content, /readSystemHealth\(authStore\.accessToken, requestHeaders\.value\)/)
  assert.match(content, /activePanelKey === 'privacy-telemetry'/)
  assert.match(content, /activePanelKey === 'system-health'/)
  assert.match(content, /activePanelKey === 'integrations'/)

  assert.match(content, /<section v-if="activePanelKey === 'privacy-telemetry'" class="settings-panel">[\s\S]*已注册设备/)
  assert.match(content, /<section v-if="activePanelKey === 'privacy-telemetry'" class="settings-actions">/)
  assert.match(content, /<div v-if="activePanelKey === 'privacy-telemetry'" class="settings-save">/)
  assert.match(content, /activePanelKey === 'integrations'[\s\S]*registryCapabilities/)
})
