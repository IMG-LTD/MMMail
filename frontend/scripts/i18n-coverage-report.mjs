import { mkdir, writeFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { collectI18nPageCoverageReport, formatI18nPageCoverageMarkdown } from '../utils/i18n-coverage.ts'

const SCRIPT_DIR = dirname(fileURLToPath(import.meta.url))
const FRONTEND_DIR = resolve(SCRIPT_DIR, '..')
const REPO_DIR = resolve(FRONTEND_DIR, '..')
const PAGES_DIR = resolve(FRONTEND_DIR, 'pages')
const ARTIFACT_DIR = resolve(REPO_DIR, 'artifacts')
const JSON_REPORT = resolve(ARTIFACT_DIR, 'i18n-page-coverage-report.json')
const MARKDOWN_REPORT = resolve(ARTIFACT_DIR, 'i18n-page-coverage-report.md')

async function main() {
  const report = await collectI18nPageCoverageReport(PAGES_DIR)
  await mkdir(ARTIFACT_DIR, { recursive: true })
  await writeFile(JSON_REPORT, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
  await writeFile(MARKDOWN_REPORT, formatI18nPageCoverageMarkdown(report), 'utf8')

  console.log(`[i18n-coverage] pages=${report.totalPages} useI18n=${report.pagesUsingI18n} staticKeys=${report.pagesWithStaticKeys}`)
  console.log(`[i18n-coverage] json=${JSON_REPORT}`)
  console.log(`[i18n-coverage] md=${MARKDOWN_REPORT}`)
}

await main()
