import { readdir, readFile } from 'node:fs/promises'
import { basename, join } from 'node:path'

type SupportedLocale = 'en' | 'zh-CN' | 'zh-TW'

const DEFAULT_LOCALE: SupportedLocale = 'en'
const SUPPORTED_LOCALES: SupportedLocale[] = ['en', 'zh-CN', 'zh-TW']

const LOCALE_SUFFIX_MAP = {
  En: 'en',
  ZhCN: 'zh-CN',
  ZhTW: 'zh-TW'
} as const satisfies Record<string, SupportedLocale>

const DEFAULT_EXPORT_LOCALE_MAP = {
  en: 'en',
  'zh-CN': 'zh-CN',
  'zh-TW': 'zh-TW'
} as const satisfies Record<string, SupportedLocale>

export interface I18nModuleReport {
  moduleId: string
  keyCounts: Record<SupportedLocale, number>
  missingKeys: Record<SupportedLocale, string[]>
  placeholderMismatches: I18nPlaceholderMismatch[]
}

export interface I18nCatalogReport {
  generatedAt: string
  localeDir: string
  totalModules: number
  totalKeys: Record<SupportedLocale, number>
  modulesWithGaps: string[]
  modulesWithPlaceholderMismatches: string[]
  moduleReports: I18nModuleReport[]
}

export interface I18nPlaceholderMismatch {
  key: string
  expectedPlaceholders: string[]
  localePlaceholders: Record<SupportedLocale, string[]>
}

interface LocaleBlock {
  moduleId: string
  locale: SupportedLocale
  entries: LocaleEntry[]
}

interface LocaleEntry {
  key: string
  placeholders: string[]
}

interface LocaleCatalog {
  keys: Set<string>
  placeholdersByKey: Map<string, string[]>
}

type NamedLocaleSuffix = keyof typeof LOCALE_SUFFIX_MAP
type DefaultExportLocaleName = keyof typeof DEFAULT_EXPORT_LOCALE_MAP
const PLACEHOLDER_PATTERN = /\{([a-zA-Z0-9_]+)\}/g

function createLocaleRecord<T>(factory: () => T): Record<SupportedLocale, T> {
  return {
    en: factory(),
    'zh-CN': factory(),
    'zh-TW': factory()
  }
}

function isNamedLocaleSuffix(value: string): value is NamedLocaleSuffix {
  return value in LOCALE_SUFFIX_MAP
}

function isDefaultExportLocaleName(value: string): value is DefaultExportLocaleName {
  return value in DEFAULT_EXPORT_LOCALE_MAP
}

function createLocaleCatalog(): LocaleCatalog {
  return {
    keys: new Set<string>(),
    placeholdersByKey: new Map<string, string[]>(),
  }
}

function parseLocaleEntry(line: string): LocaleEntry | null {
  const keyValueMatch = line.match(/^\s*'([^']+)':\s*'((?:\\'|[^'])*)'/)
  if (!keyValueMatch) {
    return null
  }
  const [, key, rawValue] = keyValueMatch
  return {
    key,
    placeholders: extractPlaceholders(rawValue),
  }
}

function extractPlaceholders(value: string): string[] {
  return [...new Set([...value.matchAll(PLACEHOLDER_PATTERN)].map((match) => match[1]))].sort()
}

