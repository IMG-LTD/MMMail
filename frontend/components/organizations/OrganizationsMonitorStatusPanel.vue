<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { OrgMonitorStatus } from '~/types/organization-admin'
import {
  buildOrganizationMonitorMetricCards,
  buildOrganizationMonitorSemanticCards
} from '~/utils/organization-monitor-status'

const props = defineProps<{
  status: OrgMonitorStatus | null
  loading: boolean
  canManage: boolean
}>()

const { t } = useI18n()
const semanticCards = computed(() => buildOrganizationMonitorSemanticCards(props.status, t))
const metricCards = computed(() => buildOrganizationMonitorMetricCards(props.status, t))
const oldestEventAt = computed(() => props.status?.oldestEventAt || t('organizations.monitor.status.metrics.empty'))
const visibilityValue = computed(() => t(`organizations.monitor.status.visibility.${props.status?.visibilityScope || 'ALL_ADMINS'}`))
const retentionValue = computed(() => t(`organizations.monitor.status.retention.${props.status?.retentionMode || 'PERMANENT'}`))
</script>

<template>
  <article v-loading="loading" class="mm-card panel">
    <div class="panel-head">
      <div>
        <h2 class="mm-section-title">{{ t('organizations.monitor.status.title') }}</h2>
        <p class="mm-muted">{{ t('organizations.monitor.status.description') }}</p>
      </div>
      <div v-if="canManage" class="status-badges">
        <el-tag size="small" effect="dark" type="success">{{ t('organizations.monitor.status.semantics.alwaysOn.value') }}</el-tag>
        <el-tag size="small" effect="plain" type="primary">{{ retentionValue }}</el-tag>
        <el-tag size="small" effect="plain">{{ visibilityValue }}</el-tag>
      </div>
    </div>

    <el-empty v-if="!canManage" :description="t('organizations.monitor.status.readOnly')" />
    <template v-else>
      <div class="semantic-grid">
        <article v-for="card in semanticCards" :key="card.label" class="semantic-card">
          <div class="semantic-label">{{ card.label }}</div>
          <div class="semantic-value">{{ card.value }}</div>
          <div class="semantic-hint">{{ card.hint }}</div>
        </article>
      </div>

      <div class="metrics-grid">
        <article v-for="card in metricCards" :key="card.label" class="metric-card">
          <div class="metric-label">{{ card.label }}</div>
          <div class="metric-value">{{ card.value }}</div>
          <div class="metric-hint">{{ card.hint }}</div>
        </article>
      </div>

      <div class="status-footer">
        <div class="footer-item">
          <span class="footer-label">{{ t('organizations.monitor.status.footer.oldestEvent') }}</span>
          <span class="footer-value">{{ oldestEventAt }}</span>
        </div>
        <div class="footer-item">
          <span class="footer-label">{{ t('organizations.monitor.status.footer.noDelete') }}</span>
          <span class="footer-value">{{ t('organizations.monitor.status.footer.noDeleteValue') }}</span>
        </div>
      </div>
    </template>
  </article>
</template>

<style scoped>
.panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 24px;
  border-radius: 24px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  background:
    radial-gradient(circle at top left, rgba(67, 97, 238, 0.12), transparent 38%),
    linear-gradient(140deg, rgba(255, 255, 255, 0.98), rgba(242, 247, 255, 0.94));
}

.panel-head,
.status-badges,
.status-footer {
  display: flex;
  gap: 12px;
}

.panel-head,
.status-footer {
  justify-content: space-between;
}

.status-badges {
  flex-wrap: wrap;
  align-items: flex-start;
}

.semantic-grid,
.metrics-grid {
  display: grid;
  gap: 14px;
}

.semantic-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.metrics-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.semantic-card,
.metric-card {
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid rgba(30, 41, 59, 0.08);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(10px);
}

.semantic-label,
.metric-label,
.semantic-hint,
.metric-hint,
.footer-label,
.mm-muted {
  color: var(--mm-muted);
}

.semantic-label,
.metric-label,
.footer-label {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.semantic-value,
.metric-value,
.footer-value {
  color: #10233f;
}

.semantic-value,
.metric-value {
  margin: 10px 0 6px;
  font-size: 24px;
  font-weight: 700;
}

.status-footer {
  padding-top: 8px;
  border-top: 1px solid rgba(148, 163, 184, 0.18);
}

.footer-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

@media (max-width: 1200px) {
  .semantic-grid,
  .metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .panel-head,
  .status-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .semantic-grid,
  .metrics-grid {
    grid-template-columns: 1fr;
  }
}
</style>
