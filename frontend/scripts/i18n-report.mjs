import { mkdir, writeFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { collectI18nCatalogReport, formatI18nCatalogMarkdown } from '../utils/i18n-governance.ts'

const SCRIPT_DIR = dirname(fileURLToPath(import.meta.url))
const FRONTEND_DIR = resolve(SCRIPT_DIR, '..')
const REPO_DIR = resolve(FRONTEND_DIR, '..')
const LOCALE_DIR = resolve(FRONTEND_DIR, 'locales')
const ARTIFACT_DIR = resolve(REPO_DIR, 'artifacts')
const JSON_REPORT = resolve(ARTIFACT_DIR, 'i18n-consistency-report.json')
const MARKDOWN_REPORT = resolve(ARTIFACT_DIR, 'i18n-consistency-report.md')

async function main() {
  const report = await collectI18nCatalogReport(LOCALE_DIR)
  await mkdir(ARTIFACT_DIR, { recursive: true })
  await writeFile(JSON_REPORT, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
  await writeFile(MARKDOWN_REPORT, formatI18nCatalogMarkdown(report), 'utf8')

  console.log(`[i18n-report] modules=${report.totalModules} gaps=${report.modulesWithGaps.length}`)
  console.log(`[i18n-report] json=${JSON_REPORT}`)
  console.log(`[i18n-report] md=${MARKDOWN_REPORT}`)

  if (report.modulesWithGaps.length > 0) {
    console.error(`[i18n-report] missing locale keys detected: ${report.modulesWithGaps.join(', ')}`)
    process.exitCode = 1
  }
}

await main()