function parseLocaleBlocks(source: string, fileName: string): LocaleBlock[] {
  const lines = source.split('\n')
  const blocks: LocaleBlock[] = []
  let activeBlock: LocaleBlock | null = null

  for (const line of lines) {
    const namedExportMatch = line.match(/^export const (\w+?)(En|ZhCN|ZhTW) = \{$/)
    if (namedExportMatch) {
      const [, moduleBaseName, suffix] = namedExportMatch
      if (!isNamedLocaleSuffix(suffix)) {
        throw new Error(`Unsupported locale suffix: ${suffix}`)
      }
      activeBlock = {
        moduleId: moduleBaseName,
        locale: LOCALE_SUFFIX_MAP[suffix],
        entries: []
      }
      continue
    }

    const coreLocaleConstMatch = line.match(/^const \w+ = \{$/)
    if (coreLocaleConstMatch) {
      const localeName = basename(fileName, '.ts')
      if (isDefaultExportLocaleName(localeName)) {
        activeBlock = {
          moduleId: 'core',
          locale: DEFAULT_EXPORT_LOCALE_MAP[localeName],
          entries: []
        }
        continue
      }
    }

    const defaultExportMatch = line.match(/^export default \{$/)
    if (defaultExportMatch) {
      const localeName = basename(fileName, '.ts')
      if (!isDefaultExportLocaleName(localeName)) {
        throw new Error(`Unsupported default export locale file: ${fileName}`)
      }
      activeBlock = {
        moduleId: 'core',
        locale: DEFAULT_EXPORT_LOCALE_MAP[localeName],
        entries: []
      }
      continue
    }

    if (!activeBlock) {
      continue
    }

    if (line.trim() === '}') {
      blocks.push(activeBlock)
      activeBlock = null
      continue
    }

    const entry = parseLocaleEntry(line)
    if (entry) {
      activeBlock.entries.push(entry)
    }
  }

  return blocks
}

function buildModuleReport(moduleId: string, localeMap: Record<SupportedLocale, LocaleCatalog>): I18nModuleReport {
  const unionKeys = new Set<string>()
  for (const locale of SUPPORTED_LOCALES) {
    for (const key of localeMap[locale].keys) {
      unionKeys.add(key)
    }
  }

  const missingKeys = createLocaleRecord(() => [] as string[])
  const keyCounts = createLocaleRecord(() => 0)
  for (const locale of SUPPORTED_LOCALES) {
    keyCounts[locale] = localeMap[locale].keys.size
    missingKeys[locale] = [...unionKeys].filter((key) => !localeMap[locale].keys.has(key)).sort()
  }

  return {
    moduleId,
    keyCounts,
    missingKeys,
    placeholderMismatches: collectPlaceholderMismatches(unionKeys, localeMap),
  }
}

function collectPlaceholderMismatches(
  unionKeys: Set<string>,
  localeMap: Record<SupportedLocale, LocaleCatalog>,
): I18nPlaceholderMismatch[] {
  const mismatches: I18nPlaceholderMismatch[] = []

  for (const key of unionKeys) {
    if (SUPPORTED_LOCALES.some((locale) => !localeMap[locale].keys.has(key))) {
      continue
    }

    const localePlaceholders = createLocaleRecord(
      () => [] as string[],
    )
    for (const locale of SUPPORTED_LOCALES) {
      localePlaceholders[locale] = localeMap[locale].placeholdersByKey.get(key) ?? []
    }

    const signatures = new Set(
      SUPPORTED_LOCALES.map((locale) => localePlaceholders[locale].join('|')),
    )
    if (signatures.size <= 1) {
      continue
    }

    mismatches.push({
      key,
      expectedPlaceholders: localePlaceholders[DEFAULT_LOCALE],
      localePlaceholders,
    })
  }

  return mismatches.sort((left, right) => left.key.localeCompare(right.key))
}

export async function collectI18nCatalogReport(localeDir: string): Promise<I18nCatalogReport> {
  const files = (await readdir(localeDir))
    .filter((file) => file.endsWith('.ts') && file !== 'index.ts')
    .sort()

  const moduleMap = new Map<string, Record<SupportedLocale, LocaleCatalog>>()

  for (const file of files) {
    const source = await readFile(join(localeDir, file), 'utf8')
    const blocks = parseLocaleBlocks(source, file)
    for (const block of blocks) {
      const localeMap = moduleMap.get(block.moduleId) ?? createLocaleRecord(createLocaleCatalog)
      for (const entry of block.entries) {
        localeMap[block.locale].keys.add(entry.key)
        localeMap[block.locale].placeholdersByKey.set(entry.key, entry.placeholders)
      }
      moduleMap.set(block.moduleId, localeMap)
    }
  }

  const moduleReports = [...moduleMap.entries()]
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([moduleId, localeMap]) => buildModuleReport(moduleId, localeMap))

  const totalKeys = createLocaleRecord(() => 0)
  const uniqueKeysByLocale = createLocaleRecord(() => new Set<string>())
  for (const [, localeMap] of moduleMap.entries()) {
    for (const locale of SUPPORTED_LOCALES) {
      for (const key of localeMap[locale].keys) {
        uniqueKeysByLocale[locale].add(key)
      }
    }
  }
  for (const locale of SUPPORTED_LOCALES) {
    totalKeys[locale] = uniqueKeysByLocale[locale].size
  }

  return {
    generatedAt: new Date().toISOString(),
    localeDir,
    totalModules: moduleReports.length,
    totalKeys,
    modulesWithGaps: moduleReports
      .filter((report) => SUPPORTED_LOCALES.some((locale) => report.missingKeys[locale].length > 0))
      .map((report) => report.moduleId),
    modulesWithPlaceholderMismatches: moduleReports
      .filter((report) => report.placeholderMismatches.length > 0)
      .map((report) => report.moduleId),
    moduleReports
  }
}

export function formatI18nCatalogMarkdown(report: I18nCatalogReport): string {
  const lines: string[] = [
    '# I18n Consistency Report',
    '',
    `- Generated at: \`${report.generatedAt}\``,
    `- Locale dir: \`${report.localeDir}\``,
    `- Default locale: \`${DEFAULT_LOCALE}\``,
    `- Total modules: \`${report.totalModules}\``,
    `- Total keys: \`en=${report.totalKeys.en}\` \`zh-CN=${report.totalKeys['zh-CN']}\` \`zh-TW=${report.totalKeys['zh-TW']}\``,
    `- Modules with gaps: \`${report.modulesWithGaps.length}\``,
    `- Modules with placeholder mismatches: \`${report.modulesWithPlaceholderMismatches.length}\``,
    '',
    '| Module | en | zh-CN | zh-TW | Missing | Placeholder mismatches |',
    '| --- | ---: | ---: | ---: | --- | ---: |'
  ]

  for (const item of report.moduleReports) {
    const missingParts = SUPPORTED_LOCALES
      .filter((locale) => item.missingKeys[locale].length > 0)
      .map((locale) => `${locale}: ${item.missingKeys[locale].join(', ')}`)
    lines.push(`| ${item.moduleId} | ${item.keyCounts.en} | ${item.keyCounts['zh-CN']} | ${item.keyCounts['zh-TW']} | ${missingParts.join('<br>') || 'None'} | ${item.placeholderMismatches.length} |`)
  }

  const mismatchedModules = report.moduleReports.filter((item) => item.placeholderMismatches.length > 0)
  if (mismatchedModules.length > 0) {
    lines.push('', '## Placeholder Mismatches', '')
    for (const moduleReport of mismatchedModules) {
      lines.push(`### ${moduleReport.moduleId}`, '')
      for (const mismatch of moduleReport.placeholderMismatches) {
        const localeDetails = SUPPORTED_LOCALES
          .map((locale) => `${locale}: ${mismatch.localePlaceholders[locale].join(', ') || 'None'}`)
          .join(' · ')
        lines.push(`- \`${mismatch.key}\` → ${localeDetails}`)
      }
      lines.push('')
    }
  }

  return `${lines.join('\n')}\n`
}
