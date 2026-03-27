<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type {
  AuditEvent,
  BlockedDomain,
  BlockedSender,
  RuleResolution,
  SuiteRemediationAction,
  SuiteRemediationExecutionResult,
  SuiteSecurityPosture,
  TrustedDomain,
  TrustedSender,
  UserSession
} from '~/types/api'
import type { CreatePassMailAliasRequest, PassMailAlias, PassMailbox } from '~/types/pass-business'
import { useAuthApi } from '~/composables/useAuthApi'
import { useAuthStore } from '~/stores/auth'
import { useAuditApi } from '~/composables/useAuditApi'
import { useSettingsApi } from '~/composables/useSettingsApi'
import { useI18n } from '~/composables/useI18n'
import { useSuiteApi } from '~/composables/useSuiteApi'
import { usePassApi } from '~/composables/usePassApi'

const authStore = useAuthStore()
const { t } = useI18n()

const loadingEvents = ref(false)
const loadingSessions = ref(false)
const loadingBlockedSenders = ref(false)
const loadingTrustedSenders = ref(false)
const loadingBlockedDomains = ref(false)
const loadingTrustedDomains = ref(false)
const loadingRuleResolution = ref(false)
const loadingPosture = ref(false)
const loadingAliasSecurity = ref(false)
const aliasMutationId = ref('')
const runningPostureActionCode = ref('')
const events = ref<AuditEvent[]>([])
const sessions = ref<UserSession[]>([])
const blockedSenders = ref<BlockedSender[]>([])
const trustedSenders = ref<TrustedSender[]>([])
const blockedDomains = ref<BlockedDomain[]>([])
const trustedDomains = ref<TrustedDomain[]>([])
const securityPosture = ref<SuiteSecurityPosture | null>(null)
const securityAliases = ref<PassMailAlias[]>([])
const securityMailboxes = ref<PassMailbox[]>([])
const ruleTargetEmail = ref('')
const ruleResolution = ref<RuleResolution | null>(null)
const blockedSenderEmail = ref('')
const trustedSenderEmail = ref('')
const blockedDomainName = ref('')
const trustedDomainName = ref('')
const lastPostureExecutionResult = ref<SuiteRemediationExecutionResult | null>(null)
const walletPostureAlerts = computed(() => {
  return (securityPosture.value?.alerts || []).filter((alert) => alert.toLowerCase().includes('wallet'))
})
const walletPostureActions = computed(() => {
  return (securityPosture.value?.recommendedActions || []).filter((action) => action.productCode === 'WALLET')
})
const securityForwardTargetOptions = computed(() => securityMailboxes.value
  .filter(mailbox => mailbox.status === 'VERIFIED')
  .map((mailbox) => ({
    label: [
      mailbox.mailboxEmail,
      mailbox.defaultMailbox ? 'Default' : 'Verified',
      mailbox.primaryMailbox ? 'Primary' : ''
    ].filter(Boolean).join(' · '),
    value: mailbox.mailboxEmail
  })))
const currentUserEmail = computed(() => authStore.user?.email || '')

const { fetchEvents } = useAuditApi()
const { logoutAll, listSessions, revokeSession } = useAuthApi()
const { getSecurityPosture, executeRemediationAction } = useSuiteApi()
const { listAliases, listMailboxes, createAlias } = usePassApi()
const {
  listBlockedSenders,
  addBlockedSender,
  removeBlockedSender,
  listTrustedSenders,
  addTrustedSender,
  removeTrustedSender,
  listBlockedDomains,
  addBlockedDomain,
  removeBlockedDomain,
  listTrustedDomains,
  addTrustedDomain,
  removeTrustedDomain,
  resolveRule
} = useSettingsApi()

function riskTagType(level: SuiteSecurityPosture['overallRiskLevel']): 'success' | 'warning' | 'danger' | 'info' {
  if (level === 'LOW') {
    return 'success'
  }
  if (level === 'MEDIUM') {
    return 'warning'
  }
  if (level === 'HIGH' || level === 'CRITICAL') {
    return 'danger'
  }
  return 'info'
}

function priorityTagType(priority: 'P0' | 'P1' | 'P2'): 'danger' | 'warning' | 'info' {
  if (priority === 'P0') {
    return 'danger'
  }
  if (priority === 'P1') {
    return 'warning'
  }
  return 'info'
}

function canExecuteAction(action: SuiteRemediationAction): boolean {
  return Boolean(action.actionCode)
}

