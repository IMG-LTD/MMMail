<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { OrgPolicy, OrgRole } from '~/types/api'
import type { OrgMemberProductAccess, OrgProductKey } from '~/types/organization-admin'
import {
  buildOrganizationProductCatalog,
  canRemoveOrganizationMember,
  organizationRoleLabel,
  productAccessLabel
} from '~/utils/organization-admin'

const props = defineProps<{
  rows: OrgMemberProductAccess[]
  selectedOrgRole: OrgRole | null
  policy: OrgPolicy | null
  canManageProducts: boolean
  canManageRoles: boolean
  memberMutationId: string
  productMutationId: string
}>()

const emit = defineEmits<{
  'toggle-product': [memberId: string, productKey: OrgProductKey, enabled: boolean]
  'update-role': [memberId: string, role: Exclude<OrgRole, 'OWNER'>]
  'batch-update-role': [memberIds: string[], role: Exclude<OrgRole, 'OWNER'>]
  remove: [memberId: string]
  'batch-remove': [memberIds: string[]]
}>()

const { t } = useI18n()
const selectedMemberIds = ref<string[]>([])
const batchRole = ref<Exclude<OrgRole, 'OWNER'>>('MEMBER')

const productCatalog = computed(() => buildOrganizationProductCatalog(t))
const eligibleRows = computed(() => props.rows.filter(row => row.role !== 'OWNER'))
const allSelected = computed(() => {
  return eligibleRows.value.length > 0
    && eligibleRows.value.every(row => selectedMemberIds.value.includes(row.memberId))
})
const membershipMutationLoading = computed(() => Boolean(props.memberMutationId))

watch(
  () => props.rows.map(row => row.memberId).join('|'),
  () => {
    selectedMemberIds.value = selectedMemberIds.value.filter(memberId => {
      return props.rows.some(row => row.memberId === memberId)
    })
  }
)

function isEnabled(memberId: string, productKey: OrgProductKey): boolean {
  const row = props.rows.find(item => item.memberId === memberId)
  const product = row?.products.find(item => item.productKey === productKey)
  return product?.accessState === 'ENABLED'
}

function isSelected(memberId: string): boolean {
  return selectedMemberIds.value.includes(memberId)
}

function toggleSelection(memberId: string, checked: boolean): void {
  if (checked) {
    selectedMemberIds.value = [...new Set([...selectedMemberIds.value, memberId])]
    return
  }
  selectedMemberIds.value = selectedMemberIds.value.filter(item => item !== memberId)
}

function toggleSelectAll(checked: boolean): void {
  selectedMemberIds.value = checked ? eligibleRows.value.map(row => row.memberId) : []
}

function clearSelection(): void {
  selectedMemberIds.value = []
}

function canRemoveRow(row: OrgMemberProductAccess): boolean {
  return canRemoveOrganizationMember(props.selectedOrgRole, row, props.policy)
}

function submitBatchRole(): void {
  emit('batch-update-role', selectedMemberIds.value, batchRole.value)
}

function submitBatchRemove(): void {
  emit('batch-remove', selectedMemberIds.value)
}
</script>

