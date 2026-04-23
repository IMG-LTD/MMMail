<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { lt, useLocaleText } from '@/locales'
import {
  listPassItems,
  listPassMailboxes,
  readPassMonitor,
  type PassMailbox,
  type PassMonitorItem,
  type PassMonitorOverview,
  type PassWorkspaceItemSummary
} from '@/service/api/pass'
import { findSurface, passSections } from '@/shared/content/route-surfaces'
import { useAuthStore } from '@/store/modules/auth'
import {
  PASS_ITEMS_FETCH_LIMIT,
  createPassMonitorIssueMap,
  derivePassSectionState,
  isPassItemShared,
  isPassMailboxVerified
} from './pass-section-state'

interface PassSurfaceEntry {
  key: string
  id: string
  title: string
  subtitle: string
  meta: string
  badge: string
  avatar: string
  kind: 'item' | 'mailbox'
  item?: PassWorkspaceItemSummary
  mailbox?: PassMailbox
  monitorItem?: PassMonitorItem
}

interface PassCardFact {
  label: string
  value: string
}

interface PassDetailCard {
  label: string
  title: string
  copy: string
  facts: PassCardFact[]
}

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { tr } = useLocaleText()

const surfaceKey = computed(() => String(route.meta.surfaceKey ?? 'pass'))
const current = computed(() => findSurface(passSections, surfaceKey.value, 'pass'))

const passItems = ref<PassWorkspaceItemSummary[]>([])
const passMailboxes = ref<PassMailbox[]>([])
const passMonitor = ref<PassMonitorOverview | null>(null)
const passLoading = ref(false)
const loadError = ref('')
const selectedEntryKey = ref('')

let latestPassRequest = 0

const sharedItems = computed(() => {
  return passItems.value
    .filter(item => isPassItemShared(item))
    .slice()
    .sort(compareItemsForVault)
})

const personalItems = computed(() => {
  return passItems.value
    .filter(item => !isPassItemShared(item))
    .slice()
    .sort(compareItemsForVault)
})

const secureLinkItems = computed(() => {
  return passItems.value
    .filter(item => item.secureLinkCount > 0)
    .slice()
    .sort(compareItemsForSecureLinks)
})

const aliasItems = computed(() => {
  return passItems.value
    .filter(item => item.itemType === 'ALIAS')
    .slice()
    .sort(compareItemsByUpdated)
})

const monitorIssueMap = computed<Map<string, PassMonitorItem>>(() => {
  return createPassMonitorIssueMap(passMonitor.value)
})

const policyItems = computed<PassMonitorItem[]>(() => {
  return Array.from(monitorIssueMap.value.values()).sort(compareMonitorItems)
})

const derivedState = computed(() => {
  return derivePassSectionState(passItems.value, passMailboxes.value, passMonitor.value, PASS_ITEMS_FETCH_LIMIT)
})

const totalMonitorSignals = computed(() => {
  return derivedState.value.weakPasswordCount
    + derivedState.value.reusedPasswordCount
    + derivedState.value.inactiveTwoFactorCount
})

const visibleEntries = computed<PassSurfaceEntry[]>(() => {
  if (surfaceKey.value === 'pass-mailbox') {
    return passMailboxes.value
      .slice()
      .sort(compareMailboxes)
      .map(mailbox => createMailboxEntry(mailbox))
  }

  if (surfaceKey.value === 'pass-shared-library') {
    return sharedItems.value.map(item => createItemEntry(item, surfaceKey.value))
  }

  if (surfaceKey.value === 'pass-secure-links') {
    return secureLinkItems.value.map(item => createItemEntry(item, surfaceKey.value))
  }

  if (surfaceKey.value === 'pass-alias-center') {
    return aliasItems.value.map(item => createItemEntry(item, surfaceKey.value))
  }

  if (surfaceKey.value === 'pass-business-policy') {
    return policyItems.value.map(item => createMonitorEntry(item))
  }

  return personalItems.value.map(item => createItemEntry(item, surfaceKey.value))
})

const selectedEntry = computed<PassSurfaceEntry | null>(() => {
  return visibleEntries.value.find(entry => entry.key === selectedEntryKey.value) || visibleEntries.value[0] || null
})

const selectedItem = computed<PassWorkspaceItemSummary | null>(() => {
  if (selectedEntry.value?.item) {
    return selectedEntry.value.item
  }

  const selectedMonitorItemId = selectedEntry.value?.monitorItem?.id || ''
  return passItems.value.find(item => item.id === selectedMonitorItemId) || null
})

const selectedMailbox = computed<PassMailbox | null>(() => {
  return selectedEntry.value?.mailbox || null
})

const selectedMonitorItem = computed<PassMonitorItem | null>(() => {
  if (selectedEntry.value?.monitorItem) {
    return selectedEntry.value.monitorItem
  }

  const selectedItemId = selectedItem.value?.id || ''
  return selectedItemId ? monitorIssueMap.value.get(selectedItemId) || null : null
})

