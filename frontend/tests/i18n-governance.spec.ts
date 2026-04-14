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

  it('detects missing keys in core locale files exported via const bindings', async () => {
    const localeDir = await mkdtemp(resolve(tmpdir(), 'mmmail-i18n-governance-core-'))

    await Promise.all([
      writeFile(resolve(localeDir, 'en.ts'), [
        'const en = {',
        "  'core.alpha': 'Alpha',",
        "  'core.beta': 'Beta',",
        '}',
        '',
        'export default en',
        '',
      ].join('\n')),
      writeFile(resolve(localeDir, 'zh-CN.ts'), [
        'const zhCN = {',
        "  'core.alpha': '甲',",
        "  'core.beta': '乙',",
        '}',
        '',
        'export default zhCN',
        '',
      ].join('\n')),
      writeFile(resolve(localeDir, 'zh-TW.ts'), [
        'const zhTW = {',
        "  'core.alpha': '甲',",
        '}',
        '',
        'export default zhTW',
        '',
      ].join('\n')),
    ])

    const report = await collectI18nCatalogReport(localeDir)

    expect(report.totalModules).toBe(1)
    expect(report.modulesWithGaps).toEqual(['core'])
    expect(report.moduleReports[0]).toMatchObject({
      moduleId: 'core',
      keyCounts: {
        en: 2,
        'zh-CN': 2,
        'zh-TW': 1,
      },
      missingKeys: {
        en: [],
        'zh-CN': [],
        'zh-TW': ['core.beta'],
      },
    })
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
