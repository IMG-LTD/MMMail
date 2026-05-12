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
import MailComposePanel from './mail/MailComposePanel.vue'
import MailFolderRail from './mail/MailFolderRail.vue'
import MailMessageList from './mail/MailMessageList.vue'
import MailThreadReader from './mail/MailThreadReader.vue'
import { createEmptyMailDraft, isEmailLike, resolveIdentityLabel, resolveRouteString, validateMailDraft } from './mail/mail-view-helpers'
import type { MailDraft } from './mail/mail-types'
import './mail-surface-view.css'

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
const composeForm = ref<MailDraft>(createEmptyMailDraft())
const composeSendError = ref('')
const discardConfirmationOpen = ref(false)
let latestWorkspaceRequest = 0
let latestRecipientTrustRequest = 0

const localNav = computed<SurfaceOption[]>(() => [
  ...mailFolderSurfaces,
  { key: 'contacts', label: lt('联系人', '聯絡人', 'Contacts'), description: lt('发件身份与可信通讯地址。', '寄件身分與可信通訊地址。', 'Sender identities and trusted contact addresses.') },
  { key: 'search', label: lt('搜索', '搜尋', 'Search'), description: lt('按关键字读取已认证邮件结果。', '依關鍵字讀取已驗證郵件結果。', 'Read authenticated mail results by keyword.') },
  { key: 'compose', label: lt('写邮件', '寫郵件', 'Compose'), description: lt('基于已认证身份与收件人信任状态写信。', '基於已驗證身分與收件者信任狀態寫信。', 'Compose with authenticated identities and recipient trust state.') }
])
const surfaceKey = computed(() => String(route.meta.surfaceKey ?? 'inbox'))
const surface = computed(() => findSurface(localNav.value, surfaceKey.value, 'inbox'))
const isCompose = computed(() => surfaceKey.value === 'compose')
const isContacts = computed(() => surfaceKey.value === 'contacts')
const isSearch = computed(() => surfaceKey.value === 'search')
const isConversation = computed(() => surfaceKey.value === 'conversation')
const currentFolder = computed(() => isConversation.value ? 'inbox' : isSearch.value ? 'search' : surfaceKey.value)
const folderQuery = computed(() => ({ keyword: isSearch.value ? searchDraft.value || undefined : undefined, page: 1, size: 20 }))
const isThreadSurface = computed(() => !isCompose.value && !isContacts.value && !isSearch.value)
const detailTrustCopy = computed(resolveDetailTrustCopy)
const composeTrustCopy = computed(resolveComposeTrustCopy)

onMounted(() => {
  void copilotPanel.loadCapabilities().catch(() => {})
})

function syncComposeFromRoute() {
  const nextDraft = { ...composeForm.value }
  nextDraft.toEmail = resolveRouteString(route.query.to) || nextDraft.toEmail
  nextDraft.fromEmail = resolveRouteString(route.query.from) || nextDraft.fromEmail
  nextDraft.subject = resolveRouteString(route.query.subject) || nextDraft.subject
  nextDraft.body = resolveRouteString(route.query.body) || nextDraft.body
  composeForm.value = nextDraft
  searchDraft.value = resolveRouteString(route.query.q)
}

function ensureComposeFromEmail() {
  const available = senderOptions.value.map(item => item.emailAddress.trim()).filter(Boolean)
  const fallback = available[0] || authStore.user?.email || ''
  const current = composeForm.value.fromEmail.trim()
  if (!current || (available.length && !available.includes(current))) {
    composeForm.value = { ...composeForm.value, fromEmail: fallback }
  }
}

async function loadWorkspace() {
  const request = createWorkspaceRequest()
  syncComposeFromRoute()
  if (!request.token) return resetSignedOutWorkspace(request)
  workspaceLoading.value = true
  loadError.value = ''
  try {
    const items = await loadFolderForRequest(request)
    await loadSenderOptionsForRequest(request)
    await loadDetailForRequest(request, items)
  } finally {
    if (isCurrentWorkspaceRequest(request)) workspaceLoading.value = false
  }
}

function createWorkspaceRequest() {
  latestWorkspaceRequest += 1
  return { id: latestWorkspaceRequest, path: route.fullPath, token: authStore.accessToken }
}

function isCurrentWorkspaceRequest(request: { id: number; path: string; token: string }) {
  return request.id === latestWorkspaceRequest && request.token === authStore.accessToken && request.path === route.fullPath
}

function resetSignedOutWorkspace(request: { id: number; path: string; token: string }) {
  if (!isCurrentWorkspaceRequest(request)) return
  mailItems.value = []
  activeMail.value = null
  senderOptions.value = []
  recipientTrust.value = null
  loadError.value = ''
  workspaceLoading.value = false
}

