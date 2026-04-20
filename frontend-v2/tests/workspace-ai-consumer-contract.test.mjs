import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const commandCenterFile = new URL('../src/views/app/CommandCenterView.vue', import.meta.url)
const settingsWorkspaceFile = new URL('../src/views/app/SettingsWorkspaceView.vue', import.meta.url)
const mailSurfaceFile = new URL('../src/views/app/MailSurfaceView.vue', import.meta.url)
const calendarViewFile = new URL('../src/views/app/CalendarView.vue', import.meta.url)

test('workspace views use the frozen ai and mcp composables', async () => {
  const [commandCenterContent, settingsWorkspaceContent, mailSurfaceContent, calendarViewContent] = await Promise.all([
    readFile(commandCenterFile, 'utf8'),
    readFile(settingsWorkspaceFile, 'utf8'),
    readFile(mailSurfaceFile, 'utf8'),
    readFile(calendarViewFile, 'utf8')
  ])

  assert.match(commandCenterContent, /useAutomationRunbook/)
  assert.match(settingsWorkspaceContent, /useMcpRegistry/)
  assert.match(mailSurfaceContent, /useCopilotPanel/)
  assert.match(calendarViewContent, /useCopilotPanel/)
})
