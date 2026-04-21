import { lt, type TextLike } from '@/locales'

export interface SurfaceOption {
  key: string
  label: TextLike
  description: TextLike
  badge?: TextLike
}

export interface SurfaceStory {
  key: string
  title: TextLike
  eyebrow: TextLike
  description: TextLike
  body: TextLike[]
  actions: TextLike[]
}

export const mobilePrimaryNav: SurfaceOption[] = [
  { key: 'mail', label: lt('邮件', '郵件', 'Mail'), description: lt('收件箱、写信、联系人与加密会话。', '收件匣、寫信、聯絡人與加密會話。', 'Inbox, compose, contacts, and encrypted conversations.') },
  { key: 'calendar', label: lt('日历', '日曆', 'Calendar'), description: lt('排期、邀请与可用性。', '排程、邀請與可用性。', 'Scheduling, invitations, and availability.') },
  { key: 'drive', label: lt('云盘', '雲端硬碟', 'Drive'), description: lt('文件、共享、上传与恢复提醒。', '檔案、共享、上傳與復原提醒。', 'Files, shares, uploads, and recovery notices.') },
  { key: 'pass', label: lt('密码库', '密碼庫', 'Pass'), description: lt('保险库、安全链接、别名与监控。', '保險庫、安全連結、別名與監控。', 'Vaults, secure links, aliases, and monitor.') },
  { key: 'more', label: lt('更多', '更多', 'More'), description: lt('Suite、管理、设置与预览模块。', 'Suite、管理、設定與預覽模組。', 'Suite, admin, settings, and preview modules.') }
]

export const mailFolderSurfaces: SurfaceOption[] = [
  { key: 'inbox', label: lt('收件箱', '收件匣', 'Inbox'), description: lt('用于主处理的最新安全会话队列。', '用於主處理的最新安全會話佇列。', 'Primary triage queue with the latest secure threads.') },
  { key: 'starred', label: lt('星标', '已加星號', 'Starred'), description: lt('需要后续跟进的置顶会话。', '需要後續跟進的置頂會話。', 'Pinned conversations that require follow-up.') },
  { key: 'snoozed', label: lt('稍后处理', '稍後處理', 'Snoozed'), description: lt('延后到未来提醒窗口的邮件。', '延後到未來提醒視窗的郵件。', 'Deferred mail held until a future reminder window.') },
  { key: 'drafts', label: lt('草稿', '草稿', 'Drafts'), description: lt('草稿会话与待发送加密回复。', '草稿會話與待傳送加密回覆。', 'Draft conversations and pending encrypted replies.') },
  { key: 'scheduled', label: lt('定时发送', '排程傳送', 'Scheduled'), description: lt('等待延时投递的消息。', '等待延遲投遞的訊息。', 'Messages queued for delayed delivery.') },
  { key: 'outbox', label: lt('发件箱', '寄件匣', 'Outbox'), description: lt('待发送、重试与安全投递状态。', '待傳送、重試與安全投遞狀態。', 'Pending sends, retries, and secure delivery state.') },
  { key: 'sent', label: lt('已发送', '已傳送', 'Sent'), description: lt('附带投递证明的已送达会话。', '附帶投遞證明的已送達會話。', 'Delivered conversations with delivery proofs.') },
  { key: 'archive', label: lt('归档', '封存', 'Archive'), description: lt('已完成线程的保留区。', '已完成執行緒的保留區。', 'Completed threads preserved for later reference.') },
  { key: 'spam', label: lt('垃圾邮件', '垃圾郵件', 'Spam'), description: lt('隔离的邮件与可疑投递尝试。', '隔離的郵件與可疑投遞嘗試。', 'Quarantined mail and suspicious delivery attempts.') },
  { key: 'trash', label: lt('废纸篓', '垃圾桶', 'Trash'), description: lt('永久删除前暂存的已删除邮件。', '永久刪除前暫存的已刪除郵件。', 'Deleted mail retained before permanent removal.') },
  { key: 'unread', label: lt('未读', '未讀', 'Unread'), description: lt('跨文件夹筛出的未读会话。', '跨資料夾篩選出的未讀會話。', 'Unread conversations filtered across folders.') }
]

