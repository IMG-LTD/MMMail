<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from '~/composables/useI18n'
import { useSimpleLoginApi } from '~/composables/useSimpleLoginApi'
import type { OrgCustomDomain } from '~/types/organization-admin'
import type { PassMailbox } from '~/types/pass-business'
import type {
  CreateSimpleLoginRelayPolicyRequest,
  SimpleLoginRelayPolicy,
  SimpleLoginSubdomainMode,
  UpdateSimpleLoginRelayPolicyRequest
} from '~/types/simplelogin'
import { simpleLoginSubdomainModeLabel } from '~/utils/simplelogin'

const props = defineProps<{
  selectedOrgId: string
  domains: OrgCustomDomain[]
  mailboxes: PassMailbox[]
  canManage: boolean
}>()

const emit = defineEmits<{
  changed: []
}>()

const simpleLoginApi = useSimpleLoginApi()
const { t } = useI18n()
const policies = ref<SimpleLoginRelayPolicy[]>([])
const activeDomainId = ref('')
const loading = reactive({
  list: false,
  mutationId: ''
})
const form = reactive({
  policyId: '',
  customDomainId: '',
  catchAllEnabled: false,
  subdomainMode: 'DISABLED' as SimpleLoginSubdomainMode,
  defaultMailboxId: '',
  note: ''
})

const subdomainOptions = computed(() => [
  { label: t('simplelogin.subdomain.disabled'), value: 'DISABLED' },
  { label: t('simplelogin.subdomain.teamPrefix'), value: 'TEAM_PREFIX' },
  { label: t('simplelogin.subdomain.anyPrefix'), value: 'ANY_PREFIX' }
] as const)

const verifiedDomains = computed(() => props.domains.filter((domain) => domain.status === 'VERIFIED'))
const verifiedMailboxOptions = computed(() => props.mailboxes
  .filter((mailbox) => mailbox.status === 'VERIFIED')
  .map((mailbox) => ({
    label: [
      mailbox.mailboxEmail,
      mailbox.defaultMailbox ? t('simplelogin.relay.tag.default') : t('simplelogin.relay.tag.verified')
    ].filter(Boolean).join(' · '),
    value: mailbox.id
  })))
const policyMap = computed(() => new Map(policies.value.map((policy) => [policy.customDomainId, policy])))

watch(
  () => [props.selectedOrgId, props.domains.map((domain) => `${domain.id}:${domain.status}`).join('|')].join('::'),
  () => {
    void loadPolicies()
  },
  { immediate: true }
)

async function loadPolicies(): Promise<void> {
  if (!props.selectedOrgId) {
    policies.value = []
    closeEditor()
    return
  }
  loading.list = true
  try {
    policies.value = await simpleLoginApi.listRelayPolicies(props.selectedOrgId)
    if (activeDomainId.value && !props.domains.some((domain) => domain.id === activeDomainId.value)) {
      closeEditor()
    }
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('simplelogin.relay.errors.load')))
  } finally {
    loading.list = false
  }
}

function openCreate(domainId: string): void {
  form.policyId = ''
  form.customDomainId = domainId
  form.catchAllEnabled = false
  form.subdomainMode = 'DISABLED'
  form.defaultMailboxId = verifiedMailboxOptions.value[0]?.value || ''
  form.note = ''
  activeDomainId.value = domainId
}

function openEdit(policy: SimpleLoginRelayPolicy): void {
  form.policyId = policy.id
  form.customDomainId = policy.customDomainId
  form.catchAllEnabled = policy.catchAllEnabled
  form.subdomainMode = policy.subdomainMode
  form.defaultMailboxId = policy.defaultMailboxId
  form.note = policy.note || ''
  activeDomainId.value = policy.customDomainId
}

function closeEditor(): void {
  activeDomainId.value = ''
  form.policyId = ''
  form.customDomainId = ''
  form.catchAllEnabled = false
  form.subdomainMode = 'DISABLED'
  form.defaultMailboxId = ''
  form.note = ''
}

