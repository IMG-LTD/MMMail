import { mkdir, writeFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { collectI18nLiteralScanReport, formatI18nLiteralScanMarkdown } from '../utils/i18n-literal-scan.ts'

const SCRIPT_DIR = dirname(fileURLToPath(import.meta.url))
const FRONTEND_DIR = resolve(SCRIPT_DIR, '..')
const REPO_DIR = resolve(FRONTEND_DIR, '..')
const ARTIFACT_DIR = resolve(REPO_DIR, 'artifacts')
const JSON_REPORT = resolve(ARTIFACT_DIR, 'i18n-literal-scan-report.json')
const MARKDOWN_REPORT = resolve(ARTIFACT_DIR, 'i18n-literal-scan-report.md')
const TARGETS = [
  resolve(FRONTEND_DIR, 'components/suite'),
  resolve(FRONTEND_DIR, 'composables/useSuiteOperationsWorkspace.ts'),
]

async function main() {
  const report = await collectI18nLiteralScanReport(FRONTEND_DIR, TARGETS)
  await mkdir(ARTIFACT_DIR, { recursive: true })
  await writeFile(JSON_REPORT, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
  await writeFile(MARKDOWN_REPORT, formatI18nLiteralScanMarkdown(report), 'utf8')

  console.log(`[i18n-literals] files=${report.totalFiles} violations=${report.totalViolations}`)
  console.log(`[i18n-literals] json=${JSON_REPORT}`)
  console.log(`[i18n-literals] md=${MARKDOWN_REPORT}`)

  if (report.totalViolations > 0) {
    console.error(`[i18n-literals] literal violations detected: ${report.filesWithViolations.join(', ')}`)
    process.exitCode = 1
  }
}

await main()
