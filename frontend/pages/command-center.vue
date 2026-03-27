<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type {
  SuiteBatchGovernanceReviewResult,
  SuiteBatchRemediationExecutionResult,
  SuiteCommandCenter,
  SuiteCommandFeedItem,
  SuiteGovernanceChangeRequest
} from '~/types/api'
import { useSuiteApi } from '~/composables/useSuiteApi'
import { useI18n } from '~/composables/useI18n'
import { useOrgAccessStore } from '~/stores/org-access'
import {
  filterSuiteCommandCenterByAccess,
  filterSuiteCommandFeedByAccess
} from '~/utils/org-product-surface-filter'

const loading = ref(false)
const executingBatchActions = ref(false)
const reviewingBatchRequests = ref(false)
const commandCenter = ref<SuiteCommandCenter | null>(null)
const commandFeedItems = ref<SuiteCommandFeedItem[]>([])
const governanceRequests = ref<SuiteGovernanceChangeRequest[]>([])
const selectedActionCodes = ref<string[]>([])
const selectedGovernanceRequests = ref<SuiteGovernanceChangeRequest[]>([])
const batchDecision = ref<'APPROVE' | 'REJECT'>('APPROVE')
const batchReviewNote = ref('')
const lastBatchActionResult = ref<SuiteBatchRemediationExecutionResult | null>(null)
const lastBatchReviewResult = ref<SuiteBatchGovernanceReviewResult | null>(null)
const orgAccessStore = useOrgAccessStore()
const { t } = useI18n()

const {
  getCommandCenter,
  getCommandFeed,
  listGovernanceChangeRequests,
  batchExecuteRemediationActions,
  batchReviewGovernanceChangeRequests
} = useSuiteApi()

const visibleCommandCenter = computed(() => filterSuiteCommandCenterByAccess(
  commandCenter.value,
  orgAccessStore.isProductEnabled
))
const visibleCommandFeed = computed(() => filterSuiteCommandFeedByAccess(
  commandFeedItems.value
    ? {
        generatedAt: commandCenter.value?.generatedAt || '',
        limit: commandFeedItems.value.length,
        total: commandFeedItems.value.length,
        items: commandFeedItems.value
      }
    : null,
  orgAccessStore.isProductEnabled
))

const pendingGovernanceRequests = computed(() => {
  return governanceRequests.value.filter((item) => {
    return item.status === 'PENDING_REVIEW' || item.status === 'PENDING_SECOND_REVIEW'
  })
})

const commandSummary = computed(() => {
  if (!visibleCommandCenter.value) {
    return t('commandCenter.summary.loading')
  }
  const routeCount = visibleCommandCenter.value.quickRoutes.length
  const actionCount = visibleCommandCenter.value.recommendedActions.length
  return t('commandCenter.summary.value', {
    routes: routeCount,
    actions: actionCount,
    pending: visibleCommandCenter.value.pendingGovernanceCount
  })
})

useHead(() => ({
  title: t('page.commandCenter.title')
}))

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const [centerData, feedData, requests] = await Promise.all([
      getCommandCenter(),
      getCommandFeed(30),
      listGovernanceChangeRequests()
    ])
    commandCenter.value = centerData
    commandFeedItems.value = feedData.items
    governanceRequests.value = requests
    selectedActionCodes.value = []
    selectedGovernanceRequests.value = []
  } finally {
    loading.value = false
  }
}

async function navigateToRoute(routePath: string): Promise<void> {
  if (!routePath) {
    return
  }
  await navigateTo(routePath)
}

async function executeSelectedActions(): Promise<void> {
  if (selectedActionCodes.value.length === 0) {
    ElMessage.warning(t('commandCenter.messages.selectAction'))
    return
  }
  executingBatchActions.value = true
  try {
    lastBatchActionResult.value = await batchExecuteRemediationActions({
      actionCodes: selectedActionCodes.value
    })
    ElMessage.success(t('commandCenter.messages.batchExecuted', { count: lastBatchActionResult.value.successCount }))
    await loadData()
  } finally {
    executingBatchActions.value = false
  }
}

async function reviewSelectedRequests(): Promise<void> {
  if (selectedGovernanceRequests.value.length === 0) {
    ElMessage.warning(t('commandCenter.messages.selectRequest'))
    return
  }
  reviewingBatchRequests.value = true
  try {
    const requestIds = selectedGovernanceRequests.value.map((item) => item.requestId)
    lastBatchReviewResult.value = await batchReviewGovernanceChangeRequests({
      requestIds,
      decision: batchDecision.value,
      reviewNote: batchReviewNote.value || undefined
    })
    ElMessage.success(t('commandCenter.messages.batchReviewed', { count: lastBatchReviewResult.value.successCount }))
    await loadData()
  } finally {
    reviewingBatchRequests.value = false
  }
}

function onGovernanceSelectionChange(rows: SuiteGovernanceChangeRequest[]): void {
  selectedGovernanceRequests.value = rows
}

onMounted(() => {
  void loadData()
})

watch(() => orgAccessStore.activeOrgId, () => {
  void loadData()
})
</script>

