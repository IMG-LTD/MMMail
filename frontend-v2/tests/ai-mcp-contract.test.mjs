import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const aiFile = new URL('../src/shared/composables/useCopilotPanel.ts', import.meta.url)
const automationFile = new URL('../src/shared/composables/useAutomationRunbook.ts', import.meta.url)
const mcpFile = new URL('../src/shared/composables/useMcpRegistry.ts', import.meta.url)

test('shared ai and mcp composables exist with the frozen names', async () => {
  const [ai, automation, mcp] = await Promise.all([
    readFile(aiFile, 'utf8'),
    readFile(automationFile, 'utf8'),
    readFile(mcpFile, 'utf8')
  ])

  assert.match(ai, /readAiPlatformCapabilities/)
  assert.match(automation, /'overview' \| 'automation' \| 'runs'/)
  assert.match(mcp, /readMcpRegistryCapabilities/)
})
