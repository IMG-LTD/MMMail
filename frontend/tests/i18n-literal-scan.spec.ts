import { mkdtemp, mkdir, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import { collectI18nLiteralScanReport } from '../utils/i18n-literal-scan'

describe('i18n literal scan', () => {
  it('keeps suite operator surfaces free from bare literals', async () => {
    const rootDir = process.cwd()
    const report = await collectI18nLiteralScanReport(rootDir, [
      resolve(rootDir, 'components/suite'),
      resolve(rootDir, 'composables/useSuiteOperationsWorkspace.ts'),
    ])

    expect(report.totalFiles).toBeGreaterThan(3)
    expect(report.totalViolations).toBe(0)
    expect(report.filesWithViolations).toEqual([])
  })

  it('detects template literals and ElMessage literals', async () => {
    const rootDir = await mkdtemp(resolve(tmpdir(), 'mmmail-i18n-literals-'))
    const componentsDir = resolve(rootDir, 'components/suite')
    const composablesDir = resolve(rootDir, 'composables')
    await mkdir(componentsDir, { recursive: true })
    await mkdir(composablesDir, { recursive: true })

    await writeFile(resolve(componentsDir, 'DemoPanel.vue'), [
      '<template>',
      '  <section>',
      '    <h2>Readiness Command Center</h2>',
      '    <el-input placeholder="Search commands..." />',
      '  </section>',
      '</template>',
    ].join('\n'))
    await writeFile(resolve(composablesDir, 'useDemo.ts'), [
      'import { ElMessage } from "element-plus"',
      'export function runDemo() {',
      '  ElMessage.warning("Search keyword is required")',
      '}',
    ].join('\n'))

    const report = await collectI18nLiteralScanReport(rootDir, [
      resolve(rootDir, 'components/suite'),
      resolve(rootDir, 'composables/useDemo.ts'),
    ])

    expect(report.totalViolations).toBe(3)
    expect(report.filesWithViolations).toEqual([
      'components/suite/DemoPanel.vue',
      'composables/useDemo.ts',
    ])
  })
})
