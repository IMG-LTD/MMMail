import { readFile } from 'node:fs/promises'
import { join, relative } from 'node:path'

const SURFACE_TARGET_FILES = [
  'components/organizations/OrganizationsPolicyPanel.vue',
  'components/security/SecurityAliasQuickCreate.vue',
  'components/suite/SuiteCommandSearchPanel.vue',
  'components/suite/SuiteGovernancePanel.vue',
  'components/suite/SuiteReadinessSecurityPanel.vue',
  'components/suite/SuiteRemediationPanel.vue',
  'composables/useSuiteOperationsWorkspace.ts'
] as const

const VUE_STYLE_BLOCK_PATTERN = /<style\b[\s\S]*?<\/style>/g
const VUE_TEMPLATE_PATTERN = /<template>([\s\S]*?)<\/template>/
const VUE_SCRIPT_PATTERN = /<script\b[^>]*>([\s\S]*?)<\/script>/
const TEMPLATE_ATTRIBUTE_PATTERN = /\s(?:placeholder|title|description|label)=["']([^"'{}:][^"']*)["']/g
const TEMPLATE_TEXT_PATTERN = />\s*([^<>{\n][^<>{]*?)\s*</g
const MESSAGE_LITERAL_PATTERN = /ElMessage(?:Box)?(?:\.\w+)?\(\s*(['"`])((?:\\.|(?!\1).)+)\1/g
const OBJECT_LITERAL_PATTERN = /\b(?:label|description|title|subtitle|message)\s*:\s*(['"`])((?:\\.|(?!\1).)+)\1/g
const STRING_LITERAL_PATTERN = /(['"`])((?:\\.|(?!\1).)+)\1/g

export interface I18nSurfaceLiteralFinding {
  filePath: string
  kind: 'template-text' | 'template-attribute' | 'message-call' | 'script-literal'
  line: number
  value: string
}

export interface I18nSurfaceFileReport {
  filePath: string
  usesI18n: boolean
  findings: I18nSurfaceLiteralFinding[]
}

export interface I18nSurfaceLiteralReport {
  generatedAt: string
  rootDir: string
  totalFiles: number
  filesWithFindings: string[]
  totalFindings: number
  fileReports: I18nSurfaceFileReport[]
}

function normalizeLiteral(value: string): string {
  return value.replace(/\s+/g, ' ').trim()
}

function hasVisibleLetters(value: string): boolean {
  return /[A-Za-z\u4e00-\u9fff]/.test(value)
}

function shouldKeepTemplateLiteral(value: string): boolean {
  const normalized = normalizeLiteral(value)
  if (!normalized || !hasVisibleLetters(normalized)) {
    return false
  }
  return !/^[•·×→+\-:#/\\]+$/.test(normalized)
}

function stripTemplateExpressions(value: string): string {
  return value.replace(/\$\{[^}]+\}/g, '')
}

function shouldKeepScriptLiteral(value: string): boolean {
  const normalized = normalizeLiteral(value)
  const visibleText = normalizeLiteral(stripTemplateExpressions(normalized))
  if (!visibleText || !hasVisibleLetters(visibleText)) {
    return false
  }
  if (normalized.includes('/') || normalized.includes('~/') || normalized.includes('./')) {
    return false
  }
  if (normalized.includes('.') && !normalized.includes(' ')) {
    return false
  }
  if (/^[A-Z0-9_]+$/.test(normalized)) {
    return false
  }
  if (/^[a-z][a-z0-9]*(?::[a-z0-9-]+)+$/.test(normalized)) {
    return false
  }
  if (/^[a-z0-9-]+$/i.test(normalized) && !normalized.includes(' ')) {
    return false
  }
  if (normalized.includes('${') && /^[a-z0-9_:-]+$/i.test(visibleText.replace(/\s+/g, ''))) {
    return false
  }
  return !/^[•·×→+\-:#/\\()]+$/.test(visibleText)
}

function lineNumberOf(source: string, index: number): number {
  return source.slice(0, index).split('\n').length
}

function pushFinding(
  findings: I18nSurfaceLiteralFinding[],
  seen: Set<string>,
  filePath: string,
  kind: I18nSurfaceLiteralFinding['kind'],
  line: number,
  value: string,
): void {
  const normalized = normalizeLiteral(value)
  const key = `${kind}:${line}:${normalized}`
  if (seen.has(key)) {
    return
  }
  seen.add(key)
  findings.push({ filePath, kind, line, value: normalized })
}

function collectTemplateFindings(
  findings: I18nSurfaceLiteralFinding[],
  seen: Set<string>,
  filePath: string,
  templateSource: string,
): void {
  for (const match of templateSource.matchAll(TEMPLATE_ATTRIBUTE_PATTERN)) {
    const value = match[1]
    if (!shouldKeepTemplateLiteral(value)) {
      continue
    }
    pushFinding(findings, seen, filePath, 'template-attribute', lineNumberOf(templateSource, match.index ?? 0), value)
  }

  for (const match of templateSource.matchAll(TEMPLATE_TEXT_PATTERN)) {
    const value = match[1]
    if (!shouldKeepTemplateLiteral(value)) {
      continue
    }
    pushFinding(findings, seen, filePath, 'template-text', lineNumberOf(templateSource, match.index ?? 0), value)
  }
}

function isImportLiteral(scriptSource: string, index: number): boolean {
  const prefix = scriptSource.slice(Math.max(0, index - 24), index)
  return /from\s*$/.test(prefix) || /import\s*$/.test(prefix)
}

function collectScriptFindings(
  findings: I18nSurfaceLiteralFinding[],
  seen: Set<string>,
  filePath: string,
  scriptSource: string,
): void {
  for (const match of scriptSource.matchAll(MESSAGE_LITERAL_PATTERN)) {
    if (shouldKeepScriptLiteral(match[2])) {
      pushFinding(findings, seen, filePath, 'message-call', lineNumberOf(scriptSource, match.index ?? 0), match[2])
    }
  }

  for (const match of scriptSource.matchAll(OBJECT_LITERAL_PATTERN)) {
    if (shouldKeepScriptLiteral(match[2])) {
      pushFinding(findings, seen, filePath, 'script-literal', lineNumberOf(scriptSource, match.index ?? 0), match[2])
    }
  }

  for (const match of scriptSource.matchAll(STRING_LITERAL_PATTERN)) {
    const index = match.index ?? 0
    if (isImportLiteral(scriptSource, index)) {
      continue
    }
    if (shouldKeepScriptLiteral(match[2])) {
      pushFinding(findings, seen, filePath, 'script-literal', lineNumberOf(scriptSource, index), match[2])
    }
  }
}

function splitVueSource(source: string): { template: string; script: string } {
  const safeSource = source.replace(VUE_STYLE_BLOCK_PATTERN, '')
  return {
    template: safeSource.match(VUE_TEMPLATE_PATTERN)?.[1] ?? '',
    script: safeSource.match(VUE_SCRIPT_PATTERN)?.[1] ?? ''
  }
}

export async function collectI18nSurfaceLiteralReport(rootDir: string): Promise<I18nSurfaceLiteralReport> {
  const fileReports: I18nSurfaceFileReport[] = []

  for (const target of SURFACE_TARGET_FILES) {
    const absolutePath = join(rootDir, target)
    const source = await readFile(absolutePath, 'utf8')
    const relativePath = relative(rootDir, absolutePath)
    const findings: I18nSurfaceLiteralFinding[] = []
    const seen = new Set<string>()
    const usesI18n = /useI18n\s*\(/.test(source)

    if (absolutePath.endsWith('.vue')) {
      const sections = splitVueSource(source)
      collectTemplateFindings(findings, seen, relativePath, sections.template)
      collectScriptFindings(findings, seen, relativePath, sections.script)
    } else {
      collectScriptFindings(findings, seen, relativePath, source)
    }

    fileReports.push({
      filePath: relativePath,
      usesI18n,
      findings
    })
  }

  const filesWithFindings = fileReports.filter((item) => item.findings.length > 0).map((item) => item.filePath)
  const totalFindings = fileReports.reduce((sum, item) => sum + item.findings.length, 0)

  return {
    generatedAt: new Date().toISOString(),
    rootDir,
    totalFiles: fileReports.length,
    filesWithFindings,
    totalFindings,
    fileReports
  }
}

export function formatI18nSurfaceLiteralMarkdown(report: I18nSurfaceLiteralReport): string {
  const lines = [
    '# I18n Surface Literal Report',
    '',
    `- Generated at: \`${report.generatedAt}\``,
    `- Total files: \`${report.totalFiles}\``,
    `- Files with findings: \`${report.filesWithFindings.length}\``,
    `- Total findings: \`${report.totalFindings}\``,
    '',
    '| File | useI18n | Findings |',
    '| --- | --- | ---: |'
  ]

  for (const file of report.fileReports) {
    lines.push(`| ${file.filePath} | ${file.usesI18n ? 'yes' : 'no'} | ${file.findings.length} |`)
  }

  for (const file of report.fileReports.filter((item) => item.findings.length > 0)) {
    lines.push('', `## ${file.filePath}`, '')
    for (const finding of file.findings) {
      lines.push(`- L${finding.line} [${finding.kind}] \`${finding.value}\``)
    }
  }

  return `${lines.join('\n')}\n`
}