export const driveSections: SurfaceOption[] = [
  { key: 'drive', label: lt('我的云盘', '我的雲端硬碟', 'My Drive'), description: lt('主加密工作区与共享规划文件。', '主要加密工作區與共享規劃檔案。', 'Primary encrypted workspace and shared planning files.') },
  { key: 'drive-shared', label: lt('与我共享', '與我共享', 'Shared with me'), description: lt('来自同事和外部伙伴的共享内容。', '來自同事和外部夥伴的共享內容。', 'Inbound shares from teammates and external partners.') },
  { key: 'drive-recent', label: lt('最近使用', '最近使用', 'Recent'), description: lt('最近打开和协作过的文档。', '最近開啟與協作過的文件。', 'Recently opened and collaborated documents.') },
  { key: 'drive-starred', label: lt('星标', '已加星號', 'Starred'), description: lt('便于快速访问的置顶文件和文件夹。', '便於快速存取的置頂檔案與資料夾。', 'Pinned files and folders for rapid access.') },
  { key: 'drive-trash', label: lt('废纸篓', '垃圾桶', 'Trash'), description: lt('保留用于恢复的已移除资产。', '保留用於復原的已移除資產。', 'Removed assets retained for recovery.') }
]

export const passSections: SurfaceOption[] = [
  { key: 'pass', label: lt('个人保险库', '個人保險庫', 'Personal Vault'), description: lt('主要凭据、卡片、安全笔记与 2FA 代码。', '主要憑證、卡片、安全筆記與 2FA 代碼。', 'Primary credentials, cards, secure notes, and 2FA codes.') },
  { key: 'pass-shared-library', label: lt('共享资料库', '共享資料庫', 'Shared Library'), description: lt('团队保险库与接收的共享项目。', '團隊保險庫與收到的共享項目。', 'Team vaults and inbound shared items.') },
  { key: 'pass-secure-links', label: lt('安全链接', '安全連結', 'Secure Links'), description: lt('受保护链接、二维码导出与访问历史。', '受保護連結、QR 匯出與存取歷史。', 'Protected links, QR exports, and access history.') },
  { key: 'pass-alias-center', label: lt('别名中心', '別名中心', 'Alias Center'), description: lt('邮箱别名、收件路由与发件控制。', '電子郵件別名、收件路由與寄件控制。', 'Email aliases, inbox routing, and sender controls.') },
  { key: 'pass-mailbox', label: lt('收件地址', '收件地址', 'Mailbox'), description: lt('接收地址与默认别名行为。', '接收地址與預設別名行為。', 'Receiving addresses and default alias behavior.') },
  { key: 'pass-business-policy', label: lt('企业策略', '企業策略', 'Business Policy'), description: lt('保险库范围、导出控制与生命周期策略。', '保險庫範圍、匯出控制與生命週期策略。', 'Vault scope, export controls, and lifecycle policy.') }
]

export const suiteSections: SurfaceOption[] = [
  { key: 'overview', label: lt('总览', '總覽', 'Overview'), description: lt('跨产品 Hero、产品卡片与活动总览。', '跨產品 Hero、產品卡片與活動總覽。', 'Cross-product hero, product cards, and activity overview.') },
  { key: 'plans', label: lt('套餐', '方案', 'Plans'), description: lt('套餐目录、定价姿态与能力矩阵。', '方案目錄、定價姿態與能力矩陣。', 'Plan catalog, pricing posture, and capability matrix.') },
  { key: 'billing', label: lt('计费', '計費', 'Billing'), description: lt('发票、报价草稿与托管计费状态。', '發票、報價草稿與代管計費狀態。', 'Invoices, draft quotes, and hosted billing posture.') },
  { key: 'operations', label: lt('运营', '營運', 'Operations'), description: lt('就绪度、治理、整改与指挥状态。', '就緒度、治理、整改與指揮狀態。', 'Readiness, governance, remediation, and command posture.') },
  { key: 'boundary', label: lt('边界', '邊界', 'Boundary'), description: lt('GA/Beta/Preview 支持矩阵与自托管责任。', 'GA/Beta/Preview 支援矩陣與自託管責任。', 'GA/Beta/Preview support matrix and self-host responsibilities.') }
]

