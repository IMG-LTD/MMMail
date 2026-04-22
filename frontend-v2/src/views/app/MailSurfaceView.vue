<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { lt, useLocaleText } from '@/locales'
import {
  listMailFolder,
  listSenderIdentities,
  readMailDetail,
  readRecipientTrustState,
  sendMail,
  type MailDetail,
  type MailSenderIdentity,
  type MailSummary,
  type RecipientTrustState
} from '@/service/api/mail'
import { useCopilotPanel } from '@/shared/composables/useCopilotPanel'
import { findSurface, mailFolderSurfaces, type SurfaceOption } from '@/shared/content/route-surfaces'
import { useAuthStore } from '@/store/modules/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { tr } = useLocaleText()
const copilotPanel = useCopilotPanel()
const copilotOpen = copilotPanel.open

const mailItems = ref<MailSummary[]>([])
const activeMail = ref<MailDetail | null>(null)
const senderOptions = ref<MailSenderIdentity[]>([])
const recipientTrust = ref<RecipientTrustState | null>(null)
const workspaceLoading = ref(false)
const recipientTrustLoading = ref(false)
const composeSending = ref(false)
const loadError = ref('')
const searchDraft = ref('')
const composeForm = ref({
  body: '',
  fromEmail: '',
  subject: '',
  toEmail: ''
})
let latestWorkspaceRequest = 0
let latestRecipientTrustRequest = 0

const localNav = computed<SurfaceOption[]>(() => {
  return [
    ...mailFolderSurfaces,
    { key: 'contacts', label: lt('联系人', '聯絡人', 'Contacts'), description: lt('发件身份与可信通讯地址。', '寄件身分與可信通訊地址。', 'Sender identities and trusted contact addresses.') },
    { key: 'search', label: lt('搜索', '搜尋', 'Search'), description: lt('按关键字读取已认证邮件结果。', '依關鍵字讀取已驗證郵件結果。', 'Read authenticated mail results by keyword.') },
    { key: 'compose', label: lt('写邮件', '寫郵件', 'Compose'), description: lt('基于已认证身份与收件人信任状态写信。', '基於已驗證身分與收件者信任狀態寫信。', 'Compose with authenticated identities and recipient trust state.') }
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
const currentFolder = computed(() => {
  if (isConversation.value) {
    return 'inbox'
  }

  if (isSearch.value) {
    return 'search'
  }

  return surfaceKey.value
})

const folderQuery = computed<Record<string, string | number | undefined>>(() => {
  return {
    keyword: isSearch.value ? searchDraft.value || undefined : undefined,
    page: 1,
    size: 20
  }
})

const activeMailParagraphs = computed(() => {
  return activeMail.value?.body
    ?.split(/\n+/)
    .map(item => item.trim())
    .filter(Boolean) || []
})

const composeTrustCopy = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt('登录后即可校验收件人信任状态。', '登入後即可驗證收件者信任狀態。', 'Sign in to verify recipient trust state.'))
  }

  if (recipientTrustLoading.value) {
    return tr(lt('正在检查收件人的加密投递状态。', '正在檢查收件者的加密投遞狀態。', 'Checking the recipient encrypted-delivery status.'))
  }

  if (recipientTrust.value?.message) {
    return recipientTrust.value.message
  }

  return tr(lt('填写发件人与收件人后，系统会读取信任状态。', '填寫寄件人與收件者後，系統會讀取信任狀態。', 'Enter sender and recipient details to load the trust state.'))
})

const composeTrustClass = computed(() => {
  return {
    'mail-compose__trust--blocked': recipientTrust.value?.status === 'blocked',
    'mail-compose__trust--ready': recipientTrust.value?.status === 'ready',
    'mail-compose__trust--warning': recipientTrust.value?.status === 'warning'
  }
})

const detailTrustCopy = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt('登录后即可读取已认证邮件详情。', '登入後即可讀取已驗證郵件詳情。', 'Sign in to read authenticated mail detail.'))
  }

  if (workspaceLoading.value) {
    return tr(lt('正在读取已认证邮件数据。', '正在讀取已驗證郵件資料。', 'Loading authenticated mail data.'))
  }

  if (activeMail.value) {
    return tr(lt('此消息详情来自已认证运行时数据。', '此訊息詳情來自已驗證執行期資料。', 'This message detail comes from authenticated runtime data.'))
  }

  return tr(lt('选择一封邮件以查看详情。', '選擇一封郵件以查看詳情。', 'Select a message to view detail.'))
})

