import type { RouteRecordRaw } from 'vue-router'
import { redirectRegistry } from './redirect-registry'

const BaseLayoutMeta = { layout: 'base' as const }
const BlankLayoutMeta = { layout: 'blank' as const }
const FlushWorkspaceMeta = { ...BaseLayoutMeta, contentMode: 'flush' as const, section: 'workspace' }

const MailSurfaceView = () => import('@/views/app/MailSurfaceView.vue')
const DriveSectionView = () => import('@/views/app/DriveSectionView.vue')
const PassSectionView = () => import('@/views/app/PassSectionView.vue')
const SuiteSectionView = () => import('@/views/app/SuiteSectionView.vue')
const OrganizationsSectionView = () => import('@/views/app/OrganizationsSectionView.vue')
const DocsEditorView = () => import('@/views/app/DocsEditorView.vue')
const SheetsEditorView = () => import('@/views/app/SheetsEditorView.vue')
const LabsModuleView = () => import('@/views/app/LabsModuleView.vue')
const PassMonitorView = () => import('@/views/app/PassMonitorView.vue')
const PublicPassShareView = () => import('@/views/public/PublicPassShareView.vue')
const StorySurfaceView = () => import('@/views/public/StorySurfaceView.vue')

const publicRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/suite'
  },
  {
    path: '/login',
    component: () => import('@/views/public/LoginView.vue'),
    meta: BlankLayoutMeta
  },
  {
    path: '/register',
    component: () => import('@/views/public/RegisterView.vue'),
    meta: BlankLayoutMeta
  },
  {
    path: '/boundary',
    component: () => import('@/views/public/BoundaryView.vue'),
    meta: BlankLayoutMeta
  },
  {
    path: '/product-access-blocked',
    component: () => import('@/views/public/ProductAccessBlockedView.vue'),
    meta: BlankLayoutMeta
  },
  {
    path: '/share/mail/:token',
    component: () => import('@/views/public/PublicMailShareView.vue'),
    meta: BlankLayoutMeta
  },
  {
    path: '/share/drive/:token',
    component: () => import('@/views/public/PublicDriveShareView.vue'),
    meta: BlankLayoutMeta
  },
  {
    path: '/public/drive/shares/:token',
    redirect: to => `/share/drive/${String(to.params.token || '')}`
  },
  {
    path: '/share/pass/:token',
    component: PublicPassShareView,
    meta: BlankLayoutMeta
  },
  {
    path: '/onboarding/:storyKey',
    component: StorySurfaceView,
    meta: { ...BlankLayoutMeta, storyGroup: 'onboarding' }
  },
  {
    path: '/failure-modes',
    component: StorySurfaceView,
    meta: { ...BlankLayoutMeta, storyGroup: 'failure', storyKey: 'f01' }
  },
  {
    path: '/failure-modes/:storyKey',
    component: StorySurfaceView,
    meta: { ...BlankLayoutMeta, storyGroup: 'failure' }
  },
  {
    path: '/404',
    component: () => import('@/views/public/System404View.vue'),
    meta: BlankLayoutMeta
  },
  {
    path: '/500',
    component: () => import('@/views/public/System500View.vue'),
    meta: BlankLayoutMeta
  },
  {
    path: '/offline',
    component: () => import('@/views/public/SystemOfflineView.vue'),
    meta: BlankLayoutMeta
  },
  {
    path: '/maintenance',
    component: () => import('@/views/public/SystemMaintenanceView.vue'),
    meta: BlankLayoutMeta
  }
]

const mailRoutes: RouteRecordRaw[] = [
  ...[
    { path: '/inbox', surfaceKey: 'inbox', label: 'Mail' },
    { path: '/starred', surfaceKey: 'starred', label: 'Mail' },
    { path: '/snoozed', surfaceKey: 'snoozed', label: 'Mail' },
    { path: '/drafts', surfaceKey: 'drafts', label: 'Mail' },
    { path: '/scheduled', surfaceKey: 'scheduled', label: 'Mail' },
    { path: '/outbox', surfaceKey: 'outbox', label: 'Mail' },
    { path: '/sent', surfaceKey: 'sent', label: 'Mail' },
    { path: '/archive', surfaceKey: 'archive', label: 'Mail' },
    { path: '/spam', surfaceKey: 'spam', label: 'Mail' },
    { path: '/trash', surfaceKey: 'trash', label: 'Mail' },
    { path: '/unread', surfaceKey: 'unread', label: 'Mail' },
    { path: '/contacts', surfaceKey: 'contacts', label: 'Mail' },
    { path: '/search', surfaceKey: 'search', label: 'Mail' },
    { path: '/compose', surfaceKey: 'compose', label: 'Mail' },
    { path: '/conversations/:id', surfaceKey: 'conversation', label: 'Mail' },
    { path: '/folders/:id', surfaceKey: 'archive', label: 'Mail' },
    { path: '/labels/:id', surfaceKey: 'starred', label: 'Mail' }
  ].map(item => ({
    path: item.path,
    component: MailSurfaceView,
    meta: {
      ...FlushWorkspaceMeta,
      label: item.label,
      surfaceKey: item.surfaceKey
    }
  })),
  {
    path: '/mail/:id',
    redirect: to => `/conversations/${String(to.params.id || '')}`
  }
]