async function submitForm(): Promise<void> {
  if (!props.selectedOrgId) {
    ElMessage.error(t('simplelogin.relay.messages.selectOrg'))
    return
  }
  if (!form.defaultMailboxId) {
    ElMessage.error(t('simplelogin.relay.messages.selectMailbox'))
    return
  }
  loading.mutationId = form.policyId || form.customDomainId
  try {
    if (form.policyId) {
      await simpleLoginApi.updateRelayPolicy(props.selectedOrgId, form.policyId, buildUpdatePayload())
      ElMessage.success(t('simplelogin.relay.messages.updated'))
    } else {
      await simpleLoginApi.createRelayPolicy(props.selectedOrgId, buildCreatePayload())
      ElMessage.success(t('simplelogin.relay.messages.created'))
    }
    await loadPolicies()
    closeEditor()
    emit('changed')
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('simplelogin.relay.errors.save')))
  } finally {
    loading.mutationId = ''
  }
}

async function removePolicy(policy: SimpleLoginRelayPolicy): Promise<void> {
  try {
    await ElMessageBox.confirm(
      t('simplelogin.relay.confirm.removeMessage', { domain: policy.domain || t('common.none') }),
      t('simplelogin.relay.confirm.removeTitle'),
      { type: 'warning' }
    )
  } catch {
    return
  }
  loading.mutationId = policy.id
  try {
    await simpleLoginApi.removeRelayPolicy(props.selectedOrgId, policy.id)
    await loadPolicies()
    if (activeDomainId.value === policy.customDomainId) {
      closeEditor()
    }
    emit('changed')
    ElMessage.success(t('simplelogin.relay.messages.removed'))
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('simplelogin.relay.errors.remove')))
  } finally {
    loading.mutationId = ''
  }
}

function policyFor(domainId: string): SimpleLoginRelayPolicy | null {
  return policyMap.value.get(domainId) || null
}

function buildCreatePayload(): CreateSimpleLoginRelayPolicyRequest {
  return {
    customDomainId: form.customDomainId,
    catchAllEnabled: form.catchAllEnabled,
    subdomainMode: form.subdomainMode,
    defaultMailboxId: form.defaultMailboxId,
    note: form.note || undefined
  }
}

function buildUpdatePayload(): UpdateSimpleLoginRelayPolicyRequest {
  return {
    catchAllEnabled: form.catchAllEnabled,
    subdomainMode: form.subdomainMode,
    defaultMailboxId: form.defaultMailboxId,
    note: form.note || undefined
  }
}

function resolveMessage(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback
}
</script>