async function loadSecurityPosture(): Promise<void> {
  loadingPosture.value = true
  try {
    securityPosture.value = await getSecurityPosture()
  } catch {
    securityPosture.value = null
  } finally {
    loadingPosture.value = false
  }
}

async function loadAliasSecurityPanel(): Promise<void> {
  loadingAliasSecurity.value = true
  try {
    const [aliases, mailboxes] = await Promise.all([listAliases(), listMailboxes()])
    securityAliases.value = aliases
    securityMailboxes.value = mailboxes
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to load alias security panel'
    ElMessage.error(message)
  } finally {
    loadingAliasSecurity.value = false
  }
}

async function loadEvents(): Promise<void> {
  loadingEvents.value = true
  try {
    events.value = await fetchEvents()
  } catch {
    events.value = []
  } finally {
    loadingEvents.value = false
  }
}

async function loadSessions(): Promise<void> {
  loadingSessions.value = true
  try {
    sessions.value = await listSessions()
  } catch {
    sessions.value = []
  } finally {
    loadingSessions.value = false
  }
}

async function loadBlockedSenderList(): Promise<void> {
  loadingBlockedSenders.value = true
  try {
    blockedSenders.value = await listBlockedSenders()
  } catch {
    blockedSenders.value = []
  } finally {
    loadingBlockedSenders.value = false
  }
}

async function loadTrustedSenderList(): Promise<void> {
  loadingTrustedSenders.value = true
  try {
    trustedSenders.value = await listTrustedSenders()
  } catch {
    trustedSenders.value = []
  } finally {
    loadingTrustedSenders.value = false
  }
}

async function loadBlockedDomainList(): Promise<void> {
  loadingBlockedDomains.value = true
  try {
    blockedDomains.value = await listBlockedDomains()
  } catch {
    blockedDomains.value = []
  } finally {
    loadingBlockedDomains.value = false
  }
}

async function loadTrustedDomainList(): Promise<void> {
  loadingTrustedDomains.value = true
  try {
    trustedDomains.value = await listTrustedDomains()
  } catch {
    trustedDomains.value = []
  } finally {
    loadingTrustedDomains.value = false
  }
}

async function logoutAllDevices(): Promise<void> {
  await logoutAll()
  ElMessage.success('All sessions signed out')
  await navigateTo('/login')
}

async function onRevokeSession(sessionId: string): Promise<void> {
  await revokeSession(sessionId)
  ElMessage.success('Session revoked')
  await Promise.all([loadSessions(), loadSecurityPosture()])
}

async function onAddBlockedSender(): Promise<void> {
  const email = blockedSenderEmail.value.trim()
  if (!email) {
    return
  }
  try {
    await addBlockedSender(email)
    blockedSenderEmail.value = ''
    ElMessage.success('Blocked sender added')
    await Promise.all([loadBlockedSenderList(), loadSecurityPosture()])
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to add blocked sender'
    ElMessage.error(message)
  }
}

async function onRemoveBlockedSender(blockedSenderId: string): Promise<void> {
  try {
    await removeBlockedSender(blockedSenderId)
    ElMessage.success('Blocked sender removed')
    await Promise.all([loadBlockedSenderList(), loadSecurityPosture()])
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to remove blocked sender'
    ElMessage.error(message)
  }
}

async function onAddTrustedSender(): Promise<void> {
  const email = trustedSenderEmail.value.trim()
  if (!email) {
    return
  }
  try {
    await addTrustedSender(email)
    trustedSenderEmail.value = ''
    ElMessage.success('Trusted sender added')
    await Promise.all([loadTrustedSenderList(), loadSecurityPosture()])
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to add trusted sender'
    ElMessage.error(message)
  }
}

async function onRemoveTrustedSender(trustedSenderId: string): Promise<void> {
  try {
    await removeTrustedSender(trustedSenderId)
    ElMessage.success('Trusted sender removed')
    await Promise.all([loadTrustedSenderList(), loadSecurityPosture()])
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to remove trusted sender'
    ElMessage.error(message)
  }
}

async function onAddBlockedDomain(): Promise<void> {
  const domain = blockedDomainName.value.trim()
  if (!domain) {
    return
  }
  try {
    await addBlockedDomain(domain)
    blockedDomainName.value = ''
    ElMessage.success('Blocked domain added')
    await Promise.all([loadBlockedDomainList(), loadTrustedDomainList(), loadSecurityPosture()])
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to add blocked domain'
    ElMessage.error(message)
  }
}

