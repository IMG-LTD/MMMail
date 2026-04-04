import { describe, expect, it } from 'vitest'
import { mkdtemp, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { resolve } from 'node:path'
import { collectI18nCatalogReport } from '../utils/i18n-governance'

describe('i18n governance', () => {
  it('keeps locale catalogs aligned across supported languages', async () => {
    const report = await collectI18nCatalogReport(resolve(process.cwd(), 'locales'))

    expect(report.totalModules).toBeGreaterThan(10)
    expect(report.totalKeys.en).toBeGreaterThan(100)
    expect(report.modulesWithGaps).toEqual([])
    expect(report.modulesWithPlaceholderMismatches).toEqual([])
  })

  it('detects placeholder mismatches across locales', async () => {
    const localeDir = await mkdtemp(resolve(tmpdir(), 'mmmail-i18n-governance-'))

    await writeFile(resolve(localeDir, 'demo.ts'), [
      "export const demoEn = {",
      "  'demo.message': 'Count: {count}'",
      '}',
      '',
      "export const demoZhCN = {",
      "  'demo.message': '数量：{total}'",
      '}',
      '',
      "export const demoZhTW = {",
      "  'demo.message': '數量：{count}'",
      '}',
      '',
    ].join('\n'))

    const report = await collectI18nCatalogReport(localeDir)

    expect(report.modulesWithGaps).toEqual([])
    expect(report.modulesWithPlaceholderMismatches).toEqual(['demo'])
    expect(report.moduleReports[0]?.placeholderMismatches).toEqual([
      {
        key: 'demo.message',
        expectedPlaceholders: ['count'],
        localePlaceholders: {
          en: ['count'],
          'zh-CN': ['total'],
          'zh-TW': ['count'],
        },
      },
    ])
  })
})