<template>
  <section class="command-center-page mm-card">
    <header class="hero">
      <div>
        <h1>{{ t('commandCenter.hero.title') }}</h1>
        <p>{{ commandSummary }}</p>
      </div>
      <el-button :loading="loading" @click="loadData">{{ t('common.actions.refresh') }}</el-button>
    </header>

    <el-skeleton v-if="loading" :rows="6" animated />

    <template v-else>
      <div class="grid">
        <article class="panel mm-card">
          <h2>{{ t('commandCenter.sections.quickRoutes') }}</h2>
          <div class="chips">
            <el-button
              v-for="item in visibleCommandCenter?.quickRoutes || []"
              :key="`${item.commandType}-${item.label}`"
              text
              bg
              @click="navigateToRoute(item.routePath)"
            >
              {{ item.label }}
            </el-button>
          </div>
        </article>

        <article class="panel mm-card">
          <h2>{{ t('commandCenter.sections.recentKeywords') }}</h2>
          <div class="chips">
            <el-tag
              v-for="keyword in visibleCommandCenter?.recentKeywords || []"
              :key="keyword"
              effect="plain"
              class="keyword-tag"
              @click="navigateToRoute(`/search?keyword=${keyword}`)"
            >
              {{ keyword }}
            </el-tag>
          </div>
        </article>
      </div>

      <article class="panel mm-card">
        <h2>{{ t('commandCenter.sections.pinnedSearches') }}</h2>
        <el-table :data="visibleCommandCenter?.pinnedSearches || []" size="small">
          <el-table-column prop="label" :label="t('commandCenter.columns.name')" />
          <el-table-column prop="description" :label="t('commandCenter.columns.description')" />
          <el-table-column :label="t('commandCenter.columns.action')" width="120">
            <template #default="{ row }">
              <el-button link type="primary" @click="navigateToRoute(row.routePath)">{{ t('commandCenter.actions.open') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </article>

      <article class="panel mm-card">
        <div class="panel-title">
          <h2>{{ t('commandCenter.sections.commandFeed') }}</h2>
          <el-button text type="primary" @click="loadData">{{ t('commandCenter.actions.refreshFeed') }}</el-button>
        </div>
        <el-table :data="visibleCommandFeed?.items || []" size="small">
          <el-table-column prop="title" :label="t('commandCenter.columns.title')" width="240" />
          <el-table-column prop="category" :label="t('commandCenter.columns.category')" width="140" />
          <el-table-column prop="detail" :label="t('commandCenter.columns.detail')" />
          <el-table-column :label="t('commandCenter.columns.route')" width="120">
            <template #default="{ row }">
              <el-button link type="primary" @click="navigateToRoute(row.routePath)">{{ t('commandCenter.actions.open') }}</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" :label="t('commandCenter.columns.at')" width="200" />
        </el-table>
      </article>

      <article class="panel mm-card">
        <div class="panel-title">
          <h2>{{ t('commandCenter.sections.batchActions') }}</h2>
          <el-button type="primary" :loading="executingBatchActions" @click="executeSelectedActions">
            {{ t('commandCenter.actions.executeSelected') }}
          </el-button>
        </div>
        <el-checkbox-group v-model="selectedActionCodes" class="action-grid">
          <el-checkbox
            v-for="item in visibleCommandCenter?.recommendedActions || []"
            :key="item.actionCode || item.action"
            :label="item.actionCode || item.action"
            class="action-item"
          >
            <div>{{ item.action }}</div>
            <small>{{ item.productCode }} · {{ item.priority }}</small>
          </el-checkbox>
        </el-checkbox-group>
        <el-alert
          v-if="lastBatchActionResult"
          type="success"
          :closable="false"
          class="result-alert"
          :title="t('commandCenter.messages.lastBatch', { success: lastBatchActionResult.successCount, total: lastBatchActionResult.totalCount })"
        />
      </article>

      <article class="panel mm-card">
        <div class="panel-title">
          <h2>{{ t('commandCenter.sections.batchReview') }}</h2>
          <div class="review-tools">
            <el-select v-model="batchDecision" style="width: 150px">
              <el-option :label="t('commandCenter.review.approve')" value="APPROVE" />
              <el-option :label="t('commandCenter.review.reject')" value="REJECT" />
            </el-select>
            <el-input v-model="batchReviewNote" :placeholder="t('commandCenter.review.notePlaceholder')" style="width: 260px" />
            <el-button type="warning" :loading="reviewingBatchRequests" @click="reviewSelectedRequests">
              {{ t('commandCenter.actions.batchReview') }}
            </el-button>
          </div>
        </div>
        <el-table
          :data="pendingGovernanceRequests"
          size="small"
          row-key="requestId"
          @selection-change="onGovernanceSelectionChange"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="requestId" label="Request ID" width="220" />
          <el-table-column prop="templateName" label="Template" width="220" />
          <el-table-column prop="status" label="Status" width="180" />
          <el-table-column prop="reason" label="Reason" />
        </el-table>
        <el-alert
          v-if="lastBatchReviewResult"
          type="success"
          :closable="false"
          class="result-alert"
          :title="`Last batch review: ${lastBatchReviewResult.successCount}/${lastBatchReviewResult.totalCount} succeeded`"
        />
      </article>
    </template>
  </section>
</template>

<style scoped>
.command-center-page {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.hero h1 {
  margin: 0;
  font-size: 26px;
  color: #0f3f4f;
}

.hero p {
  margin: 4px 0 0;
  color: #4b6670;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.panel {
  padding: 16px;
}

.panel h2 {
  margin: 0 0 12px;
  font-size: 18px;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.keyword-tag {
  cursor: pointer;
}

.panel-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  gap: 12px;
}

.review-tools {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.action-item {
  margin-right: 0;
  padding: 10px 12px;
  border: 1px solid #dde7eb;
  border-radius: 10px;
}

.action-item small {
  display: block;
  color: #6a7f87;
}

.result-alert {
  margin-top: 12px;
}

@media (max-width: 1024px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .action-grid {
    grid-template-columns: 1fr;
  }
}
</style>
