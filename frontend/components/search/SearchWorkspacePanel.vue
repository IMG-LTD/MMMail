<script setup lang="ts">
import { computed } from 'vue'
import type { LabelItem } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import {
  buildSearchFolderOptions,
  buildSearchReadStateOptions,
  buildSearchStarStateOptions,
  type SearchReadState,
  type SearchStarState
} from '~/utils/search-workspace'

defineProps<{
  labels: LabelItem[]
}>()

const keyword = defineModel<string>('keyword', { required: true })
const folder = defineModel<string>('folder', { required: true })
const dateRange = defineModel<string[]>('dateRange', { required: true })
const label = defineModel<string>('label', { required: true })
const unread = defineModel<SearchReadState>('unread', { required: true })
const starred = defineModel<SearchStarState>('starred', { required: true })
const saveName = defineModel<string>('saveName', { required: true })

const emit = defineEmits<{
  search: []
  save: []
}>()

const { t } = useI18n()
const folderOptions = computed(() => buildSearchFolderOptions(t))
const readOptions = computed(() => buildSearchReadStateOptions(t))
const starOptions = computed(() => buildSearchStarStateOptions(t))
</script>

<template>
  <section class="mm-card panel">
    <h1 class="mm-section-title">{{ t('search.panel.title') }}</h1>
    <div class="filters">
      <el-input
        v-model="keyword"
        :placeholder="t('search.filters.keywordPlaceholder')"
        @keyup.enter="emit('search')"
      />
      <el-select v-model="folder" clearable :placeholder="t('search.filters.folder')">
        <el-option
          v-for="item in folderOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="datetimerange"
        :start-placeholder="t('search.filters.from')"
        :end-placeholder="t('search.filters.to')"
        value-format="YYYY-MM-DDTHH:mm:ss"
      />
      <el-select v-model="label" clearable filterable :placeholder="t('search.filters.label')">
        <el-option v-for="item in labels" :key="item.id" :label="item.name" :value="item.name" />
      </el-select>
      <el-select v-model="unread" :placeholder="t('search.filters.readState')">
        <el-option
          v-for="item in readOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <el-select v-model="starred" :placeholder="t('search.filters.starState')">
        <el-option
          v-for="item in starOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <el-button type="primary" @click="emit('search')">{{ t('search.filters.search') }}</el-button>
    </div>
    <div class="save-row">
      <el-input v-model="saveName" :placeholder="t('search.filters.saveName')" />
      <el-button type="success" plain @click="emit('save')">{{ t('search.filters.save') }}</el-button>
    </div>
  </section>
</template>

<style scoped>
.panel {
  padding: 16px;
}

.filters {
  display: grid;
  grid-template-columns: 2fr 1fr 1.5fr 1fr 1fr 1fr auto;
  gap: 8px;
}

.save-row {
  margin-top: 10px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}

@media (max-width: 1080px) {
  .filters,
  .save-row {
    grid-template-columns: 1fr;
  }
}
</style>
