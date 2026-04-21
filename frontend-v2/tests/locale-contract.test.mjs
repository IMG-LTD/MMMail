import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const localeFile = new URL('../src/locales/index.ts', import.meta.url)
const appStoreFile = new URL('../src/store/modules/app.ts', import.meta.url)
const shellFiles = {
  topBar: new URL('../src/layouts/modules/ShellTopBar.vue', import.meta.url),
  sideNav: new URL('../src/layouts/modules/ShellSideNav.vue', import.meta.url),
  mobileTabs: new URL('../src/layouts/modules/MobileTabBar.vue', import.meta.url),
  themeDrawer: new URL('../src/layouts/modules/ThemeDrawer.vue', import.meta.url),
  localeSwitcher: new URL('../src/shared/components/LocaleSwitcher.vue', import.meta.url)
}

const routedViewFiles = [
  '../src/views/public/LoginView.vue',
  '../src/views/public/RegisterView.vue',
  '../src/views/public/BoundaryView.vue',
  '../src/views/public/ProductAccessBlockedView.vue',
  '../src/views/public/PublicMailShareView.vue',
  '../src/views/public/PublicDriveShareView.vue',
  '../src/views/public/StorySurfaceView.vue',
  '../src/views/public/System404View.vue',
  '../src/views/public/System500View.vue',
  '../src/views/public/SystemOfflineView.vue',
  '../src/views/public/SystemMaintenanceView.vue',
  '../src/views/app/MailSurfaceView.vue',
  '../src/views/app/DriveSectionView.vue',
  '../src/views/app/PassSectionView.vue',
  '../src/views/app/SuiteSectionView.vue',
  '../src/views/app/OrganizationsSectionView.vue',
  '../src/views/app/SecurityCenterView.vue',
  '../src/views/app/SettingsWorkspaceView.vue',
  '../src/views/app/CollaborationView.vue',
  '../src/views/app/CommandCenterView.vue',
  '../src/views/app/NotificationsView.vue',
  '../src/views/app/LabsOverviewView.vue',
  '../src/views/app/LabsModuleView.vue',
  '../src/views/app/PassMonitorView.vue',
  '../src/views/app/DocsWorkspaceView.vue',
  '../src/views/app/DocsEditorView.vue',
  '../src/views/app/SheetsWorkspaceView.vue',
  '../src/views/app/SheetsEditorView.vue',
  '../src/views/app/CalendarView.vue',
  '../src/views/app/BusinessOverviewView.vue'
]

const removedRegressionChecks = [
  {
    file: new URL('../src/views/app/BusinessOverviewView.vue', import.meta.url),
    pattern: /:key="metric\.label"/
  },
  {
    file: new URL('../src/views/app/CalendarView.vue', import.meta.url),
    pattern: /:key="calendar\.name"/
  },
  {
    file: new URL('../src/views/app/MailSurfaceView.vue', import.meta.url),
    pattern: /\['Elena Rostova', lt\('设计系统'/
  },
  {
    file: new URL('../src/views/app/MailSurfaceView.vue', import.meta.url),
    pattern: /value="Phoenix launch handoff"/
  },
  {
    file: new URL('../src/views/app/PassMonitorView.vue', import.meta.url),
    pattern: /:key="column\.title"/
  },
  {
    file: new URL('../src/views/app/OrganizationsSectionView.vue', import.meta.url),
    pattern: /\['Executive Board', 'Owner', 'Enabled', '2 mins ago'\]/
  },
  {
    file: new URL('../src/views/app/SecurityCenterView.vue', import.meta.url),
    pattern: /\['TOTP Authenticator', 'Enabled'\]/
  },
  {
    file: new URL('../src/views/app/SettingsWorkspaceView.vue', import.meta.url),
    pattern: /\['MacBook Pro 14"', 'Zurich, CH', 'Current Session'\]/
  }
]

test('locale infrastructure keeps real zh-CN, zh-TW, and en support', async () => {
  const [localeContent, appStoreContent, topBarContent, localeSwitcherContent] = await Promise.all([
    readFile(localeFile, 'utf8'),
    readFile(appStoreFile, 'utf8'),
    readFile(shellFiles.topBar, 'utf8'),
    readFile(shellFiles.localeSwitcher, 'utf8')
  ])

  assert.match(localeContent, /export type AppLocale = 'zh-CN' \| 'zh-TW' \| 'en'/)
  assert.match(localeContent, /'zh-TW': zhTW/)
  assert.match(localeContent, /'zh-TW': dateZhTW/)
  assert.match(localeContent, /normalized\.startsWith\('zh-tw'\)/)
  assert.match(localeContent, /normalized\.startsWith\('zh-hk'\)/)
  assert.match(localeContent, /normalized\.startsWith\('zh-mo'\)/)
  assert.doesNotMatch(localeContent, /'zh-TW': zhCN/)
  assert.doesNotMatch(localeContent, /'zh-TW': dateZhCN/)

  assert.match(appStoreContent, /useStorage<AppLocale>\('mmmail-app-locale', detectAppLocale\(\)\)/)
  assert.match(appStoreContent, /function setLocale\(value: AppLocale\)/)
  assert.match(topBarContent, /LocaleSwitcher/)
  assert.match(localeSwitcherContent, /v-for="option in options"/)
})

test('routed shell and routed surfaces remain locale-aware', async () => {
  const shellContents = await Promise.all(Object.values(shellFiles).map(file => readFile(file, 'utf8')))

  for (const content of shellContents) {
    assert.match(content, /useLocaleText|LocaleSwitcher|locale-switcher/)
  }

  const routedContents = await Promise.all(
    routedViewFiles.map(async relativePath => {
      const content = await readFile(new URL(relativePath, import.meta.url), 'utf8')
      return { relativePath, content }
    })
  )

  for (const { relativePath, content } of routedContents) {
    assert.match(
      content,
      /useLocaleText|lt\(|'zh-CN':|'zh-TW':|en:/,
      `${relativePath} is missing locale-aware copy wiring`
    )
  }
})

test('known i18n regressions stay removed from routed surfaces', async () => {
  const contents = await Promise.all(
    removedRegressionChecks.map(async check => ({
      pattern: check.pattern,
      content: await readFile(check.file, 'utf8')
    }))
  )

  for (const { pattern, content } of contents) {
    assert.doesNotMatch(content, pattern)
  }
})
