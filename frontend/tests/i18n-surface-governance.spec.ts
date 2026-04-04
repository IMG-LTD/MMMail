import { describe, expect, it } from 'vitest'
import { resolve } from 'node:path'
import { collectI18nSurfaceLiteralReport } from '../utils/i18n-surface-governance'

describe('i18n surface literal governance', () => {
  it('keeps targeted operator surfaces free from raw user-facing literals', async () => {
    const report = await collectI18nSurfaceLiteralReport(resolve(process.cwd()))

    expect(report.totalFiles).toBe(7)
    expect(report.filesWithFindings).toEqual([])
    expect(report.totalFindings).toBe(0)
    expect(report.fileReports.every((item) => item.usesI18n)).toBe(true)
  })
})
