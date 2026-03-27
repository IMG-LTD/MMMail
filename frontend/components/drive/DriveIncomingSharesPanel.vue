<script setup lang="ts">
import { computed } from 'vue'
import type { DriveIncomingCollaboratorShare } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { countPendingDriveIncomingShares, getDriveCollaboratorStatusI18nKey } from '~/utils/drive-collaboration'

const props = defineProps<{
  items: DriveIncomingCollaboratorShare[]
  loading: boolean
  mutationId: string
}>()

const emit = defineEmits<{
  respond: [payload: { shareId: string; response: 'ACCEPT' | 'DECLINE' }]
  open: [item: DriveIncomingCollaboratorShare]
}>()

const { t } = useI18n()

const pendingCount = computed(() => countPendingDriveIncomingShares(props.items))
const acceptedCount = computed(() => props.items.filter((item) => item.responseStatus === 'ACCEPTED').length)

function formatTime(value: string): string {
  return value ? value.replace('T', ' ').slice(0, 19) : t('common.none')
}

function getTagType(status: string): 'warning' | 'success' | 'info' | 'danger' {
  if (status === 'NEEDS_ACTION') {
    return 'warning'
  }
  if (status === 'ACCEPTED') {
    return 'success'
  }
  if (status === 'DECLINED') {
    return 'info'
  }
  return 'danger'
}
</script>

<template>
  <section class="incoming-panel">
    <div class="incoming-hero mm-card">
      <div>
        <p class="eyebrow">{{ t('drive.collaboration.incoming.badge') }}</p>
        <h2>{{ t('drive.collaboration.incoming.title') }}</h2>
        <p class="description">{{ t('drive.collaboration.incoming.description') }}</p>
      </div>
      <div class="hero-metrics">
        <article class="metric-card">
          <span>{{ t('drive.collaboration.incoming.metrics.total') }}</span>
          <strong>{{ items.length }}</strong>
        </article>
        <article class="metric-card">
          <span>{{ t('drive.collaboration.incoming.metrics.pending') }}</span>
          <strong>{{ pendingCount }}</strong>
        </article>
        <article class="metric-card">
          <span>{{ t('drive.collaboration.incoming.metrics.accepted') }}</span>
          <strong>{{ acceptedCount }}</strong>
        </article>
      </div>
    </div>

    <section class="incoming-list mm-card" v-loading="loading">
      <header class="incoming-list__head">
        <div>
          <h3>{{ t('drive.collaboration.incoming.listTitle') }}</h3>
          <p>{{ t('drive.collaboration.incoming.listDescription') }}</p>
        </div>
      </header>

      <el-empty
        v-if="!loading && items.length === 0"
        :description="t('drive.collaboration.incoming.empty')"
        :image-size="72"
      />

      <div v-else class="incoming-list__body">
        <article v-for="item in items" :key="item.shareId" class="incoming-item">
          <div class="incoming-item__meta">
            <div class="incoming-item__title-row">
              <div>
                <p class="eyebrow">{{ item.itemType === 'FOLDER' ? t('drive.search.types.folder') : t('drive.search.types.file') }}</p>
                <h4>{{ item.itemName }}</h4>
              </div>
              <div class="incoming-item__tags">
                <el-tag size="small" effect="plain">
                  {{ item.permission === 'EDIT' ? t('docs.share.edit') : t('docs.share.view') }}
                </el-tag>
                <el-tag :type="getTagType(item.responseStatus)" size="small" effect="plain">
                  {{ t(getDriveCollaboratorStatusI18nKey(item.responseStatus)) }}
                </el-tag>
              </div>
            </div>
            <p class="incoming-item__owner">
              {{ t('drive.collaboration.incoming.owner', { name: item.ownerDisplayName || item.ownerEmail, email: item.ownerEmail }) }}
            </p>
            <div class="incoming-item__facts">
              <span>{{ t('drive.collaboration.incoming.updatedAt', { time: formatTime(item.updatedAt) }) }}</span>
            </div>
          </div>
          <div class="incoming-item__actions">
            <template v-if="item.responseStatus === 'NEEDS_ACTION'">
              <el-button
                type="primary"
                :loading="mutationId === item.shareId"
                @click="emit('respond', { shareId: item.shareId, response: 'ACCEPT' })"
              >
                {{ t('drive.collaboration.incoming.actions.accept') }}
              </el-button>
              <el-button
                plain
                :loading="mutationId === item.shareId"
                @click="emit('respond', { shareId: item.shareId, response: 'DECLINE' })"
              >
                {{ t('drive.collaboration.incoming.actions.decline') }}
              </el-button>
            </template>
            <el-button
              v-else-if="item.responseStatus === 'ACCEPTED'"
              type="primary"
              plain
              @click="emit('open', item)"
            >
              {{ t('drive.collaboration.incoming.actions.open') }}
            </el-button>
          </div>
        </article>
      </div>
    </section>
  </section>
</template>

<style scoped>
.incoming-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.incoming-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(260px, 0.9fr);
  gap: 18px;
  padding: 20px;
  background:
    radial-gradient(circle at top left, rgba(105, 123, 255, 0.14), transparent 30%),
    linear-gradient(135deg, rgba(15, 31, 61, 0.98), rgba(10, 24, 47, 0.98));
  color: #eff6ff;
}

.eyebrow,
.description,
.incoming-list__head h3,
.incoming-list__head p,
.incoming-item__owner,
.incoming-item__facts,
.incoming-item__title-row h4 {
  margin: 0;
}

.eyebrow {
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(207, 220, 255, 0.72);
}

.description,
.incoming-item__owner,
.incoming-item__facts,
.incoming-list__head p {
  color: var(--mm-muted);
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.metric-card,
.incoming-item {
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
  color: rgba(226, 240, 255, 0.78);
}

.metric-card strong {
  display: block;
  margin-top: 10px;
  font-size: 24px;
}

.incoming-list {
  padding: 18px;
}

.incoming-list__head {
  margin-bottom: 14px;
}

.incoming-list__body {
  display: grid;
  gap: 14px;
}

.incoming-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  background: linear-gradient(180deg, rgba(250, 252, 255, 0.98), rgba(242, 246, 255, 0.92));
}

.incoming-item__meta,
.incoming-item__actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.incoming-item__meta {
  flex: 1;
}

.incoming-item__title-row,
.incoming-item__facts {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.incoming-item__tags,
.incoming-item__actions {
  align-items: flex-end;
}

.incoming-item__actions {
  justify-content: center;
}

@media (max-width: 960px) {
  .incoming-hero {
    grid-template-columns: 1fr;
  }
}
</style>
