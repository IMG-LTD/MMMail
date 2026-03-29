import { readdir, readFile } from 'node:fs/promises'
import { join, relative, sep } from 'node:path'

const PAGE_EXTENSION = '.vue'
const INDEX_BASENAME = 'index'
const PERCENT_SCALE = 10
const EMPTY_ROUTE = '/'
const TRANSLATION_BINDING_PATTERNS = [
  /\btitle-key\s*=\s*(['"`])([^'"`]+)\1/g
] as const

export interface I18nPageCoverageEntry {
  route: string
  filePath: string
  usesI18n: boolean
  hasTranslationBinding: boolean
  staticKeyCount: number
  staticKeys: string[]
  keyPrefixes: string[]
}

export interface I18nPageCoverageReport {
  generatedAt: string
  pagesDir: string
  totalPages: number
  pagesUsingI18n: number
  localizedPages: number
  pagesWithStaticKeys: number
  coveragePercent: number
  pagesWithoutI18n: string[]
  pagesWithoutStaticKeys: string[]
  pageReports: I18nPageCoverageEntry[]
}

function roundPercent(value: number): number {
  return Math.round(value * PERCENT_SCALE) / PERCENT_SCALE
}

async function collectVueFiles(rootDir: string, currentDir = rootDir): Promise<string[]> {
  const entries = await readdir(currentDir, { withFileTypes: true })
  const files: string[] = []

  for (const entry of entries) {
    const fullPath = join(currentDir, entry.name)
    if (entry.isDirectory()) {
      files.push(...await collectVueFiles(rootDir, fullPath))
      continue
    }
    if (entry.isFile() && entry.name.endsWith(PAGE_EXTENSION)) {
      files.push(fullPath)
    }
  }

  return files.sort((left, right) => left.localeCompare(right))
}

function resolveRouteFromFile(pagesDir: string, filePath: string): string {
  const normalized = relative(pagesDir, filePath).split(sep).join('/')
  const withoutExtension = normalized.slice(0, -PAGE_EXTENSION.length)
  if (withoutExtension === INDEX_BASENAME) {
    return EMPTY_ROUTE
  }
  if (withoutExtension.endsWith(`/${INDEX_BASENAME}`)) {
    return `/${withoutExtension.slice(0, -(INDEX_BASENAME.length + 1))}`
  }
  return `/${withoutExtension}`
}

function extractStaticTranslationKeys(source: string): string[] {
  const matches = source.matchAll(/\bt\(\s*(['"`])([^'"`$\\]+)\1/g)
  const boundKeys = TRANSLATION_BINDING_PATTERNS
    .flatMap((pattern) => [...source.matchAll(pattern)].map((match) => match[2]))
  return [...new Set([
    ...[...matches].map((match) => match[2]).filter(Boolean),
    ...boundKeys.filter(Boolean)
  ])].sort()
}

function extractKeyPrefixes(keys: string[]): string[] {
  return [...new Set(keys
    .filter((key) => key.includes('.'))
    .map((key) => key.split('.', 1)[0]))].sort()
}

export async function collectI18nPageCoverageReport(pagesDir: string): Promise<I18nPageCoverageReport> {
  const files = await collectVueFiles(pagesDir)
  const pageReports: I18nPageCoverageEntry[] = []

  for (const filePath of files) {
    const source = await readFile(filePath, 'utf8')
    const staticKeys = extractStaticTranslationKeys(source)
    pageReports.push({
      route: resolveRouteFromFile(pagesDir, filePath),
      filePath,
      usesI18n: /useI18n\s*\(/.test(source),
      hasTranslationBinding: staticKeys.length > 0,
      staticKeyCount: staticKeys.length,
      staticKeys,
      keyPrefixes: extractKeyPrefixes(staticKeys)
    })
  }

  const pagesUsingI18n = pageReports.filter((item) => item.usesI18n).length
  const localizedPages = pageReports.filter((item) => item.hasTranslationBinding).length
  const pagesWithStaticKeys = pageReports.filter((item) => item.staticKeyCount > 0).length
  const coveragePercent = pageReports.length
    ? roundPercent((localizedPages / pageReports.length) * 100)
    : 0

  return {
    generatedAt: new Date().toISOString(),
    pagesDir,
    totalPages: pageReports.length,
    pagesUsingI18n,
    localizedPages,
    pagesWithStaticKeys,
    coveragePercent,
    pagesWithoutI18n: pageReports.filter((item) => !item.usesI18n).map((item) => item.route),
    pagesWithoutStaticKeys: pageReports.filter((item) => item.usesI18n && item.staticKeyCount === 0).map((item) => item.route),
    pageReports
  }
}

export function formatI18nPageCoverageMarkdown(report: I18nPageCoverageReport): string {
  const lines: string[] = [
    '# I18n Page Coverage Report',
    '',
    `- Generated at: \`${report.generatedAt}\``,
    `- Pages dir: \`${report.pagesDir}\``,
    `- Total pages: \`${report.totalPages}\``,
    `- Pages using i18n: \`${report.pagesUsingI18n}\``,
    `- Localized pages: \`${report.localizedPages}\``,
    `- Pages with static translation keys: \`${report.pagesWithStaticKeys}\``,
    `- Coverage: \`${report.coveragePercent}%\``,
    `- Pages without i18n: \`${report.pagesWithoutI18n.length}\``,
    `- Pages without static keys: \`${report.pagesWithoutStaticKeys.length}\``,
    '',
    '| Route | useI18n | Localized | Static keys | Prefixes |',
    '| --- | --- | --- | ---: | --- |'
  ]

  for (const item of report.pageReports) {
    lines.push(`| ${item.route} | ${item.usesI18n ? 'yes' : 'no'} | ${item.hasTranslationBinding ? 'yes' : 'no'} | ${item.staticKeyCount} | ${item.keyPrefixes.join(', ') || 'None'} |`)
  }

  if (report.pagesWithoutI18n.length > 0) {
    lines.push('', '## Pages Without useI18n', '')
    for (const route of report.pagesWithoutI18n) {
      lines.push(`- \`${route}\``)
    }
  }

  if (report.pagesWithoutStaticKeys.length > 0) {
    lines.push('', '## Pages Without Static Translation Keys', '')
    for (const route of report.pagesWithoutStaticKeys) {
      lines.push(`- \`${route}\``)
    }
  }

  return `${lines.join('\n')}\n`
}