const composeSendDisabled = computed(() => {
  return !authStore.accessToken || composeSending.value || !composeForm.value.toEmail.trim() || !composeForm.value.subject.trim()
})

onMounted(() => {
  void copilotPanel.loadCapabilities().catch(() => {})
})

function resolveRouteString(value: unknown) {
  if (Array.isArray(value)) {
    return resolveRouteString(value[0])
  }

  return typeof value === 'string' ? value : ''
}

function syncComposeFromRoute() {
  const routeTo = resolveRouteString(route.query.to)
  const routeFrom = resolveRouteString(route.query.from)
  const routeSubject = resolveRouteString(route.query.subject)
  const routeBody = resolveRouteString(route.query.body)
  const routeSearch = resolveRouteString(route.query.q)

  if (routeTo) {
    composeForm.value.toEmail = routeTo
  }

  if (routeFrom) {
    composeForm.value.fromEmail = routeFrom
  }

  if (routeSubject) {
    composeForm.value.subject = routeSubject
  }

  if (routeBody) {
    composeForm.value.body = routeBody
  }

  searchDraft.value = routeSearch
}

function ensureComposeFromEmail() {
  const currentFromEmail = composeForm.value.fromEmail.trim()
  const availableFromEmails = senderOptions.value
    .map(item => item.emailAddress.trim())
    .filter(Boolean)
  const fallbackFromEmail = availableFromEmails[0] || authStore.user?.email || ''

  if (!currentFromEmail) {
    composeForm.value.fromEmail = fallbackFromEmail
    return
  }

  if (availableFromEmails.length && availableFromEmails.includes(currentFromEmail)) {
    return
  }

  if (!availableFromEmails.length && currentFromEmail === fallbackFromEmail) {
    return
  }

  composeForm.value.fromEmail = fallbackFromEmail
}

function isEmailLike(value: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
}

async function loadWorkspace() {
  const requestId = ++latestWorkspaceRequest
  const requestToken = authStore.accessToken
  const requestPath = route.fullPath

  syncComposeFromRoute()

  if (!requestToken) {
    if (requestId !== latestWorkspaceRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath) {
      return
    }

    mailItems.value = []
    activeMail.value = null
    senderOptions.value = []
    recipientTrust.value = null
    loadError.value = ''
    workspaceLoading.value = false
    return
  }

  workspaceLoading.value = true
  loadError.value = ''

  const shouldLoadFolder = !isCompose.value && !isContacts.value
  let nextDetailId = ''
  let folderItems: MailSummary[] = []
  let folderLoaded = false

  try {
    if (shouldLoadFolder) {
      try {
        const folderResponse = await listMailFolder(currentFolder.value, requestToken, folderQuery.value)

        if (requestId !== latestWorkspaceRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath) {
          return
        }

        folderItems = folderResponse.data || []
        folderLoaded = true
        mailItems.value = folderItems
      } catch (error) {
        if (requestId !== latestWorkspaceRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath) {
          return
        }

        folderItems = []
        folderLoaded = false
        mailItems.value = []
        activeMail.value = null
        loadError.value = error instanceof Error
          ? error.message
          : tr(lt('无法加载邮件列表。', '無法載入郵件清單。', 'Unable to load the mail list.'))
      }
    } else {
      mailItems.value = []
      activeMail.value = null
    }

    try {
      const senderResponse = await listSenderIdentities(requestToken)

      if (requestId !== latestWorkspaceRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath) {
        return
      }

      senderOptions.value = senderResponse.data || []
      ensureComposeFromEmail()
    } catch {
      if (requestId !== latestWorkspaceRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath) {
        return
      }

      senderOptions.value = []
      ensureComposeFromEmail()
    }

    nextDetailId = shouldLoadFolder && folderLoaded
      ? resolveRouteString(route.params.id) || (folderItems[0]?.id || '')
      : ''

    if (!nextDetailId) {
      if (requestId !== latestWorkspaceRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath) {
        return
      }

      activeMail.value = null
      return
    }

    try {
      const detailResponse = await readMailDetail(nextDetailId, requestToken)

      if (requestId !== latestWorkspaceRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath) {
        return
      }

      activeMail.value = detailResponse.data
    } catch (error) {
      if (requestId !== latestWorkspaceRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath) {
        return
      }

      activeMail.value = null
      loadError.value = loadError.value || (error instanceof Error
        ? error.message
        : tr(lt('无法加载邮件详情。', '無法載入郵件詳情。', 'Unable to load the mail detail.')))
    }
  } finally {
    if (requestId === latestWorkspaceRequest && requestToken === authStore.accessToken && requestPath === route.fullPath) {
      workspaceLoading.value = false
    }
  }
}

