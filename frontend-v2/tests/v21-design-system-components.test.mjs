import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const files = {
  statusBadge: new URL('../src/design-system/components/StatusBadge.vue', import.meta.url),
  emptyState: new URL('../src/design-system/components/EmptyState.vue', import.meta.url),
  sectionHeader: new URL('../src/design-system/components/SectionHeader.vue', import.meta.url),
  maturityBadge: new URL('../src/shared/components/MaturityBadge.vue', import.meta.url),
  metricTile: new URL('../src/shared/components/MetricTile.vue', import.meta.url)
}

test('v2.1 design-system components expose stable shell APIs', async () => {
  const [statusBadge, emptyState, sectionHeader] = await Promise.all([
    readFile(files.statusBadge, 'utf8'),
    readFile(files.emptyState, 'utf8'),
    readFile(files.sectionHeader, 'utf8')
  ])

  assert.match(statusBadge, /type StatusTone = 'neutral' \| 'success' \| 'info' \| 'warning' \| 'danger' \| 'premium' \| 'hosted' \| 'preview'/)
  assert.match(statusBadge, /status-badge--premium/)
  assert.match(statusBadge, /status-badge--hosted/)
  assert.match(statusBadge, /status-badge--preview/)
  assert.match(emptyState, /variant\?: 'empty' \| 'error' \| 'permission' \| 'premium'/)
  assert.match(emptyState, /empty-state--permission/)
  assert.match(emptyState, /empty-state--premium/)
  assert.match(sectionHeader, /eyebrow\?: string/)
  assert.match(sectionHeader, /actions/)
})

test('legacy shared components use v2.1 semantic tokens', async () => {
  const [maturityBadge, metricTile] = await Promise.all([
    readFile(files.maturityBadge, 'utf8'),
    readFile(files.metricTile, 'utf8')
  ])

  assert.match(maturityBadge, /--mm-brand-primary/)
  assert.match(maturityBadge, /--mm-product-docs/)
  assert.match(maturityBadge, /--mm-preview/)
  assert.match(metricTile, /--mm-text-primary/)
  assert.match(metricTile, /--mm-text-secondary/)
  assert.match(metricTile, /--mm-surface/)
})
