import type { LocationQuery } from 'vue-router'

export const SUITE_SECTION_CODES = [
  'overview',
  'plans',
  'billing',
  'operations',
  'boundary'
] as const

export type SuiteSectionCode = typeof SUITE_SECTION_CODES[number]

export interface SuiteSectionDefinition {
  code: SuiteSectionCode
  labelKey: string
  descriptionKey: string
}

export const DEFAULT_SUITE_SECTION: SuiteSectionCode = 'overview'

export const SUITE_SECTIONS: readonly SuiteSectionDefinition[] = [
  {
    code: 'overview',
    labelKey: 'suite.sectionNav.sections.overview.label',
    descriptionKey: 'suite.sectionNav.sections.overview.description'
  },
  {
    code: 'plans',
    labelKey: 'suite.sectionNav.sections.plans.label',
    descriptionKey: 'suite.sectionNav.sections.plans.description'
  },
  {
    code: 'billing',
    labelKey: 'suite.sectionNav.sections.billing.label',
    descriptionKey: 'suite.sectionNav.sections.billing.description'
  },
  {
    code: 'operations',
    labelKey: 'suite.sectionNav.sections.operations.label',
    descriptionKey: 'suite.sectionNav.sections.operations.description'
  },
  {
    code: 'boundary',
    labelKey: 'suite.sectionNav.sections.boundary.label',
    descriptionKey: 'suite.sectionNav.sections.boundary.description'
  }
]

export function isSuiteSectionCode(value: string): value is SuiteSectionCode {
  return SUITE_SECTION_CODES.includes(value as SuiteSectionCode)
}

export function resolveSuiteSection(rawValue: unknown): SuiteSectionCode {
  const value = typeof rawValue === 'string' ? rawValue.trim().toLowerCase() : ''
  return isSuiteSectionCode(value) ? value : DEFAULT_SUITE_SECTION
}

export function buildSuiteSectionQuery(
  query: LocationQuery,
  section: SuiteSectionCode
): LocationQuery {
  const nextQuery: LocationQuery = { ...query }
  if (section === DEFAULT_SUITE_SECTION) {
    delete nextQuery.section
    return nextQuery
  }
  nextQuery.section = section
  return nextQuery
}
