<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import type { SuiteReadinessItem, SuiteReadinessReport, SuiteSecurityPosture } from '~/types/api'
import { riskTagType } from '~/utils/suite-operations'

interface Props {
  loading: boolean
  readiness: SuiteReadinessReport | null
  walletReadiness: SuiteReadinessItem | null
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

function signalValue(item: SuiteReadinessItem | null, key: string): number {
  if (!item) {
    return 0
  }
  return item.signals.find(signal => signal.key === key)?.value || 0
}

function onRiskFilterChange(value: string | number | boolean): void {
  emit('updateReadinessRiskFilter', String(value) as Props['readinessRiskFilter'])
}
</script>

<template>
  <section class="mm-card suite-panel">
    <div class="panel-head">
      <div>
        <h2 class="mm-section-title">Readiness Command Center</h2>
        <p class="mm-muted">Cross-product operational readiness and risk heat map.</p>
      </div>
      <div class="panel-actions">
        <el-button :loading="props.loading" @click="void props.refreshOperations()">Refresh Readiness</el-button>
      </div>
    </div>

    <div v-if="props.readiness" class="summary-grid">
      <article class="summary-card">
        <div class="score-number">{{ props.readiness.overallScore }}</div>
        <div class="score-label">Overall Readiness Score</div>
        <el-tag :type="riskTagType(props.readiness.overallRiskLevel)">
          {{ props.readiness.overallRiskLevel }}
        </el-tag>
      </article>
      <article class="summary-card">
        <div class="score-number danger">{{ props.readiness.highRiskProductCount }}</div>
        <div class="score-label">High Risk Products</div>
      </article>
      <article class="summary-card">
        <div class="score-number danger">{{ props.readiness.criticalRiskProductCount }}</div>
        <div class="score-label">Critical Risk Products</div>
      </article>
      <article class="summary-card">
        <div class="score-number">{{ props.filteredReadinessItems.length }}</div>
        <div class="score-label">Visible Products</div>
      </article>
    </div>

    <div v-if="props.walletReadiness" class="wallet-risk-card">
      <div class="panel-head">
        <h3 class="mm-section-subtitle">Wallet Execution Risk</h3>
        <el-tag :type="riskTagType(props.walletReadiness.riskLevel)">{{ props.walletReadiness.riskLevel }}</el-tag>
      </div>
      <div class="wallet-risk-metrics">
        <el-tag type="warning">Pending: {{ signalValue(props.walletReadiness, 'pending_tx_count') }}</el-tag>
        <el-tag type="info">Signed: {{ signalValue(props.walletReadiness, 'signed_tx_count') }}</el-tag>
        <el-tag type="info">Broadcasted: {{ signalValue(props.walletReadiness, 'broadcasted_tx_count') }}</el-tag>
        <el-tag type="danger">Blocked: {{ signalValue(props.walletReadiness, 'blocked_mid_stage_count') }}</el-tag>
        <el-tag type="danger">Failed: {{ signalValue(props.walletReadiness, 'failed_tx_count') }}</el-tag>
      </div>
      <ul v-if="props.walletReadiness.actions.length" class="action-list">
        <li v-for="action in props.walletReadiness.actions.slice(0, 3)" :key="`wallet-${action.action}`">
          <el-tag size="small" :type="action.priority === 'P0' ? 'danger' : action.priority === 'P1' ? 'warning' : 'info'">
            {{ action.priority }}
          </el-tag>
          <span>{{ action.action }}</span>
        </li>
      </ul>
    </div>

    <div class="risk-filter-row">
      <span class="mm-muted">Risk Filter</span>
      <el-radio-group :model-value="props.readinessRiskFilter" size="small" @change="onRiskFilterChange">
        <el-radio-button value="ALL">ALL</el-radio-button>
        <el-radio-button value="LOW">LOW</el-radio-button>
        <el-radio-button value="MEDIUM">MEDIUM</el-radio-button>
        <el-radio-button value="HIGH">HIGH</el-radio-button>
        <el-radio-button value="CRITICAL">CRITICAL</el-radio-button>
      </el-radio-group>
    </div>

    <div class="readiness-grid">
      <article v-for="item in props.filteredReadinessItems" :key="item.productCode" class="readiness-card">
        <div class="product-head">
          <h3 class="mm-section-subtitle">{{ item.productName }} ({{ item.productCode }})</h3>
          <el-tag :type="riskTagType(item.riskLevel)">{{ item.riskLevel }}</el-tag>
        </div>
        <div class="readiness-meta">
          <span>Score: {{ item.score }}</span>
          <span>Category: {{ item.category }}</span>
          <span>Plan Access: {{ item.enabledByPlan ? 'YES' : 'NO' }}</span>
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
      <h2 class="mm-section-title">Security Posture Snapshot</h2>
      <div class="summary-grid">
        <article class="summary-card">
          <h3 class="mm-section-subtitle">Security Score</h3>
          <p class="score-number">{{ props.securityPosture.securityScore }}</p>
          <el-tag :type="riskTagType(props.securityPosture.overallRiskLevel)">
            {{ props.securityPosture.overallRiskLevel }}
          </el-tag>
        </article>
        <article class="summary-card">
          <h3 class="mm-section-subtitle">Controls</h3>
          <p>Active sessions: {{ props.securityPosture.activeSessionCount }}</p>
          <p>Blocked senders/domains: {{ props.securityPosture.blockedSenderCount }} / {{ props.securityPosture.blockedDomainCount }}</p>
          <p>Trusted senders/domains: {{ props.securityPosture.trustedSenderCount }} / {{ props.securityPosture.trustedDomainCount }}</p>
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

.wallet-risk-card {
  margin-top: 14px;
  border: 1px solid var(--mm-line);
  border-radius: 10px;
  padding: 12px;
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.wallet-risk-metrics,
.signal-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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
