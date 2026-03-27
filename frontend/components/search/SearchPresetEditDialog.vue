<script setup lang="ts">
import { computed } from 'vue'
import type { LabelItem } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import {
  buildSearchFolderOptions,
  buildSearchReadStateOptions,
  buildSearchStarStateOptions,
  type SearchPresetEditorState
} from '~/utils/search-workspace'

defineProps<{
  labels: LabelItem[]
}>()

const visible = defineModel<boolean>('visible', { required: true })
const form = defineModel<SearchPresetEditorState>('form', { required: true })

const emit = defineEmits<{
  save: []
}>()

const { t } = useI18n()
const folderOptions = computed(() => buildSearchFolderOptions(t))
const readOptions = computed(() => buildSearchReadStateOptions(t))
const starOptions = computed(() => buildSearchStarStateOptions(t))
</script>

<template>
  <el-dialog v-model="visible" :title="t('search.dialog.title')" width="720px">
    <div class="edit-grid">
      <el-form-item :label="t('search.dialog.fields.name')">
        <el-input v-model="form.name" maxlength="64" />
      </el-form-item>
      <el-form-item :label="t('search.dialog.fields.keyword')">
        <el-input v-model="form.keyword" maxlength="512" />
      </el-form-item>
      <el-form-item :label="t('search.dialog.fields.folder')">
        <el-select v-model="form.folder" clearable :placeholder="t('search.filters.folder')">
          <el-option
            v-for="item in folderOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('search.dialog.fields.dateRange')">
        <el-date-picker
          v-model="form.dateRange"
          type="datetimerange"
          :start-placeholder="t('search.filters.from')"
          :end-placeholder="t('search.filters.to')"
          value-format="YYYY-MM-DDTHH:mm:ss"
        />
      </el-form-item>
      <el-form-item :label="t('search.dialog.fields.label')">
        <el-select v-model="form.label" clearable filterable :placeholder="t('search.filters.label')">
          <el-option v-for="item in labels" :key="item.id" :label="item.name" :value="item.name" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('search.dialog.fields.readState')">
        <el-select v-model="form.unread">
          <el-option
            v-for="item in readOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('search.dialog.fields.starState')">
        <el-select v-model="form.starred">
          <el-option
            v-for="item in starOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
    </div>
    <template #footer>
      <el-button @click="visible = false">{{ t('common.actions.cancel') }}</el-button>
      <el-button type="primary" @click="emit('save')">{{ t('common.actions.save') }}</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.edit-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

@media (max-width: 1080px) {
  .edit-grid {
    grid-template-columns: 1fr;
  }
}
</style>