const cappedStateNotice = computed(() => {
  if (!authStore.accessToken || !derivedState.value.itemCoverageCapped) {
    return ''
  }

  return tr(lt(
    '当前仅显示前 200 个项目，项目派生分区与摘要基于这部分结果。',
    '目前僅顯示前 200 個項目，項目衍生分區與摘要以這部分結果為準。',
    'Showing the first 200 items only. Item-derived sections and summaries reflect this subset.'
  ))
})

const boardSubtitle = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt('登录后即可切换到已认证的密码库运行时数据。', '登入後即可切換到已驗證的密碼庫執行期資料。', 'Sign in to switch this pass surface to authenticated runtime data.'))
  }

  if (loadError.value) {
    return loadError.value
  }

  if (passLoading.value && !passItems.value.length && !passMailboxes.value.length && !passMonitor.value) {
    return tr(lt('正在读取项目、收件地址与监控摘要。', '正在讀取項目、收件地址與監控摘要。', 'Loading items, mailboxes, and monitor summary.'))
  }

  if (surfaceKey.value === 'pass-shared-library') {
    return `${sharedItems.value.length} ${tr(lt('个共享项目已载入', '個共享項目已載入', 'shared items loaded'))} · ${derivedState.value.sharedVaultCount} ${tr(lt('个资料库', '個資料庫', 'vaults'))}`
  }

  if (surfaceKey.value === 'pass-secure-links') {
    return `${secureLinkItems.value.length} ${tr(lt('个带链接项目已载入', '個帶連結項目已載入', 'linked items loaded'))} · ${derivedState.value.totalSecureLinkCount} ${tr(lt('条链接', '條連結', 'links'))}`
  }

  if (surfaceKey.value === 'pass-alias-center') {
    return `${aliasItems.value.length} ${tr(lt('个别名项目已载入', '個別名項目已載入', 'alias items loaded'))} · ${derivedState.value.verifiedMailboxCount} ${tr(lt('个已验证收件地址', '個已驗證收件地址', 'verified mailboxes'))}`
  }

  if (surfaceKey.value === 'pass-mailbox') {
    return `${derivedState.value.mailboxCount} ${tr(lt('个收件地址', '個收件地址', 'mailboxes'))} · ${derivedState.value.verifiedMailboxCount} ${tr(lt('个已验证', '個已驗證', 'verified'))}`
  }

  if (surfaceKey.value === 'pass-business-policy') {
    return `${derivedState.value.policyItemCount} ${tr(lt('个治理项目', '個治理項目', 'governance items'))} · ${derivedState.value.trackedItemCount} ${tr(lt('个已跟踪', '個已追蹤', 'tracked'))}`
  }

  return `${personalItems.value.length} ${tr(lt('个个人项目已载入', '個個人項目已載入', 'personal items loaded'))} · ${derivedState.value.trackedItemCount} ${tr(lt('个已监控', '個已監控', 'monitored'))}`
})

const emptyCopy = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt('登录后即可加载密码库工作区。', '登入後即可載入密碼庫工作區。', 'Sign in to load your pass workspace.'))
  }

  if (passLoading.value) {
    return tr(lt('正在同步密码库运行时状态。', '正在同步密碼庫執行期狀態。', 'Syncing pass runtime state.'))
  }

  if (surfaceKey.value === 'pass-mailbox') {
    return tr(lt('当前没有可显示的收件地址。', '目前沒有可顯示的收件地址。', 'No mailboxes are available for this surface.'))
  }

  if (surfaceKey.value === 'pass-secure-links') {
    return tr(lt('当前没有带安全链接的项目。', '目前沒有帶安全連結的項目。', 'No items with secure links are available.'))
  }

  if (surfaceKey.value === 'pass-alias-center') {
    return tr(lt('当前没有可显示的别名项目。', '目前沒有可顯示的別名項目。', 'No alias items are available for this surface.'))
  }

  if (surfaceKey.value === 'pass-business-policy') {
    return tr(lt('当前没有需要重点处理的策略信号。', '目前沒有需要重點處理的政策訊號。', 'No policy signals need attention right now.'))
  }

  if (surfaceKey.value === 'pass-shared-library') {
    return tr(lt('当前没有共享资料库项目。', '目前沒有共享資料庫項目。', 'No shared-library items are available.'))
  }

  return tr(lt('当前视图没有可显示的项目。', '目前檢視沒有可顯示的項目。', 'No items are available for this view.'))
})

const primaryCard = computed<PassDetailCard>(() => {
  if (!authStore.accessToken) {
    return {
      label: tr(lt('已认证运行时', '已驗證執行期', 'Authenticated runtime')),
      title: tr(lt('等待登录', '等待登入', 'Waiting for sign-in')),
      copy: tr(lt('登录后即可读取密码项目、收件地址与监控摘要。', '登入後即可讀取密碼項目、收件地址與監控摘要。', 'Sign in to load pass items, mailboxes, and monitor summary.')),
      facts: []
    }
  }

  if (surfaceKey.value === 'pass-mailbox') {
    return createMailboxPrimaryCard(selectedMailbox.value)
  }

  if (surfaceKey.value === 'pass-business-policy') {
    return createPolicyPrimaryCard(selectedMonitorItem.value)
  }

  return createItemPrimaryCard(selectedItem.value, selectedMonitorItem.value)
})