<template>
  <article class="mm-card panel">
    <div class="panel-head">
      <div>
        <h2 class="mm-section-title">{{ t('organizations.access.title') }}</h2>
        <p class="mm-muted">{{ t('organizations.access.description') }}</p>
      </div>
    </div>

    <el-alert
      v-if="canManageProducts && !canManageRoles"
      :title="t('organizations.access.noticeAdminLimited')"
      type="info"
      :closable="false"
    />
    <el-alert
      :title="t('orgAccess.runtimeEffect')"
      type="warning"
      :closable="false"
    />

    <div v-if="canManageRoles && eligibleRows.length > 0" class="batch-toolbar">
      <el-checkbox
        :model-value="allSelected"
        :disabled="membershipMutationLoading"
        @update:model-value="toggleSelectAll(Boolean($event))"
      >
        {{ t('organizations.access.selectAll') }}
      </el-checkbox>
      <el-tag effect="plain">{{ t('organizations.access.selectedCount', { count: selectedMemberIds.length }) }}</el-tag>
      <el-select
        v-model="batchRole"
        class="batch-role-select"
        :disabled="selectedMemberIds.length === 0 || membershipMutationLoading"
        :placeholder="t('organizations.access.batchRolePlaceholder')"
      >
        <el-option :label="organizationRoleLabel('MEMBER', t)" value="MEMBER" />
        <el-option :label="organizationRoleLabel('ADMIN', t)" value="ADMIN" />
      </el-select>
      <el-button
        :disabled="selectedMemberIds.length === 0 || membershipMutationLoading"
        :loading="membershipMutationLoading"
        @click="submitBatchRole"
      >
        {{ t('organizations.access.applyRole') }}
      </el-button>
      <el-button
        type="danger"
        plain
        :disabled="selectedMemberIds.length === 0 || membershipMutationLoading"
        :loading="membershipMutationLoading"
        @click="submitBatchRemove"
      >
        {{ t('organizations.access.removeSelected') }}
      </el-button>
      <el-button
        :disabled="selectedMemberIds.length === 0 || membershipMutationLoading"
        @click="clearSelection"
      >
        {{ t('organizations.access.clearSelection') }}
      </el-button>
    </div>

    <el-empty v-if="rows.length === 0" :description="t('organizations.access.empty')" />
    <div v-else class="matrix-list">
      <article
        v-for="row in rows"
        :key="row.memberId"
        class="matrix-card"
        :class="{ 'matrix-card--selected': isSelected(row.memberId) }"
      >
        <div class="member-head">
          <div class="member-summary">
            <el-checkbox
              v-if="canManageRoles && row.role !== 'OWNER'"
              :model-value="isSelected(row.memberId)"
              :disabled="membershipMutationLoading"
              @update:model-value="toggleSelection(row.memberId, Boolean($event))"
            />
            <div>
              <div class="member-name">
                {{ row.userEmail }}
                <el-tag v-if="row.currentUser" size="small" effect="plain">{{ t('organizations.access.currentUser') }}</el-tag>
              </div>
              <div class="member-meta">{{ t('organizations.access.productsEnabled', { count: row.enabledProductCount }) }}</div>
            </div>
          </div>
          <div class="member-actions">
            <el-select
              v-if="canManageRoles && row.role !== 'OWNER'"
              :disabled="membershipMutationLoading"
              :model-value="row.role"
              @update:model-value="emit('update-role', row.memberId, $event)"
            >
              <el-option :label="organizationRoleLabel('ADMIN', t)" value="ADMIN" />
              <el-option :label="organizationRoleLabel('MEMBER', t)" value="MEMBER" />
            </el-select>
            <el-tag v-else effect="dark">{{ organizationRoleLabel(row.role, t) }}</el-tag>
            <el-button
              v-if="canRemoveRow(row)"
              size="small"
              type="danger"
              plain
              :disabled="membershipMutationLoading"
              @click="emit('remove', row.memberId)"
            >
              {{ t('organizations.access.remove') }}
            </el-button>
          </div>
        </div>
        <div class="product-grid">
          <div v-for="product in productCatalog" :key="product.key" class="product-item">
            <div class="product-copy">
              <span class="product-label">{{ product.label }}</span>
              <span class="product-state">{{ productAccessLabel(isEnabled(row.memberId, product.key) ? 'ENABLED' : 'DISABLED', t) }}</span>
            </div>
            <el-switch
              :disabled="!canManageProducts || productMutationId === row.memberId"
              :model-value="isEnabled(row.memberId, product.key)"
              @change="emit('toggle-product', row.memberId, product.key, Boolean($event))"
            />
          </div>
        </div>
      </article>
    </div>
  </article>
</template>

<style scoped>
.panel,
.matrix-list,
.matrix-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel {
  padding: 20px;
}

.panel-head,
.member-head,
.member-actions,
.product-copy,
.member-summary,
.batch-toolbar {
  display: flex;
  gap: 12px;
}

.panel-head,
.member-head {
  justify-content: space-between;
}

.batch-toolbar {
  flex-wrap: wrap;
  align-items: center;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(15, 110, 110, 0.08);
}

.batch-role-select {
  min-width: 150px;
}

.matrix-card {
  padding: 16px;
  border-radius: 18px;
  background: rgba(15, 110, 110, 0.05);
  border: 1px solid transparent;
}

.matrix-card--selected {
  border-color: rgba(15, 110, 110, 0.32);
  box-shadow: 0 10px 24px rgba(15, 110, 110, 0.1);
}

.member-summary {
  align-items: flex-start;
}

.member-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  color: var(--mm-primary-dark);
}

.member-meta,
.mm-muted,
.product-state {
  color: var(--mm-muted);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.product-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.7);
}

.product-copy {
  flex-direction: column;
  gap: 4px;
}

.product-label {
  font-weight: 600;
}

@media (max-width: 768px) {
  .panel-head,
  .member-head,
  .member-actions,
  .member-summary {
    flex-direction: column;
    align-items: stretch;
  }

  .product-grid {
    grid-template-columns: 1fr;
  }
}
</style>
