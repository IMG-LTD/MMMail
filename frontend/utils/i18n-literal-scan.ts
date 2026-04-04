import { readdir, readFile, stat } from 'node:fs/promises'
import { basename, join, relative, sep } from 'node:path'

const USER_FACING_ATTRIBUTES = ['description', 'label', 'placeholder', 'title'] as const
const TEXT_LITERAL_PATTERN = />\s*([^<{][^<]*[A-Za-z][^<]*)\s*</g
const EL_MESSAGE_LITERAL_PATTERN = /ElMessage\.(success|warning|error|info)\(\s*(['"`])([^'"`$\\]+)\2/g

export type I18nLiteralViolationKind = 'el-message' | 'template-attribute' | 'template-text'

export interface I18nLiteralViolation {
  filePath: string
  line: number
  kind: I18nLiteralViolationKind
  literal: string
}

export interface I18nLiteralScanReport {
  generatedAt: string
  rootDir: string
  targets: string[]
  totalFiles: number
  totalViolations: number
  filesWithViolations: string[]
  violations: I18nLiteralViolation[]
}

export async function collectI18nLiteralScanReport(
  rootDir: string,
  targets: string[],
): Promise<I18nLiteralScanReport> {
  const files = await collectTargetFiles(targets)
  const violations = []

  for (const filePath of files) {
    const source = await readFile(filePath, 'utf8')
    violations.push(...scanSource(rootDir, filePath, source))
  }

  return {
    generatedAt: new Date().toISOString(),
    rootDir,
    targets: targets.map((target) => normalizePath(relative(rootDir, target))),
    totalFiles: files.length,
    totalViolations: violations.length,
    filesWithViolations: [...new Set(violations.map((item) => item.filePath))].sort(),
    violations,
  }
}

async function collectTargetFiles(targets: string[]): Promise<string[]> {
  const files = new Set<string>()

  for (const target of targets) {
    const entry = await stat(target)
    if (entry.isDirectory()) {
      for (const filePath of await collectDirectoryFiles(target)) {
        files.add(filePath)
      }
      continue
    }
    files.add(target)
  }

  return [...files].sort((left, right) => left.localeCompare(right))
}

async function collectDirectoryFiles(directory: string): Promise<string[]> {
  const entries = await readdir(directory, { withFileTypes: true })
  const files: string[] = []

  for (const entry of entries) {
    const fullPath = join(directory, entry.name)
    if (entry.isDirectory()) {
      files.push(...await collectDirectoryFiles(fullPath))
      continue
    }
    if (entry.isFile() && entry.name.endsWith('.vue')) {
      files.push(fullPath)
    }
  }

  return files
}

function scanSource(rootDir: string, filePath: string, source: string): I18nLiteralViolation[] {
  const normalizedPath = normalizePath(relative(rootDir, filePath))
  return [
    ...scanTemplateLiterals(normalizedPath, source),
    ...scanElMessageLiterals(normalizedPath, source),
  ]
}

function scanTemplateLiterals(filePath: string, source: string): I18nLiteralViolation[] {
  const template = extractTemplateBlock(source)
  if (!template) {
    return []
  }

  return [
    ...scanTemplateAttributes(filePath, template.content, template.baseLine),
    ...scanTemplateTexts(filePath, template.content, template.baseLine),
  ]
}

function extractTemplateBlock(source: string): { content: string, baseLine: number } | null {
  const match = source.match(/<template>([\s\S]*?)<\/template>/)
  if (!match || typeof match.index !== 'number') {
    return null
  }
  return {
    content: match[1],
    baseLine: lineNumberAt(source, match.index),
  }
}

function scanTemplateAttributes(
  filePath: string,
  template: string,
  baseLine: number,
): I18nLiteralViolation[] {
  const pattern = new RegExp(
    String.raw`(?:^|\s)(${USER_FACING_ATTRIBUTES.join('|')})\s*=\s*(['"])([^'"` + '`' + String.raw`]+)\2`,
    'g',
  )
  return collectMatches(filePath, template, pattern, 'template-attribute', baseLine)
}

function scanTemplateTexts(
  filePath: string,
  template: string,
  baseLine: number,
): I18nLiteralViolation[] {
  return collectMatches(filePath, template, TEXT_LITERAL_PATTERN, 'template-text', baseLine)
}

function scanElMessageLiterals(filePath: string, source: string): I18nLiteralViolation[] {
  return collectMatches(filePath, source, EL_MESSAGE_LITERAL_PATTERN, 'el-message', 0)
}

function collectMatches(
  filePath: string,
  source: string,
  pattern: RegExp,
  kind: I18nLiteralViolationKind,
  baseLine: number,
): I18nLiteralViolation[] {
  const violations: I18nLiteralViolation[] = []

  for (const match of source.matchAll(pattern)) {
    const literal = normalizeLiteral(match[3] ?? match[1] ?? '')
    if (!isReportableLiteral(literal)) {
      continue
    }
    violations.push({
      filePath,
      line: baseLine + lineNumberAt(source, match.index ?? 0),
      kind,
      literal,
    })
  }

  return violations
}

function normalizeLiteral(value: string): string {
  return value.replace(/\s+/g, ' ').trim()
}

function isReportableLiteral(value: string): boolean {
  return value.length > 0 && /[A-Za-z]/.test(value)
}

function lineNumberAt(source: string, index: number): number {
  return source.slice(0, index).split('\n').length
}

function normalizePath(value: string): string {
  return value.split(sep).join('/')
}

export function formatI18nLiteralScanMarkdown(report: I18nLiteralScanReport): string {
  const lines = [
    '# I18n Literal Scan Report',
    '',
    `- Generated at: \`${report.generatedAt}\``,
    `- Root dir: \`${report.rootDir}\``,
    `- Targets: \`${report.targets.join(', ')}\``,
    `- Total files: \`${report.totalFiles}\``,
    `- Total violations: \`${report.totalViolations}\``,
    `- Files with violations: \`${report.filesWithViolations.length}\``,
    '',
    '| File | Line | Kind | Literal |',
    '| --- | ---: | --- | --- |',
  ]

  for (const violation of report.violations) {
    lines.push(`| ${violation.filePath} | ${violation.line} | ${violation.kind} | ${escapeCell(violation.literal)} |`)
  }

  return `${lines.join('\n')}\n`
}

function escapeCell(value: string): string {
  return value.replace(/\|/g, '\\|')
}