async function refreshRecipientTrust() {
  const requestId = ++latestRecipientTrustRequest
  const requestToken = authStore.accessToken
  const toEmail = composeForm.value.toEmail.trim()
  const fromEmail = composeForm.value.fromEmail.trim()

  if (!requestToken) {
    if (requestId === latestRecipientTrustRequest && requestToken === authStore.accessToken) {
      recipientTrust.value = null
      recipientTrustLoading.value = false
    }
    return
  }

  if (!toEmail || !fromEmail) {
    if (requestId === latestRecipientTrustRequest && requestToken === authStore.accessToken) {
      recipientTrust.value = null
      recipientTrustLoading.value = false
    }
    return
  }

  if (!isEmailLike(toEmail) || !isEmailLike(fromEmail)) {
    if (requestId === latestRecipientTrustRequest && requestToken === authStore.accessToken) {
      recipientTrust.value = null
      recipientTrustLoading.value = false
    }
    return
  }

  recipientTrustLoading.value = true

  try {
    const response = await readRecipientTrustState(toEmail, fromEmail, requestToken)

    if (requestId !== latestRecipientTrustRequest || requestToken !== authStore.accessToken) {
      return
    }

    recipientTrust.value = response.data
  } catch {
    if (requestId !== latestRecipientTrustRequest || requestToken !== authStore.accessToken) {
      return
    }

    recipientTrust.value = {
      status: 'warning',
      message: tr(lt('暂时无法读取收件人信任状态。', '暫時無法讀取收件者信任狀態。', 'The recipient trust state is temporarily unavailable.'))
    }
  } finally {
    if (requestId === latestRecipientTrustRequest && requestToken === authStore.accessToken) {
      recipientTrustLoading.value = false
    }
  }
}

async function submitCompose() {
  if (composeSendDisabled.value || !authStore.accessToken) {
    return
  }

  composeSending.value = true
  loadError.value = ''

  try {
    await sendMail({
      body: composeForm.value.body.trim(),
      fromEmail: composeForm.value.fromEmail.trim() || undefined,
      subject: composeForm.value.subject.trim(),
      toEmail: composeForm.value.toEmail.trim()
    }, authStore.accessToken)

    composeForm.value = {
      body: '',
      fromEmail: composeForm.value.fromEmail,
      subject: '',
      toEmail: ''
    }
    recipientTrust.value = null
    await router.push('/sent')
  } catch (error) {
    loadError.value = error instanceof Error
      ? error.message
      : tr(lt('邮件发送失败。', '郵件傳送失敗。', 'Failed to send the message.'))
  } finally {
    composeSending.value = false
  }
}

function openConversation(mailId: string) {
  void router.push(`/conversations/${mailId}`)
}

function openComposeForIdentity(emailAddress: string) {
  void router.push({
    path: '/compose',
    query: { from: emailAddress }
  })
}

function runSearch() {
  void router.push({
    path: '/search',
    query: searchDraft.value.trim() ? { q: searchDraft.value.trim() } : {}
  })
}

function openSurface(item: SurfaceOption) {
  const pathMap: Record<string, string> = {
    archive: '/archive',
    compose: '/compose',
    contacts: '/contacts',
    drafts: '/drafts',
    inbox: '/inbox',
    outbox: '/outbox',
    scheduled: '/scheduled',
    search: '/search',
    sent: '/sent',
    snoozed: '/snoozed',
    spam: '/spam',
    starred: '/starred',
    trash: '/trash',
    unread: '/unread'
  }

  void router.push(pathMap[item.key] ?? '/inbox')
}

