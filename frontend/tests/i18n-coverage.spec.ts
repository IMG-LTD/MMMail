import { describe, expect, it } from 'vitest'
import { resolve } from 'node:path'
import { collectI18nPageCoverageReport } from '../utils/i18n-coverage'

const EXPECTED_I18N_ROUTES = [
  '/calendar',
  '/docs',
  '/drive',
  '/organizations',
  '/settings',
  '/sheets'
] as const

describe('i18n page coverage', () => {
  it('tracks translation coverage for key workspace pages', async () => {
    const report = await collectI18nPageCoverageReport(resolve(process.cwd(), 'pages'))

    expect(report.totalPages).toBeGreaterThan(30)
    expect(report.pagesUsingI18n).toBeGreaterThan(20)
    expect(report.coveragePercent).toBeGreaterThan(50)

    for (const route of EXPECTED_I18N_ROUTES) {
      const page = report.pageReports.find((item) => item.route === route)
      expect(page, `${route} should be present in page coverage`).toBeDefined()
      expect(page?.usesI18n, `${route} should call useI18n`).toBe(true)
      expect(page?.staticKeyCount, `${route} should reference static translation keys`).toBeGreaterThan(0)
    }

    expect(report.pageReports.find((item) => item.route === '/docs')?.keyPrefixes).toContain('docs')
    expect(report.pageReports.find((item) => item.route === '/sheets')?.keyPrefixes).toContain('sheets')
    expect(report.pageReports.find((item) => item.route === '/calendar')?.keyPrefixes).toContain('calendar')
  })
})