const secondaryCard = computed<PassDetailCard>(() => {
  if (!authStore.accessToken) {
    return {
      label: tr(lt('运行时摘要', '執行期摘要', 'Runtime summary')),
      title: tr(lt('尚未读取', '尚未讀取', 'Not loaded yet')),
      copy: tr(lt('密码库详情会在认证完成后从运行时接口读取。', '密碼庫詳情會在驗證完成後從執行期介面讀取。', 'Pass details load from runtime APIs after authentication.')),
      facts: []
    }
  }

  if (surfaceKey.value === 'pass-shared-library') {
    return {
      label: tr(lt('共享资料库', '共享資料庫', 'Shared library')),
      title: `${sharedItems.value.length} ${tr(lt('个共享项目已载入', '個共享項目已載入', 'shared items loaded'))}`,
      copy: `${derivedState.value.sharedVaultCount} ${tr(lt('个团队资料库', '個團隊資料庫', 'team vaults'))} · ${derivedState.value.sharedSecureLinkCount} ${tr(lt('条关联链接', '條關聯連結', 'linked links'))}`,
      facts: [
        { label: tr(lt('已载入项目', '已載入項目', 'Loaded items')), value: `${sharedItems.value.length}` },
        { label: tr(lt('资料库', '資料庫', 'Vaults')), value: `${derivedState.value.sharedVaultCount}` },
        { label: tr(lt('关联链接', '關聯連結', 'Linked links')), value: `${derivedState.value.sharedSecureLinkCount}` },
        { label: tr(lt('重点信号', '重點訊號', 'Signals')), value: `${totalMonitorSignals.value}` }
      ]
    }
  }

  if (surfaceKey.value === 'pass-secure-links') {
    return {
      label: tr(lt('安全链接摘要', '安全連結摘要', 'Secure link summary')),
      title: `${derivedState.value.totalSecureLinkCount} ${tr(lt('条已载入链接', '條已載入連結', 'loaded links'))}`,
      copy: selectedItem.value
        ? `${selectedItem.value.title || tr(lt('未命名项目', '未命名項目', 'Untitled item'))} · ${selectedItem.value.secureLinkCount} ${tr(lt('条已载入链接', '條已載入連結', 'loaded links'))}`
        : tr(lt('选择一个带安全链接的项目以查看详情。', '選擇一個帶安全連結的項目以查看詳情。', 'Select an item with secure links to inspect link detail.')),
      facts: [
        { label: tr(lt('有链接项目', '有連結項目', 'Items with links')), value: `${secureLinkItems.value.length}` },
        { label: tr(lt('已载入链接', '已載入連結', 'Loaded links')), value: `${derivedState.value.totalSecureLinkCount}` },
        { label: tr(lt('已跟踪项目', '已追蹤項目', 'Tracked items')), value: `${derivedState.value.trackedItemCount}` },
        { label: tr(lt('弱密码', '弱密碼', 'Weak passwords')), value: `${derivedState.value.weakPasswordCount}` }
      ]
    }
  }

  if (surfaceKey.value === 'pass-alias-center') {
    return {
      label: tr(lt('别名路由', '別名路由', 'Alias routing')),
      title: `${aliasItems.value.length} ${tr(lt('个别名项目已载入', '個別名項目已載入', 'alias items loaded'))}`,
      copy: derivedState.value.defaultMailboxEmail
        ? `${tr(lt('默认收件地址', '預設收件地址', 'Default mailbox'))} · ${derivedState.value.defaultMailboxEmail}`
        : tr(lt('当前没有默认收件地址。', '目前沒有預設收件地址。', 'No default mailbox is configured yet.')),
      facts: [
        { label: tr(lt('已载入别名项目', '已載入別名項目', 'Loaded alias items')), value: `${aliasItems.value.length}` },
        { label: tr(lt('收件地址', '收件地址', 'Mailboxes')), value: `${derivedState.value.mailboxCount}` },
        { label: tr(lt('已验证', '已驗證', 'Verified')), value: `${derivedState.value.verifiedMailboxCount}` },
        { label: tr(lt('默认地址', '預設地址', 'Default')), value: derivedState.value.defaultMailboxEmail || tr(lt('未设置', '未設定', 'Not set')) }
      ]
    }
  }

  if (surfaceKey.value === 'pass-mailbox') {
    return {
      label: tr(lt('收件地址摘要', '收件地址摘要', 'Mailbox summary')),
      title: `${derivedState.value.mailboxCount} ${tr(lt('个收件地址', '個收件地址', 'mailboxes'))}`,
      copy: derivedState.value.primaryMailboxEmail
        ? `${tr(lt('主地址', '主地址', 'Primary mailbox'))} · ${derivedState.value.primaryMailboxEmail}`
        : tr(lt('当前没有主收件地址。', '目前沒有主收件地址。', 'No primary mailbox is configured yet.')),
      facts: [
        { label: tr(lt('总数', '總數', 'Total')), value: `${derivedState.value.mailboxCount}` },
        { label: tr(lt('已验证', '已驗證', 'Verified')), value: `${derivedState.value.verifiedMailboxCount}` },
        { label: tr(lt('默认地址', '預設地址', 'Default')), value: derivedState.value.defaultMailboxEmail || tr(lt('未设置', '未設定', 'Not set')) },
        { label: tr(lt('主地址', '主地址', 'Primary')), value: derivedState.value.primaryMailboxEmail || tr(lt('未设置', '未設定', 'Not set')) }
      ]
    }
  }

  if (surfaceKey.value === 'pass-business-policy') {
    return {
      label: tr(lt('策略姿态', '政策姿態', 'Policy posture')),
      title: `${totalMonitorSignals.value} ${tr(lt('项重点信号', '項重點訊號', 'priority signals'))}`,
      copy: `${derivedState.value.trackedItemCount} ${tr(lt('个项目已纳入监控', '個項目已納入監控', 'items are tracked by monitor'))}`,
      facts: [
        { label: tr(lt('弱密码', '弱密碼', 'Weak passwords')), value: `${derivedState.value.weakPasswordCount}` },
        { label: tr(lt('重复密码', '重複密碼', 'Reused passwords')), value: `${derivedState.value.reusedPasswordCount}` },
        { label: tr(lt('2FA 未激活', '2FA 未啟用', '2FA inactive')), value: `${derivedState.value.inactiveTwoFactorCount}` },
        { label: tr(lt('已排除', '已排除', 'Excluded')), value: `${derivedState.value.excludedItemCount}` }
      ]
    }
  }

  return {
    label: tr(lt('监控摘要', '監控摘要', 'Monitor summary')),
    title: `${derivedState.value.trackedItemCount} / ${passMonitor.value?.totalItemCount || 0}`,
    copy: tr(lt('项目、收件地址和监控状态都来自已认证运行时接口。', '項目、收件地址和監控狀態都來自已驗證執行期介面。', 'Items, mailboxes, and monitor state all come from authenticated runtime APIs.')),
    facts: [
      { label: tr(lt('弱密码', '弱密碼', 'Weak passwords')), value: `${derivedState.value.weakPasswordCount}` },
      { label: tr(lt('重复密码', '重複密碼', 'Reused passwords')), value: `${derivedState.value.reusedPasswordCount}` },
      { label: tr(lt('2FA 未激活', '2FA 未啟用', '2FA inactive')), value: `${derivedState.value.inactiveTwoFactorCount}` },
      { label: tr(lt('已排除', '已排除', 'Excluded')), value: `${derivedState.value.excludedItemCount}` }
    ]
  }
})

