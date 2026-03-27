<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from '~/composables/useI18n'
import { useSystemApi } from '~/composables/useSystemApi'
import { useAuthStore } from '~/stores/auth'
import type { SystemHealthOverview } from '~/types/system'

definePageMeta({ layout: 'default' })

const authStore = useAuthStore()
const { t } = useI18n()
const { fetchSystemHealth } = useSystemApi()

const loading = ref(false)
const errorMessage = ref('')
const overview = ref<SystemHealthOverview | null>(null)
const isAdmin = computed(() => authStore.user?.role === 'ADMIN')
const metrics = computed(() => overview.value?.metrics ?? null)

async function loadOverview(): Promise<void> {
  if (!isAdmin.value) {
    errorMessage.value = t('systemHealth.accessDenied')
    overview.value = null
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    overview.value = await fetchSystemHealth()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('systemHealth.messages.loadFailed')
  } finally {
    loading.value = false
  }
}

function formatNumber(value: number | null | undefined): string {
  if (value == null || Number.isNaN(value)) {
    return '—'
  }
  return new Intl.NumberFormat().format(value)
}

function formatDateTime(value?: string | null): string {
  if (!value) {
    return '—'
  }
  return new Date(value).toLocaleString()
}

useHead(() => ({
  title: t('page.systemHealth.title'),
}))

onMounted(() => {
  void loadOverview()
})
</script>

