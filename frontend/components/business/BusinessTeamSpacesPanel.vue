<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import type { OrgRole } from '~/types/api'
import type { OrgTeamSpace } from '~/types/business'
import { calculateBusinessStoragePercent, formatBusinessBytes, formatBusinessTime } from '~/utils/business'

const props = defineProps<{
  teamSpaces: OrgTeamSpace[]
  selectedTeamSpaceId: string
  currentRole: OrgRole | null | undefined
}>()

const emit = defineEmits<{
  select: [teamSpaceId: string]
}>()
const { t } = useI18n()

function storagePercent(space: OrgTeamSpace): number {
  return calculateBusinessStoragePercent(space.storageBytes, space.storageLimitBytes)
}
</script>

<template>
  <aside class="mm-card spaces-panel">
    <div class="panel-head">
      <div>
        <h2 class="mm-section-title">{{ t('business.teamSpaces.title') }}</h2>
        <p class="mm-muted">{{ t('business.teamSpaces.subtitle') }}</p>
      </div>
      <el-tag v-if="currentRole" type="success">{{ currentRole }}</el-tag>
    </div>
    <el-empty v-if="teamSpaces.length === 0" :description="t('business.teamSpaces.empty')" />
    <div v-else class="space-list">
      <button
        v-for="space in teamSpaces"
        :key="space.id"
        type="button"
        class="space-card"
        :class="{ 'space-card--active': space.id === selectedTeamSpaceId }"
        @click="emit('select', space.id)"
      >
        <div class="space-card__top">
          <div>
            <div class="space-card__title">{{ space.name }}</div>
            <div class="space-card__slug">/{{ space.slug }}</div>
          </div>
          <div class="space-card__badges">
            <el-tag effect="plain">{{ t('business.teamSpaces.items', { count: space.itemCount }) }}</el-tag>
            <el-tag v-if="space.currentAccessRole" type="primary" effect="dark">{{ space.currentAccessRole }}</el-tag>
          </div>
        </div>
        <p class="space-card__desc">{{ space.description || t('business.teamSpaces.fallbackDescription') }}</p>
        <el-progress :percentage="storagePercent(space)" :show-text="false" :stroke-width="8" />
        <div class="space-card__meta">
          <span>{{ formatBusinessBytes(space.storageBytes) }} / {{ formatBusinessBytes(space.storageLimitBytes) }}</span>
          <span>{{ formatBusinessTime(space.updatedAt) }}</span>
        </div>
      </button>
    </div>
  </aside>
</template>

<style scoped>
.spaces-panel {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.panel-head,
.space-card__top,
.space-card__badges {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.space-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.space-card {
  padding: 16px;
  border: 1px solid var(--mm-border);
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(243, 248, 249, 0.96));
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.space-card:hover,
.space-card--active {
  border-color: rgba(15, 110, 110, 0.45);
  box-shadow: 0 12px 24px rgba(15, 110, 110, 0.12);
  transform: translateY(-1px);
}

.space-card__title {
  font-size: 17px;
  font-weight: 700;
  color: var(--mm-primary-dark);
}

.space-card__slug,
.space-card__desc,
.space-card__meta,
.mm-muted {
  color: var(--mm-muted);
}

.space-card__desc {
  margin: 12px 0;
  min-height: 40px;
}

.space-card__meta {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

@media (max-width: 768px) {
  .panel-head,
  .space-card__top,
  .space-card__meta {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