async function onRemoveBlockedDomain(blockedDomainId: string): Promise<void> {
  try {
    await removeBlockedDomain(blockedDomainId)
    ElMessage.success('Blocked domain removed')
    await Promise.all([loadBlockedDomainList(), loadSecurityPosture()])
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to remove blocked domain'
    ElMessage.error(message)
  }
}

async function onAddTrustedDomain(): Promise<void> {
  const domain = trustedDomainName.value.trim()
  if (!domain) {
    return
  }
  try {
    await addTrustedDomain(domain)
    trustedDomainName.value = ''
    ElMessage.success('Trusted domain added')
    await Promise.all([loadTrustedDomainList(), loadBlockedDomainList(), loadSecurityPosture()])
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to add trusted domain'
    ElMessage.error(message)
  }
}

async function onRemoveTrustedDomain(trustedDomainId: string): Promise<void> {
  try {
    await removeTrustedDomain(trustedDomainId)
    ElMessage.success('Trusted domain removed')
    await Promise.all([loadTrustedDomainList(), loadSecurityPosture()])
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to remove trusted domain'
    ElMessage.error(message)
  }
}

async function onCheckRuleResolution(): Promise<void> {
  const senderEmail = ruleTargetEmail.value.trim()
  if (!senderEmail) {
    return
  }
  loadingRuleResolution.value = true
  try {
    ruleResolution.value = await resolveRule(senderEmail)
    ElMessage.success('Policy checked successfully')
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to check policy'
    ElMessage.error(message)
    ruleResolution.value = null
  } finally {
    loadingRuleResolution.value = false
  }
}

async function onExecutePostureAction(action: SuiteRemediationAction): Promise<void> {
  if (!action.actionCode) {
    ElMessage.info('This remediation action requires manual execution')
    return
  }
  runningPostureActionCode.value = action.actionCode
  try {
    const result = await executeRemediationAction(action.actionCode)
    lastPostureExecutionResult.value = result
    if (result.status === 'SUCCESS') {
      ElMessage.success(result.message || 'Remediation action executed')
    } else if (result.status === 'NO_OP') {
      ElMessage.info(result.message || 'No changes were required')
    } else {
      ElMessage.warning(result.message || 'Remediation action completed with warnings')
    }
    await Promise.all([loadSecurityPosture(), loadEvents()])
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to execute remediation action'
    ElMessage.error(message)
  } finally {
    runningPostureActionCode.value = ''
  }
}

async function copyAliasToClipboard(aliasEmail: string): Promise<void> {
  if (!navigator.clipboard?.writeText) {
    throw new Error('Clipboard API is unavailable in the current browser context')
  }
  await navigator.clipboard.writeText(aliasEmail)
}

async function onCreateSecurityAlias(payload: CreatePassMailAliasRequest): Promise<void> {
  aliasMutationId.value = 'create'
  try {
    const alias = await createAlias(payload)
    await Promise.all([loadAliasSecurityPanel(), loadSecurityPosture(), loadEvents()])
    await copyAliasToClipboard(alias.aliasEmail)
    ElMessage.success('Alias created and copied')
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to create alias'
    ElMessage.error(message)
  } finally {
    aliasMutationId.value = ''
  }
}

async function openPassAliasCenter(): Promise<void> {
  await navigateTo('/pass')
}

onMounted(() => {
  loadSessions()
  loadSecurityPosture()
  loadAliasSecurityPanel()
  loadEvents()
  loadBlockedSenderList()
  loadTrustedSenderList()
  loadBlockedDomainList()
  loadTrustedDomainList()
})
</script>

