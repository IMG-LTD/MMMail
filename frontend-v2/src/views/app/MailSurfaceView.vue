<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { lt, type TextLike, useLocaleText } from '@/locales'
import { useCopilotPanel } from '@/shared/composables/useCopilotPanel'
import { findSurface, mailFolderSurfaces, type SurfaceOption } from '@/shared/content/route-surfaces'

const route = useRoute()
const router = useRouter()
const { tr } = useLocaleText()
const copilotPanel = useCopilotPanel()
const copilotOpen = copilotPanel.open

onMounted(() => {
  void copilotPanel.loadCapabilities()
})

const messages = [
  {
    id: 'secure-ops',
    sender: lt('安全运营', '安全營運', 'Secure Operations'),
    subject: lt('凤凰项目：最终资产交付', '鳳凰專案：最終資產交付', 'Project Phoenix: Final Asset Transfer'),
    preview: lt('加密载荷已验证。请在最终同步前查看附件。', '加密負載已驗證。請在最終同步前查看附件。', 'Encrypted payload verified. Review attachments before the final sync.'),
    time: '10:42 AM',
    badge: lt('已验证', '已驗證', 'Verified')
  },
  {
    id: 'amir-horne',
    sender: 'Dr. Amir Horne',
    subject: lt('Q3 审计报告 - 需要处理', 'Q3 稽核報告 - 需要處理', 'Q3 Audit Reports - Action Required'),
    preview: lt('预发布环境仍有少量差异，需完成签核。', '預發佈環境仍有少量差異，需完成簽核。', 'A few discrepancies remain in staging and require sign-off.'),
    time: '09:41 AM',
    badge: lt('未读', '未讀', 'Unread')
  },
  {
    id: 'system-alerts',
    sender: lt('系统告警', '系統告警', 'System Alerts'),
    subject: lt('每周安全摘要', '每週安全摘要', 'Weekly Security Digest'),
    preview: lt('当前工作区未检测到未授权访问尝试。', '目前工作區未偵測到未授權存取嘗試。', 'No unauthorized access attempts detected in the current workspace.'),
    time: lt('昨天', '昨天', 'Yesterday'),
    badge: lt('摘要', '摘要', 'Digest')
  }
]

interface ContactCard {
  name: string
  team: TextLike
  email: string
}

const contacts: ContactCard[] = [
  { name: 'Elena Rostova', team: lt('设计系统', '設計系統', 'Design Systems'), email: 'elena@phoenix-secure.com' },
  { name: 'Marcus Lin', team: lt('运营', '營運', 'Operations'), email: 'marcus@phoenix-secure.com' },
  { name: 'Priya Sharma', team: lt('安全', '安全', 'Security'), email: 'priya@phoenix-secure.com' }
]

const searchResults = [
  { id: 'mail', group: lt('邮件', '郵件', 'Mail'), title: lt('凤凰项目发布清单', '鳳凰專案發佈清單', 'Phoenix rollout checklist'), note: lt('在主题与附件元数据中找到', '在主旨與附件中繼資料中找到', 'Found in subject and attachment metadata') },
  { id: 'files', group: lt('文件', '檔案', 'Files'), title: lt('节点拓扑图', '節點拓撲圖', 'Node topology map'), note: lt('与当前会话关联的云盘结果', '與目前會話關聯的雲端硬碟結果', 'Drive result linked from the active thread') },
  { id: 'people', group: lt('人员', '人員', 'People'), title: 'Amir Horne', note: lt('最近的发件人与审阅人', '最近的寄件者與審閱者', 'Recent sender and reviewer') }
]

const localNav = computed<SurfaceOption[]>(() => {
  return [
    ...mailFolderSurfaces.slice(0, 4),
    { key: 'contacts', label: lt('联系人', '聯絡人', 'Contacts'), description: lt('可信发件人与通讯录卡片。', '可信寄件者與通訊錄卡片。', 'Trusted senders and directory cards.') },
    { key: 'search', label: lt('搜索', '搜尋', 'Search'), description: lt('已保存与高级搜索视图。', '已儲存與進階搜尋檢視。', 'Saved and advanced search views.') },
    { key: 'compose', label: lt('写邮件', '寫郵件', 'Compose'), description: lt('展开式写信界面。', '展開式寫信介面。', 'Expanded compose surface.') }
  ]
})

