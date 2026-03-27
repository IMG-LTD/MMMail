<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type {
  OrganizationAuthenticationSecurity,
  OrganizationAuthenticationSecurityMember
} from '~/types/organization-auth-security'
import type { OrganizationSummaryCard } from '~/types/organization-admin'
import type { OrgRole } from '~/types/api'
import type { TwoFactorEnforcementLevel } from '~/types/organization-policy'
import {
  authenticationCoveragePercent,
  buildTwoFactorEnforcementOptions,
  twoFactorEnforcementLabel
} from '~/utils/organization-auth-security'
import { organizationRoleLabel } from '~/utils/organization-admin'

const props = defineProps<{
  overview: OrganizationAuthenticationSecurity | null
  cards: OrganizationSummaryCard[]
  selectedOrgRole: OrgRole | null
  canManage: boolean
  canEditPolicy: boolean
  savingPolicy: boolean
  loading: boolean
  sendingReminders: boolean
  reminderMemberIds: string[]
  twoFactorEnforcementLevel: TwoFactorEnforcementLevel
  twoFactorGracePeriodDays: number
}>()

const emit = defineEmits<{
  'update:reminder-member-ids': [value: string[]]
  'update:two-factor-enforcement-level': [value: TwoFactorEnforcementLevel]
  'update:two-factor-grace-period-days': [value: number]
  refresh: []
  'save-policy': []
  'send-reminders': []
}>()

const { t } = useI18n()
const memberKeyword = ref('')
const onlyAtRisk = ref(false)

const enforcementOptions = computed(() => buildTwoFactorEnforcementOptions(t))
const coveragePercent = computed(() => authenticationCoveragePercent(props.overview))
const filteredMembers = computed(() => {
  const keyword = memberKeyword.value.trim().toLowerCase()
  return (props.overview?.members || []).filter((member) => {
    if (onlyAtRisk.value && member.twoFactorEnabled) {
      return false
    }
    if (!keyword) {
      return true
    }
    return member.memberEmail.toLowerCase().includes(keyword)
  })
})

function toggleReminder(memberId: string, checked: boolean): void {
  const next = checked
    ? [...props.reminderMemberIds, memberId]
    : props.reminderMemberIds.filter(item => item !== memberId)
  emit('update:reminder-member-ids', Array.from(new Set(next)))
}

function selectAllAtRisk(): void {
  const next = filteredMembers.value
    .filter(member => !member.twoFactorEnabled)
    .map(member => member.memberId)
  emit('update:reminder-member-ids', next)
}

function clearSelection(): void {
  emit('update:reminder-member-ids', [])
}

function reminderChecked(memberId: string): boolean {
  return props.reminderMemberIds.includes(memberId)
}

function reminderDisabled(member: OrganizationAuthenticationSecurityMember): boolean {
  return member.twoFactorEnabled
}
</script>