// Folder detail compatibility is tracked in sameShapeCompatibilityRoutes because
// legacy and canonical URLs share the same Vue Router path shape.

const workspaceRoutes: RouteRecordRaw[] = [
  {
    path: '/calendar',
    component: () => import('@/views/app/CalendarView.vue'),
    meta: { ...FlushWorkspaceMeta, label: 'Calendar' }
  },
  {
    path: '/drive',
    component: DriveSectionView,
    meta: { ...FlushWorkspaceMeta, label: 'Drive', surfaceKey: 'drive' }
  },
  {
    path: '/drive/shared',
    component: DriveSectionView,
    meta: { ...FlushWorkspaceMeta, label: 'Drive', surfaceKey: 'drive-shared' }
  },
  {
    path: '/drive/recent',
    component: DriveSectionView,
    meta: { ...FlushWorkspaceMeta, label: 'Drive', surfaceKey: 'drive-recent' }
  },
  {
    path: '/drive/starred',
    component: DriveSectionView,
    meta: { ...FlushWorkspaceMeta, label: 'Drive', surfaceKey: 'drive-starred' }
  },
  {
    path: '/drive/trash',
    component: DriveSectionView,
    meta: { ...FlushWorkspaceMeta, label: 'Drive', surfaceKey: 'drive-trash' }
  },
  {
    path: '/pass',
    component: PassSectionView,
    meta: { ...FlushWorkspaceMeta, label: 'Pass', surfaceKey: 'pass' }
  },
  {
    path: '/pass/shared-library',
    component: PassSectionView,
    meta: { ...FlushWorkspaceMeta, label: 'Pass', surfaceKey: 'pass-shared-library' }
  },
  {
    path: '/pass/secure-links',
    component: PassSectionView,
    meta: { ...FlushWorkspaceMeta, label: 'Pass', surfaceKey: 'pass-secure-links' }
  },
  {
    path: '/pass/alias-center',
    component: PassSectionView,
    meta: { ...FlushWorkspaceMeta, label: 'Pass', surfaceKey: 'pass-alias-center' }
  },
  {
    path: '/pass/mailbox',
    component: PassSectionView,
    meta: { ...FlushWorkspaceMeta, label: 'Pass', surfaceKey: 'pass-mailbox' }
  },
  {
    path: '/pass/business-policy',
    component: PassSectionView,
    meta: { ...FlushWorkspaceMeta, label: 'Pass', surfaceKey: 'pass-business-policy' }
  },
  {
    path: '/pass/monitor',
    component: PassMonitorView,
    meta: { ...FlushWorkspaceMeta, label: 'Pass', surfaceKey: 'pass-monitor' }
  },
  {
    path: '/docs',
    component: () => import('@/views/app/DocsWorkspaceView.vue'),
    meta: { ...BaseLayoutMeta, section: 'workspace', label: 'Docs' }
  },
  {
    path: '/docs/:id',
    component: DocsEditorView,
    meta: { ...BaseLayoutMeta, section: 'workspace', label: 'Docs' }
  },
  {
    path: '/sheets',
    component: () => import('@/views/app/SheetsWorkspaceView.vue'),
    meta: { ...BaseLayoutMeta, section: 'workspace', label: 'Sheets' }
  },
  {
    path: '/sheets/:id',
    component: SheetsEditorView,
    meta: { ...BaseLayoutMeta, section: 'workspace', label: 'Sheets' }
  }
]

const aggregationRoutes: RouteRecordRaw[] = [
  {
    path: '/collaboration',
    component: () => import('@/views/app/CollaborationView.vue'),
    meta: { ...BaseLayoutMeta, section: 'aggregation', label: 'Collaboration' }
  },
  {
    path: '/command-center',
    component: () => import('@/views/app/CommandCenterView.vue'),
    meta: { ...BaseLayoutMeta, section: 'aggregation', label: 'Command Center' }
  },
  {
    path: '/notifications',
    component: () => import('@/views/app/NotificationsView.vue'),
    meta: { ...BaseLayoutMeta, section: 'aggregation', label: 'Notifications' }
  }
]