async function loadPass() {
  const requestId = ++latestPassRequest
  const requestToken = authStore.accessToken
  const requestPath = route.fullPath

  if (!requestToken) {
    if (requestId !== latestPassRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath) {
      return
    }

    passItems.value = []
    passMailboxes.value = []
    passMonitor.value = null
    selectedEntryKey.value = ''
    loadError.value = ''
    passLoading.value = false
    return
  }

  passLoading.value = true
  loadError.value = ''

  try {
    const [itemsResponse, mailboxesResponse, monitorResponse] = await Promise.all([
      listPassItems(requestToken, { limit: String(PASS_ITEMS_FETCH_LIMIT) }),
      listPassMailboxes(requestToken),
      readPassMonitor(requestToken)
    ])

    if (requestId !== latestPassRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath) {
      return
    }

    passItems.value = Array.isArray(itemsResponse.data) ? itemsResponse.data : []
    passMailboxes.value = Array.isArray(mailboxesResponse.data) ? mailboxesResponse.data : []
    passMonitor.value = monitorResponse.data || null
  } catch (error) {
    if (requestId !== latestPassRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath) {
      return
    }

    passItems.value = []
    passMailboxes.value = []
    passMonitor.value = null
    selectedEntryKey.value = ''
    loadError.value = resolveErrorMessage(
      error,
      tr(lt('读取密码库数据失败，请稍后重试。', '讀取密碼庫資料失敗，請稍後重試。', 'Failed to load pass data. Please try again later.'))
    )
  } finally {
    if (requestId === latestPassRequest && requestToken === authStore.accessToken && requestPath === route.fullPath) {
      passLoading.value = false
    }
  }
}

function openSection(key: string) {
  const pathMap: Record<string, string> = {
    pass: '/pass',
    'pass-shared-library': '/pass/shared-library',
    'pass-secure-links': '/pass/secure-links',
    'pass-alias-center': '/pass/alias-center',
    'pass-mailbox': '/pass/mailbox',
    'pass-business-policy': '/pass/business-policy'
  }
  void router.push(pathMap[key] ?? '/pass')
}

function selectEntry(entryKey: string) {
  selectedEntryKey.value = entryKey
}

