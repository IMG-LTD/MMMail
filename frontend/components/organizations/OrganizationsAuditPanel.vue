<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { OrgAuditEvent } from '~/types/api'
import type { OrgAuditSortDirection } from '~/types/organization-admin'
import { formatOrganizationAuditType } from '~/utils/organization-admin'

const props = defineProps<{
  auditEvents: OrgAuditEvent[]
  loading: boolean
  canManage: boolean
  eventType: string
  actorEmail: string
  keyword: string
  dateRange: string[]
  sortDirection: OrgAuditSortDirection
}>()

const emit = defineEmits<{
  'update:event-type': [value: string]
  'update:actor-email': [value: string]
  'update:keyword': [value: string]
  'update:date-range': [value: string[]]
  'update:sort-direction': [value: OrgAuditSortDirection]
  apply: []
  refresh: []
  export: []
}>()

const { t } = useI18n()
const dateRangeModel = computed({
  get: () => props.dateRange,
  set: (value: string[]) => emit('update:date-range', value || [])
})
</script>

<template>
  <article class="mm-card panel">
    <div class="panel-head">
      <div>
        <h2 class="mm-section-title">{{ t('organizations.audit.title') }}</h2>
        <p class="mm-muted">{{ t('organizations.audit.description') }}</p>
      </div>
      <div v-if="canManage" class="panel-actions">
        <el-button :loading="loading" @click="emit('refresh')">{{ t('organizations.audit.refresh') }}</el-button>
        <el-button type="primary" plain @click="emit('export')">{{ t('organizations.audit.export') }}</el-button>
      </div>
    </div>

    <el-empty v-if="!canManage" :description="t('organizations.audit.readOnly')" />
    <template v-else>
      <div class="filters">
        <el-input :model-value="eventType" :placeholder="t('organizations.audit.eventType')" @update:model-value="emit('update:event-type', $event)" />
        <el-input :model-value="actorEmail" :placeholder="t('organizations.audit.actorEmail')" @update:model-value="emit('update:actor-email', $event)" />
        <el-input :model-value="keyword" :placeholder="t('organizations.audit.keyword')" @update:model-value="emit('update:keyword', $event)" />
        <el-date-picker
          v-model="dateRangeModel"
          type="daterange"
          value-format="YYYY-MM-DD"
          :range-separator="t('organizations.audit.dateRange')"
          :start-placeholder="t('organizations.audit.startDate')"
          :end-placeholder="t('organizations.audit.endDate')"
        />
        <el-select
          :model-value="sortDirection"
          :placeholder="t('organizations.audit.sort')"
          @update:model-value="emit('update:sort-direction', $event)"
        >
          <el-option :label="t('organizations.audit.sort.newest')" value="DESC" />
          <el-option :label="t('organizations.audit.sort.oldest')" value="ASC" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="emit('apply')">{{ t('organizations.audit.apply') }}</el-button>
      </div>

      <el-empty v-if="auditEvents.length === 0 && !loading" :description="t('organizations.audit.empty')" />
      <div v-else class="audit-list">
        <article v-for="event in auditEvents" :key="event.id" class="audit-card">
          <div class="audit-type">{{ formatOrganizationAuditType(event.eventType, t) }}</div>
          <div class="audit-detail">{{ event.detail }}</div>
          <div class="audit-meta">{{ event.actorEmail || t('organizations.audit.system') }} · {{ event.createdAt }}</div>
        </article>
      </div>
    </template>
  </article>
</template>

<style scoped>
.panel,
.audit-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel {
  padding: 20px;
}

.panel-head,
.filters,
.panel-actions {
  display: flex;
  gap: 12px;
}

.panel-head {
  justify-content: space-between;
}

.panel-actions {
  flex-wrap: wrap;
}

.filters {
  flex-wrap: wrap;
}

.filters :deep(.el-input),
.filters :deep(.el-select),
.filters :deep(.el-date-editor) {
  min-width: 180px;
}

.audit-card {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(15, 110, 110, 0.05);
}

.audit-type {
  font-weight: 700;
  color: var(--mm-primary-dark);
}

.audit-detail,
.audit-meta,
.mm-muted {
  color: var(--mm-muted);
}

@media (max-width: 768px) {
  .panel-head,
  .filters {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
