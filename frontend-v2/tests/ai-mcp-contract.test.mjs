import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const aiPlatformFile = new URL('../src/service/api/ai-platform.ts', import.meta.url)
const automationRunbookFile = new URL('../src/shared/composables/useAutomationRunbook.ts', import.meta.url)
const mcpRegistryFile = new URL('../src/service/api/mcp-registry.ts', import.meta.url)

test('ai and mcp shared contracts stay frozen', async () => {
  const [aiPlatformContent, automationRunbookContent, mcpRegistryContent] = await Promise.all([
    readFile(aiPlatformFile, 'utf8'),
    readFile(automationRunbookFile, 'utf8'),
    readFile(mcpRegistryFile, 'utf8')
  ])

  assert.match(aiPlatformContent, /readAiPlatformCapabilities/)
  assert.match(automationRunbookContent, /'overview' \| 'automation' \| 'runs'/)
  assert.match(mcpRegistryContent, /readMcpRegistryCapabilities/)
})