<template>
  <div class="mm-page">
    <section class="mm-card panel">
      <div class="head">
        <h1 class="mm-section-title">{{ t('page.security.title') }}</h1>
        <div class="head-actions">
          <el-button @click="loadSessions" :loading="loadingSessions">Refresh Sessions</el-button>
          <el-button type="danger" plain @click="logoutAllDevices">Logout all devices</el-button>
        </div>
      </div>
      <p class="rule-note">
        Rule priority: Trusted Sender &gt; Blocked Sender &gt; Trusted Domain &gt; Blocked Domain.
      </p>
      <el-table :data="sessions" style="width: 100%" v-loading="loadingSessions">
        <el-table-column label="Current" width="100">
          <template #default="scope">
            <el-tag v-if="scope.row.current" type="success">Current</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="Created At" min-width="180">
          <template #default="scope">
            {{ new Date(scope.row.createdAt).toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column label="Expires At" min-width="180">
          <template #default="scope">
            {{ new Date(scope.row.expiresAt).toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column label="Action" width="180">
          <template #default="scope">
            <el-button
              size="small"
              type="danger"
              text
              :disabled="scope.row.current"
              @click="onRevokeSession(scope.row.id)"
            >
              Revoke
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="mm-card panel">
      <SecurityAliasQuickCreate
        :aliases="securityAliases"
        :forward-target-options="securityForwardTargetOptions"
        :current-user-email="currentUserEmail"
        :loading="loadingAliasSecurity"
        :mutation-id="aliasMutationId"
        @create="onCreateSecurityAlias"
        @jump="openPassAliasCenter"
      />
    </section>

    <section class="mm-card panel" v-loading="loadingPosture">
      <div class="head">
        <h2 class="mm-section-title">Unified Security Posture</h2>
        <div class="head-actions">
          <el-button @click="loadSecurityPosture" :loading="loadingPosture">Refresh Posture</el-button>
        </div>
      </div>
      <div v-if="securityPosture" class="posture-grid">
        <article class="posture-card">
          <h3 class="mm-section-subtitle">Score</h3>
          <div class="posture-score">{{ securityPosture.securityScore }}</div>
          <el-tag :type="riskTagType(securityPosture.overallRiskLevel)">
            {{ securityPosture.overallRiskLevel }}
          </el-tag>
        </article>
        <article class="posture-card">
          <h3 class="mm-section-subtitle">Security Signals</h3>
          <p>Active sessions: {{ securityPosture.activeSessionCount }}</p>
          <p>Blocked senders/domains: {{ securityPosture.blockedSenderCount }} / {{ securityPosture.blockedDomainCount }}</p>
          <p>Trusted senders/domains: {{ securityPosture.trustedSenderCount }} / {{ securityPosture.trustedDomainCount }}</p>
          <p>High/Critical products: {{ securityPosture.highRiskProductCount }} / {{ securityPosture.criticalRiskProductCount }}</p>
        </article>
      </div>

      <div v-if="securityPosture?.alerts.length" class="posture-alert-list">
        <el-alert
          v-for="alert in securityPosture.alerts"
          :key="alert"
          :closable="false"
          type="warning"
          show-icon
          :title="alert"
        />
      </div>

      <el-alert
        v-if="lastPostureExecutionResult"
        :type="lastPostureExecutionResult.status === 'SUCCESS' ? 'success' : lastPostureExecutionResult.status === 'NO_OP' ? 'info' : 'warning'"
        :closable="false"
        class="wallet-alert"
        show-icon
        :title="`${lastPostureExecutionResult.productCode} / ${lastPostureExecutionResult.actionCode} -> ${lastPostureExecutionResult.status}`"
        :description="lastPostureExecutionResult.message"
      />

      <el-table v-if="securityPosture?.recommendedActions.length" :data="securityPosture.recommendedActions" style="width: 100%">
        <el-table-column label="Priority" width="100">
          <template #default="scope">
            <el-tag :type="priorityTagType(scope.row.priority)">{{ scope.row.priority }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="productCode" label="Product" width="130" />
        <el-table-column label="Recommended Action" min-width="320">
          <template #default="scope">
            <div class="recommended-action-cell">
              <span>{{ scope.row.action }}</span>
              <el-tag v-if="scope.row.actionCode" size="small" type="info">{{ scope.row.actionCode }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Execute" width="140">
          <template #default="scope">
            <el-button
              size="small"
              type="primary"
              plain
              :disabled="!canExecuteAction(scope.row)"
              :loading="scope.row.actionCode ? runningPostureActionCode === scope.row.actionCode : false"
              @click="onExecutePostureAction(scope.row)"
            >
              {{ canExecuteAction(scope.row) ? 'Execute' : 'Manual' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="mm-card panel">
      <div class="head">
        <h2 class="mm-section-title">Wallet Remediation Focus</h2>
        <div class="head-actions">
          <el-button @click="loadSecurityPosture" :loading="loadingPosture">Refresh Wallet Risk</el-button>
        </div>
      </div>
      <el-alert
        v-for="alert in walletPostureAlerts"
        :key="`wallet-alert-${alert}`"
        type="warning"
        :closable="false"
        show-icon
        :title="alert"
        class="wallet-alert"
      />
      <el-table v-if="walletPostureActions.length" :data="walletPostureActions" style="width: 100%">
        <el-table-column label="Priority" width="100">
          <template #default="scope">
            <el-tag :type="priorityTagType(scope.row.priority)">{{ scope.row.priority }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Wallet Action" min-width="320">
          <template #default="scope">
            <div class="recommended-action-cell">
              <span>{{ scope.row.action }}</span>
              <el-tag v-if="scope.row.actionCode" size="small" type="info">{{ scope.row.actionCode }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Execute" width="140">
          <template #default="scope">
            <el-button
              size="small"
              type="primary"
              plain
              :disabled="!canExecuteAction(scope.row)"
              :loading="scope.row.actionCode ? runningPostureActionCode === scope.row.actionCode : false"
              @click="onExecutePostureAction(scope.row)"
            >
              {{ canExecuteAction(scope.row) ? 'Execute' : 'Manual' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <p v-else class="rule-note">No wallet-specific remediation action currently. Keep monitoring execution queue.</p>
    </section>

    <section class="mm-card panel">
      <div class="head">
        <h2 class="mm-section-title">Blocked Senders</h2>
        <div class="head-actions">
          <el-button @click="loadBlockedSenderList" :loading="loadingBlockedSenders">Refresh List</el-button>
        </div>
      </div>
      <div class="blocked-add-row">
        <el-input
          v-model="blockedSenderEmail"
          placeholder="sender@example.com"
          clearable
          @keyup.enter="onAddBlockedSender"
        />
        <el-button type="warning" @click="onAddBlockedSender">Add Blocked Sender</el-button>
      </div>
      <el-table :data="blockedSenders" style="width: 100%" v-loading="loadingBlockedSenders">
        <el-table-column prop="email" label="Email" min-width="260" />
        <el-table-column label="Created At" min-width="180">
          <template #default="scope">
            {{ new Date(scope.row.createdAt).toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column label="Action" width="140">
          <template #default="scope">
            <el-button size="small" type="danger" text @click="onRemoveBlockedSender(scope.row.id)">Remove</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="mm-card panel">
      <div class="head">
        <h2 class="mm-section-title">Trusted Senders</h2>
        <div class="head-actions">
          <el-button @click="loadTrustedSenderList" :loading="loadingTrustedSenders">Refresh List</el-button>
        </div>
      </div>
      <div class="blocked-add-row">
        <el-input
          v-model="trustedSenderEmail"
          placeholder="trusted@example.com"
          clearable
          @keyup.enter="onAddTrustedSender"
        />
        <el-button type="success" @click="onAddTrustedSender">Add Trusted Sender</el-button>
      </div>
      <el-table :data="trustedSenders" style="width: 100%" v-loading="loadingTrustedSenders">
        <el-table-column prop="email" label="Email" min-width="260" />
        <el-table-column label="Created At" min-width="180">
          <template #default="scope">
            {{ new Date(scope.row.createdAt).toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column label="Action" width="140">
          <template #default="scope">
            <el-button size="small" type="danger" text @click="onRemoveTrustedSender(scope.row.id)">Remove</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="mm-card panel">
      <div class="head">
        <h2 class="mm-section-title">Blocked Domains</h2>
        <div class="head-actions">
          <el-button @click="loadBlockedDomainList" :loading="loadingBlockedDomains">Refresh List</el-button>
        </div>
      </div>
      <div class="blocked-add-row">
        <el-input
          v-model="blockedDomainName"
          placeholder="example.com or *.example.com"
          clearable
          @keyup.enter="onAddBlockedDomain"
        />
        <el-button type="warning" @click="onAddBlockedDomain">Add Blocked Domain</el-button>
      </div>
      <p class="rule-note">Wildcard matches subdomains only (for example, *.example.com).</p>
      <el-table :data="blockedDomains" style="width: 100%" v-loading="loadingBlockedDomains">
        <el-table-column prop="domain" label="Domain" min-width="260" />
        <el-table-column label="Created At" min-width="180">
          <template #default="scope">
            {{ new Date(scope.row.createdAt).toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column label="Action" width="140">
          <template #default="scope">
            <el-button size="small" type="danger" text @click="onRemoveBlockedDomain(scope.row.id)">Remove</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="mm-card panel">
      <div class="head">
        <h2 class="mm-section-title">Trusted Domains</h2>
        <div class="head-actions">
          <el-button @click="loadTrustedDomainList" :loading="loadingTrustedDomains">Refresh List</el-button>
        </div>
      </div>
      <div class="blocked-add-row">
        <el-input
          v-model="trustedDomainName"
          placeholder="example.com or *.example.com"
          clearable
          @keyup.enter="onAddTrustedDomain"
        />
        <el-button type="success" @click="onAddTrustedDomain">Add Trusted Domain</el-button>
      </div>
      <p class="rule-note">Wildcard matches subdomains only (for example, *.example.com).</p>
      <el-table :data="trustedDomains" style="width: 100%" v-loading="loadingTrustedDomains">
        <el-table-column prop="domain" label="Domain" min-width="260" />
        <el-table-column label="Created At" min-width="180">
          <template #default="scope">
            {{ new Date(scope.row.createdAt).toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column label="Action" width="140">
          <template #default="scope">
            <el-button size="small" type="danger" text @click="onRemoveTrustedDomain(scope.row.id)">Remove</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="mm-card panel">
      <div class="head">
        <h2 class="mm-section-title">Rule Resolution Inspector</h2>
      </div>
      <div class="blocked-add-row">
        <el-input
          v-model="ruleTargetEmail"
          placeholder="sender@example.com"
          clearable
          @keyup.enter="onCheckRuleResolution"
        />
        <el-button type="primary" :loading="loadingRuleResolution" @click="onCheckRuleResolution">Check Policy</el-button>
      </div>
      <el-descriptions v-if="ruleResolution" :column="2" border>
        <el-descriptions-item label="Sender">{{ ruleResolution.senderEmail }}</el-descriptions-item>
        <el-descriptions-item label="Domain">{{ ruleResolution.senderDomain || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Trusted Sender">
          <el-tag :type="ruleResolution.trustedSender ? 'success' : 'info'">
            {{ ruleResolution.trustedSender ? 'true' : 'false' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Blocked Sender">
          <el-tag :type="ruleResolution.blockedSender ? 'danger' : 'info'">
            {{ ruleResolution.blockedSender ? 'true' : 'false' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Trusted Domain">
          <el-tag :type="ruleResolution.trustedDomain ? 'success' : 'info'">
            {{ ruleResolution.trustedDomain ? 'true' : 'false' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Blocked Domain">
          <el-tag :type="ruleResolution.blockedDomain ? 'danger' : 'info'">
            {{ ruleResolution.blockedDomain ? 'true' : 'false' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Effective Folder">
          <el-tag :type="ruleResolution.effectiveFolder === 'SPAM' ? 'danger' : 'success'">
            {{ ruleResolution.effectiveFolder }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Reason">
          <el-tag type="warning">{{ ruleResolution.reason }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Matched Rule">
          <el-tag type="primary">{{ ruleResolution.matchedRule || '-' }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </section>

    <section class="mm-card panel">
      <div class="head">
        <h2 class="mm-section-title">Audit Events</h2>
        <div class="head-actions">
          <el-button @click="loadEvents" :loading="loadingEvents">Refresh Events</el-button>
        </div>
      </div>
      <el-table :data="events" style="width: 100%" v-loading="loadingEvents">
        <el-table-column prop="eventType" label="Event" min-width="180" />
        <el-table-column prop="ipAddress" label="IP" width="140" />
        <el-table-column prop="detail" label="Detail" min-width="260" />
        <el-table-column label="Time" min-width="180">
          <template #default="scope">
            {{ new Date(scope.row.createdAt).toLocaleString() }}
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.panel {
  padding: 20px;
  margin-bottom: 12px;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.head-actions {
  display: flex;
  gap: 8px;
}

.rule-note {
  margin: 0 0 12px;
  color: #5c6370;
  font-size: 13px;
}

.blocked-add-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  margin-bottom: 12px;
}

.posture-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.posture-card {
  border: 1px solid var(--mm-line);
  border-radius: 10px;
  padding: 12px;
  background: #fff;
}

.posture-score {
  font-size: 30px;
  font-weight: 700;
  margin-bottom: 8px;
}

.posture-alert-list {
  display: grid;
  gap: 8px;
  margin-bottom: 12px;
}

.wallet-alert {
  margin-bottom: 8px;
}

.recommended-action-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

@media (max-width: 900px) {
  .head {
    flex-direction: column;
    align-items: flex-start;
  }

  .blocked-add-row {
    grid-template-columns: 1fr;
  }

  .posture-grid {
    grid-template-columns: 1fr;
  }
}
</style>
