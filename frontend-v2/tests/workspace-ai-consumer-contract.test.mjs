import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const files = [
  new URL('../src/views/app/CommandCenterView.vue', import.meta.url),
  new URL('../src/views/app/SettingsWorkspaceView.vue', import.meta.url),
  new URL('../src/views/app/MailSurfaceView.vue', import.meta.url),
  new URL('../src/views/app/CalendarView.vue', import.meta.url)
]

test('workspace views consume shared ai and mcp composables', async () => {
  const contents = await Promise.all(files.map(file => readFile(file, 'utf8')))

  assert.match(contents[0], /useAutomationRunbook/)
  assert.match(contents[1], /useMcpRegistry/)
  assert.match(contents[2], /useCopilotPanel/)
  assert.match(contents[3], /useCopilotPanel/)
})