export const organizationSections: SurfaceOption[] = [
  { key: 'summary', label: lt('概览', '概覽', 'Summary'), description: lt('组织健康度、近期动作与范围状态。', '組織健康度、近期動作與範圍狀態。', 'Organization health, recent actions, and scope posture.') },
  { key: 'members', label: lt('成员', '成員', 'Members'), description: lt('成员清单、角色与生命周期动作。', '成員清單、角色與生命週期動作。', 'Member inventory, roles, and lifecycle actions.') },
  { key: 'product-access', label: lt('产品访问', '產品存取', 'Product Access'), description: lt('产品访问矩阵与策略继承。', '產品存取矩陣與策略繼承。', 'Product access matrix and policy inheritance.') },
  { key: 'domains', label: lt('域名', '網域', 'Domains'), description: lt('域名验证、MX、SPF、DKIM 与 DMARC 指引。', '網域驗證、MX、SPF、DKIM 與 DMARC 指引。', 'Domain verification, MX, SPF, DKIM, and DMARC guidance.') },
  { key: 'mail-identities', label: lt('邮件身份', '郵件身分', 'Mail Identities'), description: lt('共享签名与发件身份状态。', '共享簽名與寄件身分狀態。', 'Shared signatures and sender identity posture.') },
  { key: 'policy', label: lt('策略', '政策', 'Policy'), description: lt('密码、2FA、导出与安全策略控制。', '密碼、2FA、匯出與安全政策控制。', 'Password, 2FA, export, and security policy controls.') },
  { key: 'monitor', label: lt('监控', '監控', 'Monitor'), description: lt('会话状态、重点告警与风险活动。', '工作階段狀態、重點告警與風險活動。', 'Session posture, notable alerts, and risk activity.') },
  { key: 'session-monitor', label: lt('会话监控', '工作階段監控', 'Session Monitor'), description: lt('当前会话状态、闲置设备与撤销操作。', '目前工作階段狀態、閒置裝置與撤銷操作。', 'Current session state, idle devices, and revocations.') },
  { key: 'audit', label: lt('审计', '稽核', 'Audit'), description: lt('可导出的审计时间线与治理证据。', '可匯出的稽核時間線與治理證據。', 'Exportable audit timeline and governance evidence.') }
]

export const labsModules: SurfaceOption[] = [
  { key: 'authenticator', label: lt('Authenticator', 'Authenticator', 'Authenticator'), description: lt('轻量设备流程的验证器预览模块。', '輕量裝置流程的驗證器預覽模組。', 'Preview authenticator module with lightweight device flows.'), badge: lt('预览', '預覽', 'Preview') },
  { key: 'simplelogin', label: lt('SimpleLogin', 'SimpleLogin', 'SimpleLogin'), description: lt('别名和邮箱预览模块外壳。', '別名與郵箱預覽模組外殼。', 'Alias and mailbox preview module shell.'), badge: lt('预览', '預覽', 'Preview') },
  { key: 'standard-notes', label: lt('Standard Notes', 'Standard Notes', 'Standard Notes'), description: lt('安全笔记预览外壳与能力说明。', '安全筆記預覽外殼與能力說明。', 'Secure notes preview shell and capability statement.'), badge: lt('预览', '預覽', 'Preview') },
  { key: 'vpn', label: lt('VPN', 'VPN', 'VPN'), description: lt('网络保护预览外壳与成熟度声明。', '網路保護預覽外殼與成熟度聲明。', 'Network protection preview shell and maturity disclaimer.'), badge: lt('预览', '預覽', 'Preview') },
  { key: 'meet', label: lt('Meet', 'Meet', 'Meet'), description: lt('会议预览外壳与当前协作边界。', '會議預覽外殼與目前協作邊界。', 'Meeting preview shell and current collaboration scope.'), badge: lt('预览', '預覽', 'Preview') },
  { key: 'wallet', label: lt('Wallet', 'Wallet', 'Wallet'), description: lt('钱包预览外壳与合规边界摘要。', '錢包預覽外殼與合規邊界摘要。', 'Wallet preview shell and compliance boundary summary.'), badge: lt('预览', '預覽', 'Preview') },
  { key: 'lumo', label: lt('Lumo', 'Lumo', 'Lumo'), description: lt('Lumo 预览外壳与实验反馈入口。', 'Lumo 預覽外殼與實驗回饋入口。', 'Lumo preview shell and experiment feedback entry point.'), badge: lt('预览', '預覽', 'Preview') }
]