const surfaceKey = computed(() => {
  return String(route.meta.surfaceKey ?? 'inbox')
})

const surface = computed(() => {
  return findSurface(localNav.value, surfaceKey.value, 'inbox')
})

const isCompose = computed(() => surfaceKey.value === 'compose')
const isContacts = computed(() => surfaceKey.value === 'contacts')
const isSearch = computed(() => surfaceKey.value === 'search')
const isConversation = computed(() => surfaceKey.value === 'conversation')
const isThreadSurface = computed(() => !isCompose.value && !isContacts.value && !isSearch.value)

function openConversation() {
  router.push('/conversations/phoenix')
}

function openSurface(item: SurfaceOption) {
  const pathMap: Record<string, string> = {
    archive: '/archive',
    compose: '/compose',
    contacts: '/contacts',
    drafts: '/drafts',
    inbox: '/inbox',
    search: '/search',
    scheduled: '/scheduled',
    snoozed: '/snoozed',
    starred: '/starred'
  }
  router.push(pathMap[item.key] ?? '/inbox')
}
</script>

<template>
  <section
    class="mail-surface"
    :class="{
      'mail-surface--conversation': isConversation,
      'mail-surface--thread': isThreadSurface && !isConversation
    }"
  >
    <header class="mail-surface__toolbar">
      <div class="mail-surface__toolbar-left">
        <strong>{{ tr(surface.label) }}</strong>
        <span>{{ tr(surface.description) }}</span>
      </div>
      <div class="mail-surface__toolbar-right">
        <button type="button">{{ tr(lt('全部', '全部', 'All')) }}</button>
        <button type="button">{{ tr(lt('未读', '未讀', 'Unread')) }}</button>
        <button type="button">{{ tr(lt('排序', '排序', 'Sort')) }}</button>
        <button type="button" class="metric-chip" @click="copilotPanel.toggle()">
          {{ copilotOpen ? tr(lt('Copilot 已打开', 'Copilot 已開啟', 'Copilot open')) : tr(lt('切换 Copilot', '切換 Copilot', 'Toggle Copilot')) }}
        </button>
      </div>
    </header>

    <div class="mail-surface__local-nav">
      <button
        v-for="item in localNav"
        :key="item.key"
        type="button"
        :class="{ 'mail-surface__local-nav--active': item.key === surface.key }"
        @click="openSurface(item)"
      >
        {{ tr(item.label) }}
      </button>
    </div>

    <section v-if="isCompose" class="mail-compose">
      <div class="mail-compose__panel">
        <header class="mail-compose__head">
          <div>
            <span class="section-label">{{ tr(lt('写邮件', '寫郵件', 'Compose')) }}</span>
            <h1>{{ tr(lt('加密投递已就绪', '加密投遞已就緒', 'Encrypted delivery ready')) }}</h1>
          </div>
          <div class="mail-compose__actions">
            <button type="button">{{ tr(lt('丢弃', '捨棄', 'Discard')) }}</button>
            <button type="button">{{ tr(lt('定时发送', '排程傳送', 'Schedule')) }}</button>
            <button class="mail-compose__primary" type="button">{{ tr(lt('发送', '傳送', 'Send')) }}</button>
          </div>
        </header>

        <div class="mail-compose__trust">
          {{ tr(lt('所有收件人都已准备就绪。只有指定收件人可以读取消息内容。', '所有收件者都已準備就緒。只有指定收件者可以讀取訊息內容。', 'All recipients are ready. Only the intended recipient can read the message contents.')) }}
        </div>

        <div class="mail-compose__fields">
          <label>
            <span class="section-label">{{ tr(lt('收件人', '收件者', 'To')) }}</span>
            <div class="mail-compose__chips">
              <span class="metric-chip">amir@phoenix-secure.com</span>
              <span class="metric-chip">ops@phoenix-secure.com</span>
            </div>
          </label>
          <label>
            <span class="section-label">{{ tr(lt('主题', '主旨', 'Subject')) }}</span>
            <input type="text" :value="tr(lt('凤凰上线交接', '鳳凰上線交接', 'Phoenix launch handoff'))" />
          </label>
          <label class="mail-compose__editor">
            <span class="section-label">{{ tr(lt('正文', '訊息', 'Message')) }}</span>
            <textarea :value="tr(lt('附件中包含最终资产、就绪说明以及切换窗口的恢复方案。', '附件中包含最終資產、就緒說明以及切換視窗的復原方案。', 'Attached are the final assets, readiness notes, and the recovery plan for the switch-over window.'))" />
          </label>
        </div>
      </div>

      <aside class="mail-compose__side">
        <article class="surface-card mail-compose__note">
          <span class="section-label">{{ tr(lt('附件', '附件', 'Attachments')) }}</span>
          <strong>{{ tr(lt('2 个加密文件', '2 個加密檔案', '2 encrypted files')) }}</strong>
          <p class="page-subtitle">{{ tr(lt('超过安全限制的文件会自动改走云盘分享链接。', '超過安全限制的檔案會自動改走雲端硬碟分享連結。', 'Files over the secure limit are automatically redirected through Drive share links.')) }}</p>
        </article>
        <article class="surface-card mail-compose__note">
          <span class="section-label">{{ tr(lt('定时发送', '排程傳送', 'Scheduling')) }}</span>
          <strong>14:00 CEST</strong>
          <p class="page-subtitle">{{ tr(lt('考虑时区的排程会让发送队列与收件人工作时间保持一致。', '考量時區的排程會讓傳送佇列與收件者工作時間保持一致。', 'Time-zone aware scheduling keeps the send queue aligned with recipient working hours.')) }}</p>
        </article>
      </aside>
    </section>

    <section v-else-if="isContacts" class="mail-directory">
      <article class="surface-card mail-directory__hero">
        <span class="section-label">{{ tr(lt('可信联系人', '可信聯絡人', 'Trusted contacts')) }}</span>
        <strong>{{ tr(lt('内部目录与最近的加密发件人', '內部通訊錄與最近的加密寄件者', 'Internal directory and recent encrypted senders')) }}</strong>
        <p class="page-subtitle">{{ tr(lt('联系人默认保持人类可读，验证细节则保持一键可达。', '聯絡人預設保持人類可讀，驗證細節則保持一鍵可達。', 'Contacts stay human-readable by default, while verification detail remains one click away.')) }}</p>
      </article>
      <div class="mail-directory__grid">
        <article v-for="contact in contacts" :key="contact.email" class="surface-card mail-directory__card">
          <span class="mail-directory__avatar">{{ contact.name.charAt(0) }}</span>
          <strong>{{ contact.name }}</strong>
          <span>{{ tr(contact.team) }}</span>
          <p>{{ contact.email }}</p>
          <button type="button">{{ tr(lt('打开会话', '開啟會話', 'Open thread')) }}</button>
        </article>
      </div>
    </section>

    <section v-else-if="isSearch" class="mail-search">
      <article class="surface-card mail-search__filters">
        <span class="section-label">{{ tr(lt('高级搜索', '進階搜尋', 'Advanced search')) }}</span>
        <div class="mail-search__chips">
          <span class="metric-chip">from: secure operations</span>
          <span class="metric-chip">has:attachment</span>
          <span class="metric-chip">is:encrypted</span>
        </div>
      </article>
      <article class="surface-card mail-search__results">
        <div v-for="result in searchResults" :key="result.id" class="mail-search__row">
          <span class="section-label">{{ tr(result.group) }}</span>
          <strong>{{ tr(result.title) }}</strong>
          <p>{{ tr(result.note) }}</p>
        </div>
      </article>
    </section>

    <section v-else class="mail-workspace">
      <article v-if="!isConversation" class="mail-workspace__list">
        <button
          v-for="message in messages"
          :key="message.id"
          class="mail-workspace__row"
          type="button"
          @click="openConversation"
        >
          <div>
            <div class="mail-workspace__row-head">
              <strong>{{ tr(message.sender) }}</strong>
              <span>{{ tr(message.time) }}</span>
            </div>
            <p>{{ tr(message.subject) }}</p>
            <small>{{ tr(message.preview) }}</small>
          </div>
          <span class="mail-workspace__badge">{{ tr(message.badge) }}</span>
        </button>
      </article>

      <article class="mail-workspace__detail surface-card">
        <div class="mail-workspace__detail-head">
          <div>
            <span class="section-label">{{ isConversation ? tr(lt('会话', '會話', 'Conversation')) : tr(surface.label) }}</span>
            <h1>{{ tr(lt('凤凰项目：最终资产交付', '鳳凰專案：最終資產交付', 'Project Phoenix: Final Asset Transfer')) }}</h1>
          </div>
          <button v-if="isConversation" type="button" @click="router.push('/inbox')">{{ tr(lt('返回收件箱', '返回收件匣', 'Back to inbox')) }}</button>
        </div>
        <div class="mail-workspace__trust">{{ tr(lt('只有你和收件人可以读取此消息。', '只有你和收件者可以讀取此訊息。', 'Only you and the recipient can read this message.')) }}</div>
        <div class="mail-workspace__body">
          <p>{{ tr(lt('团队，所有节点上的加密载荷都已验证完成。请在最终同步前查看附件。', '團隊，所有節點上的加密負載都已驗證完成。請在最終同步前查看附件。', 'Team, the encrypted payload has been verified across all nodes. Review the attachments before final sync.')) }}</p>
          <p>{{ tr(lt('旧密钥将在切换窗口关闭后按计划退役。', '舊金鑰將在切換視窗關閉後按計畫退役。', 'Legacy keys are scheduled for retirement after the switch-over window closes.')) }}</p>
        </div>
        <div class="mail-workspace__attachments">
          <article class="mail-workspace__attachment">
            <strong>Phoenix_Schematics_v4.pdf</strong>
            <span>2.4 MB</span>
          </article>
          <article class="mail-workspace__attachment">
            <strong>Node_Topology_Map.png</strong>
            <span>1.1 MB</span>
          </article>
        </div>
      </article>
    </section>
  </section>
