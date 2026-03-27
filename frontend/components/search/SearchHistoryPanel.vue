<script setup lang="ts">
import type { SearchHistoryItem } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { buildSearchUsageText } from '~/utils/search-workspace'

const props = defineProps<{
  items: SearchHistoryItem[]
  loading?: boolean
}>()

const emit = defineEmits<{
  apply: [keyword: string]
  remove: [historyId: string]
  clear: []
}>()

const { locale, t } = useI18n()
</script>

<template>
  <section class="mm-card panel">
    <div class="panel-head">
      <h2 class="mm-section-subtitle">{{ t('search.history.title') }}</h2>
      <el-button size="small" text type="danger" @click="emit('clear')">{{ t('search.history.clearAll') }}</el-button>
    </div>
    <el-skeleton :loading="loading" animated :rows="2">
      <template #default>
        <div v-if="items.length === 0" class="empty">{{ t('search.history.empty') }}</div>
        <div v-else class="history-list">
          <div v-for="item in items" :key="item.id" class="history-item">
            <button class="history-keyword" @click="emit('apply', item.keyword)">{{ item.keyword }}</button>
            <div class="saved-meta">
              {{ buildSearchUsageText(item, locale, t) }}
            </div>
            <el-button size="small" text type="danger" @click="emit('remove', item.id)">{{ t('search.history.delete') }}</el-button>
          </div>
        </div>
      </template>
    </el-skeleton>
  </section>
</template>

<style scoped>
.panel {
  padding: 16px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.history-list {
  display: grid;
  gap: 8px;
}

.history-item {
  border: 1px solid var(--mm-line);
  border-radius: 10px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.history-keyword {
  all: unset;
  color: var(--mm-primary-dark);
  cursor: pointer;
  font-weight: 600;
  flex: 1;
}

.saved-meta,
.empty {
  font-size: 13px;
  color: var(--mm-muted);
}

@media (max-width: 1080px) {
  .history-item {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
