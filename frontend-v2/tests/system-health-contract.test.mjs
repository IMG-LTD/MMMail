import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const apiFile = new URL('../src/service/api/system-health.ts', import.meta.url)
const typeFile = new URL('../src/shared/types/system-health.ts', import.meta.url)

async function readOrEmpty(file) {
  try {
    return await readFile(file, 'utf8')
  } catch {
    return ''
  }
}

test('system health client keeps token and scope header contracts', async () => {
  const content = await readOrEmpty(apiFile)

  assert.match(content, /export async function readSystemHealth\(token\?: string, scopeHeaders\?: Record<string, string>\)/)
  assert.match(content, /httpClient\.get<ApiResponse<SystemHealthOverview>>\('\/api\/v1\/system\/health', \{ token, scopeHeaders \}\)/)
})

test('system health overview type keeps current rendering fields', async () => {
  const content = await readOrEmpty(typeFile)

  assert.match(content, /export interface SystemHealthOverview/)
  assert.match(content, /metrics:/)
  assert.match(content, /errorTracking:/)
  assert.match(content, /jobs:/)
  assert.match(content, /prometheusPath: string/)
})