export const onboardingStories: SurfaceStory[] = [
  {
    key: 'invitation-landing',
    eyebrow: lt('新手引导', '新手引導', 'Onboarding'),
    title: lt('邀请落地页', '邀請落地頁', 'Invitation landing'),
    description: lt('首次触达页面会说明隐私、范围以及邀请为何重要。', '首次觸達頁面會說明隱私、範圍以及邀請為何重要。', 'The first-touch story frames privacy, scope, and the reason the invite matters.'),
    body: [
      lt('新成员会进入一个安静的邀请页，解释 MMMail 将在其工作区中解锁什么能力。', '新成員會進入一個安靜的邀請頁，說明 MMMail 將在其工作區中解鎖哪些能力。', 'A new teammate lands on a quiet invitation page that explains what MMMail unlocks inside their workspace.'),
      lt('主操作是接受邀请，次级说明则解释托管与自托管边界。', '主要操作是接受邀請，次級說明則解釋代管與自託管邊界。', 'The primary call to action is accepting the invitation, while the secondary callout explains hosted versus self-host expectations.')
    ],
    actions: [lt('接受邀请', '接受邀請', 'Accept invitation'), lt('打开边界文档', '開啟邊界文件', 'Open boundary doc')]
  },
  {
    key: 'create-password',
    eyebrow: lt('新手引导', '新手引導', 'Onboarding'),
    title: lt('创建密码并准备隐私恢复', '建立密碼並準備隱私復原', 'Create password and prepare privacy'),
    description: lt('密码设置与恢复包教育合并在一个平静的设置页。', '密碼設定與復原包教育合併在一個平靜的設定頁。', 'Password setup and recovery education are combined into a single calm setup surface.'),
    body: [
      lt('用户创建账户密码，同时看到恢复包为何重要的直接说明。', '使用者建立帳號密碼，同時看到復原包為何重要的直接說明。', 'The user creates the account password and sees a direct explanation of why the recovery kit matters.'),
      lt('辅助面板明确说明：没有用户持有的恢复材料，MMMail 无法恢复历史加密数据。', '輔助面板明確說明：沒有使用者持有的復原材料，MMMail 無法恢復歷史加密資料。', 'A supporting panel explains that historical encrypted data cannot be restored by MMMail without the user-held recovery material.')
    ],
    actions: [lt('设置密码', '設定密碼', 'Set password'), lt('查看恢复说明', '檢視復原說明', 'Review recovery guidance')]
  },
  {
    key: 'inbox-progress',
    eyebrow: lt('新手引导', '新手引導', 'Onboarding'),
    title: lt('收件箱引导进度', '收件匣引導進度', 'Inbox onboarding progress'),
    description: lt('收件箱保留轻量进度条和可隐藏任务抽屉。', '收件匣保留輕量進度條與可隱藏任務抽屜。', 'The inbox carries a lightweight progress rail and a hidden task drawer.'),
    body: [
      lt('收件箱保持可用，同时以 3/5 或 4/5 形式显示进度，而不是强制引导。', '收件匣保持可用，同時以 3/5 或 4/5 形式顯示進度，而不是強制導覽。', 'The inbox stays fully usable while progress remains visible as 3/5 or 4/5 instead of forcing a blocking tour.'),
      lt('任务包括下载恢复包、验证可信设备，以及发送第一封加密邮件。', '任務包括下載復原包、驗證可信裝置，以及傳送第一封加密郵件。', 'Tasks include downloading the recovery kit, validating a trusted device, and sending the first encrypted message.')
    ],
    actions: [lt('继续设置', '繼續設定', 'Continue setup'), lt('隐藏任务抽屉', '隱藏任務抽屜', 'Hide task drawer')]
  },
  {
    key: 'first-compose-e2ee',
    eyebrow: lt('新手引导', '新手引導', 'Onboarding'),
    title: lt('第一次加密写信', '第一次加密寫信', 'First encrypted compose'),
    description: lt('写信界面用绿色信任条解释安全发送路径，而不暴露术语。', '寫信介面用綠色信任條解釋安全傳送路徑，而不暴露術語。', 'The compose surface teaches the green-lock path without exposing implementation jargon.'),
    body: [
      lt('绿色信任条说明只有收件人可以读取消息内容。', '綠色信任條說明只有收件者可以讀取訊息內容。', 'A green trust strip explains that only the recipient can read the message.'),
      lt('辅助卡片用自然语言说明下一步，而不是算法名称或密钥标识。', '輔助卡片用自然語言說明下一步，而不是演算法名稱或金鑰識別。', 'The supporting card gives the next action in natural language instead of algorithm names or key identifiers.')
    ],
    actions: [lt('发送加密邮件', '傳送加密郵件', 'Send encrypted mail'), lt('查看信任详情', '查看信任詳情', 'Review trust details')]
  },
  {
    key: 'recovery-inline-card',
    eyebrow: lt('新手引导', '新手引導', 'Onboarding'),
    title: lt('恢复包内联卡片', '復原包內嵌卡片', 'Recovery inline card'),
    description: lt('低打扰内联卡片保持恢复要求可见，但不阻断日常工作。', '低干擾內嵌卡片保持復原要求可見，但不阻斷日常工作。', 'A quiet inline card keeps the recovery requirement visible without blocking daily work.'),
    body: [
      lt('卡片位于收件箱上方，提醒用户丢失恢复包可能导致历史数据锁定。', '卡片位於收件匣上方，提醒使用者遺失復原包可能導致歷史資料鎖定。', 'The card sits above the inbox list and warns that losing the recovery kit may lock historical data.'),
      lt('它提供明确主操作与温和延期路径，并显示提醒策略。', '它提供明確主要操作與溫和延後路徑，並顯示提醒策略。', 'It offers a clear primary action and a softer defer path with a visible reminder policy.')
    ],
    actions: [lt('下载恢复包', '下載復原包', 'Download recovery kit'), lt('稍后提醒我', '稍後提醒我', 'Remind me later')]
  },
  {
    key: 'recovery-drawer',
    eyebrow: lt('新手引导', '新手引導', 'Onboarding'),
    title: lt('恢复包抽屉', '復原包抽屜', 'Recovery drawer'),
    description: lt('抽屉解释离线保存、下载确认以及它为何重要。', '抽屜解釋離線保存、下載確認以及它為何重要。', 'The drawer explains offline storage, download confirmation, and why it matters.'),
    body: [
      lt('抽屉使用清晰语言，不使用恐惧型暗黑模式，但仍明确后果。', '抽屜使用清楚語言，不使用恐懼型暗黑模式，但仍明確後果。', 'The drawer uses clear language and avoids fear-based dark patterns while still making the consequence explicit.'),
      lt('在用户关闭任务前，包含一份确认清单。', '在使用者關閉任務前，包含一份確認清單。', 'It includes a confirmation checklist before the user dismisses the task.')
    ],
    actions: [lt('下载文件', '下載檔案', 'Download file'), lt('我已安全保存', '我已安全保存', 'I stored it safely')]
  },
  {
    key: 'cross-device-validation',
    eyebrow: lt('新手引导', '新手引導', 'Onboarding'),
    title: lt('跨设备验证对话框', '跨裝置驗證對話框', 'Cross-device validation dialog'),
    description: lt('引导中唯一的硬阻断点，用于在访问历史数据前验证新设备。', '引導中唯一的硬阻斷點，用於在存取歷史資料前驗證新裝置。', 'The only hard stop in onboarding validates a newly trusted device before historical data access.'),
    body: [
      lt('对话框解释新设备必须由现有可信设备批准，或走管理员恢复流程。', '對話框解釋新裝置必須由現有可信裝置批准，或走管理員復原流程。', 'The dialog explains that the new device must be approved from an existing trusted device or by an administrator recovery flow.'),
      lt('它提供查看可用设备与请求帮助的直接路径。', '它提供檢視可用裝置與請求協助的直接路徑。', 'It provides a direct path to review available devices and request help.')
    ],
    actions: [lt('验证此设备', '驗證此裝置', 'Validate this device'), lt('请求管理员恢复', '請求管理員復原', 'Request administrator recovery')]
  },
  {
    key: 'retention-banners',
    eyebrow: lt('新手引导', '新手引導', 'Onboarding'),
    title: lt('留存横幅', '留存橫幅', 'Retention banners'),
    description: lt('只有在恢复包仍未保存时，T+24h、T+3d 与 T+7d 横幅才逐级升级。', '只有在復原包仍未保存時，T+24h、T+3d 與 T+7d 橫幅才逐級升級。', 'T+24h, T+3d, and T+7d banners escalate copy only when the recovery kit still has not been saved.'),
    body: [
      lt('第一条横幅为蓝色教育态，第二条为黄色警示态，第三条仅在高风险动作前变红。', '第一條橫幅為藍色教育態，第二條為黃色警示態，第三條僅在高風險動作前變紅。', 'The first banner is blue and educational, the second is yellow and cautionary, and the third becomes red only before a risky action.'),
      lt('升级是情境化的，而非全局惩罚式。', '升級是情境化的，而非全域懲罰式。', 'The escalation is contextual rather than globally punitive.')
    ],
    actions: [lt('查看提醒', '查看提醒', 'Review reminders'), lt('调整提醒时间', '調整提醒時間', 'Adjust timing')]
  },
  {
    key: 'task-drawer',
    eyebrow: lt('新手引导', '新手引導', 'Onboarding'),
    title: lt('隐藏任务抽屉', '隱藏任務抽屜', 'Hidden task drawer'),
    description: lt('可关闭抽屉展示完整引导清单，而不会劫持主流程。', '可關閉抽屜展示完整引導清單，而不會劫持主流程。', 'A dismissible drawer reveals the full onboarding checklist without hijacking the main workflow.'),
    body: [
      lt('抽屉将设置任务、已完成任务与支持链接集中到同一处。', '抽屜將設定任務、已完成任務與支援連結集中到同一處。', 'The drawer groups setup tasks, completed tasks, and support links in one place.'),
      lt('用户可随时关闭，并稍后从进度入口返回。', '使用者可隨時關閉，並稍後從進度入口返回。', 'The user can close it at any time and return later from the progress affordance.')
    ],
    actions: [lt('展开任务', '展開任務', 'Expand tasks'), lt('继续工作', '繼續工作', 'Keep working')]
  }
]