<template>
  <div class="system-health-page">
    <section class="mm-card hero">
      <div>
        <p class="hero__eyebrow">{{ t('systemHealth.hero.badge') }}</p>
        <h1 data-testid="system-health-title">{{ t('systemHealth.hero.title') }}</h1>
        <p class="hero__copy">{{ t('systemHealth.hero.description') }}</p>
      </div>
      <div class="hero__actions">
        <button
          data-testid="system-health-refresh"
          class="mm-button"
          type="button"
          :disabled="loading"
          @click="loadOverview"
        >
          {{ t('systemHealth.actions.refresh') }}
        </button>
        <a
          v-if="overview?.prometheusPath"
          :href="overview.prometheusPath"
          target="_blank"
          rel="noreferrer"
          data-testid="system-health-prometheus-link"
        >
          {{ t('systemHealth.prometheus.link') }}
        </a>
      </div>
    </section>

    <section v-if="errorMessage" class="mm-card alert" data-testid="system-health-error">
      <strong>{{ errorMessage }}</strong>
    </section>

    <section v-if="loading && !overview" class="mm-card" data-testid="system-health-loading">
      {{ t('systemHealth.loading') }}
    </section>

    <template v-if="overview">
      <section class="summary-grid">
        <article class="mm-card summary-card" data-testid="system-health-application">
          <span>{{ t('systemHealth.summary.application') }}</span>
          <strong>{{ overview.applicationName }}</strong>
          <small>{{ overview.applicationVersion }}</small>
        </article>
        <article class="mm-card summary-card" data-testid="system-health-status">
          <span>{{ t('systemHealth.summary.status') }}</span>
          <strong>{{ overview.status }}</strong>
          <small>{{ formatDateTime(overview.generatedAt) }}</small>
        </article>
        <article class="mm-card summary-card" data-testid="system-health-uptime">
          <span>{{ t('systemHealth.summary.uptime') }}</span>
          <strong>{{ formatNumber(overview.uptimeSeconds) }}</strong>
          <small>{{ overview.activeProfiles.join(', ') }}</small>
        </article>
      </section>

      <section class="panel-grid">
        <article class="mm-card panel">
          <h2>{{ t('systemHealth.sections.metrics') }}</h2>
          <dl v-if="metrics" class="metric-list">
            <div data-testid="system-health-total-requests">
              <dt>{{ t('systemHealth.metrics.totalRequests') }}</dt>
              <dd>{{ formatNumber(metrics.totalRequests) }}</dd>
            </div>
            <div>
              <dt>{{ t('systemHealth.metrics.failedRequests') }}</dt>
              <dd>{{ formatNumber(metrics.failedRequests) }}</dd>
            </div>
            <div>
              <dt>{{ t('systemHealth.metrics.processCpuUsage') }}</dt>
              <dd>{{ formatNumber(metrics.processCpuUsage) }}</dd>
            </div>
            <div>
              <dt>{{ t('systemHealth.metrics.systemCpuUsage') }}</dt>
              <dd>{{ formatNumber(metrics.systemCpuUsage) }}</dd>
            </div>
            <div>
              <dt>{{ t('systemHealth.metrics.usedMemoryMb') }}</dt>
              <dd>{{ formatNumber(metrics.usedMemoryMb) }}</dd>
            </div>
            <div>
              <dt>{{ t('systemHealth.metrics.activeDbConnections') }}</dt>
              <dd>{{ formatNumber(metrics.activeDbConnections) }}</dd>
            </div>
          </dl>

          <table class="data-table" data-testid="system-health-module-table">
            <thead>
              <tr>
                <th>{{ t('systemHealth.moduleMetrics.module') }}</th>
                <th>{{ t('systemHealth.moduleMetrics.totalRequests') }}</th>
                <th>{{ t('systemHealth.moduleMetrics.failedRequests') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in metrics?.modules || []" :key="item.module">
                <td>{{ item.module }}</td>
                <td>{{ formatNumber(item.totalRequests) }}</td>
                <td>{{ formatNumber(item.failedRequests) }}</td>
              </tr>
            </tbody>
          </table>
        </article>

        <article class="mm-card panel">
          <h2>{{ t('systemHealth.sections.components') }}</h2>
          <table class="data-table" data-testid="system-health-components-table">
            <thead>
              <tr>
                <th>{{ t('systemHealth.components.name') }}</th>
                <th>{{ t('systemHealth.components.status') }}</th>
                <th>{{ t('systemHealth.components.details') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="component in overview.components" :key="component.name">
                <td>{{ component.name }}</td>
                <td>{{ component.status }}</td>
                <td>{{ component.details || '—' }}</td>
              </tr>
            </tbody>
          </table>
        </article>
      </section>

      <section class="panel-grid">
        <article class="mm-card panel">
          <h2>{{ t('systemHealth.sections.errors') }}</h2>
          <dl class="metric-list">
            <div>
              <dt>{{ t('systemHealth.errors.totalEvents') }}</dt>
              <dd>{{ formatNumber(overview.errorTracking.totalEvents) }}</dd>
            </div>
            <div>
              <dt>{{ t('systemHealth.errors.clientEvents') }}</dt>
              <dd>{{ formatNumber(overview.errorTracking.clientEvents) }}</dd>
            </div>
            <div>
              <dt>{{ t('systemHealth.errors.serverEvents') }}</dt>
              <dd>{{ formatNumber(overview.errorTracking.serverEvents) }}</dd>
            </div>
            <div>
              <dt>{{ t('systemHealth.errors.lastOccurredAt') }}</dt>
              <dd>{{ formatDateTime(overview.errorTracking.lastOccurredAt) }}</dd>
            </div>
          </dl>
          <ul class="event-list" data-testid="system-health-errors-list">
            <li v-for="item in overview.recentErrors" :key="item.eventId">
              <strong>{{ item.source }} · {{ item.category }}</strong>
              <p>{{ item.message }}</p>
              <small>{{ formatDateTime(item.occurredAt) }}</small>
            </li>
          </ul>
        </article>

        <article class="mm-card panel">
          <h2>{{ t('systemHealth.sections.jobs') }}</h2>
          <dl class="metric-list">
            <div>
              <dt>{{ t('systemHealth.jobs.totalRuns') }}</dt>
              <dd>{{ formatNumber(overview.jobs.totalRuns) }}</dd>
            </div>
            <div>
              <dt>{{ t('systemHealth.jobs.failedRuns') }}</dt>
              <dd>{{ formatNumber(overview.jobs.failedRuns) }}</dd>
            </div>
            <div>
              <dt>{{ t('systemHealth.jobs.activeRuns') }}</dt>
              <dd>{{ formatNumber(overview.jobs.activeRuns) }}</dd>
            </div>
            <div>
              <dt>{{ t('systemHealth.jobs.lastCompletedAt') }}</dt>
              <dd>{{ formatDateTime(overview.jobs.lastCompletedAt) }}</dd>
            </div>
          </dl>
          <ul class="event-list" data-testid="system-health-jobs-list">
            <li v-for="job in overview.recentJobs" :key="job.runId">
              <strong>{{ job.jobName }} · {{ job.status }}</strong>
              <p>{{ job.detail }}</p>
              <small>{{ formatDateTime(job.completedAt || job.startedAt) }}</small>
            </li>
          </ul>
        </article>
      </section>
    </template>
  </div>
</template>

<style scoped>
.system-health-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero,
.panel,
.summary-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hero {
  justify-content: space-between;
}

.hero__eyebrow {
  margin: 0;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #0f6e6e;
}

.hero__copy {
  margin: 8px 0 0;
  color: var(--mm-muted);
  line-height: 1.6;
}

.hero__actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.summary-grid,
.panel-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.summary-card span,
.metric-list dt {
  font-size: 12px;
  color: var(--mm-muted);
}

.summary-card strong,
.metric-list dd {
  font-size: 20px;
  margin: 0;
  color: #12363a;
}

.metric-list {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  margin: 0;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 10px 12px;
  border-bottom: 1px solid rgba(18, 54, 58, 0.08);
  text-align: left;
  vertical-align: top;
}

.event-list {
  display: grid;
  gap: 12px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.event-list li {
  padding: 12px;
  border-radius: 14px;
  background: rgba(12, 90, 90, 0.05);
}

.event-list p,
.event-list small {
  margin: 6px 0 0;
  color: var(--mm-muted);
}

.alert {
  border: 1px solid rgba(220, 38, 38, 0.25);
  color: #991b1b;
}

.mm-button {
  border: none;
  border-radius: 999px;
  background: #0f6e6e;
  color: #fff;
  padding: 10px 16px;
  cursor: pointer;
}

.mm-button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}
</style>
