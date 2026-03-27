<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import type { OrgTeamSpaceTrashItem } from '~/types/business'
import { formatBusinessBytes, formatBusinessTime } from '~/utils/business'

const props = defineProps<{
  modelValue: boolean
  items: OrgTeamSpaceTrashItem[]
  loading: boolean
  mutationId: string
  canManage: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  refresh: []
  restore: [item: OrgTeamSpaceTrashItem]
  purge: [item: OrgTeamSpaceTrashItem]
}>()
const { t } = useI18n()
</script>

<template>
  <el-drawer :model-value="modelValue" size="560px" @close="emit('update:modelValue', false)">
    <template #header>
      <div>
        <h2 class="drawer-title">{{ t('business.trash.title') }}</h2>
        <p class="drawer-subtitle">{{ t('business.trash.subtitle') }}</p>
      </div>
    </template>

    <div class="drawer-actions">
      <el-button :loading="loading" @click="emit('refresh')">{{ t('common.actions.refresh') }}</el-button>
    </div>

    <el-empty v-if="items.length === 0 && !loading" :description="t('business.trash.empty')" />
    <div v-else class="trash-list">
      <article v-for="item in items" :key="item.id" class="trash-card">
        <div>
          <div class="trash-title">{{ item.name }}</div>
          <div class="trash-meta">
            {{ item.itemType }} · {{ item.ownerEmail || t('business.trash.unknownOwner') }} · {{ formatBusinessBytes(item.sizeBytes) }}
          </div>
          <div class="trash-meta">
            {{ t('business.trash.trashedAt', { time: formatBusinessTime(item.trashedAt) }) }} · {{ t('business.trash.purgeAt', { time: formatBusinessTime(item.purgeAfterAt) }) }}
          </div>
        </div>
        <div v-if="canManage" class="trash-actions">
          <el-button size="small" link type="primary" :loading="mutationId === item.id" @click="emit('restore', item)">{{ t('business.trash.restore') }}</el-button>
          <el-button size="small" link type="danger" :loading="mutationId === item.id" @click="emit('purge', item)">{{ t('business.trash.purge') }}</el-button>
        </div>
      </article>
    </div>
  </el-drawer>
</template>

<style scoped>
.drawer-title {
  margin: 0;
  font-size: 22px;
}

.drawer-subtitle,
.trash-meta {
  color: var(--mm-muted);
}

.drawer-actions,
.trash-card,
.trash-actions {
  display: flex;
  gap: 10px;
}

.drawer-actions {
  margin-bottom: 14px;
}

.trash-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.trash-card {
  justify-content: space-between;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid var(--mm-border);
  background: rgba(255, 255, 255, 0.9);
}

.trash-title {
  font-weight: 700;
  color: var(--mm-primary-dark);
}

.trash-actions {
  align-items: center;
}

@media (max-width: 768px) {
  .trash-card {
    flex-direction: column;
  }
}
</style>