function createItemEntry(item: PassWorkspaceItemSummary, key: string): PassSurfaceEntry {
  const title = item.title || tr(lt('未命名项目', '未命名項目', 'Untitled item'))
  const fallbackSubtitle = item.username || item.website || resolvePassItemType(item.itemType)
  const badge = key === 'pass-secure-links'
    ? `${item.secureLinkCount} ${tr(lt('条链接', '條連結', 'links'))}`
    : item.favorite
      ? tr(lt('星标', '星標', 'Starred'))
      : resolvePassItemType(item.itemType)

  return {
    key: `item:${item.id}`,
    id: item.id,
    title,
    subtitle: fallbackSubtitle,
    meta: joinText([
      resolvePassScope(item.scopeType),
      key === 'pass-shared-library' && item.sharedVaultId ? tr(lt('共享资料库', '共享資料庫', 'Shared vault')) : null,
      formatDateTime(item.updatedAt)
    ]),
    badge,
    avatar: createAvatar(title),
    kind: 'item',
    item
  }
}

function createMailboxEntry(mailbox: PassMailbox): PassSurfaceEntry {
  return {
    key: `mailbox:${mailbox.id}`,
    id: mailbox.id,
    title: mailbox.mailboxEmail,
    subtitle: resolveMailboxStatus(mailbox),
    meta: joinText([
      mailbox.defaultMailbox ? tr(lt('默认地址', '預設地址', 'Default')) : null,
      mailbox.primaryMailbox ? tr(lt('主地址', '主地址', 'Primary')) : null,
      formatDateTime(mailbox.verifiedAt || mailbox.updatedAt)
    ]),
    badge: mailbox.defaultMailbox
      ? tr(lt('默认', '預設', 'Default'))
      : mailbox.primaryMailbox
        ? tr(lt('主地址', '主地址', 'Primary'))
        : resolveMailboxStatus(mailbox),
    avatar: createAvatar(mailbox.mailboxEmail),
    kind: 'mailbox',
    mailbox
  }
}

function createMonitorEntry(item: PassMonitorItem): PassSurfaceEntry {
  const title = item.title || tr(lt('未命名项目', '未命名項目', 'Untitled item'))

  return {
    key: `policy:${item.id}`,
    id: item.id,
    title,
    subtitle: item.website || item.username || resolvePassItemType(item.itemType),
    meta: joinText([
      resolveMonitorFlags(item),
      formatDateTime(item.updatedAt)
    ]),
    badge: `${countMonitorSignals(item)} ${tr(lt('项', '項', 'signals'))}`,
    avatar: createAvatar(title),
    kind: 'item',
    monitorItem: item
  }
}

function createItemPrimaryCard(item: PassWorkspaceItemSummary | null, monitorItem: PassMonitorItem | null): PassDetailCard {
  if (!item) {
    return {
      label: tr(lt('已选项目', '已選項目', 'Selected item')),
      title: tr(lt('尚未选择项目', '尚未選取項目', 'No item selected')),
      copy: tr(lt('从列表中选择一个项目以查看已认证详情。', '從清單中選擇一個項目以查看已驗證詳情。', 'Select an item from the list to inspect authenticated detail.')),
      facts: []
    }
  }

  return {
    label: tr(lt('已选项目', '已選項目', 'Selected item')),
    title: item.title || tr(lt('未命名项目', '未命名項目', 'Untitled item')),
    copy: joinText([
      item.website,
      item.username,
      monitorItem ? resolveMonitorFlags(monitorItem) : null
    ]) || tr(lt('该项目已从运行时接口载入。', '該項目已從執行期介面載入。', 'This item was loaded from a runtime API.')),
    facts: [
      { label: tr(lt('类型', '類型', 'Type')), value: resolvePassItemType(item.itemType) },
      { label: tr(lt('范围', '範圍', 'Scope')), value: resolvePassScope(item.scopeType) },
      { label: tr(lt('安全链接', '安全連結', 'Secure links')), value: `${item.secureLinkCount}` },
      { label: tr(lt('更新时间', '更新時間', 'Updated')), value: formatDateTime(item.updatedAt) }
    ]
  }
}

function createMailboxPrimaryCard(mailbox: PassMailbox | null): PassDetailCard {
  if (!mailbox) {
    return {
      label: tr(lt('已选收件地址', '已選收件地址', 'Selected mailbox')),
      title: tr(lt('尚未选择收件地址', '尚未選取收件地址', 'No mailbox selected')),
      copy: tr(lt('从列表中选择一个收件地址以查看认证状态。', '從清單中選擇一個收件地址以查看驗證狀態。', 'Select a mailbox from the list to inspect verification state.')),
      facts: []
    }
  }

  return {
    label: tr(lt('已选收件地址', '已選收件地址', 'Selected mailbox')),
    title: mailbox.mailboxEmail,
    copy: describeMailbox(mailbox),
    facts: [
      { label: tr(lt('状态', '狀態', 'Status')), value: resolveMailboxStatus(mailbox) },
      { label: tr(lt('默认地址', '預設地址', 'Default')), value: mailbox.defaultMailbox ? tr(lt('是', '是', 'Yes')) : tr(lt('否', '否', 'No')) },
      { label: tr(lt('主地址', '主地址', 'Primary')), value: mailbox.primaryMailbox ? tr(lt('是', '是', 'Yes')) : tr(lt('否', '否', 'No')) },
      { label: tr(lt('更新时间', '更新時間', 'Updated')), value: formatDateTime(mailbox.updatedAt) }
    ]
  }
}

