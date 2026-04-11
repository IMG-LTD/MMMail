import { describe, expect, it } from 'vitest'
import { resolve } from 'node:path'
import { collectI18nPageCoverageReport } from '../utils/i18n-coverage'

const EXPECTED_I18N_ROUTES = [
  '/',
  '/archive',
  '/calendar',
  '/contacts',
  '/docs',
  '/drafts',
  '/drive',
  '/inbox',
  '/outbox',
  '/organizations',
  '/scheduled',
  '/sent',
  '/settings',
  '/sheets',
  '/snoozed',
  '/spam',
  '/trash'
] as const

describe('i18n page coverage', () => {
  it('tracks translation coverage for key workspace pages', async () => {
    const report = await collectI18nPageCoverageReport(resolve(process.cwd(), 'pages'))

    expect(report.totalPages).toBeGreaterThan(30)
    expect(report.pagesUsingI18n).toBe(report.totalPages)
    expect(report.localizedPages).toBe(report.totalPages)
    expect(report.coveragePercent).toBe(100)
    expect(report.pagesWithoutI18n).toEqual([])
    expect(report.pagesWithoutStaticKeys).toEqual([])

    for (const route of EXPECTED_I18N_ROUTES) {
      const page = report.pageReports.find((item) => item.route === route)
      expect(page, `${route} should be present in page coverage`).toBeDefined()
      expect(page?.usesI18n, `${route} should call useI18n`).toBe(true)
      expect(page?.staticKeyCount, `${route} should reference static translation keys`).toBeGreaterThan(0)
    }

    expect(report.pageReports.find((item) => item.route === '/docs')?.keyPrefixes).toContain('docs')
    expect(report.pageReports.find((item) => item.route === '/sheets')?.keyPrefixes).toContain('sheets')
    expect(report.pageReports.find((item) => item.route === '/calendar')?.keyPrefixes).toContain('calendar')
    expect(report.pageReports.find((item) => item.route === '/inbox')?.hasTranslationBinding).toBe(true)
    expect(report.pageReports.find((item) => item.route === '/archive')?.keyPrefixes).toContain('nav')
    expect(report.pageReports.find((item) => item.route === '/')?.keyPrefixes).toContain('marketing')
    expect(report.pageReports.find((item) => item.route === '/conversations')?.keyPrefixes).toContain('mailWorkspace')
    expect(report.pageReports.find((item) => item.route === '/contacts')?.keyPrefixes).toContain('contacts')
    expect(report.pageReports.find((item) => item.route === '/mail/[id]')?.hasTranslationBinding).toBe(true)
  })
})
