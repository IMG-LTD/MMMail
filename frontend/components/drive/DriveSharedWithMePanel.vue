<script setup lang="ts">
import { computed } from 'vue'
import type { DriveSavedShare } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { getDriveSavedShareStatusI18nKey } from '~/utils/drive-share'

const props = defineProps<{
  items: DriveSavedShare[]
  loading: boolean
  removingId: string
}>()

const emit = defineEmits<{
  open: [item: DriveSavedShare]
  remove: [item: DriveSavedShare]
}>()

const { t } = useI18n()

const activeCount = computed(() => props.items.filter(item => item.status === 'ACTIVE').length)
const unavailableCount = computed(() => props.items.filter(item => item.status !== 'ACTIVE').length)

function formatTime(value: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 19) : t('common.none')
}

function getStatusTagType(status: string): 'success' | 'warning' | 'info' | 'danger' {
  if (status === 'ACTIVE') {
    return 'success'
  }
  if (status === 'EXPIRED') {
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
        <p class="eyebrow">{{ t('drive.sharedWithMe.badge') }}</p>
        <h2>{{ t('drive.sharedWithMe.title') }}</h2>
        <p class="description">{{ t('drive.sharedWithMe.description') }}</p>
      </div>
      <div class="hero-metrics">
        <article class="metric-card">
          <span>{{ t('drive.sharedWithMe.metrics.total') }}</span>
          <strong>{{ items.length }}</strong>
        </article>
        <article class="metric-card">
          <span>{{ t('drive.sharedWithMe.metrics.active') }}</span>
          <strong>{{ activeCount }}</strong>
        </article>
        <article class="metric-card">
          <span>{{ t('drive.sharedWithMe.metrics.unavailable') }}</span>
          <strong>{{ unavailableCount }}</strong>
        </article>
      </div>
    </div>

    <section class="shared-list mm-card" v-loading="loading">
      <header class="shared-list__head">
        <div>
          <h3>{{ t('drive.sharedWithMe.listTitle') }}</h3>
          <p>{{ t('drive.sharedWithMe.listDescription') }}</p>
        </div>
      </header>

      <el-empty
        v-if="!loading && items.length === 0"
        :description="t('drive.sharedWithMe.empty')"
        :image-size="76"
      />

      <div v-else class="shared-list__body">
        <article v-for="item in items" :key="item.id" class="shared-item" :class="{ 'shared-item--unavailable': !item.available }">
          <div class="shared-item__meta">
            <div class="shared-item__title-row">
              <div>
                <p class="shared-item__eyebrow">{{ item.itemType === 'FOLDER' ? t('drive.search.types.folder') : t('drive.search.types.file') }}</p>
                <h4>{{ item.itemName }}</h4>
              </div>
              <div class="shared-item__tags">
                <el-tag size="small" effect="plain">{{ item.permission === 'EDIT' ? t('docs.share.edit') : t('docs.share.view') }}</el-tag>
                <el-tag :type="getStatusTagType(item.status)" size="small" effect="plain">
                  {{ t(getDriveSavedShareStatusI18nKey(item.status)) }}
                </el-tag>
              </div>
            </div>
            <p class="shared-item__owner">
              {{ t('drive.sharedWithMe.owner', { name: item.ownerDisplayName || item.ownerEmail, email: item.ownerEmail }) }}
            </p>
            <div class="shared-item__facts">
              <span>{{ t('drive.sharedWithMe.savedAt', { time: formatTime(item.savedAt) }) }}</span>
              <span>{{ t('drive.sharedWithMe.expiresAt', { time: formatTime(item.expiresAt) }) }}</span>
            </div>
          </div>
          <div class="shared-item__actions">
            <el-button type="primary" @click="emit('open', item)">{{ t('drive.sharedWithMe.actions.open') }}</el-button>
            <el-button
              type="danger"
              plain
              :loading="removingId === item.id"
              @click="emit('remove', item)"
            >
              {{ t('drive.sharedWithMe.actions.remove') }}
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
  grid-template-columns: minmax(0, 1.25fr) minmax(280px, 0.95fr);
  gap: 18px;
  padding: 20px;
  background:
    radial-gradient(circle at top left, rgba(118, 140, 255, 0.18), transparent 26%),
    radial-gradient(circle at bottom right, rgba(64, 214, 176, 0.16), transparent 28%),
    linear-gradient(135deg, rgba(10, 43, 64, 0.98), rgba(9, 28, 48, 0.98));
  color: #f5fbff;
}

.eyebrow,
.shared-item__eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(212, 231, 255, 0.72);
}

.shared-hero h2,
.shared-list__head h3,
.shared-item__title-row h4,
.description,
.shared-list__head p,
.shared-item__owner,
.shared-item__facts {
  margin: 0;
}

.description {
  color: rgba(236, 245, 255, 0.82);
  line-height: 1.75;
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
  color: rgba(226, 241, 255, 0.76);
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
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.shared-list__head p,
.shared-item__owner,
.shared-item__facts {
  color: var(--mm-muted);
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
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.96), rgba(241, 247, 252, 0.92));
}

.shared-item--unavailable {
  background: linear-gradient(180deg, rgba(247, 249, 252, 0.95), rgba(239, 242, 247, 0.92));
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

  .hero-metrics {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .shared-item {
    flex-direction: column;
  }

  .shared-item__actions {
    align-items: stretch;
  }
}

@media (max-width: 640px) {
  .hero-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