export const failureModes: SurfaceStory[] = [
  {
    key: 'f01',
    eyebrow: lt('失败模式', '失敗模式', 'Failure Mode'),
    title: lt('F01 邮件无法解密', 'F01 郵件無法解密', 'F01 Mail decrypt unavailable'),
    description: lt('当前设备无法解密历史邮件。', '目前裝置無法解密歷史郵件。', 'Historical mail cannot be decrypted on the current device.'),
    body: [
      lt('界面会解释发生了什么、意味着什么，以及最快的恢复动作。', '介面會解釋發生了什麼、意味著什麼，以及最快的復原動作。', 'The UI explains what happened, what it means, and the fastest recovery action.'),
      lt('主恢复操作是导入恢复包，次级操作则保留当前列表状态。', '主要復原操作是匯入復原包，次級操作則保留目前清單狀態。', 'Primary recovery goes to importing a recovery kit, while a secondary action preserves access to the current list state.')
    ],
    actions: [lt('导入恢复包', '匯入復原包', 'Import recovery kit'), lt('查看帮助文章', '查看說明文章', 'Review help article')]
  },
  {
    key: 'f03',
    eyebrow: lt('失败模式', '失敗模式', 'Failure Mode'),
    title: lt('F03 SMTP 降级投递', 'F03 SMTP 降級投遞', 'F03 SMTP delivery fallback'),
    description: lt('收件人尚未准备好完整 E2EE，因此改走安全密码流程。', '收件者尚未準備好完整 E2EE，因此改走安全密碼流程。', 'A recipient is not ready for full E2EE, so delivery falls back to a secure password flow.'),
    body: [
      lt('写信体验保持可读，重点解释投递结果而非协议细节。', '寫信體驗保持可讀，重點解釋投遞結果而非協定細節。', 'The compose experience stays readable and focuses on the delivery outcome rather than protocol details.'),
      lt('辅助操作说明收件人的密码访问流程。', '輔助操作說明收件者的密碼存取流程。', 'A supporting action explains how the password flow works for the recipient.')
    ],
    actions: [lt('设置投递密码', '設定投遞密碼', 'Set delivery password'), lt('查看收件人步骤', '查看收件者步驟', 'See recipient steps')]
  },
  {
    key: 'f05',
    eyebrow: lt('失败模式', '失敗模式', 'Failure Mode'),
    title: lt('F05 签名不匹配', 'F05 簽章不相符', 'F05 Signature mismatch'),
    description: lt('当前消息的完整性无法被完全验证。', '目前訊息的完整性無法被完全驗證。', 'The integrity of the current message cannot be fully verified.'),
    body: [
      lt('内容会降级显示，附件动作被禁用，直到用户查看告警。', '內容會降級顯示，附件動作被停用，直到使用者查看告警。', 'The content is visually degraded and attachment actions are disabled until the user reviews the warning.'),
      lt('恢复路径提供上报、验证细节与来源检查。', '復原路徑提供回報、驗證細節與來源檢查。', 'The recovery path offers reporting, verification details, and source review.')
    ],
    actions: [lt('查看技术详情', '查看技術詳情', 'View technical details'), lt('上报此消息', '回報此訊息', 'Report this message')]
  },
  {
    key: 'f07',
    eyebrow: lt('失败模式', '失敗模式', 'Failure Mode'),
    title: lt('F07 云盘额度已满', 'F07 雲端硬碟額度已滿', 'F07 Drive quota full'),
    description: lt('云盘已达到当前配额，继续上传前必须清理或升级。', '雲端硬碟已達到目前配額，繼續上傳前必須清理或升級。', 'Drive has reached the current quota and must be cleaned or upgraded before more uploads continue.'),
    body: [
      lt('横幅会在文件上下文中原位出现。', '橫幅會在檔案內容脈絡中原位出現。', 'The banner appears in-place and keeps the file context visible.'),
      lt('操作优先给出清理建议与套餐查看入口。', '操作優先給出清理建議與方案檢視入口。', 'Actions prioritize cleanup suggestions and plan review.')
    ],
    actions: [lt('打开清理指引', '開啟清理指引', 'Open cleanup guide'), lt('查看套餐', '查看方案', 'Review plans')]
  },
  {
    key: 'f09',
    eyebrow: lt('失败模式', '失敗模式', 'Failure Mode'),
    title: lt('F09 会话中断', 'F09 工作階段中斷', 'F09 Session interruption'),
    description: lt('软认证中断会暂停安全动作，但不会丢失整个页面状态。', '軟性驗證中斷會暫停安全動作，但不會遺失整個頁面狀態。', 'A soft authentication interruption pauses secure actions without dropping the whole page state.'),
    body: [
      lt('界面会锁住动作上下文，并在信任恢复后允许继续。', '介面會鎖住動作內容脈絡，並在信任恢復後允許繼續。', 'The UI locks the action context and lets the user resume once trust is restored.'),
      lt('被打断的请求会在解锁后自动重放。', '被打斷的請求會在解鎖後自動重播。', 'Requests that were interrupted are replayed after unlock.')
    ],
    actions: [lt('解锁并继续', '解鎖並繼續', 'Unlock and resume'), lt('查看可信设备', '查看可信裝置', 'Review trusted devices')]
  },
  {
    key: 'f12',
    eyebrow: lt('失败模式', '失敗模式', 'Failure Mode'),
    title: lt('F12 设置保存失败', 'F12 設定儲存失敗', 'F12 Settings save failed'),
    description: lt('某个设置分组无法保存，但失败被局部限制在该面板内。', '某個設定分組無法儲存，但失敗被局部限制在該面板內。', 'A scoped settings block could not be saved and the failure is localized to that panel.'),
    body: [
      lt('只有受影响面板进入错误态，其余工作区仍保持可用。', '只有受影響面板進入錯誤態，其餘工作區仍保持可用。', 'Only the affected panel enters error state while the rest of the workspace remains usable.'),
      lt('重试与回滚动作会直接在面板内显示。', '重試與還原動作會直接在面板內顯示。', 'Retry and rollback actions are visible inline.')
    ],
    actions: [lt('重试保存', '重試儲存', 'Retry save'), lt('放弃更改', '捨棄變更', 'Discard changes')]
  },
  {
    key: 'f14',
    eyebrow: lt('失败模式', '失敗模式', 'Failure Mode'),
    title: lt('F14 协作冲突', 'F14 協作衝突', 'F14 Collaboration conflict'),
    description: lt('协作 Beta 界面同时存在远程更改和本地更改。', '協作 Beta 介面同時存在遠端變更與本機變更。', 'Remote changes and local changes are both present in a collaborative beta surface.'),
    body: [
      lt('冲突横幅提供查看远端和保留本地两个动作。', '衝突橫幅提供檢視遠端與保留本機兩個動作。', 'A conflict banner offers view-remote and keep-mine actions.'),
      lt('界面会在覆盖前先明确说明状态保留策略。', '介面會在覆寫前先明確說明狀態保留策略。', 'The surface makes state preservation explicit before any override occurs.')
    ],
    actions: [lt('查看远端更新', '查看遠端更新', 'View remote update'), lt('保留我的更改', '保留我的變更', 'Keep my changes')]
  },
  {
    key: 'f16',
    eyebrow: lt('失败模式', '失敗模式', 'Failure Mode'),
    title: lt('F16 命令索引暂停', 'F16 命令索引暫停', 'F16 Command index paused'),
    description: lt('本地搜索索引仍不完整或已暂停。', '本機搜尋索引仍不完整或已暫停。', 'The local search index is still incomplete or paused.'),
    body: [
      lt('命令面板仍可使用，并明确提示结果可能不完整。', '命令面板仍可使用，並明確提示結果可能不完整。', 'The command palette remains usable and clearly states that results may be partial.'),
      lt('设置中提供重新构建索引的动作。', '設定中提供重新建立索引的動作。', 'A rebuild action is available from settings.')
    ],
    actions: [lt('重建本地索引', '重建本機索引', 'Rebuild local index'), lt('打开设置', '開啟設定', 'Open settings')]
  },
  {
    key: 'f17',
    eyebrow: lt('失败模式', '失敗模式', 'Failure Mode'),
    title: lt('F17 预览能力不可用', 'F17 預覽能力不可用', 'F17 Preview capability unavailable'),
    description: lt('当前范围中可见的预览模块或托管专属界面暂不可用。', '目前範圍中可見的預覽模組或代管專屬介面暫不可用。', 'A preview module or hosted-only surface is visible but currently unavailable in the current scope.'),
    body: [
      lt('页面不会形成死路，而是引导用户返回已启用界面并理解当前成熟度。', '頁面不會形成死路，而是引導使用者返回已啟用介面並理解目前成熟度。', 'The page avoids dead ends by linking back to enabled surfaces and current maturity expectations.'),
      lt('它会明确说明限制来自范围还是成熟度。', '它會明確說明限制來自範圍還是成熟度。', 'It clearly states whether the limitation is scope-based or maturity-based.')
    ],
    actions: [lt('返回已启用产品', '返回已啟用產品', 'Return to enabled products'), lt('打开边界矩阵', '開啟邊界矩陣', 'Open boundary matrix')]
  },
  {
    key: 'f21',
    eyebrow: lt('失败模式', '失敗模式', 'Failure Mode'),
    title: lt('F21 管理员辅助恢复', 'F21 管理員協助復原', 'F21 Administrator-assisted recovery'),
    description: lt('原始可信设备丢失，需要管理员辅助恢复令牌。', '原始可信裝置遺失，需要管理員協助復原權杖。', 'The original trusted device is lost and an administrator-assisted recovery token is required.'),
    body: [
      lt('页面解释令牌如何工作、有效期多久、在哪里批准。', '頁面解釋權杖如何運作、有效期多久、在哪裡批准。', 'The page explains how the token works, how long it remains valid, and where the request is approved.'),
      lt('它通过明确临时性来保持信任模型完整。', '它透過明確臨時性來保持信任模型完整。', 'It preserves the trust model by making the temporary nature of the token explicit.')
    ],
    actions: [lt('申请恢复令牌', '申請復原權杖', 'Request recovery token'), lt('查看组织安全策略', '查看組織安全政策', 'Review organization security')]
  }
]

export function findSurface<T extends SurfaceOption | SurfaceStory>(items: T[], key: string, fallbackKey?: string) {
  return items.find(item => item.key === key) ?? items.find(item => item.key === fallbackKey) ?? items[0]
}