const governanceRoutes: RouteRecordRaw[] = [
  {
    path: '/suite',
    component: SuiteSectionView,
    meta: { ...BaseLayoutMeta, section: 'governance', label: 'Suite', surfaceKey: 'overview' }
  },
  {
    path: '/suite/plans',
    component: SuiteSectionView,
    meta: { ...BaseLayoutMeta, section: 'governance', label: 'Suite', surfaceKey: 'plans' }
  },
  {
    path: '/suite/billing',
    component: SuiteSectionView,
    meta: { ...BaseLayoutMeta, section: 'governance', label: 'Suite', surfaceKey: 'billing' }
  },
  {
    path: '/suite/operations',
    component: SuiteSectionView,
    meta: { ...BaseLayoutMeta, section: 'governance', label: 'Suite', surfaceKey: 'operations' }
  },
  {
    path: '/suite/boundary',
    component: SuiteSectionView,
    meta: { ...BaseLayoutMeta, section: 'governance', label: 'Suite', surfaceKey: 'boundary' }
  },
  {
    path: '/business',
    component: () => import('@/views/app/BusinessOverviewView.vue'),
    meta: { ...BaseLayoutMeta, section: 'governance', label: 'Business' }
  },
  {
    path: '/organizations',
    component: OrganizationsSectionView,
    meta: { ...BaseLayoutMeta, section: 'governance', label: 'Organizations', surfaceKey: 'summary' }
  },
  {
    path: '/organizations/members',
    component: OrganizationsSectionView,
    meta: { ...BaseLayoutMeta, section: 'governance', label: 'Organizations', surfaceKey: 'members' }
  },
  {
    path: '/organizations/product-access',
    component: OrganizationsSectionView,
    meta: { ...BaseLayoutMeta, section: 'governance', label: 'Organizations', surfaceKey: 'product-access' }
  },
  {
    path: '/organizations/domains',
    component: OrganizationsSectionView,
    meta: { ...BaseLayoutMeta, section: 'governance', label: 'Organizations', surfaceKey: 'domains' }
  },
  {
    path: '/organizations/mail-identities',
    component: OrganizationsSectionView,
    meta: { ...BaseLayoutMeta, section: 'governance', label: 'Organizations', surfaceKey: 'mail-identities' }
  },
  {
    path: '/organizations/policy',
    component: OrganizationsSectionView,
    meta: { ...BaseLayoutMeta, section: 'governance', label: 'Organizations', surfaceKey: 'policy' }
  },
  {
    path: '/organizations/monitor',
    component: OrganizationsSectionView,
    meta: { ...BaseLayoutMeta, section: 'governance', label: 'Organizations', surfaceKey: 'monitor' }
  },
  {
    path: '/organizations/session-monitor',
    component: OrganizationsSectionView,
    meta: { ...BaseLayoutMeta, section: 'governance', label: 'Organizations', surfaceKey: 'session-monitor' }
  },
  {
    path: '/organizations/audit',
    component: OrganizationsSectionView,
    meta: { ...BaseLayoutMeta, section: 'governance', label: 'Organizations', surfaceKey: 'audit' }
  },
  {
    path: '/security',
    component: () => import('@/views/app/SecurityCenterView.vue'),
    meta: { ...BaseLayoutMeta, section: 'governance', label: 'Security' }
  },
  {
    path: '/settings',
    component: () => import('@/views/app/SettingsWorkspaceView.vue'),
    meta: { ...BaseLayoutMeta, section: 'governance', label: 'Settings' }
  }
]

const researchRoutes: RouteRecordRaw[] = [
  {
    path: '/labs',
    component: () => import('@/views/app/LabsOverviewView.vue'),
    meta: { ...BaseLayoutMeta, section: 'research', label: 'Labs' }
  },
  {
    path: '/labs/:moduleKey',
    component: LabsModuleView,
    meta: { ...BaseLayoutMeta, section: 'research', label: 'Labs' }
  }
]

// Redirect compatibility routes are generated from redirectRegistry.
const redirectRoutes: RouteRecordRaw[] = redirectRegistry.map(rule => ({
  path: rule.from,
  redirect: rule.to
}))

export const routes: RouteRecordRaw[] = [
  ...publicRoutes,
  ...mailRoutes,
  ...workspaceRoutes,
  ...aggregationRoutes,
  ...governanceRoutes,
  ...researchRoutes,
  ...redirectRoutes,
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  }
]