</template>

<style scoped>
.mail-surface {
  display: grid;
  grid-template-rows: auto auto 1fr;
  min-height: calc(100vh - 56px);
  background: var(--mm-card);
}

.mail-surface__toolbar,
.mail-surface__local-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--mm-border);
}

.mail-surface__toolbar-left,
.mail-surface__toolbar-right,
.mail-surface__local-nav {
  flex-wrap: wrap;
}

.mail-surface__toolbar-left {
  display: grid;
  gap: 4px;
}

.mail-surface__toolbar-left span {
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.mail-surface__toolbar-right button,
.mail-surface__local-nav button,
.mail-directory__card button,
.mail-search__row,
.mail-workspace__row,
.mail-workspace__detail-head button,
.mail-compose__actions button {
  border: 1px solid var(--mm-border);
  border-radius: 10px;
  background: var(--mm-card);
}

.mail-surface__local-nav {
  justify-content: start;
  overflow-x: auto;
}

.mail-surface__local-nav button {
  min-height: 34px;
  padding: 0 12px;
}

.mail-surface__local-nav--active {
  border-color: var(--mm-accent-border) !important;
  background: var(--mm-accent-soft) !important;
  color: var(--mm-primary);
}

.mail-compose,
.mail-workspace {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  min-height: 0;
}

.mail-compose {
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 16px;
  padding: 16px;
}

.mail-compose__panel,
.mail-directory,
.mail-search,
.mail-workspace__detail,
.mail-compose__note {
  border: 1px solid var(--mm-border);
  border-radius: 16px;
  background: var(--mm-card);
}

.mail-compose__panel {
  padding: 18px;
}

.mail-compose__head,
.mail-workspace__detail-head {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.mail-compose__head h1,
.mail-workspace__detail h1 {
  margin: 8px 0 0;
  font-size: 24px;
  letter-spacing: -0.04em;
}

.mail-compose__actions,
.mail-compose__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.mail-compose__primary {
  background: linear-gradient(180deg, var(--mm-primary) 0%, var(--mm-primary-pressed) 100%) !important;
  color: #fff;
}

.mail-compose__trust,
.mail-workspace__trust {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  margin-top: 18px;
  padding: 0 14px;
  border: 1px solid rgba(19, 138, 107, 0.2);
  border-radius: 999px;
  background: rgba(19, 138, 107, 0.08);
  color: var(--mm-security);
  font-size: 12px;
}

.mail-compose__fields {
  display: grid;
  gap: 16px;
  margin-top: 18px;
}

.mail-compose__fields label,
.mail-compose__editor textarea {
  display: grid;
  gap: 10px;
}

.mail-compose__fields input,
.mail-compose__editor textarea {
  width: 100%;
  padding: 14px;
  border: 1px solid var(--mm-border);
  border-radius: 14px;
  background: var(--mm-card-muted);
}

.mail-compose__editor textarea {
  min-height: 220px;
  resize: vertical;
}

.mail-compose__side,
.mail-directory,
.mail-search {
  display: grid;
  gap: 16px;
}

.mail-compose__note,
.mail-search__filters,
.mail-search__results {
  padding: 16px;
}

.mail-directory {
  padding: 16px;
}

.mail-directory__hero,
.mail-directory__card {
  padding: 18px;
}

.mail-directory__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.mail-directory__avatar {
  display: inline-grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: var(--mm-accent-soft);
  color: var(--mm-primary);
  font-weight: 700;
}

.mail-directory__card {
  display: grid;
  gap: 10px;
}

.mail-directory__card span:not(.mail-directory__avatar),
.mail-directory__card p {
  color: var(--mm-text-secondary);
  font-size: 12px;
  margin: 0;
}

.mail-directory__card button {
  min-height: 34px;
}

.mail-search {
  padding: 16px;
}

.mail-search__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.mail-search__row {
  display: grid;
  gap: 8px;
  padding: 16px;
}

.mail-search__row p {
  margin: 0;
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.mail-workspace__list {
  border-right: 1px solid var(--mm-border);
}

.mail-workspace__row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  width: 100%;
  padding: 16px;
  border-width: 0 0 1px;
  border-radius: 0;
  text-align: left;
}

.mail-workspace__row-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.mail-workspace__row p,
.mail-workspace__row small,
.mail-workspace__body p {
  margin: 6px 0 0;
}

.mail-workspace__row p {
  font-size: 13px;
  color: var(--mm-ink);
}

.mail-workspace__row small,
.mail-workspace__badge,
.mail-workspace__attachment span {
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.mail-workspace__detail {
  margin: 16px;
  padding: 20px;
}

.mail-workspace__body {
  margin-top: 18px;
  color: var(--mm-text-secondary);
  font-size: 13px;
  line-height: 1.65;
}

.mail-workspace__attachments {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.mail-workspace__attachment {
  display: grid;
  gap: 6px;
  padding: 14px;
  border: 1px solid var(--mm-border);
  border-radius: 14px;
  background: var(--mm-card-muted);
}

@media (max-width: 1100px) {
  .mail-compose,
  .mail-workspace,
  .mail-directory__grid {
    grid-template-columns: 1fr;
  }

  .mail-workspace__list {
    border-right: 0;
    border-bottom: 1px solid var(--mm-border);
  }
}

@media (max-width: 820px) {
  .mail-surface {
    padding-bottom: 88px;
  }

  .mail-surface__toolbar {
    align-items: start;
    flex-direction: column;
  }

  .mail-surface__toolbar-right {
    width: 100%;
    justify-content: flex-start;
  }

  .mail-surface--thread .mail-workspace {
    grid-template-columns: 1fr;
  }

  .mail-surface--thread .mail-workspace__detail {
    display: none;
  }

  .mail-surface--conversation .mail-workspace__list {
    display: none;
  }

  .mail-workspace__detail {
    margin: 0;
    border-radius: 0;
    border-width: 0;
  }

  .mail-workspace__attachments {
    grid-template-columns: 1fr;
  }
}
</style>
