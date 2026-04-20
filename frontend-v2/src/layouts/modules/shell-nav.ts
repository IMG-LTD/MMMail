import { lt, type TextLike } from '@/locales'

type NavTone = 'calendar' | 'docs' | 'drive' | 'governance' | 'labs' | 'mail' | 'neutral' | 'pass' | 'sheets'
type MaturityLevel = 'ga' | 'beta' | 'preview'

export interface NavItem {
  key: string
  label: TextLike
  path: string
  tone: NavTone
  matchPrefixes?: readonly string[]
  maturity?: MaturityLevel
  badge?: TextLike
}

export interface NavGroup {
  title: TextLike
  caption?: TextLike
  items: NavItem[]
}

export interface MailNavItem {
  key: string
  label: TextLike
  icon: string
  path: string
  badge?: TextLike
}

export interface MailNavSection {
  title: TextLike
  items: MailNavItem[]
}

export interface MobilePrimaryTab {
  hint: TextLike
  key: string
  label: TextLike
  matchPrefixes: readonly string[]
  path: string
}

const mailRoutePrefixes = [
  '/archive',
  '/compose',
  '/contacts',
  '/conversations',
  '/drafts',
  '/folders',
  '/inbox',
  '/labels',
  '/outbox',
  '/scheduled',
  '/search',
  '/sent',
  '/snoozed',
  '/spam',
  '/starred',
  '/trash',
  '/unread'
] as const

export const shellNavGroups: NavGroup[] = [
  {
    title: lt('工作区', '工作區', 'Workspace'),
    caption: lt('已认证', '已驗證', 'Authenticated'),
    items: [
      { key: 'mail', label: lt('邮件', '郵件', 'Mail'), path: '/inbox', tone: 'mail', matchPrefixes: mailRoutePrefixes },
      { key: 'calendar', label: lt('日历', '日曆', 'Calendar'), path: '/calendar', tone: 'calendar', matchPrefixes: ['/calendar'] },
      { key: 'drive', label: lt('云盘', '雲端硬碟', 'Drive'), path: '/drive', tone: 'drive', matchPrefixes: ['/drive'] },
      { key: 'pass', label: lt('密码库', '密碼庫', 'Pass'), path: '/pass', tone: 'pass', matchPrefixes: ['/pass', '/pass-monitor'] },
      { key: 'docs', label: lt('文档', '文件', 'Docs'), path: '/docs', tone: 'docs', matchPrefixes: ['/docs'], maturity: 'beta' },
      { key: 'sheets', label: lt('表格', '試算表', 'Sheets'), path: '/sheets', tone: 'sheets', matchPrefixes: ['/sheets'], maturity: 'beta' }
    ]
  },
  {
    title: lt('聚合', '聚合', 'Aggregation'),
    caption: lt('预览聚合面', '預覽聚合面', 'Preview surfaces'),
    items: [
      { key: 'collaboration', label: lt('协作', '協作', 'Collaboration'), path: '/collaboration', tone: 'governance', matchPrefixes: ['/collaboration'], maturity: 'preview' },
      { key: 'command-center', label: lt('指挥中心', '指揮中心', 'Command Center'), path: '/command-center', tone: 'governance', matchPrefixes: ['/command-center'], maturity: 'preview' },
      { key: 'notifications', label: lt('通知', '通知', 'Notifications'), path: '/notifications', tone: 'neutral', matchPrefixes: ['/notifications'], maturity: 'preview' }
    ]
  },
  {
    title: lt('治理', '治理', 'Governance'),
    items: [
      { key: 'suite', label: lt('套件总览', '套件總覽', 'Suite'), path: '/suite', tone: 'governance', matchPrefixes: ['/suite'] },
      { key: 'business', label: lt('业务', '業務', 'Business'), path: '/business', tone: 'governance', matchPrefixes: ['/business'] },
      { key: 'organizations', label: lt('组织', '組織', 'Organizations'), path: '/organizations', tone: 'governance', matchPrefixes: ['/organizations'] },
      { key: 'security', label: lt('安全', '安全', 'Security'), path: '/security', tone: 'neutral', matchPrefixes: ['/security'] },
      { key: 'settings', label: lt('设置', '設定', 'Settings'), path: '/settings', tone: 'neutral', matchPrefixes: ['/settings'] }
    ]
  },
  {
    title: lt('实验', '實驗', 'Research'),
    caption: lt('统一占位与成熟度声明', '統一佔位與成熟度聲明', 'Unified placeholders and maturity notes'),
    items: [
      { key: 'labs', label: lt('Labs', 'Labs', 'Labs'), path: '/labs', tone: 'labs', matchPrefixes: ['/labs'], maturity: 'preview' }
    ]
  }
]

