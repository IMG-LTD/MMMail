<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { SheetsIncomingShare, SheetsShareResponseStatus } from '~/types/sheets'
import { formatSheetsTime } from '~/utils/sheets'

const props = defineProps<{
  items: SheetsIncomingShare[]
  loading: boolean
  errorMessage: string
  mutationId: string
}>()

const emit = defineEmits<{
  refresh: []
  respond: [shareId: string, response: 'ACCEPT' | 'DECLINE']
  open: [item: SheetsIncomingShare]
}>()

const { t } = useI18n()

const totalCount = computed(() => props.items.length)
const pendingCount = computed(() => props.items.filter((item) => item.responseStatus === 'NEEDS_ACTION').length)
const acceptedCount = computed(() => props.items.filter((item) => item.responseStatus === 'ACCEPTED').length)

function getStatusTagType(status: SheetsShareResponseStatus): 'warning' | 'success' | 'info' {
  if (status === 'NEEDS_ACTION') {
    return 'warning'
  }
  if (status === 'ACCEPTED') {
    return 'success'
  }
  return 'info'
}

function getStatusLabel(status: SheetsShareResponseStatus): string {
  if (status === 'ACCEPTED') {
    return t('sheets.share.status.accepted')
  }
  if (status === 'DECLINED') {
    return t('sheets.share.status.declined')
  }
  return t('sheets.share.status.needsAction')
}
</script>

<template>
  <section class="incoming-panel">
    <div class="incoming-hero mm-card">
      <div>
        <p class="incoming-hero__eyebrow">{{ t('sheets.incoming.eyebrow') }}</p>
        <h2>{{ t('sheets.incoming.title') }}</h2>
        <p class="incoming-hero__description">{{ t('sheets.incoming.description') }}</p>
      </div>
      <div class="incoming-hero__metrics">
        <article class="incoming-metric">
          <span>{{ t('sheets.incoming.metrics.total') }}</span>
          <strong data-testid="sheets-incoming-total">{{ totalCount }}</strong>
        </article>
        <article class="incoming-metric">
          <span>{{ t('sheets.incoming.metrics.pending') }}</span>
          <strong data-testid="sheets-incoming-pending">{{ pendingCount }}</strong>
        </article>
        <article class="incoming-metric">
          <span>{{ t('sheets.incoming.metrics.accepted') }}</span>
          <strong data-testid="sheets-incoming-accepted">{{ acceptedCount }}</strong>
        </article>
      </div>
    </div>

    <section data-testid="sheets-incoming-list" class="incoming-list mm-card" v-loading="loading">
      <header class="incoming-list__header">
        <div>
          <h3>{{ t('sheets.filters.incoming') }}</h3>
          <p>{{ t('sheets.incoming.description') }}</p>
        </div>
        <el-button data-testid="sheets-incoming-refresh" plain @click="emit('refresh')">
          {{ t('common.actions.refresh') }}
        </el-button>
      </header>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        :closable="false"
      />

      <el-empty
        v-if="!loading && items.length === 0"
        data-testid="sheets-incoming-empty"
        :description="t('sheets.incoming.empty')"
        :image-size="72"
      />

      <div v-else data-testid="sheets-incoming-body" class="incoming-list__body">
        <article v-for="item in items" :key="item.shareId" class="incoming-item">
          <div class="incoming-item__meta">
            <div class="incoming-item__title">
              <div>
                <p class="incoming-item__eyebrow">{{ t('sheets.filters.shared') }}</p>
                <h4>{{ item.workbookTitle }}</h4>
              </div>
              <div class="incoming-item__tags">
                <el-tag size="small" effect="plain">{{ item.permission === 'EDIT' ? t('sheets.share.permission.edit') : t('sheets.share.permission.view') }}</el-tag>
                <el-tag size="small" effect="plain" :type="getStatusTagType(item.responseStatus)">
                  {{ getStatusLabel(item.responseStatus) }}
                </el-tag>
              </div>
            </div>
            <p class="incoming-item__owner">{{ t('sheets.incoming.owner', { value: item.ownerDisplayName || item.ownerEmail }) }}</p>
            <small>{{ t('sheets.incoming.updatedAt', { value: formatSheetsTime(item.updatedAt) }) }}</small>
          </div>

          <div class="incoming-item__actions">
            <template v-if="item.responseStatus === 'NEEDS_ACTION'">
              <el-button
                :data-testid="`sheets-incoming-accept-${item.shareId}`"
                type="primary"
                :loading="mutationId === item.shareId"
                @click="emit('respond', item.shareId, 'ACCEPT')"
              >
                {{ t('sheets.incoming.accept') }}
              </el-button>
              <el-button
                :data-testid="`sheets-incoming-decline-${item.shareId}`"
                plain
                :loading="mutationId === item.shareId"
                @click="emit('respond', item.shareId, 'DECLINE')"
              >
                {{ t('sheets.incoming.decline') }}
              </el-button>
            </template>
            <el-button
              :data-testid="`sheets-incoming-open-${item.shareId}`"
              v-else-if="item.responseStatus === 'ACCEPTED'"
              type="primary"
              plain
              @click="emit('open', item)"
            >
              {{ t('sheets.incoming.open') }}
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
  grid-template-columns: minmax(0, 1.2fr) minmax(240px, 0.95fr);
  gap: 18px;
  padding: 20px;
  background:
    radial-gradient(circle at top left, rgba(88, 114, 255, 0.18), transparent 28%),
    radial-gradient(circle at bottom right, rgba(15, 110, 110, 0.18), transparent 32%),
    linear-gradient(135deg, rgba(10, 43, 64, 0.98), rgba(8, 32, 53, 0.98));
  color: #f4fbff;
}

.incoming-hero__eyebrow,
.incoming-item__eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(212, 231, 255, 0.72);
}

.incoming-hero h2,
.incoming-hero__description,
.incoming-list__header h3,
.incoming-list__header p,
.incoming-item__owner,
.incoming-item__title h4,
.incoming-item__title p,
.incoming-item__meta small {
  margin: 0;
}

.incoming-hero__description,
.incoming-list__header p,
.incoming-item__owner,
.incoming-item__meta small {
  color: var(--mm-muted);
  line-height: 1.7;
}

.incoming-hero__description {
  color: rgba(236, 245, 255, 0.82);
}

.incoming-hero__metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.incoming-metric,
.incoming-item {
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.incoming-metric {
  padding: 16px;
  background: rgba(255, 255, 255, 0.08);
}

.incoming-metric span {
  display: block;
  font-size: 12px;
  color: rgba(226, 241, 255, 0.76);
}

.incoming-metric strong {
  display: block;
  margin-top: 10px;
  font-size: 24px;
}

.incoming-list {
  padding: 18px;
}

.incoming-list__header,
.incoming-item,
.incoming-item__title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.incoming-list__header {
  align-items: flex-start;
  margin-bottom: 14px;
}

.incoming-list__body {
  display: grid;
  gap: 14px;
}

.incoming-item {
  padding: 18px;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.96), rgba(241, 247, 252, 0.92));
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

.incoming-item__tags,
.incoming-item__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: flex-end;
}

.incoming-item__actions {
  justify-content: center;
}

@media (max-width: 960px) {
  .incoming-hero,
  .incoming-item,
  .incoming-item__title {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .incoming-item__actions {
    align-items: stretch;
  }
}
</style>