function createPolicyPrimaryCard(item: PassMonitorItem | null): PassDetailCard {
  if (!item) {
    return {
      label: tr(lt('策略焦点', '政策焦點', 'Policy focus')),
      title: tr(lt('尚未选择项目', '尚未選取項目', 'No item selected')),
      copy: tr(lt('选择一个监控项目以查看弱密码、重复密码或 2FA 状态。', '選擇一個監控項目以查看弱密碼、重複密碼或 2FA 狀態。', 'Select a monitored item to inspect weak password, reuse, or 2FA state.')),
      facts: []
    }
  }

  return {
    label: tr(lt('策略焦点', '政策焦點', 'Policy focus')),
    title: item.title || tr(lt('未命名项目', '未命名項目', 'Untitled item')),
    copy: joinText([
      item.website,
      item.username,
      resolveMonitorFlags(item)
    ]) || tr(lt('该项目需要策略关注。', '該項目需要政策關注。', 'This item needs policy attention.')),
    facts: [
      { label: tr(lt('弱密码', '弱密碼', 'Weak password')), value: item.weakPassword ? tr(lt('是', '是', 'Yes')) : tr(lt('否', '否', 'No')) },
      { label: tr(lt('重复使用', '重複使用', 'Reused')), value: item.reusedPassword ? `${item.reusedGroupSize || 2} ${tr(lt('个项目', '個項目', 'items'))}` : tr(lt('否', '否', 'No')) },
      { label: tr(lt('2FA', '2FA', '2FA')), value: item.inactiveTwoFactor ? tr(lt('未激活', '未啟用', 'Inactive')) : tr(lt('正常', '正常', 'Active')) },
      { label: tr(lt('排除状态', '排除狀態', 'Excluded')), value: item.excluded ? tr(lt('已排除', '已排除', 'Excluded')) : tr(lt('已跟踪', '已追蹤', 'Tracked')) }
    ]
  }
}


function compareItemsForVault(left: PassWorkspaceItemSummary, right: PassWorkspaceItemSummary) {
  const favoriteDelta = Number(right.favorite) - Number(left.favorite)
  if (favoriteDelta !== 0) {
    return favoriteDelta
  }

  return compareItemsByUpdated(left, right)
}

function compareItemsForSecureLinks(left: PassWorkspaceItemSummary, right: PassWorkspaceItemSummary) {
  const linkDelta = right.secureLinkCount - left.secureLinkCount
  if (linkDelta !== 0) {
    return linkDelta
  }

  return compareItemsByUpdated(left, right)
}

function compareItemsByUpdated(left: Pick<PassWorkspaceItemSummary, 'title' | 'updatedAt'>, right: Pick<PassWorkspaceItemSummary, 'title' | 'updatedAt'>) {
  return compareDateDesc(left.updatedAt, right.updatedAt) || String(left.title || '').localeCompare(String(right.title || ''))
}

function compareMailboxes(left: PassMailbox, right: PassMailbox) {
  const defaultDelta = Number(right.defaultMailbox) - Number(left.defaultMailbox)
  if (defaultDelta !== 0) {
    return defaultDelta
  }

  const primaryDelta = Number(right.primaryMailbox) - Number(left.primaryMailbox)
  if (primaryDelta !== 0) {
    return primaryDelta
  }

  const verifiedDelta = Number(isPassMailboxVerified(right)) - Number(isPassMailboxVerified(left))
  if (verifiedDelta !== 0) {
    return verifiedDelta
  }

  return compareDateDesc(left.updatedAt, right.updatedAt) || left.mailboxEmail.localeCompare(right.mailboxEmail)
}

function compareMonitorItems(left: PassMonitorItem, right: PassMonitorItem) {
  const signalDelta = countMonitorSignals(right) - countMonitorSignals(left)
  if (signalDelta !== 0) {
    return signalDelta
  }

  return compareDateDesc(left.updatedAt, right.updatedAt) || String(left.title || '').localeCompare(String(right.title || ''))
}

function countMonitorSignals(item: PassMonitorItem) {
  return Number(item.weakPassword) + Number(item.reusedPassword) + Number(item.inactiveTwoFactor) + Number(item.excluded)
}

function compareDateDesc(left: string | null | undefined, right: string | null | undefined) {
  return parseDateValue(right) - parseDateValue(left)
}

function parseDateValue(value: string | null | undefined) {
  if (!value) {
    return 0
  }

  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? 0 : parsed.getTime()
}