async function loadFolderForRequest(request: { id: number; path: string; token: string }) {
  if (isCompose.value || isContacts.value) {
    mailItems.value = []
    activeMail.value = null
    return []
  }
  try {
    const response = await listMailFolder(currentFolder.value, request.token, folderQuery.value)
    if (!isCurrentWorkspaceRequest(request)) return []
    mailItems.value = response.data || []
    return mailItems.value
  } catch (error) {
    if (isCurrentWorkspaceRequest(request)) handleWorkspaceError(error, 'Unable to load the mail list.')
    return []
  }
}

async function loadSenderOptionsForRequest(request: { id: number; path: string; token: string }) {
  try {
    const response = await listSenderIdentities(request.token)
    if (!isCurrentWorkspaceRequest(request)) return
    senderOptions.value = response.data || []
  } catch {
    if (isCurrentWorkspaceRequest(request)) senderOptions.value = []
  }
  ensureComposeFromEmail()
}

async function loadDetailForRequest(request: { id: number; path: string; token: string }, items: MailSummary[]) {
  const detailId = isCompose.value || isContacts.value ? '' : resolveDetailId(items)
  if (!detailId) {
    activeMail.value = null
    return
  }
  try {
    const response = await readMailDetail(detailId, request.token)
    if (isCurrentWorkspaceRequest(request)) activeMail.value = response.data
  } catch (error) {
    if (!isCurrentWorkspaceRequest(request)) return
    activeMail.value = null
    handleWorkspaceError(error, 'Unable to load the mail detail.')
  }
}

function resolveDetailId(items: MailSummary[]) {
  return resolveRouteString(route.params.threadId) || resolveRouteString(route.params.id) || items[0]?.id || ''
}

function handleWorkspaceError(error: unknown, fallback: string) {
  loadError.value = error instanceof Error && error.message ? error.message : fallback
  mailItems.value = []
}

async function refreshRecipientTrust() {
  const requestId = ++latestRecipientTrustRequest
  const requestToken = authStore.accessToken
  const toEmail = composeForm.value.toEmail.trim()
  const fromEmail = composeForm.value.fromEmail.trim()
  if (!requestToken || !isEmailLike(toEmail) || !isEmailLike(fromEmail)) return resetRecipientTrust(requestId, requestToken)
  recipientTrustLoading.value = true
  try {
    const response = await readRecipientTrustState(toEmail, fromEmail, requestToken)
    if (requestId === latestRecipientTrustRequest && requestToken === authStore.accessToken) recipientTrust.value = response.data
  } catch {
    if (requestId === latestRecipientTrustRequest) recipientTrust.value = { status: 'warning', message: 'The recipient trust state is temporarily unavailable.' }
  } finally {
    if (requestId === latestRecipientTrustRequest) recipientTrustLoading.value = false
  }
}

function resetRecipientTrust(requestId: number, requestToken: string) {
  if (requestId !== latestRecipientTrustRequest || requestToken !== authStore.accessToken) return
  recipientTrust.value = null
  recipientTrustLoading.value = false
}

async function submitCompose() {
  const validation = validateMailDraft(composeForm.value)
  composeSendError.value = validation
  if (validation) return
  if (!authStore.accessToken) {
    composeSendError.value = 'Sign in before sending this message.'
    return
  }
  await sendComposePayload()
}

async function sendComposePayload() {
  composeSending.value = true
  composeSendError.value = ''
  try {
    await sendMail({ ...composeForm.value, body: composeForm.value.body.trim(), subject: composeForm.value.subject.trim(), toEmail: composeForm.value.toEmail.trim() }, authStore.accessToken || '')
    composeForm.value = createEmptyMailDraft(composeForm.value.fromEmail)
    recipientTrust.value = null
    await router.push('/mail/sent')
  } catch (error) {
    composeSendError.value = error instanceof Error && error.message ? error.message : 'Failed to send the message.'
  } finally {
    composeSending.value = false
  }
}

function resolveComposeSendError() {
  return composeSendError.value || loadError.value
}

function openSurface(item: SurfaceOption) {
  const pathMap: Record<string, string> = {
    archive: '/mail/archive', compose: '/mail/compose', contacts: '/mail/contacts', drafts: '/mail/drafts',
    inbox: '/mail/inbox', outbox: '/mail/outbox', scheduled: '/mail/scheduled', search: '/mail/search',
    sent: '/mail/sent', snoozed: '/mail/snoozed', spam: '/mail/spam', starred: '/mail/starred',
    trash: '/mail/trash', unread: '/mail/unread'
  }
  void router.push(pathMap[item.key] ?? '/mail/inbox')
}

function openConversation(mailId: string) {
  void router.push(`/mail/conversations/${mailId}`)
}

function runSearch() {
  void router.push({ path: '/mail/search', query: searchDraft.value.trim() ? { q: searchDraft.value.trim() } : {} })
}

