<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import type { SuiteUnifiedSearchItem, SuiteUnifiedSearchResult } from '~/types/api'
import { commandItemTypeLabel, formatCommandUpdatedAt } from '~/utils/suite-operations'

interface Props {
  commandKeyword: string
  commandSearchLoading: boolean
  commandSearchSummary: string
  commandSearchResult: SuiteUnifiedSearchResult | null
  searchCommand: () => Promise<void>
  clearCommand: () => void
  openCommandResult: (item: SuiteUnifiedSearchItem) => Promise<void>
}

const props = defineProps<Props>()
const emit = defineEmits<{
  updateCommandKeyword: [value: string]
}>()
const { t } = useI18n()

function onKeywordUpdate(value: string): void {
  emit('updateCommandKeyword', value)
}
</script>

<template>
  <section class="mm-card suite-panel">
    <div class="panel-head">
      <div>
        <h2 class="mm-section-title">Suite Command Search</h2>
        <p class="mm-muted">{{ props.commandSearchSummary }}</p>
      </div>
    </div>

    <div class="command-search-controls">
      <el-input
        :model-value="props.commandKeyword"
        clearable
        placeholder="Try: invoice, roadmap, security baseline..."
        @update:model-value="onKeywordUpdate"
        @keyup.enter="void props.searchCommand()"
      >
        <template #append>
          <el-button :loading="props.commandSearchLoading" @click="void props.searchCommand()">Search</el-button>
        </template>
      </el-input>
      <el-button @click="props.clearCommand">{{ t('common.actions.clear') }}</el-button>
    </div>

    <el-table v-if="props.commandSearchResult" :data="props.commandSearchResult.items" style="width: 100%">
      <el-table-column prop="productCode" label="Product" width="130" />
      <el-table-column label="Type" width="150">
        <template #default="scope">
          <el-tag size="small" type="info">{{ commandItemTypeLabel(scope.row.itemType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="Title" min-width="260" />
      <el-table-column prop="summary" label="Summary" min-width="300" />
      <el-table-column label="Updated At" width="180">
        <template #default="scope">
          {{ formatCommandUpdatedAt(scope.row.updatedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="Action" width="120">
        <template #default="scope">
          <el-button size="small" type="primary" plain @click="void props.openCommandResult(scope.row)">
            Open
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty
      v-if="props.commandSearchResult && props.commandSearchResult.total === 0"
      description="No results found for current keyword"
    />
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

.command-search-controls {
  margin-top: 14px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
}

@media (max-width: 820px) {
  .command-search-controls {
    grid-template-columns: 1fr;
  }
}
</style>
