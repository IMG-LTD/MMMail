<script setup lang="ts">
import type { OrgWorkspace, SuiteGovernanceChangeRequest, SuiteGovernancePolicyTemplate } from '~/types/api'
import type { GovernanceOverviewCard } from '~/utils/suite-operations'
import {
  canExecuteGovernanceRequest,
  canReviewRequest,
  canRollbackRequest,
  formatActorSessionId,
  formatActorUserId,
  formatDateTime,
  governanceOverviewCardClass,
  governanceSlaLabel,
  governanceSlaTagType,
  governanceStatusTagType,
  isGovernanceActionLoading,
  isReviewedByCurrentSession,
  reviewStageLabel,
  reviewStageTagType,
  riskTagType,
  summarizeExecution
} from '~/utils/suite-operations'

interface ReviewerOption {
  userId: string
  label: string
}

interface Props {
  loading: boolean
  governanceOverviewCards: GovernanceOverviewCard[]
  governanceTemplates: SuiteGovernancePolicyTemplate[]
  governanceRequests: SuiteGovernanceChangeRequest[]
  managedOrganizations: OrgWorkspace[]
  selectedTemplateCode: string
  governanceReason: string
  governanceScopeType: 'PERSONAL' | 'ORG'
  governanceScopeOrgId: string
  governanceSecondReviewerUserId: string
  governanceReviewerOptions: ReviewerOption[]
  reviewNote: string
  approvalNote: string
  rollbackReason: string
  creatingGovernanceRequest: boolean
  canCreateGovernanceRequest: boolean
  selectedGovernanceTemplate: SuiteGovernancePolicyTemplate | null
  currentSessionId: string
  governanceActionLoadingRequestId: string
  governanceActionLoadingType: 'REVIEW_APPROVE' | 'REVIEW_REJECT' | 'EXECUTE' | 'ROLLBACK' | ''
  refreshOperations: () => Promise<void>
  createGovernanceRequest: () => Promise<void>
  reviewGovernanceRequest: (
    request: SuiteGovernanceChangeRequest,
    decision: 'APPROVE' | 'REJECT'
  ) => Promise<void>
  executeGovernanceRequest: (request: SuiteGovernanceChangeRequest) => Promise<void>
  rollbackGovernanceRequest: (request: SuiteGovernanceChangeRequest) => Promise<void>
}

const props = defineProps<Props>()
const emit = defineEmits<{
  updateSelectedTemplateCode: [value: string]
  updateGovernanceReason: [value: string]
  updateGovernanceScopeType: [value: 'PERSONAL' | 'ORG']
  updateGovernanceScopeOrgId: [value: string]
  updateGovernanceSecondReviewerUserId: [value: string]
  updateReviewNote: [value: string]
  updateApprovalNote: [value: string]
  updateRollbackReason: [value: string]
}>()
function governanceScopeLabel(request: SuiteGovernanceChangeRequest): string {
  if (!request.orgId) {
    return 'PERSONAL'
  }
  const org = props.managedOrganizations.find(item => item.id === request.orgId)
  if (!org) {
    return `ORG#${request.orgId}`
  }
  return `ORG:${org.name}`
}
</script>

