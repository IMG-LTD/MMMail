<script setup lang="ts">
import { computed, reactive } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type {
  CreatePassMailboxRequest,
  PassMailbox,
  VerifyPassMailboxRequest
} from '~/types/pass-business'
import { formatPassMailboxStatus, formatPassTime } from '~/utils/pass'

const props = withDefaults(defineProps<{
  mailboxes: PassMailbox[]
  loading?: boolean
  mutationId?: string
}>(), {
  mailboxes: () => [],
  loading: false,
  mutationId: ''
})

const emit = defineEmits<{
  create: [payload: CreatePassMailboxRequest]
  verify: [mailboxId: string, payload: VerifyPassMailboxRequest]
  setDefault: [mailboxId: string]
  remove: [mailboxId: string]
}>()
const { t } = useI18n()

const createForm = reactive({
  mailboxEmail: ''
})
const verifyForms = reactive<Record<string, string>>({})

const verifiedCount = computed(() => props.mailboxes.filter(item => item.status === 'VERIFIED').length)
const pendingCount = computed(() => props.mailboxes.filter(item => item.status === 'PENDING').length)
const defaultMailbox = computed(() => props.mailboxes.find(item => item.defaultMailbox) || null)

function onCreate(): void {
  emit('create', { mailboxEmail: createForm.mailboxEmail.trim() })
  createForm.mailboxEmail = ''
}

function onVerify(mailboxId: string): void {
  emit('verify', mailboxId, { verificationCode: (verifyForms[mailboxId] || '').trim() })
  verifyForms[mailboxId] = ''
}
</script>

