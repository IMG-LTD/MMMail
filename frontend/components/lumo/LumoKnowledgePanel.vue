<script setup lang="ts">
import { reactive } from 'vue'
import type { LumoProjectKnowledge } from '~/types/api'
import { useI18n } from '~/composables/useI18n'

interface CreateKnowledgePayload {
  title: string
  content: string
}

interface Props {
  activeProjectId: string
  projectKnowledge: LumoProjectKnowledge[]
  selectedKnowledgeIds: string[]
  creatingKnowledge: boolean
  deletingKnowledgeId: string
  createKnowledge: (payload: CreateKnowledgePayload) => Promise<boolean>
  deleteKnowledge: (item: LumoProjectKnowledge) => Promise<void>
}

const props = defineProps<Props>()
const emit = defineEmits<{
  updateSelectedKnowledgeIds: [value: string[]]
}>()
const { t } = useI18n()

const knowledgeForm = reactive({
  title: '',
  content: ''
})

async function onSubmitKnowledge(): Promise<void> {
  const success = await props.createKnowledge({
    title: knowledgeForm.title,
    content: knowledgeForm.content
  })
  if (!success) {
    return
  }
  knowledgeForm.title = ''
  knowledgeForm.content = ''
}

function onKnowledgeSelectionChange(values: Array<string | number | boolean>): void {
  emit('updateSelectedKnowledgeIds', values.map(String))
}
</script>

<template>
  <section class="knowledge-panel mm-card">
    <header class="knowledge-header">
      <div>
        <h3 class="mm-section-title">{{ t('lumo.knowledge.title') }}</h3>
        <p class="mm-muted">{{ t('lumo.knowledge.selectedCount', { count: props.selectedKnowledgeIds.length }) }}</p>
      </div>
    </header>

    <div v-if="!props.activeProjectId" class="knowledge-empty">
      {{ t('lumo.knowledge.emptyScope') }}
    </div>

    <template v-else>
      <el-form label-position="top" class="knowledge-form">
        <el-form-item :label="t('lumo.knowledge.formTitle')">
          <el-input
            v-model="knowledgeForm.title"
            maxlength="128"
            show-word-limit
            :placeholder="t('lumo.knowledge.formTitlePlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('lumo.knowledge.formContent')">
          <el-input
            v-model="knowledgeForm.content"
            type="textarea"
            :rows="3"
            maxlength="2000"
            show-word-limit
            :placeholder="t('lumo.knowledge.formContentPlaceholder')"
          />
        </el-form-item>
        <el-button type="info" :loading="props.creatingKnowledge" @click="onSubmitKnowledge">
          {{ t('lumo.knowledge.add') }}
        </el-button>
      </el-form>

      <el-checkbox-group
        :model-value="props.selectedKnowledgeIds"
        class="knowledge-list"
        @change="onKnowledgeSelectionChange"
      >
        <article v-for="item in props.projectKnowledge" :key="item.knowledgeId" class="knowledge-item">
          <div class="knowledge-item-head">
            <el-checkbox :value="item.knowledgeId">{{ item.title }}</el-checkbox>
            <el-button
              size="small"
              type="danger"
              text
              :loading="props.deletingKnowledgeId === item.knowledgeId"
              @click="void props.deleteKnowledge(item)"
            >
              {{ t('lumo.knowledge.delete') }}
            </el-button>
          </div>
          <p class="knowledge-item-content">{{ item.content }}</p>
          <div class="mm-muted">{{ t('lumo.knowledge.updatedAt', { value: item.updatedAt }) }}</div>
        </article>
      </el-checkbox-group>

      <div v-if="props.projectKnowledge.length === 0" class="knowledge-empty">
        {{ t('lumo.knowledge.empty') }}
      </div>
    </template>
  </section>
</template>

<style scoped>
.knowledge-panel {
  padding: 16px;
  border: 1px solid rgba(99, 115, 170, 0.12);
  background: linear-gradient(180deg, rgba(251, 252, 255, 0.98), rgba(245, 248, 255, 0.94));
}

.knowledge-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.knowledge-form {
  margin-top: 10px;
}

.knowledge-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.knowledge-item {
  border-radius: 14px;
  padding: 14px;
  border: 1px solid rgba(94, 110, 167, 0.12);
  background: rgba(255, 255, 255, 0.88);
}

.knowledge-item-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
}

.knowledge-item-content {
  margin: 10px 0;
  white-space: pre-wrap;
  color: #26304d;
  line-height: 1.6;
}

.knowledge-empty {
  margin-top: 12px;
  border: 1px dashed rgba(99, 115, 170, 0.26);
  border-radius: 14px;
  padding: 16px;
  color: #67728f;
  background: rgba(248, 250, 255, 0.72);
}
</style>
