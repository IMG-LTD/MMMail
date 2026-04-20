import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const files = {
  baseLayout: new URL('../src/layouts/base-layout/BaseLayout.vue', import.meta.url),
  topBar: new URL('../src/layouts/modules/ShellTopBar.vue', import.meta.url),
  mobileTabs: new URL('../src/layouts/modules/MobileTabBar.vue', import.meta.url),
  shellNav: new URL('../src/layouts/modules/ShellSideNav.vue', import.meta.url),
  shellModel: new URL('../src/layouts/modules/shell-nav.ts', import.meta.url),
  mailSurface: new URL('../src/views/app/MailSurfaceView.vue', import.meta.url),
  driveSurface: new URL('../src/views/app/DriveSectionView.vue', import.meta.url),
  passSurface: new URL('../src/views/app/PassSectionView.vue', import.meta.url),
  organizationsSurface: new URL('../src/views/app/OrganizationsSectionView.vue', import.meta.url)
}

test('responsive shell safeguards stay in place for mobile degradation', async () => {
  const [
    baseLayout,
    topBar,
    mobileTabs,
    shellNav,
    shellModel,
    mailSurface,
    driveSurface,
    passSurface,
    organizationsSurface
  ] = await Promise.all(Object.values(files).map(file => readFile(file, 'utf8')))

  assert.match(baseLayout, /base-layout__content--flush[\s\S]*padding:\s*0 0 88px;/)
  assert.match(topBar, /top-bar__locale,\s*\n\s*\.icon-button\s*\{\s*\n\s*display:\s*none;/)
  assert.match(mobileTabs, /mobilePrimaryTabs/)
  assert.match(mobileTabs, /isRouteMatch/)
  assert.match(shellNav, /isRouteMatch/)
  assert.match(shellModel, /matchPrefixes:\s*\['\/pass', '\/pass-monitor'\]/)
  assert.match(shellModel, /'\/suite', '\/docs', '\/sheets'/)
  assert.match(mailSurface, /mail-surface--conversation/)
  assert.match(mailSurface, /mail-surface--thread/)
  assert.match(mailSurface, /mail-workspace__detail\s*\{\s*\n\s*display:\s*none;/)
  assert.match(driveSurface, /overflow-x:\s*auto;/)
  assert.match(passSurface, /white-space:\s*nowrap;/)
  assert.match(organizationsSurface, /flex-wrap:\s*nowrap;/)
})
