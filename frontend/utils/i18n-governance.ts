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
}

export interface I18nCatalogReport {
  generatedAt: string
  localeDir: string
  totalModules: number
  totalKeys: Record<SupportedLocale, number>
  modulesWithGaps: string[]
  moduleReports: I18nModuleReport[]
}

interface LocaleBlock {
  moduleId: string
  locale: SupportedLocale
  keys: string[]
}

type NamedLocaleSuffix = keyof typeof LOCALE_SUFFIX_MAP
type DefaultExportLocaleName = keyof typeof DEFAULT_EXPORT_LOCALE_MAP

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
        keys: []
      }
      continue
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
        keys: []
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

    const keyMatch = line.match(/^\s*'([^']+)':/)
    if (keyMatch) {
      activeBlock.keys.push(keyMatch[1])
    }
  }

  return blocks
}

export async function collectI18nCatalogReport(localeDir: string): Promise<I18nCatalogReport> {
  const files = (await readdir(localeDir))
    .filter((file) => file.endsWith('.ts') && file !== 'index.ts')
    .sort()

  const moduleMap = new Map<string, Record<SupportedLocale, Set<string>>>()

  for (const file of files) {
    const source = await readFile(join(localeDir, file), 'utf8')
    const blocks = parseLocaleBlocks(source, file)
    for (const block of blocks) {
      const localeMap = moduleMap.get(block.moduleId) ?? createLocaleRecord(() => new Set<string>())
      for (const key of block.keys) {
        localeMap[block.locale].add(key)
      }
      moduleMap.set(block.moduleId, localeMap)
    }
  }

  const moduleReports = [...moduleMap.entries()]
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([moduleId, localeMap]) => {
      const unionKeys = new Set<string>()
      for (const locale of SUPPORTED_LOCALES) {
        for (const key of localeMap[locale]) {
          unionKeys.add(key)
        }
      }
      const missingKeys = createLocaleRecord(() => [] as string[])
      const keyCounts = createLocaleRecord(() => 0)
      for (const locale of SUPPORTED_LOCALES) {
        keyCounts[locale] = localeMap[locale].size
        missingKeys[locale] = [...unionKeys].filter((key) => !localeMap[locale].has(key)).sort()
      }
      return {
        moduleId,
        keyCounts,
        missingKeys
      }
    })

  const totalKeys = createLocaleRecord(() => 0)
  const uniqueKeysByLocale = createLocaleRecord(() => new Set<string>())
  for (const report of moduleReports) {
    for (const locale of SUPPORTED_LOCALES) {
      for (const key of report.missingKeys[locale]) {
        void key
      }
    }
  }
  for (const [, localeMap] of moduleMap.entries()) {
    for (const locale of SUPPORTED_LOCALES) {
      for (const key of localeMap[locale]) {
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
    '',
    '| Module | en | zh-CN | zh-TW | Missing |',
    '| --- | ---: | ---: | ---: | --- |'
  ]

  for (const item of report.moduleReports) {
    const missingParts = SUPPORTED_LOCALES
      .filter((locale) => item.missingKeys[locale].length > 0)
      .map((locale) => `${locale}: ${item.missingKeys[locale].join(', ')}`)
    lines.push(`| ${item.moduleId} | ${item.keyCounts.en} | ${item.keyCounts['zh-CN']} | ${item.keyCounts['zh-TW']} | ${missingParts.join('<br>') || 'None'} |`)
  }

  return `${lines.join('\n')}\n`
}