function resolvePassItemType(itemType: PassWorkspaceItemSummary['itemType'] | PassMonitorItem['itemType']) {
  if (itemType === 'LOGIN') {
    return tr(lt('登录', '登入', 'Login'))
  }

  if (itemType === 'PASSWORD') {
    return tr(lt('密码', '密碼', 'Password'))
  }

  if (itemType === 'NOTE') {
    return tr(lt('安全笔记', '安全筆記', 'Secure note'))
  }

  if (itemType === 'CARD') {
    return tr(lt('卡片', '卡片', 'Card'))
  }

  if (itemType === 'ALIAS') {
    return tr(lt('别名', '別名', 'Alias'))
  }

  if (itemType === 'PASSKEY') {
    return tr(lt('通行密钥', '通行金鑰', 'Passkey'))
  }

  return itemType
}

function resolvePassScope(scopeType: PassWorkspaceItemSummary['scopeType'] | PassMonitorItem['scopeType']) {
  return scopeType === 'SHARED'
    ? tr(lt('共享', '共享', 'Shared'))
    : tr(lt('个人', '個人', 'Personal'))
}

function resolveMailboxStatus(mailbox: PassMailbox) {
  return isPassMailboxVerified(mailbox)
    ? tr(lt('已验证', '已驗證', 'Verified'))
    : tr(lt('待验证', '待驗證', 'Pending verification'))
}

function resolveMonitorFlags(item: PassMonitorItem) {
  return joinText([
    item.weakPassword ? tr(lt('弱密码', '弱密碼', 'Weak password')) : null,
    item.reusedPassword ? tr(lt('重复密码', '重複密碼', 'Reused password')) : null,
    item.inactiveTwoFactor ? tr(lt('2FA 未激活', '2FA 未啟用', '2FA inactive')) : null,
    item.excluded ? tr(lt('已排除', '已排除', 'Excluded')) : null
  ])
}

function describeMailbox(mailbox: PassMailbox) {
  return joinText([
    resolveMailboxStatus(mailbox),
    mailbox.defaultMailbox ? tr(lt('默认路由', '預設路由', 'Default routing')) : null,
    mailbox.primaryMailbox ? tr(lt('主地址', '主地址', 'Primary mailbox')) : null,
    mailbox.verifiedAt ? formatDateTime(mailbox.verifiedAt) : formatDateTime(mailbox.updatedAt)
  ])
}

function createAvatar(value: string) {
  const match = value.trim().match(/[\p{L}\p{N}]/u)
  return (match?.[0] || '#').toUpperCase()
}

function joinText(parts: Array<string | null | undefined>) {
  return parts
    .filter((value): value is string => Boolean(value && value.trim()))
    .join(' · ')
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return tr(lt('未设置', '未設定', 'Not set'))
  }

  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return value
  }

  return parsed.toLocaleString()
}

function sumSecureLinks(items: PassWorkspaceItemSummary[]) {
  return items.reduce((total, item) => total + Math.max(0, item.secureLinkCount || 0), 0)
}

function resolveErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallback
}

watch(() => [route.fullPath, authStore.accessToken], () => {
  void loadPass()
}, { immediate: true })

watch(visibleEntries, (entries) => {
  if (!entries.some(entry => entry.key === selectedEntryKey.value)) {
    selectedEntryKey.value = entries[0]?.key || ''
  }
}, { immediate: true })
</script>

<template>
  <section class="pass-surface">
    <aside class="pass-surface__nav">
      <span class="section-label">{{ tr(lt('密码库', '密碼庫', 'Pass')) }}</span>
      <strong>{{ tr(current.label) }}</strong>
      <button
        v-for="item in passSections"
        :key="item.key"
        type="button"
        :class="{ 'pass-surface__nav--active': item.key === current.key }"
        @click="openSection(item.key)"
      >
        {{ tr(item.label) }}
      </button>
      <button class="pass-surface__primary" type="button">{{ tr(lt('+ 新建项目', '+ 新增項目', '+ New Item')) }}</button>
    </aside>

    <section class="pass-surface__list">
      <header class="pass-surface__head">
        <div>
          <span class="section-label">{{ tr(lt('分区', '分區', 'Section')) }}</span>
          <h1>{{ tr(current.label) }}</h1>
          <p class="page-subtitle">{{ tr(current.description) }}</p>
          <p class="page-subtitle pass-surface__status" :class="{ 'pass-surface__status--error': loadError }">{{ boardSubtitle }}</p>
          <p v-if="cappedStateNotice" class="page-subtitle pass-surface__status">{{ cappedStateNotice }}</p>
        </div>
      </header>
      <button
        v-for="entry in visibleEntries"
        :key="entry.key"
        type="button"
        class="surface-card pass-surface__item"
        :class="{ 'pass-surface__item--active': entry.key === selectedEntryKey }"
        @click="selectEntry(entry.key)"
      >
        <span class="pass-surface__avatar">{{ entry.avatar }}</span>
        <div class="pass-surface__item-body">
          <strong>{{ entry.title }}</strong>
          <span>{{ entry.subtitle }}</span>
          <span class="pass-surface__meta">{{ entry.meta }}</span>
        </div>
        <span class="pass-surface__badge">{{ entry.badge }}</span>
      </button>
      <article v-if="!visibleEntries.length" class="surface-card pass-surface__card">
        <span class="section-label">{{ tr(lt('状态', '狀態', 'Status')) }}</span>
        <strong>{{ tr(lt('暂无数据', '暫無資料', 'No data')) }}</strong>
        <p class="page-subtitle">{{ emptyCopy }}</p>
      </article>
    </section>

    <section class="pass-surface__detail">
      <article class="surface-card pass-surface__card">
        <span class="section-label">{{ primaryCard.label }}</span>
        <strong>{{ primaryCard.title }}</strong>
        <p class="page-subtitle">{{ primaryCard.copy }}</p>
        <dl v-if="primaryCard.facts.length" class="pass-surface__facts">
          <div v-for="fact in primaryCard.facts" :key="fact.label">
            <dt>{{ fact.label }}</dt>
            <dd>{{ fact.value }}</dd>
          </div>
        </dl>
      </article>
      <article class="surface-card pass-surface__card">
        <span class="section-label">{{ secondaryCard.label }}</span>
        <strong>{{ secondaryCard.title }}</strong>
        <p class="page-subtitle">{{ secondaryCard.copy }}</p>
        <dl v-if="secondaryCard.facts.length" class="pass-surface__facts">
          <div v-for="fact in secondaryCard.facts" :key="fact.label">
            <dt>{{ fact.label }}</dt>
            <dd>{{ fact.value }}</dd>
          </div>
        </dl>
      </article>
    </section>
  </section>