<template>
  <section class="mm-card suite-panel">
    <div class="panel-head">
      <div>
        <h2 class="mm-section-title">Governance Orchestrator</h2>
        <p class="mm-muted">Template-driven governance flow with review, execution and rollback.</p>
      </div>
      <div class="panel-actions">
        <el-button :loading="props.loading" @click="void props.refreshOperations()">Refresh Governance</el-button>
      </div>
    </div>

    <div v-if="props.governanceOverviewCards.length > 0" class="overview-grid">
      <article
        v-for="metric in props.governanceOverviewCards"
        :key="metric.key"
        class="summary-card overview-card"
        :class="governanceOverviewCardClass(metric.tone)"
      >
        <div class="score-number">{{ metric.value }}</div>
        <div class="score-label">{{ metric.label }}</div>
      </article>
    </div>

    <div class="governance-controls">
      <el-select
        :model-value="props.selectedTemplateCode"
        filterable
        placeholder="Select governance template"
        @update:model-value="emit('updateSelectedTemplateCode', String($event || ''))"
      >
        <el-option
          v-for="template in props.governanceTemplates"
          :key="template.templateCode"
          :label="`${template.name} (${template.templateCode})`"
          :value="template.templateCode"
        />
      </el-select>
      <el-input
        :model-value="props.governanceReason"
        placeholder="Change request reason"
        clearable
        @update:model-value="emit('updateGovernanceReason', String($event || ''))"
      />
      <el-button
        type="primary"
        :loading="props.creatingGovernanceRequest"
        :disabled="!props.canCreateGovernanceRequest"
        @click="void props.createGovernanceRequest()"
      >
        Create Request
      </el-button>
    </div>

    <div class="governance-controls scope">
      <el-select
        :model-value="props.governanceScopeType"
        @update:model-value="emit('updateGovernanceScopeType', String($event || 'PERSONAL') as 'PERSONAL' | 'ORG')"
      >
        <el-option label="Personal Scope" value="PERSONAL" />
        <el-option label="Organization Scope" value="ORG" />
      </el-select>
      <el-select
        :model-value="props.governanceScopeOrgId"
        filterable
        :disabled="props.governanceScopeType !== 'ORG'"
        :placeholder="props.governanceScopeType === 'ORG' ? 'Select organization' : 'Not required for personal scope'"
        @update:model-value="emit('updateGovernanceScopeOrgId', String($event || ''))"
      >
        <el-option
          v-for="org in props.managedOrganizations"
          :key="org.id"
          :label="`${org.name} (${org.role})`"
          :value="org.id"
        />
      </el-select>
      <el-select
        :model-value="props.governanceSecondReviewerUserId"
        filterable
        clearable
        :disabled="props.governanceScopeType !== 'ORG'"
        :placeholder="props.governanceScopeType === 'ORG' ? 'Designated second reviewer (optional)' : 'Only for organization scope'"
        @update:model-value="emit('updateGovernanceSecondReviewerUserId', String($event || ''))"
      >
        <el-option
          v-for="reviewer in props.governanceReviewerOptions"
          :key="reviewer.userId"
          :label="reviewer.label"
          :value="reviewer.userId"
        />
      </el-select>
    </div>

    <div class="governance-controls secondary">
      <el-input
        :model-value="props.reviewNote"
        placeholder="Review note (optional)"
        clearable
        @update:model-value="emit('updateReviewNote', String($event || ''))"
      />
      <el-input
        :model-value="props.approvalNote"
        placeholder="Execution note (optional)"
        clearable
        @update:model-value="emit('updateApprovalNote', String($event || ''))"
      />
      <el-input
        :model-value="props.rollbackReason"
        placeholder="Rollback reason (required for rollback)"
        clearable
        @update:model-value="emit('updateRollbackReason', String($event || ''))"
      />
    </div>

    <el-alert
      type="info"
      :closable="false"
      class="posture-alert"
      show-icon
      title="Governance control: dual-review (org policy) + session separation (review vs execute)."
    />

    <article v-if="props.selectedGovernanceTemplate" class="template-preview">
      <div class="product-head">
        <h3 class="mm-section-subtitle">{{ props.selectedGovernanceTemplate.name }}</h3>
        <el-tag :type="riskTagType(props.selectedGovernanceTemplate.riskLevel)">
          {{ props.selectedGovernanceTemplate.riskLevel }}
        </el-tag>
      </div>
      <p class="mm-muted">{{ props.selectedGovernanceTemplate.description }}</p>
      <div class="template-summary">
        <span>Actions: {{ props.selectedGovernanceTemplate.actionCodes.length }}</span>
        <span>Rollback Actions: {{ props.selectedGovernanceTemplate.rollbackActionCodes.length }}</span>
        <span>Approval Required: {{ props.selectedGovernanceTemplate.approvalRequired ? 'YES' : 'NO' }}</span>
      </div>
    </article>

    <div class="template-grid">
      <article v-for="template in props.governanceTemplates" :key="template.templateCode" class="template-card">
        <div class="product-head">
          <h3 class="mm-section-subtitle">{{ template.name }}</h3>
          <el-tag :type="riskTagType(template.riskLevel)">{{ template.riskLevel }}</el-tag>
        </div>
        <p class="mm-muted">{{ template.description }}</p>
        <div class="template-summary">
          <span>Exec: {{ template.actionCodes.length }}</span>
          <span>Rollback: {{ template.rollbackActionCodes.length }}</span>
        </div>
        <div class="action-codes">
          <el-tag v-for="actionCode in template.actionCodes" :key="actionCode" size="small" type="info">
            {{ actionCode }}
          </el-tag>
        </div>
      </article>
    </div>

    <el-table :data="props.governanceRequests" style="width: 100%">
      <el-table-column prop="requestId" label="Request ID" width="180" />
      <el-table-column label="Scope" width="210">
        <template #default="scope">
          <el-tag size="small" :type="scope.row.orgId ? 'warning' : 'info'">{{ governanceScopeLabel(scope.row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="ownerId" label="Owner" width="120" />
      <el-table-column label="Status" width="210">
        <template #default="scope">
          <el-tag :type="governanceStatusTagType(scope.row.status)">{{ scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Review Stage" width="220">
        <template #default="scope">
          <el-tag size="small" :type="reviewStageTagType(scope.row.reviewStage)">
            {{ reviewStageLabel(scope.row.reviewStage) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Template" min-width="220">
        <template #default="scope">
          <div class="stack-cell">
            <span>{{ scope.row.templateName }}</span>
            <el-tag size="small" type="info">{{ scope.row.templateCode }}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="Reason" min-width="220" />
      <el-table-column label="Requested" width="180">
        <template #default="scope">{{ formatDateTime(scope.row.requestedAt) }}</template>
      </el-table-column>
      <el-table-column label="Reviewed" width="180">
        <template #default="scope">{{ formatDateTime(scope.row.reviewedAt) }}</template>
      </el-table-column>
      <el-table-column label="Reviewed By" width="130">
        <template #default="scope">{{ formatActorUserId(scope.row.reviewedByUserId) }}</template>
      </el-table-column>
      <el-table-column label="First Reviewed By" width="150">
        <template #default="scope">{{ formatActorUserId(scope.row.firstReviewedByUserId) }}</template>
      </el-table-column>
      <el-table-column label="Designated 2nd Reviewer" width="190">
        <template #default="scope">{{ formatActorUserId(scope.row.secondReviewerUserId) }}</template>
      </el-table-column>
      <el-table-column label="Review Due At" width="190">
        <template #default="scope">{{ formatDateTime(scope.row.reviewDueAt) }}</template>
      </el-table-column>
      <el-table-column label="SLA" width="110">
        <template #default="scope">
          <el-tag size="small" :type="governanceSlaTagType(scope.row)">
            {{ governanceSlaLabel(scope.row) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Reviewed Session" width="160">
        <template #default="scope">{{ formatActorSessionId(scope.row.reviewedBySessionId) }}</template>
      </el-table-column>
      <el-table-column label="First Review Session" width="170">
        <template #default="scope">{{ formatActorSessionId(scope.row.firstReviewedBySessionId) }}</template>
      </el-table-column>
      <el-table-column label="Executed By" width="130">
        <template #default="scope">{{ formatActorUserId(scope.row.executedByUserId) }}</template>
      </el-table-column>
      <el-table-column label="Executed Session" width="160">
        <template #default="scope">{{ formatActorSessionId(scope.row.executedBySessionId) }}</template>
      </el-table-column>
      <el-table-column label="Execution" min-width="160">
        <template #default="scope">{{ summarizeExecution(scope.row.executionResults) }}</template>
      </el-table-column>
      <el-table-column label="Rollback" min-width="160">
        <template #default="scope">{{ summarizeExecution(scope.row.rollbackResults) }}</template>
      </el-table-column>
      <el-table-column label="Actions" width="420">
        <template #default="scope">
          <div class="request-actions">
            <el-button
              size="small"
              type="primary"
              plain
              :disabled="!canReviewRequest(scope.row)"
              :loading="isGovernanceActionLoading(scope.row.requestId, 'REVIEW_APPROVE', props.governanceActionLoadingRequestId, props.governanceActionLoadingType)"
              @click="void props.reviewGovernanceRequest(scope.row, 'APPROVE')"
            >
              {{ scope.row.status === 'PENDING_SECOND_REVIEW' ? 'Second Approve' : 'Review Approve' }}
            </el-button>
            <el-button
              size="small"
              type="danger"
              plain
              :disabled="!canReviewRequest(scope.row)"
              :loading="isGovernanceActionLoading(scope.row.requestId, 'REVIEW_REJECT', props.governanceActionLoadingRequestId, props.governanceActionLoadingType)"
              @click="void props.reviewGovernanceRequest(scope.row, 'REJECT')"
            >
              {{ scope.row.status === 'PENDING_SECOND_REVIEW' ? 'Second Reject' : 'Review Reject' }}
            </el-button>
            <el-button
              size="small"
              type="success"
              plain
              :disabled="!canExecuteGovernanceRequest(scope.row, props.currentSessionId)"
              :title="isReviewedByCurrentSession(scope.row, props.currentSessionId) ? 'Execution requires a different authenticated session than reviewer' : ''"
              :loading="isGovernanceActionLoading(scope.row.requestId, 'EXECUTE', props.governanceActionLoadingRequestId, props.governanceActionLoadingType)"
              @click="void props.executeGovernanceRequest(scope.row)"
            >
              Execute
            </el-button>
            <el-button
              size="small"
              type="warning"
              plain
              :disabled="!canRollbackRequest(scope.row)"
              :loading="isGovernanceActionLoading(scope.row.requestId, 'ROLLBACK', props.governanceActionLoadingRequestId, props.governanceActionLoadingType)"
              @click="void props.rollbackGovernanceRequest(scope.row)"
            >
              Rollback
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
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

.overview-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-card {
  border: 1px solid var(--mm-line);
  border-radius: 10px;
  padding: 14px;
  background: #fff;
}

.overview-card {
  border-left: 4px solid var(--mm-line);
}

.overview-card.warning {
  border-left-color: #d46b08;
}

.overview-card.danger {
  border-left-color: #cf1322;
}

.overview-card.success {
  border-left-color: #237804;
}

.score-number {
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 6px;
}

.score-label {
  font-size: 12px;
  color: var(--mm-muted);
}

.governance-controls {
  display: grid;
  grid-template-columns: 1.2fr 1.6fr auto;
  gap: 10px;
  margin-top: 12px;
}

.governance-controls.scope,
.governance-controls.secondary {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.template-preview,
.template-card {
  margin-top: 12px;
  border: 1px solid var(--mm-line);
  border-radius: 10px;
  padding: 12px;
  background: #fff;
}

.template-grid {
  margin-top: 12px;
  margin-bottom: 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.template-summary {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: var(--mm-muted);
  font-size: 13px;
}

.action-codes {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.product-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
}

.stack-cell,
.request-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.posture-alert {
  margin-top: 10px;
}

@media (max-width: 1200px) {
  .overview-grid,
  .template-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .overview-grid,
  .template-grid,
  .governance-controls,
  .governance-controls.scope,
  .governance-controls.secondary {
    grid-template-columns: 1fr;
  }

  .panel-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
