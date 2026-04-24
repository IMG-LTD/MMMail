import { lt, type TextLike } from '@/locales'

export interface OnboardingQuickStartStep {
  id: string
  title: TextLike
  description: TextLike
  actionLabel: TextLike
  targetPath: string
}

export const onboardingQuickStartSteps: OnboardingQuickStartStep[] = [
  {
    id: 'suite-overview',
    title: lt('快速开始：了解套件总览', '快速開始：了解套件總覽', 'Quick start: explore the suite overview'),
    description: lt(
      '从 Suite 总览开始，熟悉 MMMail 的核心产品、近期活动和接下来可访问的工作区。',
      '從 Suite 總覽開始，熟悉 MMMail 的核心產品、近期活動和接下來可存取的工作區。',
      'Start with the Suite overview to learn the core MMMail products, recent activity, and the workspaces you can visit next.'
    ),
    actionLabel: lt('打开套件总览', '開啟套件總覽', 'Open suite overview'),
    targetPath: '/suite'
  },
  {
    id: 'mail-inbox',
    title: lt('入门指南：查看收件箱', '入門指南：查看收件匣', 'Getting started: review your inbox'),
    description: lt(
      '进入邮件收件箱，了解安全会话、未读邮件和日常处理入口如何组织。',
      '進入郵件收件匣，了解安全會話、未讀郵件和日常處理入口如何組織。',
      'Visit the mail inbox to see how secure conversations, unread messages, and daily triage are organized.'
    ),
    actionLabel: lt('前往收件箱', '前往收件匣', 'Go to inbox'),
    targetPath: '/inbox'
  },
  {
    id: 'calendar-schedule',
    title: lt('快速开始：安排日历', '快速開始：安排日曆', 'Quick start: plan with calendar'),
    description: lt(
      '打开日历，查看日程、邀请和可用性入口，为团队协作做好准备。',
      '開啟日曆，查看行程、邀請和可用性入口，為團隊協作做好準備。',
      'Open Calendar to review schedules, invitations, and availability so team coordination is ready.'
    ),
    actionLabel: lt('打开日历', '開啟日曆', 'Open calendar'),
    targetPath: '/calendar'
  },
  {
    id: 'drive-files',
    title: lt('开始使用：整理云盘', '開始使用：整理雲端硬碟', 'Get started: organize drive'),
    description: lt(
      '最后查看云盘，了解文件、共享内容和最近协作资料放在哪里。',
      '最後查看雲端硬碟，了解檔案、共享內容和最近協作資料放在哪裡。',
      'Finish in Drive to learn where files, shared content, and recent collaboration materials live.'
    ),
    actionLabel: lt('查看云盘', '查看雲端硬碟', 'View drive'),
    targetPath: '/drive'
  }
]