export const mailNavSections: MailNavSection[] = [
  {
    title: lt('工作区', '工作區', 'Workspace'),
    items: [
      { key: 'inbox', label: lt('收件箱', '收件匣', 'Inbox'), icon: '▣', path: '/inbox', badge: '248' },
      { key: 'contacts', label: lt('联系人', '聯絡人', 'Contacts'), icon: '◫', path: '/contacts' },
      { key: 'categories', label: lt('搜索', '搜尋', 'Search'), icon: '⤢', path: '/search' }
    ]
  },
  {
    title: lt('文件夹', '資料夾', 'Folders'),
    items: [
      { key: 'all-mail', label: lt('星标', '已加星號', 'Starred'), icon: '⌂', path: '/starred', badge: '19' },
      { key: 'sent', label: lt('已发送', '已傳送', 'Sent'), icon: '↗', path: '/sent' },
      { key: 'drafts', label: lt('草稿', '草稿', 'Drafts'), icon: '◧', path: '/drafts', badge: '3' },
      { key: 'archive', label: lt('归档', '封存', 'Archive'), icon: '◫', path: '/archive' },
      { key: 'spam', label: lt('垃圾邮件', '垃圾郵件', 'Spam'), icon: '!', path: '/spam', badge: '1' },
      { key: 'trash', label: lt('废纸篓', '垃圾桶', 'Trash'), icon: '✕', path: '/trash' }
    ]
  },
  {
    title: lt('标签', '標籤', 'Labels'),
    items: [
      { key: 'urgent', label: lt('紧急', '緊急', 'Urgent'), icon: '●', path: '/labels/urgent' },
      { key: 'projects', label: lt('项目', '專案', 'Projects'), icon: '●', path: '/labels/projects' }
    ]
  }
]

export const mobilePrimaryTabs: MobilePrimaryTab[] = [
  { key: 'mail', label: lt('邮件', '郵件', 'Mail'), hint: lt('收件箱', '收件匣', 'Inbox'), path: '/inbox', matchPrefixes: mailRoutePrefixes },
  { key: 'calendar', label: lt('日历', '日曆', 'Calendar'), hint: lt('议程', '議程', 'Agenda'), path: '/calendar', matchPrefixes: ['/calendar'] },
  { key: 'drive', label: lt('云盘', '雲端硬碟', 'Drive'), hint: lt('文件', '檔案', 'Files'), path: '/drive', matchPrefixes: ['/drive'] },
  { key: 'pass', label: lt('密码库', '密碼庫', 'Pass'), hint: lt('保险库', '保險庫', 'Vault'), path: '/pass', matchPrefixes: ['/pass'] },
  {
    key: 'more',
    label: lt('更多', '更多', 'More'),
    hint: lt('治理 / Suite', '治理 / Suite', 'Governance / Suite'),
    path: '/suite',
    matchPrefixes: ['/suite', '/docs', '/sheets', '/business', '/organizations', '/security', '/settings', '/collaboration', '/command-center', '/notifications', '/labs']
  }
]

export function isRouteMatch(path: string, item: Pick<NavItem, 'path' | 'matchPrefixes'> | MobilePrimaryTab) {
  if (item.matchPrefixes?.length) {
    return item.matchPrefixes.some(prefix => path.startsWith(prefix))
  }

  return path === item.path
}

export function isMailRoute(path: string) {
  return mailRoutePrefixes.some(prefix => path.startsWith(prefix))
}

export function findShellSurface(path: string) {
  for (const group of shellNavGroups) {
    const match = group.items.find(item => isRouteMatch(path, item))

    if (match) {
      return match
    }
  }

  return null
}

export function getToneColorVar(tone: NavTone) {
  if (tone === 'mail') return 'var(--mm-mail)'
  if (tone === 'calendar') return 'var(--mm-calendar)'
  if (tone === 'docs') return 'var(--mm-docs)'
  if (tone === 'sheets') return 'var(--mm-sheets)'
  if (tone === 'pass') return 'var(--mm-pass)'
  if (tone === 'labs') return 'var(--mm-labs)'
  if (tone === 'drive') return 'var(--mm-drive)'
  if (tone === 'governance') return 'var(--mm-governance)'
  return 'var(--mm-governance)'
}
