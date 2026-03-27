<script setup lang="ts">
import { computed } from 'vue'
import type { DriveCollaboratorSharedItem } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { canOpenDriveCollaboratorShare, getDriveCollaboratorStatusI18nKey } from '~/utils/drive-collaboration'

const props = defineProps<{
  items: DriveCollaboratorSharedItem[]
  loading: boolean
  activeShareId: string
}>()

const emit = defineEmits<{
  open: [item: DriveCollaboratorSharedItem]
}>()

const { t } = useI18n()

const availableCount = computed(() => props.items.filter((item) => canOpenDriveCollaboratorShare(item)).length)
const revokedCount = computed(() => props.items.filter((item) => item.status === 'REVOKED').length)

function formatTime(value: string): string {
  return value ? value.replace('T', ' ').slice(0, 19) : t('common.none')
}

function getTagType(status: string): 'success' | 'warning' | 'info' | 'danger' {
  if (status === 'ACCEPTED') {
    return 'success'
  }
  if (status === 'NEEDS_ACTION') {
    return 'warning'
  }
  if (status === 'REVOKED') {
    return 'danger'
  }
  return 'info'
}
</script>

<template>
  <section class="shared-panel">
    <div class="shared-hero mm-card">
      <div>
        <p class="eyebrow">{{ t('drive.collaboration.shared.badge') }}</p>
        <h2>{{ t('drive.collaboration.shared.title') }}</h2>
        <p class="description">{{ t('drive.collaboration.shared.description') }}</p>
      </div>
      <div class="hero-metrics">
        <article class="metric-card">
          <span>{{ t('drive.collaboration.shared.metrics.total') }}</span>
          <strong>{{ items.length }}</strong>
        </article>
        <article class="metric-card">
          <span>{{ t('drive.collaboration.shared.metrics.available') }}</span>
          <strong>{{ availableCount }}</strong>
        </article>
        <article class="metric-card">
          <span>{{ t('drive.collaboration.shared.metrics.revoked') }}</span>
          <strong>{{ revokedCount }}</strong>
        </article>
      </div>
    </div>

    <section class="shared-list mm-card" v-loading="loading">
      <header class="shared-list__head">
        <div>
          <h3>{{ t('drive.collaboration.shared.listTitle') }}</h3>
          <p>{{ t('drive.collaboration.shared.listDescription') }}</p>
        </div>
      </header>

      <el-empty
        v-if="!loading && items.length === 0"
        :description="t('drive.collaboration.shared.empty')"
        :image-size="72"
      />

      <div v-else class="shared-list__body">
        <article
          v-for="item in items"
          :key="item.shareId"
          class="shared-item"
          :class="{
            'shared-item--inactive': !item.available,
            'shared-item--active': activeShareId === item.shareId
          }"
        >
          <div class="shared-item__meta">
            <div class="shared-item__title-row">
              <div>
                <p class="eyebrow">{{ item.itemType === 'FOLDER' ? t('drive.search.types.folder') : t('drive.search.types.file') }}</p>
                <h4>{{ item.itemName }}</h4>
              </div>
              <div class="shared-item__tags">
                <el-tag size="small" effect="plain">
                  {{ item.permission === 'EDIT' ? t('docs.share.edit') : t('docs.share.view') }}
                </el-tag>
                <el-tag :type="getTagType(item.status)" size="small" effect="plain">
                  {{ t(getDriveCollaboratorStatusI18nKey(item.status)) }}
                </el-tag>
              </div>
            </div>
            <p class="shared-item__owner">
              {{ t('drive.collaboration.shared.owner', { name: item.ownerDisplayName || item.ownerEmail, email: item.ownerEmail }) }}
            </p>
            <div class="shared-item__facts">
              <span>{{ t('drive.collaboration.shared.updatedAt', { time: formatTime(item.updatedAt) }) }}</span>
            </div>
          </div>
          <div class="shared-item__actions">
            <el-button
              type="primary"
              :disabled="!canOpenDriveCollaboratorShare(item)"
              @click="emit('open', item)"
            >
              {{ t(activeShareId === item.shareId ? 'drive.collaboration.shared.actions.opened' : 'drive.collaboration.shared.actions.open') }}
            </el-button>
          </div>
        </article>
      </div>
    </section>
  </section>
</template>

<style scoped>
.shared-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.shared-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(260px, 0.9fr);
  gap: 18px;
  padding: 20px;
  background:
    radial-gradient(circle at top right, rgba(73, 214, 187, 0.16), transparent 28%),
    linear-gradient(135deg, rgba(11, 42, 53, 0.98), rgba(8, 25, 40, 0.98));
  color: #effcff;
}

.eyebrow,
.description,
.shared-list__head h3,
.shared-list__head p,
.shared-item__owner,
.shared-item__facts,
.shared-item__title-row h4 {
  margin: 0;
}

.eyebrow {
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(212, 242, 235, 0.76);
}

.description,
.shared-list__head p,
.shared-item__owner,
.shared-item__facts {
  color: var(--mm-muted);
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.metric-card,
.shared-item {
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.metric-card {
  padding: 16px;
  background: rgba(255, 255, 255, 0.08);
}

.metric-card span {
  display: block;
  font-size: 12px;
  color: rgba(230, 248, 244, 0.78);
}

.metric-card strong {
  display: block;
  margin-top: 10px;
  font-size: 24px;
}

.shared-list {
  padding: 18px;
}

.shared-list__head {
  margin-bottom: 14px;
}

.shared-list__body {
  display: grid;
  gap: 14px;
}

.shared-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  background: linear-gradient(180deg, rgba(248, 252, 252, 0.98), rgba(239, 247, 246, 0.94));
}

.shared-item--inactive {
  background: linear-gradient(180deg, rgba(246, 248, 249, 0.96), rgba(239, 242, 245, 0.92));
}

.shared-item--active {
  border-color: rgba(61, 182, 157, 0.38);
  box-shadow: 0 0 0 1px rgba(61, 182, 157, 0.14);
}

.shared-item__meta,
.shared-item__actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.shared-item__meta {
  flex: 1;
}

.shared-item__title-row,
.shared-item__facts {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.shared-item__tags,
.shared-item__actions {
  align-items: flex-end;
}

.shared-item__actions {
  justify-content: center;
}

@media (max-width: 960px) {
  .shared-hero {
    grid-template-columns: 1fr;
  }
}
</style>
