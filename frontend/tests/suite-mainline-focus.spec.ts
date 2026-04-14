import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

function readSource(path: string) {
  return readFileSync(resolve(process.cwd(), path), 'utf8')
}

describe('suite mainline focus', () => {
  it('keeps suite overview focused on the mainline story', () => {
    const source = readSource('components/suite/SuiteOverviewSection.vue')
    expect(source).toContain('suite.sectionOverview.mainlineBadge')
    expect(source).toContain('suite.sectionOverview.mainlineDescription')
    expect(source).toContain('SuiteMainlineJourneyPanel')
  })

  it('keeps suite page metadata aligned to the mainline-first overview', () => {
    const source = readSource('pages/suite.vue')
    expect(source).toContain('suite.meta.description')
  })

  it('prioritizes release boundary guidance in settings adoption resources', () => {
    const source = readSource('components/settings/SettingsAdoptionReadinessPanel.vue')
    expect(source).toContain("code: 'release-boundary'")
    expect(source).toContain("titleKey: 'settings.adoption.cards.boundary.title'")
    expect(source).toContain("href: '/suite?section=boundary'")
  })

  it('makes pass beta boundary explicit near the top-level workspace', () => {
    const source = readSource('pages/pass.vue')
    expect(source).toContain('pass.workspace.betaBoundaryTitle')
    expect(source).toContain('pass.workspace.betaBoundaryDescription')
    expect(source).toContain('data-testid="pass-beta-boundary"')
    expect(source).toContain('el-alert')
  })

  it('registers the new mainline and pass boundary locale keys in every supported locale', () => {
    const suiteNavigationLocale = readSource('locales/suite-navigation.ts')
    const suitePlansLocale = readSource('locales/suite-plans.ts')
    const adoptionLocale = readSource('locales/adoption.ts')
    const passWorkspaceLocale = readSource('locales/pass-workspace.ts')

    expect(suiteNavigationLocale.match(/'suite\.sectionOverview\.mainlineBadge'/g)?.length).toBe(3)
    expect(suiteNavigationLocale.match(/'suite\.sectionOverview\.mainlineDescription'/g)?.length).toBe(3)
    expect(suitePlansLocale.match(/'suite\.meta\.description'/g)?.length).toBe(3)
    expect(adoptionLocale.match(/'settings\.adoption\.cards\.boundary\.title'/g)?.length).toBe(3)
    expect(adoptionLocale.match(/'settings\.adoption\.cards\.boundary\.description'/g)?.length).toBe(3)
    expect(adoptionLocale.match(/'settings\.adoption\.cards\.boundary\.meta'/g)?.length).toBe(3)
    expect(adoptionLocale.match(/'settings\.adoption\.cards\.boundary\.primary'/g)?.length).toBe(3)
    expect(passWorkspaceLocale.match(/'pass\.workspace\.betaBoundaryTitle'/g)?.length).toBe(3)
    expect(passWorkspaceLocale.match(/'pass\.workspace\.betaBoundaryDescription'/g)?.length).toBe(3)
  })
})
