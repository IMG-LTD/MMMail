<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import { organizationRoleLabel } from '~/utils/organization-admin'
import type { OrgMemberSession, OrganizationSummaryCard } from '~/types/organization-admin'

const props = defineProps<{
  sessions: OrgMemberSession[]
  cards: OrganizationSummaryCard[]
  loading: boolean
  mutationId: string
  canManage: boolean
  memberEmail: string
  limit: number
}>()

const emit = defineEmits<{
  'update:member-email': [value: string]
  'update:limit': [value: number]
  refresh: []
  revoke: [sessionId: string]
}>()

const { t } = useI18n()
const limitOptions = [20, 60, 120]
</script>

<template>
  <article class="mm-card panel">
    <div class="panel-head">
      <div>
        <h2 class="mm-section-title">{{ t('organizations.monitor.title') }}</h2>
        <p class="mm-muted">{{ t('organizations.monitor.description') }}</p>
      </div>
      <el-button v-if="canManage" :loading="loading" @click="emit('refresh')">
        {{ t('organizations.monitor.refresh') }}
      </el-button>
    </div>

    <el-empty v-if="!canManage" :description="t('organizations.monitor.readOnly')" />
    <template v-else>
      <div class="summary-grid">
        <article v-for="card in cards" :key="card.label" class="summary-card">
          <div class="summary-label">{{ card.label }}</div>
          <div class="summary-value">{{ card.value }}</div>
          <div class="summary-hint">{{ card.hint }}</div>
        </article>
      </div>

      <div class="filters">
        <el-input
          :model-value="memberEmail"
          clearable
          :placeholder="t('organizations.monitor.memberEmail')"
          @update:model-value="emit('update:member-email', String($event || ''))"
        />
        <el-select
          :model-value="limit"
          :placeholder="t('organizations.monitor.limit')"
          @update:model-value="emit('update:limit', Number($event))"
        >
          <el-option v-for="item in limitOptions" :key="item" :label="String(item)" :value="item" />
        </el-select>
      </div>

      <el-empty v-if="!loading && sessions.length === 0" :description="t('organizations.monitor.empty')" />
      <div v-else class="session-list">
        <article v-for="session in sessions" :key="session.sessionId" class="session-card">
          <div class="session-main">
            <div class="session-identity">
              <div class="session-email">{{ session.memberEmail }}</div>
              <div class="session-meta">
                {{ organizationRoleLabel(session.role, t) }} · ID {{ session.sessionId }}
              </div>
            </div>
            <div class="session-tags">
              <el-tag size="small" effect="plain">{{ organizationRoleLabel(session.role, t) }}</el-tag>
              <el-tag v-if="session.current" size="small" effect="dark" type="success">
                {{ t('organizations.monitor.current') }}
              </el-tag>
            </div>
          </div>
          <div class="session-foot">
            <div class="session-time">
              <span>{{ t('organizations.monitor.createdAt', { value: session.createdAt }) }}</span>
              <span>{{ t('organizations.monitor.expiresAt', { value: session.expiresAt }) }}</span>
            </div>
            <el-button
              type="danger"
              plain
              size="small"
              :disabled="session.current"
              :loading="mutationId === session.sessionId"
              @click="emit('revoke', session.sessionId)"
            >
              {{ t('organizations.monitor.revoke') }}
            </el-button>
          </div>
        </article>
      </div>
    </template>
  </article>
</template>

<style scoped>
.panel,
.session-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel {
  padding: 20px;
}

.panel-head,
.filters,
.session-main,
.session-foot {
  display: flex;
  gap: 12px;
}

.panel-head,
.session-main,
.session-foot {
  justify-content: space-between;
}

.summary-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.summary-card,
.session-card {
  border-radius: 18px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(240, 247, 255, 0.9)),
    rgba(255, 255, 255, 0.9);
}

.summary-card {
  padding: 14px 16px;
}

.summary-label,
.summary-hint,
.session-meta,
.session-time,
.mm-muted {
  color: var(--mm-muted);
}

.summary-label,
.session-meta {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.summary-value {
  margin: 8px 0 4px;
  font-size: 28px;
  font-weight: 700;
  color: var(--mm-primary-dark);
}

.filters {
  flex-wrap: wrap;
}

.filters :deep(.el-input),
.filters :deep(.el-select) {
  min-width: 180px;
}

.session-card {
  padding: 16px;
}

.session-list {
  max-height: 560px;
  overflow: auto;
}

.session-identity,
.session-tags,
.session-time {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.session-email {
  font-size: 16px;
  font-weight: 600;
  color: #10233f;
}

.session-tags {
  align-items: flex-end;
}

.session-time {
  font-size: 13px;
}

@media (max-width: 768px) {
  .panel-head,
  .session-main,
  .session-foot {
    flex-direction: column;
    align-items: stretch;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .session-tags {
    align-items: flex-start;
  }
}
</style>