<template>
  <section class="mailboxes-shell">
    <header class="mailboxes-head">
      <div>
        <p class="mailboxes-eyebrow">{{ t('pass.mailboxes.eyebrow') }}</p>
        <h3>{{ t('pass.mailboxes.title') }}</h3>
        <p class="mailboxes-subtitle">{{ t('pass.mailboxes.subtitle') }}</p>
      </div>
      <div class="mailboxes-metrics">
        <article class="metric-pill">
          <strong>{{ mailboxes.length }}</strong>
          <span>{{ t('pass.mailboxes.metrics.total') }}</span>
        </article>
        <article class="metric-pill success">
          <strong>{{ verifiedCount }}</strong>
          <span>{{ t('pass.mailboxes.metrics.verified') }}</span>
        </article>
        <article class="metric-pill warning">
          <strong>{{ pendingCount }}</strong>
          <span>{{ t('pass.mailboxes.metrics.pending') }}</span>
        </article>
      </div>
    </header>

    <div class="mailboxes-grid">
      <section class="mailbox-card create-card">
        <header class="card-head">
          <strong>{{ t('pass.mailboxes.create.title') }}</strong>
          <span>{{ defaultMailbox?.mailboxEmail || t('pass.mailboxes.create.noDefault') }}</span>
        </header>
        <div class="form-grid">
          <el-input
            v-model="createForm.mailboxEmail"
            maxlength="254"
            :placeholder="t('pass.mailboxes.fields.targetEmail')"
          />
        </div>
        <div class="card-note">
          {{ t('pass.mailboxes.create.note') }}
        </div>
        <div class="card-actions">
          <el-button :loading="mutationId === 'create'" type="primary" @click="onCreate">{{ t('pass.mailboxes.actions.create') }}</el-button>
        </div>
      </section>

      <section class="mailbox-card list-card">
        <header class="card-head">
          <strong>{{ t('pass.mailboxes.list.title') }}</strong>
          <span>{{ loading ? t('pass.mailboxes.list.loading') : mailboxes.length }}</span>
        </header>
        <div class="mailbox-list">
          <article v-for="mailbox in mailboxes" :key="mailbox.id" class="mailbox-row">
            <div class="card-head compact">
              <strong>{{ mailbox.mailboxEmail }}</strong>
              <div class="status-strip">
                <span class="status-pill" :class="mailbox.status === 'VERIFIED' ? 'verified' : 'pending'">
                  {{ formatPassMailboxStatus(mailbox.status, t) }}
                </span>
                <span v-if="mailbox.defaultMailbox" class="status-pill default">{{ t('pass.mailboxes.badges.default') }}</span>
                <span v-if="mailbox.primaryMailbox" class="status-pill primary">{{ t('pass.mailboxes.badges.primary') }}</span>
              </div>
            </div>
            <div class="mailbox-meta">
              <span>{{ t('pass.mailboxes.meta.createdAt', { value: formatPassTime(mailbox.createdAt) }) }}</span>
              <span>
                {{ mailbox.verifiedAt
                  ? t('pass.mailboxes.meta.verifiedAt', { value: formatPassTime(mailbox.verifiedAt) })
                  : t('pass.mailboxes.meta.awaitingVerification') }}
              </span>
            </div>

            <div v-if="mailbox.status === 'PENDING'" class="verify-box">
              <el-input
                v-model="verifyForms[mailbox.id]"
                maxlength="32"
                :placeholder="t('pass.mailboxes.fields.verificationCode')"
              />
              <el-button :loading="mutationId === `verify:${mailbox.id}`" type="primary" plain @click="onVerify(mailbox.id)">
                {{ t('pass.mailboxes.actions.verify') }}
              </el-button>
            </div>

            <div class="card-actions wrap">
              <el-button
                v-if="mailbox.status === 'VERIFIED' && !mailbox.defaultMailbox"
                :loading="mutationId === `default:${mailbox.id}`"
                plain
                @click="emit('setDefault', mailbox.id)"
              >{{ t('pass.mailboxes.actions.setDefault') }}</el-button>
              <el-button
                v-if="!mailbox.primaryMailbox"
                :loading="mutationId === `delete:${mailbox.id}`"
                type="danger"
                plain
                @click="emit('remove', mailbox.id)"
              >{{ t('pass.mailboxes.actions.delete') }}</el-button>
            </div>
          </article>
          <el-empty v-if="!loading && mailboxes.length === 0" :description="t('pass.mailboxes.list.empty')" />
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
.mailboxes-shell {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.mailboxes-head,
.card-head,
.card-actions,
.mailbox-meta,
.status-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.mailboxes-eyebrow {
  margin: 0 0 6px;
  text-transform: uppercase;
  letter-spacing: 0.18em;
  font-size: 11px;
  color: #7c6cff;
}

.mailboxes-head h3,
.mailboxes-subtitle {
  margin: 0;
}

.mailboxes-subtitle,
.mailbox-meta,
.card-head span,
.card-note {
  color: #667085;
  font-size: 12px;
}

.mailboxes-metrics {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.metric-pill,
.mailbox-card,
.mailbox-row {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.88);
}

.metric-pill {
  min-width: 92px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.metric-pill strong {
  font-size: 24px;
  color: #101828;
}

.metric-pill.success {
  background: rgba(16, 185, 129, 0.08);
}

.metric-pill.warning {
  background: rgba(245, 158, 11, 0.12);
}

.mailboxes-grid {
  display: grid;
  grid-template-columns: minmax(280px, 0.8fr) minmax(0, 1.2fr);
  gap: 14px;
}

.mailbox-card {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-grid,
.mailbox-list,
.verify-box {
  display: grid;
  gap: 12px;
}

.mailbox-row {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.compact {
  align-items: flex-start;
}

.status-pill {
  border-radius: 999px;
  padding: 4px 8px;
  font-size: 11px;
}

.status-pill.verified {
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
}

.status-pill.pending {
  background: rgba(245, 158, 11, 0.12);
  color: #b45309;
}

.status-pill.default {
  background: rgba(124, 108, 255, 0.12);
  color: #5b4eb4;
}

.status-pill.primary {
  background: rgba(15, 23, 42, 0.08);
  color: #344054;
}

.verify-box {
  grid-template-columns: minmax(0, 1fr) auto;
}

.card-actions.wrap {
  flex-wrap: wrap;
}

@media (max-width: 960px) {
  .mailboxes-grid,
  .verify-box {
    grid-template-columns: 1fr;
  }
}
</style>