function openComposeForIdentity(emailAddress: string) {
  void router.push({ path: '/mail/compose', query: { from: emailAddress } })
}

function resolveDetailTrustCopy() {
  if (!authStore.accessToken) return tr(lt('登录后即可读取已认证邮件详情。', '登入後即可讀取已驗證郵件詳情。', 'Sign in to read authenticated mail detail.'))
  if (workspaceLoading.value) return tr(lt('正在读取已认证邮件数据。', '正在讀取已驗證郵件資料。', 'Loading authenticated mail data.'))
  if (activeMail.value) return tr(lt('此消息详情来自已认证运行时数据。', '此訊息詳情來自已驗證執行期資料。', 'This message detail comes from authenticated runtime data.'))
  return tr(lt('选择一封邮件以查看详情。', '選擇一封郵件以查看詳情。', 'Select a message to view detail.'))
}

function resolveComposeTrustCopy() {
  if (!authStore.accessToken) return tr(lt('登录后即可校验收件人信任状态。', '登入後即可驗證收件者信任狀態。', 'Sign in to verify recipient trust state.'))
  if (recipientTrustLoading.value) return tr(lt('正在检查收件人的加密投递状态。', '正在檢查收件者的加密投遞狀態。', 'Checking the recipient encrypted-delivery status.'))
  return recipientTrust.value?.message || tr(lt('填写发件人与收件人后，系统会读取信任状态。', '填寫寄件人與收件者後，系統會讀取信任狀態。', 'Enter sender and recipient details to load the trust state.'))
}

watch(() => [route.fullPath, authStore.accessToken], () => {
  void loadWorkspace()
}, { immediate: true })

watch(() => [composeForm.value.toEmail, composeForm.value.fromEmail, authStore.accessToken], () => {
  void refreshRecipientTrust()
})
</script>

<template>
  <section class="mail-surface" :class="{ 'mail-surface--conversation': isConversation, 'mail-surface--thread': isThreadSurface && !isConversation }">
    <header class="mail-surface__toolbar">
      <div>
        <strong>{{ tr(surface.label) }}</strong>
        <span>{{ tr(surface.description) }}</span>
      </div>
      <div class="mail-surface__toolbar-right">
        <button type="button">All</button>
        <button type="button">Unread</button>
        <button type="button" @click="copilotPanel.toggle()">{{ copilotOpen ? 'Copilot open' : 'Toggle Copilot' }}</button>
      </div>
    </header>
    <p v-if="!authStore.accessToken" class="mail-surface__notice page-subtitle">Sign in to load mail, sender identities, and recipient trust state.</p>
    <p v-else-if="loadError && !isCompose" class="mail-surface__notice page-subtitle">{{ loadError }}</p>
    <section v-if="isCompose" class="mail-compose">
      <MailComposePanel
        :discard-confirmation-open="discardConfirmationOpen"
        :draft="composeForm"
        :identities="senderOptions"
        :send-error="resolveComposeSendError()"
        :sending="composeSending"
        :trust-copy="composeTrustCopy"
        :trust-loading="recipientTrustLoading"
        :trust-state="recipientTrust"
        @discard="discardConfirmationOpen = true"
        @retry="submitCompose"
        @submit="submitCompose"
        @update:draft="composeForm = $event"
      />
    </section>
    <section v-else-if="isContacts" class="mail-directory">
      <article v-for="identity in senderOptions" :key="identity.emailAddress" class="surface-card mail-directory__card">
        <strong>{{ resolveIdentityLabel(identity) }}</strong>
        <p>{{ identity.emailAddress }}</p>
        <button type="button" @click="openComposeForIdentity(identity.emailAddress)">Compose with this identity</button>
      </article>
      <p v-if="!senderOptions.length" class="page-subtitle">Sender identities appear after sign-in.</p>
    </section>
    <section v-else-if="isSearch" class="mail-search">
      <div class="mail-search__head">
        <input v-model="searchDraft" type="search" placeholder="Search by subject or body keyword" @keyup.enter="runSearch">
        <button type="button" @click="runSearch">Search</button>
      </div>
      <MailMessageList :empty-copy="'No matching mail found.'" :loading="workspaceLoading" :messages="mailItems" @open="openConversation" />
    </section>
    <section v-else class="mail-workspace">
      <MailFolderRail :active-key="surface.key" :items="localNav" @open="openSurface" />
      <MailMessageList v-if="!isConversation" :empty-copy="'This folder is empty.'" :loading="workspaceLoading" :messages="mailItems" @open="openConversation" />
      <MailThreadReader :active-mail="activeMail" :loading="workspaceLoading" :show-back="isConversation" :trust-copy="detailTrustCopy" @back="router.push('/mail/inbox')" />
    </section>
  </section>
</template>
