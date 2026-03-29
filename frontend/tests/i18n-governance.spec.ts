import { describe, expect, it } from 'vitest'
import { resolve } from 'node:path'
import { collectI18nCatalogReport } from '../utils/i18n-governance'

describe('i18n governance', () => {
  it('keeps locale catalogs aligned across supported languages', async () => {
    const report = await collectI18nCatalogReport(resolve(process.cwd(), 'locales'))

    expect(report.totalModules).toBeGreaterThan(10)
    expect(report.totalKeys.en).toBeGreaterThan(100)
    expect(report.modulesWithGaps).toEqual([])
  })
})