<template>
  <article class="mm-card panel auth-panel">
    <div class="panel-head">
      <div>
        <h2 class="mm-section-title">{{ t('organizations.authSecurity.title') }}</h2>
        <p class="mm-muted">{{ t('organizations.authSecurity.description') }}</p>
      </div>
      <div class="panel-actions">
        <el-tag v-if="selectedOrgRole" effect="dark">{{ organizationRoleLabel(selectedOrgRole, t) }}</el-tag>
        <el-button :loading="loading" @click="emit('refresh')">{{ t('organizations.authSecurity.refresh') }}</el-button>
      </div>
    </div>

    <el-empty v-if="!canManage" :description="t('organizations.authSecurity.readOnly')" />
    <template v-else>
      <section class="coverage-strip">
        <div class="coverage-copy">
          <span class="coverage-label">{{ t('organizations.authSecurity.coverageLabel') }}</span>
          <strong>{{ coveragePercent }}%</strong>
          <span class="mm-muted">{{ t('organizations.authSecurity.coverageHint', { count: coveragePercent }) }}</span>
        </div>
        <div class="coverage-meta">
          <el-progress :percentage="coveragePercent" :stroke-width="12" :show-text="false" />
          <el-tag type="warning" effect="plain">
            {{ twoFactorEnforcementLabel(twoFactorEnforcementLevel, t) }}
          </el-tag>
        </div>
      </section>

      <div class="summary-grid">
        <article v-for="card in cards" :key="card.label" class="summary-card">
          <div class="summary-label">{{ card.label }}</div>
          <div class="summary-value">{{ card.value }}</div>
          <div class="summary-hint">{{ card.hint }}</div>
        </article>
      </div>

      <section class="policy-strip">
        <div class="policy-copy">
          <h3>{{ t('organizations.authSecurity.enforcementLabel') }}</h3>
          <p class="mm-muted">{{ t('organizations.authSecurity.enforcementHelp') }}</p>
        </div>
        <div class="policy-actions">
          <div class="policy-field">
            <span class="policy-field-label">{{ t('organizations.authSecurity.enforcementLabel') }}</span>
            <el-select
              :model-value="twoFactorEnforcementLevel"
              :disabled="!canEditPolicy"
              @update:model-value="emit('update:two-factor-enforcement-level', $event)"
            >
              <el-option
                v-for="option in enforcementOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </div>
          <div class="policy-field">
            <span class="policy-field-label">{{ t('organizations.authSecurity.gracePeriodLabel') }}</span>
            <el-input-number
              :model-value="twoFactorGracePeriodDays"
              :min="0"
              :max="90"
              :disabled="!canEditPolicy"
              @update:model-value="emit('update:two-factor-grace-period-days', Number($event || 0))"
            />
            <span class="mm-muted">{{ t('organizations.authSecurity.gracePeriodHelp') }}</span>
          </div>
          <el-button
            v-if="canEditPolicy"
            type="primary"
            :loading="savingPolicy"
            @click="emit('save-policy')"
          >
            {{ t('organizations.authSecurity.save') }}
          </el-button>
        </div>
      </section>

      <div class="toolbar">
        <el-input v-model="memberKeyword" clearable :placeholder="t('organizations.authSecurity.memberSearch')" />
        <el-checkbox v-model="onlyAtRisk">{{ t('organizations.authSecurity.onlyAtRisk') }}</el-checkbox>
        <el-button plain @click="selectAllAtRisk">{{ t('organizations.authSecurity.selectAllAtRisk') }}</el-button>
        <el-button plain @click="clearSelection">{{ t('organizations.authSecurity.clearSelection') }}</el-button>
        <el-button type="primary" :loading="sendingReminders" @click="emit('send-reminders')">
          {{ t('organizations.authSecurity.sendReminders') }}
        </el-button>
      </div>

      <div class="selection-meta">{{ t('organizations.authSecurity.selectionCount', { count: reminderMemberIds.length }) }}</div>

      <el-empty v-if="filteredMembers.length === 0 && !loading" :description="t('organizations.authSecurity.empty')" />
      <div v-else class="member-list">
        <article v-for="member in filteredMembers" :key="member.memberId" class="member-card">
          <div class="member-main">
            <div class="member-left">
              <el-checkbox
                :model-value="reminderChecked(member.memberId)"
                :disabled="reminderDisabled(member)"
                @change="toggleReminder(member.memberId, Boolean($event))"
              />
              <div>
                <div class="member-email">{{ member.memberEmail }}</div>
                <div class="member-meta">
                  {{ organizationRoleLabel(member.role, t) }} ·
                  {{ t('organizations.authSecurity.member.sessionCount', { count: member.activeSessionCount }) }} ·
                  {{ t('organizations.authSecurity.member.authenticatorCount', { count: member.authenticatorEntryCount }) }}
                </div>
              </div>
            </div>
            <div class="member-tags">
              <el-tag :type="member.twoFactorEnabled ? 'success' : 'warning'" effect="dark">
                {{ member.twoFactorEnabled ? t('organizations.authSecurity.member.twoFactorOn') : t('organizations.authSecurity.member.twoFactorOff') }}
              </el-tag>
              <el-tag v-if="member.blockedByPolicy" type="danger" effect="plain">
                {{ t('organizations.authSecurity.member.blocked') }}
              </el-tag>
              <el-tag v-else-if="member.inGracePeriod" type="warning" effect="plain">
                {{ t('organizations.authSecurity.member.inGracePeriod') }}
              </el-tag>
            </div>
          </div>
          <div class="member-foot">
            <span class="mm-muted">
              {{
                member.lastAuthenticatorAt
                  ? t('organizations.authSecurity.member.lastAuthenticator', { value: member.lastAuthenticatorAt })
                  : t('organizations.authSecurity.member.authenticatorCount', { count: member.authenticatorEntryCount })
              }}
            </span>
            <span class="mm-muted">
              {{
                member.inGracePeriod && member.gracePeriodEndsAt
                  ? t('organizations.authSecurity.member.gracePeriodEndsAt', { value: member.gracePeriodEndsAt })
                  : member.lastReminderAt
                  ? t('organizations.authSecurity.member.lastReminder', { value: member.lastReminderAt })
                  : t('organizations.authSecurity.member.noReminder')
              }}
            </span>
          </div>
        </article>
      </div>
    </template>
  </article>
</template>

<style scoped>
.panel,
.member-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel {
  padding: 20px;
}

.panel-head,
.panel-actions,
.coverage-strip,
.policy-strip,
.policy-actions,
.toolbar,
.member-main,
.member-left,
.member-tags,
.member-foot {
  display: flex;
  gap: 12px;
}

.panel-head,
.coverage-strip,
.policy-strip,
.member-main,
.member-foot {
  justify-content: space-between;
}

.panel-actions,
.member-tags {
  align-items: center;
}

.coverage-strip,
.policy-strip,
.member-card,
.summary-card {
  border-radius: 18px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(234, 244, 255, 0.92)),
    rgba(255, 255, 255, 0.92);
}

.coverage-strip,
.policy-strip,
.member-card,
.summary-card {
  padding: 16px;
}

.coverage-copy,
.coverage-meta,
.policy-copy,
.policy-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.policy-field {
  min-width: 220px;
}

.policy-field-label {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--mm-muted);
}

.coverage-label,
.summary-label,
.selection-meta,
.member-meta,
.summary-hint,
.mm-muted {
  color: var(--mm-muted);
}

.coverage-label,
.summary-label {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.coverage-copy strong,
.summary-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--mm-primary-dark);
}

.coverage-meta {
  min-width: 260px;
}

.summary-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.toolbar {
  flex-wrap: wrap;
  align-items: center;
}

.toolbar :deep(.el-input) {
  min-width: 240px;
}

.member-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.member-left {
  align-items: flex-start;
}

.member-email {
  font-size: 16px;
  font-weight: 600;
  color: #10233f;
}

@media (max-width: 1024px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .panel-head,
  .coverage-strip,
  .policy-strip,
  .policy-actions,
  .member-main,
  .member-foot {
    flex-direction: column;
    align-items: stretch;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .coverage-meta {
    min-width: auto;
  }
}
</style>
