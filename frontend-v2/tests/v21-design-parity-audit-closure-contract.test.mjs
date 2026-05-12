import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const files = {
  report: new URL('../../docs/superpowers/progress/v21-browser-visual-qa-report.md', import.meta.url),
  riskRegister: new URL('../../docs/superpowers/progress/v21-visual-parity-risk-register.md', import.meta.url),
  progress: new URL('../../docs/superpowers/progress/v21-implementation-progress.md', import.meta.url)
}

const REQUIRED_COLUMNS = ['UI group', 'Source design', 'QA evidence', 'Status', 'Notes', 'Owner slice']
const ALLOWED_STATUSES = new Set(['aligned', 'acceptable-delta', 'must-fix'])
const OWNER_SLICE = 'frontend-v21-design-parity-audit-closure'

function parseMarkdownRows(source) {
  return source
    .split('\n')
    .filter(line => line.startsWith('|') && !line.includes('---'))
    .map(line => line.split('|').slice(1, -1).map(cell => cell.trim()))
}

function parseRiskRows(source) {
  const rows = parseMarkdownRows(source)
  const header = rows.at(0) ?? []
  assert.deepEqual(header, REQUIRED_COLUMNS)
  return rows.slice(1).map(row => Object.fromEntries(header.map((column, index) => [column, row[index] ?? ''])))
}

function extractReportGroups(report) {
  const rows = parseMarkdownRows(report)
  const coverageStart = rows.findIndex(row => row[0] === 'UI group' && row[1] === 'Scenario count')
  assert.notEqual(coverageStart, -1, 'visual QA report must include UI group coverage')
  const groups = []

  for (const row of rows.slice(coverageStart + 1)) {
    if (row[0] === 'UI group' && row[1] === 'Scenario') break
    if (row.length >= 3 && row[0]) groups.push(row[0])
  }

  return [...new Set(groups)]
}

test('visual parity register covers every browser visual QA UI group', async () => {
  const [report, riskRegister] = await Promise.all([
    readFile(files.report, 'utf8'),
    readFile(files.riskRegister, 'utf8')
  ])

  const reportGroups = extractReportGroups(report)
  const riskRows = parseRiskRows(riskRegister)
  const riskGroups = new Set(riskRows.map(row => row['UI group']))
  const missingGroups = reportGroups.filter(group => !riskGroups.has(group))

  assert.deepEqual(missingGroups, [])
  assert.ok(reportGroups.length >= 13, 'visual QA report should keep broad v2.1 UI coverage')
})

test('visual parity register rows are concrete and status values are bounded', async () => {
  const riskRegister = await readFile(files.riskRegister, 'utf8')
  const riskRows = parseRiskRows(riskRegister)

  for (const row of riskRows) {
    for (const column of REQUIRED_COLUMNS) {
      assert.ok(row[column], `${row['UI group']} must include ${column}`)
    }
    assert.ok(ALLOWED_STATUSES.has(row.Status), `${row['UI group']} has invalid status ${row.Status}`)
  }

  assert.ok(riskRows.some(row => row['Owner slice'] === OWNER_SLICE))
})

test('visual QA report and progress documentation reference the audit closure', async () => {
  const [report, progress] = await Promise.all([
    readFile(files.report, 'utf8'),
    readFile(files.progress, 'utf8')
  ])

  assert.match(report, /v21-visual-parity-risk-register\.md/)
  assert.match(progress, new RegExp(OWNER_SLICE))
})
