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

    const expectedNonLocalizedRedirectRoutes = [
      '/authenticator',
      '/conversations',
      '/folders/[folderId]',
      '/labels',
      '/lumo',
      '/mail/[id]',
      '/meet',
      '/pass-monitor',
      '/public/drive/shares/[token]',
      '/settings/system-health',
      '/simplelogin',
      '/standard-notes',
      '/vpn',
      '/wallet'
    ] as const

    const m6LegacyRedirectRoutes = [
      '/conversations',
      '/mail/[id]',
      '/pass-monitor',
      '/public/drive/shares/[token]',
      '/settings/system-health'
    ] as const

    expect(report.totalPages).toBe(51)
    expect(report.pagesUsingI18n).toBe(37)
    expect(report.localizedPages).toBe(37)
    expect(report.coveragePercent).toBe(72.5)
    expect(report.pagesWithoutI18n).toEqual(expectedNonLocalizedRedirectRoutes)
    expect(report.pagesWithoutStaticKeys).toEqual([])

    for (const route of EXPECTED_I18N_ROUTES) {
      const page = report.pageReports.find((item) => item.route === route)
      expect(page, `${route} should be present in page coverage`).toBeDefined()
      expect(page?.usesI18n, `${route} should call useI18n`).toBe(true)
      expect(page?.staticKeyCount, `${route} should reference static translation keys`).toBeGreaterThan(0)
    }

    for (const route of m6LegacyRedirectRoutes) {
      const page = report.pageReports.find((item) => item.route === route)
      expect(page, `${route} should be present in page coverage`).toBeDefined()
      expect(page?.usesI18n, `${route} should stay as a non-localized redirect stub`).toBe(false)
      expect(page?.hasTranslationBinding, `${route} should not require translation bindings`).toBe(false)
      expect(page?.staticKeyCount, `${route} should not carry static translation keys`).toBe(0)
    }

    expect(report.pageReports.find((item) => item.route === '/docs')?.keyPrefixes).toContain('docs')
    expect(report.pageReports.find((item) => item.route === '/sheets')?.keyPrefixes).toContain('sheets')
    expect(report.pageReports.find((item) => item.route === '/calendar')?.keyPrefixes).toContain('calendar')
    expect(report.pageReports.find((item) => item.route === '/inbox')?.hasTranslationBinding).toBe(true)
    expect(report.pageReports.find((item) => item.route === '/archive')?.keyPrefixes).toContain('nav')
    expect(report.pageReports.find((item) => item.route === '/')?.keyPrefixes).toContain('marketing')
    expect(report.pageReports.find((item) => item.route === '/contacts')?.keyPrefixes).toContain('contacts')
  })
})