function formatMailTimestamp(value: string) {
  if (!value) {
    return tr(lt('未知时间', '未知時間', 'Unknown time'))
  }

  const parsed = new Date(value)

  if (Number.isNaN(parsed.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat(undefined, {
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    month: 'short'
  }).format(parsed)
}

function formatFileSize(value: number) {
  if (!value) {
    return '0 B'
  }

  if (value >= 1024 * 1024) {
    return `${(value / (1024 * 1024)).toFixed(1)} MB`
  }

  if (value >= 1024) {
    return `${Math.round(value / 1024)} KB`
  }

  return `${value} B`
}

function resolveMailSender(mail: MailSummary | MailDetail | null) {
  if (!mail) {
    return tr(lt('暂无发件人', '暫無寄件人', 'No sender'))
  }

  return mail.senderDisplayName || mail.senderEmail || mail.peerEmail || tr(lt('未知发件人', '未知寄件人', 'Unknown sender'))
}

function resolveIdentityLabel(identity: MailSenderIdentity) {
  return identity.displayName || identity.emailAddress || tr(lt('发件身份', '寄件身分', 'Sender identity'))
}

watch(() => [route.fullPath, authStore.accessToken], () => {
  void loadWorkspace()
}, { immediate: true })

watch(() => [composeForm.value.toEmail, composeForm.value.fromEmail, authStore.accessToken], () => {
  void refreshRecipientTrust()
})
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

    <p v-if="!authStore.accessToken" class="mail-surface__notice page-subtitle">
      {{ tr(lt('登录后即可读取邮件、身份与收件人信任状态。', '登入後即可讀取郵件、身分與收件者信任狀態。', 'Sign in to load mail, sender identities, and recipient trust state.')) }}
    </p>
    <p v-else-if="loadError" class="mail-surface__notice page-subtitle">{{ loadError }}</p>

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
            <h1>{{ tr(lt('使用已认证身份发送邮件', '使用已驗證身分傳送郵件', 'Send mail with authenticated identities')) }}</h1>
          </div>
          <div class="mail-compose__actions">
            <button
              type="button"
              @click="composeForm = { body: '', fromEmail: composeForm.fromEmail, subject: '', toEmail: '' }"
            >
              {{ tr(lt('清空', '清空', 'Clear')) }}
            </button>
            <button type="button" @click="runSearch">{{ tr(lt('搜索会话', '搜尋會話', 'Search threads')) }}</button>
            <button class="mail-compose__primary" type="button" :disabled="composeSendDisabled" @click="submitCompose">
              {{ composeSending ? tr(lt('发送中', '傳送中', 'Sending')) : tr(lt('发送', '傳送', 'Send')) }}
            </button>
          </div>
        </header>

        <div class="mail-compose__trust" :class="composeTrustClass">
          {{ composeTrustCopy }}
        </div>

        <div class="mail-compose__fields">
          <label>
            <span class="section-label">{{ tr(lt('发件人', '寄件人', 'From')) }}</span>
            <select v-model="composeForm.fromEmail">
              <option v-if="!senderOptions.length" value="">
                {{ tr(lt('暂无可用发件身份', '暫無可用寄件身分', 'No sender identities available')) }}
              </option>
              <option
                v-for="identity in senderOptions"
                :key="identity.emailAddress"
                :value="identity.emailAddress"
              >
                {{ resolveIdentityLabel(identity) }} · {{ identity.emailAddress }}
              </option>
            </select>
          </label>
          <label>
            <span class="section-label">{{ tr(lt('收件人', '收件者', 'To')) }}</span>
            <input v-model="composeForm.toEmail" type="email" :placeholder="tr(lt('name@example.com', 'name@example.com', 'name@example.com'))">
          </label>
          <label>
            <span class="section-label">{{ tr(lt('主题', '主旨', 'Subject')) }}</span>
            <input v-model="composeForm.subject" type="text" :placeholder="tr(lt('输入邮件主题', '輸入郵件主旨', 'Enter a subject'))">
          </label>
          <label class="mail-compose__editor">
            <span class="section-label">{{ tr(lt('正文', '訊息', 'Message')) }}</span>
            <textarea v-model="composeForm.body" :placeholder="tr(lt('输入消息内容', '輸入訊息內容', 'Write your message'))" />
          </label>
        </div>
      </div>

      <aside class="mail-compose__side">
        <article class="surface-card mail-compose__note">
          <span class="section-label">{{ tr(lt('发件身份', '寄件身分', 'Sender identities')) }}</span>
          <strong>{{ senderOptions.length }} {{ tr(lt('个可用身份', '個可用身分', 'available identities')) }}</strong>
          <p class="page-subtitle">
            {{ authStore.user?.email || tr(lt('当前未登录', '目前未登入', 'Not signed in')) }}
          </p>
        </article>
        <article class="surface-card mail-compose__note">
          <span class="section-label">{{ tr(lt('信任状态', '信任狀態', 'Trust state')) }}</span>
          <strong>{{ recipientTrust?.status || tr(lt('待检查', '待檢查', 'Pending')) }}</strong>
          <p class="page-subtitle">
            {{ recipientTrust?.routeCount ?? 0 }} {{ tr(lt('条投递路径', '條投遞路徑', 'delivery routes')) }}
          </p>
        </article>
      </aside>
    </section>

    <section v-else-if="isContacts" class="mail-directory">
      <article class="surface-card mail-directory__hero">
        <span class="section-label">{{ tr(lt('发件身份', '寄件身分', 'Sender identities')) }}</span>
        <strong>{{ tr(lt('已认证发件人与常用地址', '已驗證寄件人與常用地址', 'Authenticated senders and common addresses')) }}</strong>
        <p class="page-subtitle">
          {{ tr(lt('联系人卡片由运行时身份数据生成，而不是冻结示例数据。', '聯絡人卡片由執行期身分資料產生，而不是凍結示例資料。', 'Contact cards are generated from runtime identity data instead of frozen sample data.')) }}
        </p>
      </article>
      <div class="mail-directory__grid">
        <article v-for="identity in senderOptions" :key="identity.emailAddress" class="surface-card mail-directory__card">
          <span class="mail-directory__avatar">{{ resolveIdentityLabel(identity).charAt(0).toUpperCase() }}</span>
          <strong>{{ resolveIdentityLabel(identity) }}</strong>
          <span>{{ identity.source || tr(lt('发件身份', '寄件身分', 'Sender identity')) }}</span>
          <p>{{ identity.emailAddress }}</p>
          <button type="button" @click="openComposeForIdentity(identity.emailAddress)">{{ tr(lt('使用此身份写信', '使用此身分寫信', 'Compose with this identity')) }}</button>
        </article>
        <article v-if="!workspaceLoading && !senderOptions.length" class="surface-card mail-directory__card">
          <strong>{{ tr(lt('暂无身份数据', '暫無身分資料', 'No identity data')) }}</strong>
          <p>{{ tr(lt('登录后会显示发件身份。', '登入後會顯示寄件身分。', 'Sender identities appear after sign-in.')) }}</p>
        </article>
      </div>
    </section>

    <section v-else-if="isSearch" class="mail-search">
      <article class="surface-card mail-search__filters">
        <span class="section-label">{{ tr(lt('高级搜索', '進階搜尋', 'Advanced search')) }}</span>
        <div class="mail-search__head">
          <input
            v-model="searchDraft"
            class="mail-search__input"
            type="search"
            :placeholder="tr(lt('按主题或正文关键字搜索', '依主旨或內文關鍵字搜尋', 'Search by subject or body keyword'))"
            @keyup.enter="runSearch"
          >
          <button type="button" @click="runSearch">{{ tr(lt('搜索', '搜尋', 'Search')) }}</button>
        </div>
        <div class="mail-search__chips">
          <span v-if="searchDraft" class="metric-chip">keyword: {{ searchDraft }}</span>
          <span class="metric-chip">{{ mailItems.length }} {{ tr(lt('条结果', '條結果', 'results')) }}</span>
        </div>
      </article>
      <article class="surface-card mail-search__results">
        <button v-for="result in mailItems" :key="result.id" type="button" class="mail-search__row" @click="openConversation(result.id)">
          <span class="section-label">{{ formatMailTimestamp(result.sentAt) }}</span>
          <strong>{{ result.subject || tr(lt('无主题', '無主旨', 'No subject')) }}</strong>
          <p>{{ result.preview || resolveMailSender(result) }}</p>
        </button>
        <p v-if="workspaceLoading" class="page-subtitle">{{ tr(lt('正在搜索邮件。', '正在搜尋郵件。', 'Searching mail.')) }}</p>
        <p v-else-if="!mailItems.length" class="page-subtitle">{{ tr(lt('没有找到匹配邮件。', '沒有找到符合的郵件。', 'No matching mail found.')) }}</p>
      </article>
    </section>

    <section v-else class="mail-workspace">
      <article v-if="!isConversation" class="mail-workspace__list">
        <button
          v-for="message in mailItems"
          :key="message.id"
          class="mail-workspace__row"
          type="button"
          @click="openConversation(message.id)"
        >
          <div>
            <div class="mail-workspace__row-head">
              <strong>{{ resolveMailSender(message) }}</strong>
              <span>{{ formatMailTimestamp(message.sentAt) }}</span>
            </div>
            <p>{{ message.subject || tr(lt('无主题', '無主旨', 'No subject')) }}</p>
            <small>{{ message.preview || tr(lt('暂无预览内容', '暫無預覽內容', 'No preview available')) }}</small>
          </div>
          <span class="mail-workspace__badge">
            {{ message.unread ? tr(lt('未读', '未讀', 'Unread')) : tr(lt('已读', '已讀', 'Read')) }}
          </span>
        </button>
        <div v-if="workspaceLoading" class="mail-workspace__empty">{{ tr(lt('正在加载邮件列表。', '正在載入郵件清單。', 'Loading mail list.')) }}</div>
        <div v-else-if="!mailItems.length" class="mail-workspace__empty">{{ tr(lt('当前文件夹没有邮件。', '目前資料夾沒有郵件。', 'This folder is empty.')) }}</div>
      </article>

      <article class="mail-workspace__detail surface-card">
        <div class="mail-workspace__detail-head">
          <div>
            <span class="section-label">{{ isConversation ? tr(lt('会话', '會話', 'Conversation')) : tr(surface.label) }}</span>
            <h1>{{ activeMail?.subject || tr(lt('选择一封邮件', '選擇一封郵件', 'Select a message')) }}</h1>
          </div>
          <button v-if="isConversation" type="button" @click="router.push('/inbox')">{{ tr(lt('返回收件箱', '返回收件匣', 'Back to inbox')) }}</button>
        </div>
        <div class="mail-workspace__trust">{{ detailTrustCopy }}</div>
        <div v-if="activeMail" class="mail-workspace__meta">
          <span class="section-label">{{ tr(lt('发件人', '寄件人', 'Sender')) }}</span>
          <strong>{{ resolveMailSender(activeMail) }}</strong>
          <span>{{ formatMailTimestamp(activeMail.sentAt) }}</span>
        </div>
        <div v-if="workspaceLoading" class="mail-workspace__empty">{{ tr(lt('正在加载邮件详情。', '正在載入郵件詳情。', 'Loading mail detail.')) }}</div>
        <div v-else-if="activeMail" class="mail-workspace__body">
          <p v-for="(paragraph, index) in activeMailParagraphs" :key="`${activeMail.id}-${index}`">{{ paragraph }}</p>
          <p v-if="!activeMailParagraphs.length">{{ tr(lt('此邮件没有正文内容。', '此郵件沒有正文內容。', 'This message has no body content.')) }}</p>
        </div>
        <div v-else class="mail-workspace__empty">{{ tr(lt('请选择列表中的邮件查看详情。', '請選擇清單中的郵件查看詳情。', 'Choose a message from the list to inspect its detail.')) }}</div>
        <div v-if="activeMail?.attachments.length" class="mail-workspace__attachments">
          <article v-for="attachment in activeMail.attachments" :key="attachment.id" class="mail-workspace__attachment">
            <strong>{{ attachment.fileName }}</strong>
            <span>{{ formatFileSize(attachment.fileSize) }}</span>
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

.mail-surface__notice {
  margin: 0;
  padding: 12px 16px 0;
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

.mail-compose__primary:disabled {
  opacity: 0.6;
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

.mail-compose__trust--ready {
  border-color: rgba(19, 138, 107, 0.2);
  background: rgba(19, 138, 107, 0.08);
  color: var(--mm-security);
}

.mail-compose__trust--warning {
  border-color: rgba(208, 145, 31, 0.25);
  background: rgba(208, 145, 31, 0.1);
  color: #8b5e00;
}

.mail-compose__trust--blocked {
  border-color: rgba(196, 59, 59, 0.25);
  background: rgba(196, 59, 59, 0.1);
  color: #a12626;
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
.mail-compose__fields select,
.mail-compose__editor textarea,
.mail-search__input {
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

.mail-search__head {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}

.mail-search__head button {
  min-width: 96px;
  border: 1px solid var(--mm-border);
  border-radius: 14px;
  background: var(--mm-card);
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
  width: 100%;
  padding: 16px;
  text-align: left;
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
.mail-workspace__attachment span,
.mail-workspace__meta span {
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.mail-workspace__detail {
  margin: 16px;
  padding: 20px;
}

.mail-workspace__meta {
  display: grid;
  gap: 6px;
  margin-top: 18px;
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

.mail-workspace__empty {
  padding: 18px 16px;
  color: var(--mm-text-secondary);
  font-size: 13px;
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

  .mail-surface__toolbar-right,
  .mail-search__head {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
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