</template>

<style scoped>
.pass-surface {
  display: grid;
  grid-template-columns: 220px 360px minmax(0, 1fr);
  min-height: calc(100vh - 56px);
  background: var(--mm-card);
}

.pass-surface__nav,
.pass-surface__list,
.pass-surface__detail {
  display: grid;
  align-content: start;
  gap: 12px;
  padding: 16px;
}

.pass-surface__nav,
.pass-surface__list {
  border-right: 1px solid var(--mm-border);
}

.pass-surface__nav {
  background: var(--mm-side-surface);
}

.pass-surface__nav button,
.pass-surface__primary {
  min-height: 34px;
  padding: 0 12px;
  border-radius: 10px;
}

.pass-surface__nav button {
  border: 1px solid transparent;
  background: transparent;
  color: var(--mm-text-secondary);
  text-align: left;
}

.pass-surface__nav--active {
  border-color: rgba(228, 123, 57, 0.24) !important;
  background: rgba(228, 123, 57, 0.1) !important;
  color: var(--mm-pass) !important;
}

.pass-surface__primary {
  margin-top: auto;
  border: 0;
  background: linear-gradient(180deg, #1f2937 0%, #111827 100%);
  color: #fff;
}

.pass-surface__head h1 {
  margin: 8px 0 0;
  font-size: 24px;
  letter-spacing: -0.04em;
}

.pass-surface__status {
  margin-top: 8px;
}

.pass-surface__status--error {
  color: #d14343;
}

.pass-surface__item,
.pass-surface__card {
  display: grid;
  gap: 10px;
  padding: 16px;
}

.pass-surface__item {
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  border: 1px solid var(--mm-border);
  background: var(--mm-card);
  text-align: left;
}

.pass-surface__item--active {
  border-color: rgba(228, 123, 57, 0.28);
  background: rgba(228, 123, 57, 0.08);
}

.pass-surface__item-body {
  display: grid;
  gap: 4px;
}

.pass-surface__avatar {
  display: inline-grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 14px;
  background: rgba(228, 123, 57, 0.12);
  color: var(--mm-pass);
  font-weight: 700;
}

.pass-surface__item-body span,
.pass-surface__meta,
.pass-surface__badge {
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.pass-surface__badge {
  justify-self: end;
  padding-left: 8px;
  text-align: right;
}

.pass-surface__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 0;
}

.pass-surface__facts div {
  display: grid;
  gap: 4px;
}

.pass-surface__facts dt {
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.pass-surface__facts dd {
  margin: 0;
  color: var(--mm-ink);
  font-weight: 600;
}

@media (max-width: 980px) {
  .pass-surface {
    grid-template-columns: 220px minmax(0, 1fr);
  }

  .pass-surface__detail {
    display: none;
  }
}

@media (max-width: 820px) {
  .pass-surface {
    grid-template-columns: 1fr;
    padding-bottom: 88px;
  }

  .pass-surface__nav {
    align-items: center;
    display: flex;
    gap: 10px;
    overflow-x: auto;
    border-right: 0;
    border-bottom: 1px solid var(--mm-border);
    white-space: nowrap;
  }

  .pass-surface__nav > * {
    flex: 0 0 auto;
  }

  .pass-surface__nav button {
    white-space: nowrap;
  }

  .pass-surface__list {
    border-right: 0;
    border-bottom: 1px solid var(--mm-border);
  }

  .pass-surface__facts {
    grid-template-columns: 1fr;
  }
}
</style>
