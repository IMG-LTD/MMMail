<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import type { SuiteReadinessItem, SuiteReadinessReport, SuiteSecurityPosture } from '~/types/api'
import { riskLevelLabel, riskTagType } from '~/utils/suite-operations'

interface Props {
  loading: boolean
  readiness: SuiteReadinessReport | null
  featuredReadinessItem: SuiteReadinessItem | null
  readinessRiskFilter: 'ALL' | 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  filteredReadinessItems: SuiteReadinessItem[]
  securityPosture: SuiteSecurityPosture | null
  refreshOperations: () => Promise<void>
}

const props = defineProps<Props>()
const emit = defineEmits<{
  updateReadinessRiskFilter: [value: 'ALL' | 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL']
}>()
const { t } = useI18n()

function visibleSignals(item: SuiteReadinessItem | null): Array<{ key: string; value: number }> {
  if (!item) {
    return []
  }
  return item.signals
    .filter(signal => signal.value > 0)
    .slice(0, 4)
    .map(signal => ({ key: signal.key, value: signal.value }))
}

function onRiskFilterChange(value: string | number | boolean): void {
  emit('updateReadinessRiskFilter', String(value) as Props['readinessRiskFilter'])
}
</script>

<template>
  <section class="mm-card suite-panel">
    <div class="panel-head">
      <div>
        <h2 class="mm-section-title">{{ t('suite.operations.readiness.title') }}</h2>
        <p class="mm-muted">{{ t('suite.operations.readiness.description') }}</p>
      </div>
      <div class="panel-actions">
        <el-button :loading="props.loading" @click="void props.refreshOperations()">{{ t('suite.operations.readiness.actions.refresh') }}</el-button>
      </div>
    </div>

    <div v-if="props.readiness" class="summary-grid">
      <article class="summary-card">
        <div class="score-number">{{ props.readiness.overallScore }}</div>
        <div class="score-label">{{ t('suite.operations.readiness.summary.overallScore') }}</div>
        <el-tag :type="riskTagType(props.readiness.overallRiskLevel)">
          {{ riskLevelLabel(props.readiness.overallRiskLevel, t) }}
        </el-tag>
      </article>
      <article class="summary-card">
        <div class="score-number danger">{{ props.readiness.highRiskProductCount }}</div>
        <div class="score-label">{{ t('suite.operations.readiness.summary.highRiskProducts') }}</div>
      </article>
      <article class="summary-card">
        <div class="score-number danger">{{ props.readiness.criticalRiskProductCount }}</div>
        <div class="score-label">{{ t('suite.operations.readiness.summary.criticalRiskProducts') }}</div>
      </article>
      <article class="summary-card">
        <div class="score-number">{{ props.filteredReadinessItems.length }}</div>
        <div class="score-label">{{ t('suite.operations.readiness.summary.visibleProducts') }}</div>
      </article>
    </div>

    <div v-if="props.featuredReadinessItem" class="featured-risk-card" data-testid="suite-readiness-featured-card">
      <div class="panel-head">
        <div>
          <h3 class="mm-section-subtitle">{{ t('suite.operations.readiness.featured.title') }}</h3>
          <p class="mm-muted">
            {{ t('suite.operations.readiness.featured.description', { product: props.featuredReadinessItem.productName }) }}
          </p>
        </div>
        <el-tag :type="riskTagType(props.featuredReadinessItem.riskLevel)">
          {{ riskLevelLabel(props.featuredReadinessItem.riskLevel, t) }}
        </el-tag>
      </div>
      <div class="featured-risk-meta">
        <el-tag type="info">{{ t('suite.operations.readiness.card.score', { value: props.featuredReadinessItem.score }) }}</el-tag>
        <el-tag type="info">{{ t('suite.operations.readiness.card.category', { value: props.featuredReadinessItem.category }) }}</el-tag>
        <el-tag type="info">
          {{ t('suite.operations.readiness.card.planAccess', { value: t(`suite.operations.values.${props.featuredReadinessItem.enabledByPlan ? 'yes' : 'no'}`) }) }}
        </el-tag>
      </div>
      <div v-if="visibleSignals(props.featuredReadinessItem).length" class="featured-risk-block">
        <span class="featured-risk-label">{{ t('suite.operations.readiness.featured.signals') }}</span>
        <div class="featured-risk-metrics">
          <el-tag
            v-for="signal in visibleSignals(props.featuredReadinessItem)"
            :key="`${props.featuredReadinessItem.productCode}-${signal.key}`"
            type="warning"
          >
            {{ signal.key }}={{ signal.value }}
          </el-tag>
        </div>
      </div>
      <ul v-if="props.featuredReadinessItem.actions.length" class="action-list">
        <li v-for="action in props.featuredReadinessItem.actions.slice(0, 3)" :key="`featured-${action.action}`">
          <el-tag size="small" :type="action.priority === 'P0' ? 'danger' : action.priority === 'P1' ? 'warning' : 'info'">
            {{ action.priority }}
          </el-tag>
          <span>{{ action.action }}</span>
        </li>
      </ul>
    </div>

    <div class="risk-filter-row">
      <span class="mm-muted">{{ t('suite.operations.readiness.filter.label') }}</span>
      <el-radio-group :model-value="props.readinessRiskFilter" size="small" @change="onRiskFilterChange">
        <el-radio-button value="ALL">{{ t('suite.operations.riskLevel.ALL') }}</el-radio-button>
        <el-radio-button value="LOW">{{ t('suite.operations.riskLevel.LOW') }}</el-radio-button>
        <el-radio-button value="MEDIUM">{{ t('suite.operations.riskLevel.MEDIUM') }}</el-radio-button>
        <el-radio-button value="HIGH">{{ t('suite.operations.riskLevel.HIGH') }}</el-radio-button>
        <el-radio-button value="CRITICAL">{{ t('suite.operations.riskLevel.CRITICAL') }}</el-radio-button>
      </el-radio-group>
    </div>

    <div class="readiness-grid">
      <article v-for="item in props.filteredReadinessItems" :key="item.productCode" class="readiness-card">
        <div class="product-head">
          <h3 class="mm-section-subtitle">{{ item.productName }} ({{ item.productCode }})</h3>
          <el-tag :type="riskTagType(item.riskLevel)">{{ riskLevelLabel(item.riskLevel, t) }}</el-tag>
        </div>
        <div class="readiness-meta">
          <span>{{ t('suite.operations.readiness.card.score', { value: item.score }) }}</span>
          <span>{{ t('suite.operations.readiness.card.category', { value: item.category }) }}</span>
          <span>{{ t('suite.operations.readiness.card.planAccess', { value: t(`suite.operations.values.${item.enabledByPlan ? 'yes' : 'no'}`) }) }}</span>
        </div>
        <div class="signal-list">
          <el-tag v-for="signal in item.signals" :key="`${item.productCode}-${signal.key}`" size="small" type="info">
            {{ signal.key }}={{ signal.value }}
          </el-tag>
        </div>
        <ul v-if="item.blockers.length" class="issue-list">
          <li v-for="blocker in item.blockers" :key="blocker">{{ blocker }}</li>
        </ul>
        <ul v-if="item.actions.length" class="action-list">
          <li v-for="action in item.actions" :key="`${item.productCode}-${action.action}`">
            <el-tag size="small" :type="action.priority === 'P0' ? 'danger' : action.priority === 'P1' ? 'warning' : 'info'">
              {{ action.priority }}
            </el-tag>
            <span>{{ action.action }}</span>
          </li>
        </ul>
      </article>
    </div>

    <section v-if="props.securityPosture" class="security-section">
      <h2 class="mm-section-title">{{ t('suite.operations.security.title') }}</h2>
      <div class="summary-grid">
        <article class="summary-card">
          <h3 class="mm-section-subtitle">{{ t('suite.operations.security.score') }}</h3>
          <p class="score-number">{{ props.securityPosture.securityScore }}</p>
          <el-tag :type="riskTagType(props.securityPosture.overallRiskLevel)">
            {{ riskLevelLabel(props.securityPosture.overallRiskLevel, t) }}
          </el-tag>
        </article>
        <article class="summary-card">
          <h3 class="mm-section-subtitle">{{ t('suite.operations.security.controls') }}</h3>
          <p>{{ t('suite.operations.security.controls.activeSessions', { count: props.securityPosture.activeSessionCount }) }}</p>
          <p>{{ t('suite.operations.security.controls.blockedSendersDomains', { senders: props.securityPosture.blockedSenderCount, domains: props.securityPosture.blockedDomainCount }) }}</p>
          <p>{{ t('suite.operations.security.controls.trustedSendersDomains', { senders: props.securityPosture.trustedSenderCount, domains: props.securityPosture.trustedDomainCount }) }}</p>
        </article>
      </div>
      <el-alert
        v-for="alert in props.securityPosture.alerts.slice(0, 4)"
        :key="alert"
        type="warning"
        :closable="false"
        class="posture-alert"
        show-icon
        :title="alert"
      />
    </section>
  </section>
</template>

<style scoped>
.suite-panel {
  padding: 20px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.summary-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  border: 1px solid var(--mm-line);
  border-radius: 10px;
  padding: 14px;
  background: #fff;
}

.score-number {
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 6px;
}

.score-number.danger {
  color: #cf1322;
}

.score-label {
  font-size: 12px;
  color: var(--mm-muted);
}

.risk-filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 14px;
}

.featured-risk-card {
  margin-top: 14px;
  border: 1px solid var(--mm-line);
  border-radius: 10px;
  padding: 12px;
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.featured-risk-meta,
.featured-risk-metrics,
.signal-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.featured-risk-block {
  display: grid;
  gap: 8px;
}

.featured-risk-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--mm-muted);
}

.readiness-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.readiness-card {
  border: 1px solid var(--mm-line);
  border-radius: 10px;
  padding: 12px;
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.product-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
}

.readiness-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
  color: var(--mm-muted);
}

.issue-list,
.action-list {
  margin: 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.action-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.security-section {
  margin-top: 16px;
}

.posture-alert {
  margin-top: 10px;
}

@media (max-width: 1200px) {
  .readiness-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .summary-grid,
  .readiness-grid {
    grid-template-columns: 1fr;
  }

  .panel-head,
  .risk-filter-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