<template>
  <article class="mm-card panel">
    <div class="panel-head">
      <div>
        <h2 class="mm-section-title">{{ t('simplelogin.relay.title') }}</h2>
        <p class="mm-muted">{{ t('simplelogin.relay.subtitle') }}</p>
      </div>
      <el-tag effect="dark" type="info">{{ t('simplelogin.relay.badge') }}</el-tag>
    </div>

    <div class="warning-banner">
      {{ t('simplelogin.relay.warning') }}
    </div>

    <el-empty v-if="!selectedOrgId" :description="t('simplelogin.relay.empty.noOrg')" />
    <el-empty v-else-if="!domains.length && !loading.list" :description="t('simplelogin.relay.empty.noDomains')" />

    <div v-else class="domain-list">
      <article v-for="domain in domains" :key="domain.id" class="domain-card">
        <div class="title-row">
          <div>
            <div class="domain-name">{{ domain.domain }}</div>
            <div class="domain-meta">{{ t('simplelogin.relay.meta.updated', { time: domain.updatedAt }) }}</div>
          </div>
          <div class="title-tags">
            <el-tag :type="domain.status === 'VERIFIED' ? 'success' : 'warning'">{{ domain.status }}</el-tag>
            <el-tag v-if="domain.defaultDomain" effect="dark">{{ t('simplelogin.relay.tag.default') }}</el-tag>
            <el-tag v-if="policyFor(domain.id)" type="info">{{ t('simplelogin.relay.tag.policyReady') }}</el-tag>
            <el-tag v-if="policyFor(domain.id)?.catchAllEnabled" type="success">{{ t('simplelogin.relay.tag.catchAll') }}</el-tag>
          </div>
        </div>

        <div v-if="domain.status !== 'VERIFIED'" class="policy-pending">
          {{ t('simplelogin.relay.verifyDomain') }}
        </div>

        <template v-else>
          <div class="policy-summary">
            <template v-if="policyFor(domain.id)">
              <div class="summary-row">
                <span class="summary-label">{{ t('simplelogin.relay.summary.subdomainMode') }}</span>
                <strong>{{ simpleLoginSubdomainModeLabel(policyFor(domain.id)!.subdomainMode, t) }}</strong>
              </div>
              <div class="summary-row">
                <span class="summary-label">{{ t('simplelogin.relay.summary.defaultRoute') }}</span>
                <strong>{{ policyFor(domain.id)!.defaultMailboxEmail }}</strong>
              </div>
              <div class="summary-row">
                <span class="summary-label">{{ t('simplelogin.relay.summary.note') }}</span>
                <span>{{ policyFor(domain.id)!.note || t('simplelogin.relay.summary.noNote') }}</span>
              </div>
            </template>
            <div v-else class="empty-policy-copy">
              {{ t('simplelogin.relay.summary.empty') }}
            </div>
          </div>

          <div v-if="activeDomainId === domain.id && canManage" class="editor-card">
            <el-switch v-model="form.catchAllEnabled" inline-prompt :active-text="t('simplelogin.relay.editor.catchAll')" :inactive-text="t('simplelogin.relay.editor.off')" />
            <el-select v-model="form.subdomainMode" :placeholder="t('simplelogin.relay.editor.subdomainMode')">
              <el-option v-for="option in subdomainOptions" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
            <el-select v-model="form.defaultMailboxId" :placeholder="t('simplelogin.relay.editor.defaultMailboxRoute')">
              <el-option v-for="option in verifiedMailboxOptions" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
            <el-input v-model="form.note" type="textarea" :rows="3" maxlength="500" show-word-limit :placeholder="t('simplelogin.relay.editor.notePlaceholder')" />
            <div class="editor-actions">
              <el-button @click="closeEditor">{{ t('common.actions.cancel') }}</el-button>
              <el-button
                type="primary"
                :loading="loading.mutationId === (form.policyId || form.customDomainId)"
                @click="submitForm"
              >
                {{ form.policyId ? t('simplelogin.relay.editor.save') : t('simplelogin.relay.editor.create') }}
              </el-button>
            </div>
          </div>

          <div v-else class="domain-actions">
            <div v-if="!canManage" class="read-only-banner">
              {{ t('simplelogin.relay.readOnly') }}
            </div>
            <template v-else>
              <el-button size="small" @click="policyFor(domain.id) ? openEdit(policyFor(domain.id)!) : openCreate(domain.id)">
                {{ policyFor(domain.id) ? t('simplelogin.relay.actions.edit') : t('simplelogin.relay.actions.configure') }}
              </el-button>
              <el-button
                v-if="policyFor(domain.id)"
                size="small"
                type="danger"
                plain
                :loading="loading.mutationId === policyFor(domain.id)!.id"
                @click="removePolicy(policyFor(domain.id)!)"
              >
                {{ t('simplelogin.relay.actions.remove') }}
              </el-button>
            </template>
          </div>
        </template>
      </article>
    </div>

    <div v-if="verifiedDomains.length === 0 && domains.length > 0" class="footnote">
      {{ t('simplelogin.relay.footnote') }}
    </div>
  </article>
</template>

<style scoped>
.panel {
  padding: 20px;
}

.panel-head,
.title-row,
.title-tags,
.domain-actions,
.editor-actions,
.summary-row {
  display: flex;
  gap: 12px;
}

.panel-head,
.title-row,
.summary-row {
  justify-content: space-between;
}

.warning-banner,
.read-only-banner,
.policy-pending,
.footnote {
  padding: 12px 14px;
  border-radius: 14px;
  font-weight: 600;
}

.warning-banner {
  margin-bottom: 14px;
  background: rgba(88, 101, 242, 0.08);
  color: #3345aa;
}

.read-only-banner,
.policy-pending {
  background: rgba(250, 173, 20, 0.12);
  color: #8a5b00;
}

.footnote {
  margin-top: 12px;
  background: rgba(15, 23, 42, 0.06);
  color: var(--mm-muted);
}

.domain-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.domain-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(105, 119, 255, 0.07), rgba(15, 23, 42, 0.03));
  border: 1px solid rgba(88, 101, 242, 0.1);
}

.domain-name {
  font-size: 17px;
  font-weight: 700;
  color: #1f2a6b;
}

.domain-meta,
.mm-muted,
.summary-label {
  color: var(--mm-muted);
}

.policy-summary,
.editor-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.editor-card {
  padding: 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(88, 101, 242, 0.1);
}

.empty-policy-copy {
  color: var(--mm-muted);
}

@media (max-width: 768px) {
  .panel-head,
  .title-row,
  .title-tags,
  .domain-actions,
  .editor-actions,
  .summary-row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
