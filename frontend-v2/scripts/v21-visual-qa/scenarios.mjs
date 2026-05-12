export const VIEWPORTS = [
  { height: 900, mobile: false, name: 'desktop', width: 1440 },
  { height: 768, mobile: false, name: 'tablet', width: 1024 },
  { height: 844, mobile: false, name: 'mobile', width: 390 }
]

export const DESKTOP_VIEWPORT = VIEWPORTS[0]
export const ALL_VIEWPORTS = ['desktop', 'tablet', 'mobile']

const DESKTOP_ONLY = ['desktop']
const APP_SHELL_CHECKS = ['.base-layout', '.top-bar', '.base-layout__content']

export const ROUTE_SCENARIOS = [
  {
    checks: APP_SHELL_CHECKS,
    id: 'workspace-shell',
    path: '/workspace',
    uiGroup: '首页',
    viewports: ALL_VIEWPORTS,
    viewportChecks: {
      desktop: ['.side-nav', '.context-panel'],
      mobile: ['.mobile-tab-bar'],
      tablet: ['.side-nav', '.context-panel']
    }
  },
  appRoute('mail-inbox', '邮件', '/mail/inbox', ['.mail-surface', '.mail-workspace'], ALL_VIEWPORTS),
  appRoute('calendar-board', '日历', '/calendar', ['.calendar-page', '.calendar-sidebar', '.calendar-board'], ALL_VIEWPORTS),
  {
    checks: [...APP_SHELL_CHECKS, '.drive-surface', '.drive-surface__content'],
    id: 'drive-files',
    path: '/drive',
    uiGroup: '云盘',
    viewports: ALL_VIEWPORTS,
    viewportChecks: {
      desktop: ['.drive-surface__table'],
      mobile: ['.drive-surface__cards'],
      tablet: ['.drive-surface__table']
    }
  },
  appRoute('docs-workspace', '文档', '/docs', ['.docs-shell', '.docs-shell__list'], ALL_VIEWPORTS),
  appRoute('docs-editor', '文档', '/docs/demo-document', ['.docs-editor', '.docs-editor__canvas'], DESKTOP_ONLY),
  appRoute('sheets-workspace', 'Sheets和labs', '/sheets', ['.sheets-workspace', '.data-table', '.sheets-preview'], ALL_VIEWPORTS),
  appRoute('sheets-editor', 'Sheets和labs', '/sheets/demo-sheet', ['.sheets-editor', '.sheets-editor__grid'], DESKTOP_ONLY),
  appRoute('labs-overview', 'Sheets和labs', '/labs', ['.labs-grid'], DESKTOP_ONLY),
  appRoute('pass-vault', 'Pass', '/pass', ['.pass-surface', '.pass-surface__list'], ALL_VIEWPORTS),
  appRoute('pass-secure-links', 'Pass', '/pass/secure-links', ['.pass-surface__detail'], DESKTOP_ONLY),
  appRoute('pass-monitor', 'Pass', '/pass/monitor', ['.pass-monitor__hero', '.pass-monitor__grid'], DESKTOP_ONLY),
  appRoute('collaboration-overview', 'Collaboration', '/collaboration', ['.collaboration-grid'], ALL_VIEWPORTS),
  appRoute('command-center', 'CommandCenter', '/command-center', ['.command-grid', '.terminal-log'], ALL_VIEWPORTS),
  appRoute('notifications', 'Notifications', '/notifications', ['.notifications-layout', '.data-table'], ALL_VIEWPORTS),
  appRoute('admin-overview', 'Admin', '/admin', ['.admin-page', '.admin-grid'], ALL_VIEWPORTS),
  appRoute('admin-users', 'Admin', '/admin/users', ['.admin-page', '.admin-kpis'], DESKTOP_ONLY),
  appRoute('admin-system', 'Admin', '/admin/system', ['.admin-page', '.admin-panel'], DESKTOP_ONLY),
  appRoute('admin-risk', 'Admin', '/admin/risk', ['.admin-page', '.admin-panel'], DESKTOP_ONLY),
  appRoute('settings-overview', 'Setting', '/settings', ['.settings-shell', '.settings-panel'], ALL_VIEWPORTS)
]

export const PUBLIC_BOUNDARY_SCENARIOS = [
  publicRoute('login', '/login', ['.login-screen', '.signin-block']),
  publicRoute('register', '/register', ['.public-shell', '.register-card']),
  publicRoute('boundary', '/boundary', ['.public-surface-frame', '.boundary-page', '.boundary-matrix']),
  publicRoute('product-access-blocked', '/product-access-blocked', ['.public-surface-frame', '.blocked-page', '.premium-gate']),
  publicRoute('share-mail', '/share/mail/demo-token', ['.public-shell', '.share-page']),
  publicRoute('share-drive', '/share/drive/demo-token', ['.public-shell', '.share-drive']),
  publicRoute('share-pass', '/share/pass/demo-token', ['.public-shell', '.share-pass']),
  publicRoute('offline', '/offline', ['.system-state']),
  publicRoute('maintenance', '/maintenance', ['.system-state']),
  publicRoute('not-found', '/404', ['.system-state']),
  publicRoute('server-error', '/500', ['.system-state'])
]

export const OVERLAY_SCENARIOS = [
  overlay('command-palette', '首页', '/workspace', 'clickCommandPalette', ['.command-palette[role="dialog"]']),
  overlay('quick-create', '首页', '/workspace', 'clickQuickCreate', ['.mm-modal[role="dialog"][aria-modal="true"]', '.quick-create-modal']),
  overlay('theme-drawer', '首页', '/workspace', 'clickThemeDrawer', ['.mm-drawer[role="dialog"][aria-modal="true"]', '.theme-drawer']),
  overlay('mail-compose', '邮件', '/mail/compose', 'none', ['.mail-compose', '.mail-compose__panel', '.mail-compose__side']),
  overlay('drive-share-panel', '云盘', '/drive', 'none', ['.drive-surface', '.drive-surface__table']),
  overlay('docs-share-panel', '文档', '/docs/demo-document', 'none', ['.docs-editor__actions', '.docs-editor__panel']),
  overlay('sheets-protected-range', 'Sheets和labs', '/sheets/demo-sheet', 'none', ['.sheets-editor__formula', '.sheets-editor__side']),
  overlay('settings-delete-confirmation', 'Setting', '/settings', 'clickDeleteAccount', ['.mm-modal[role="dialog"][aria-modal="true"]', '.settings-delete-confirmation'])
]

export function allRouteScenarios() {
  return [...ROUTE_SCENARIOS, ...PUBLIC_BOUNDARY_SCENARIOS]
}

export function resolveScenarioChecks(scenario, viewportName) {
  return [...scenario.checks, ...(scenario.viewportChecks?.[viewportName] || [])]
}

export function resolveScenarioViewports(scenario) {
  return VIEWPORTS.filter(viewport => scenario.viewports.includes(viewport.name))
}

function appRoute(id, uiGroup, path, checks, viewports) {
  return { checks: [...APP_SHELL_CHECKS, ...checks], id, path, uiGroup, viewports }
}

function publicRoute(id, path, checks) {
  return { checks, id, path, uiGroup: 'PublicAuthShareSystem', viewports: DESKTOP_ONLY }
}

function overlay(id, uiGroup, path, action, checks) {
  return { action, checks, id, path, uiGroup }
}
