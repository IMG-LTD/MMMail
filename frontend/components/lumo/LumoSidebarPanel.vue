<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { LumoConversation, LumoProject } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import type { LumoModelCode } from '~/utils/lumo-workspace'

interface CreateProjectPayload {
  name: string
  description: string
}

interface CreateConversationPayload {
  title: string
  modelCode: LumoModelCode
  projectId?: string
}

interface Props {
  projects: LumoProject[]
  conversations: LumoConversation[]
  selectedProjectId: string
  activeConversationId: string
  creatingProject: boolean
  creatingConversation: boolean
  modelOptions: readonly LumoModelCode[]
  resolveProjectLabel: (projectId: string | null) => string
  resolveModelLabel: (modelCode: LumoModelCode) => string
  createProject: (payload: CreateProjectPayload) => Promise<boolean>
  createConversation: (payload: CreateConversationPayload) => Promise<boolean>
  changeProjectFilter: (projectId: string) => Promise<void>
  selectConversation: (conversationId: string) => Promise<void>
}

const props = defineProps<Props>()
const { t } = useI18n()

const projectForm = reactive({
  name: '',
  description: ''
})

const conversationForm = reactive<CreateConversationPayload>({
  title: '',
  modelCode: props.modelOptions[0],
  projectId: props.selectedProjectId || undefined
})

watch(
  () => props.selectedProjectId,
  (projectId) => {
    conversationForm.projectId = projectId || undefined
  },
  { immediate: true }
)

async function onProjectFilterChange(value: string | number | boolean): Promise<void> {
  await props.changeProjectFilter(String(value || ''))
}

async function onSubmitProject(): Promise<void> {
  const success = await props.createProject({
    name: projectForm.name,
    description: projectForm.description
  })
  if (!success) {
    return
  }
  projectForm.name = ''
  projectForm.description = ''
}

async function onSubmitConversation(): Promise<void> {
  const success = await props.createConversation({
    title: conversationForm.title,
    modelCode: conversationForm.modelCode,
    projectId: conversationForm.projectId
  })
  if (!success) {
    return
  }
  conversationForm.title = ''
}

function onConversationScopeChange(value: string | number | boolean): void {
  const nextValue = String(value || '')
  conversationForm.projectId = nextValue || undefined
}
</script>

<template>
  <aside class="mm-card lumo-sidebar">
    <header class="sidebar-header">
      <div>
        <h2 class="mm-section-title">{{ t('lumo.sidebar.title') }}</h2>
        <p class="mm-muted">{{ t('lumo.hero.privacy.projectScoped') }}</p>
      </div>
    </header>

    <section class="sidebar-section">
      <el-form label-position="top">
        <el-form-item :label="t('lumo.sidebar.projectFilter')">
          <el-select
            :model-value="props.selectedProjectId"
            clearable
            :placeholder="t('lumo.sidebar.allProjects')"
            @change="onProjectFilterChange"
          >
            <el-option :label="t('lumo.sidebar.allProjects')" value="" />
            <el-option
              v-for="item in props.projects"
              :key="item.projectId"
              :label="`${item.name} (${item.conversationCount})`"
              :value="item.projectId"
            />
          </el-select>
        </el-form-item>
      </el-form>
    </section>

    <section class="sidebar-section sidebar-form">
      <h3 class="mm-section-subtitle">{{ t('lumo.sidebar.newProject') }}</h3>
      <el-form label-position="top">
        <el-form-item :label="t('lumo.sidebar.projectName')">
          <el-input
            v-model="projectForm.name"
            maxlength="64"
            show-word-limit
            :placeholder="t('lumo.sidebar.projectNamePlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('lumo.sidebar.projectDescription')">
          <el-input
            v-model="projectForm.description"
            type="textarea"
            :rows="2"
            maxlength="256"
            show-word-limit
            :placeholder="t('lumo.sidebar.projectDescriptionPlaceholder')"
          />
        </el-form-item>
        <el-button type="info" :loading="props.creatingProject" @click="onSubmitProject">
          {{ t('lumo.sidebar.createProject') }}
        </el-button>
      </el-form>
    </section>

    <section class="sidebar-section sidebar-form">
      <h3 class="mm-section-subtitle">{{ t('lumo.sidebar.newConversation') }}</h3>
      <el-form label-position="top">
        <el-form-item :label="t('lumo.sidebar.conversationTitle')">
          <el-input
            v-model="conversationForm.title"
            :placeholder="t('lumo.sidebar.conversationTitlePlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('lumo.sidebar.model')">
          <el-select v-model="conversationForm.modelCode">
            <el-option
              v-for="item in props.modelOptions"
              :key="item"
              :label="props.resolveModelLabel(item)"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('lumo.sidebar.projectScope')">
          <el-select
            :model-value="conversationForm.projectId || ''"
            clearable
            :placeholder="t('lumo.sidebar.noProject')"
            @change="onConversationScopeChange"
          >
            <el-option :label="t('lumo.sidebar.noProject')" value="" />
            <el-option
              v-for="item in props.projects"
              :key="item.projectId"
              :label="item.name"
              :value="item.projectId"
            />
          </el-select>
        </el-form-item>
        <el-button type="primary" :loading="props.creatingConversation" @click="onSubmitConversation">
          {{ t('lumo.sidebar.createConversation') }}
        </el-button>
      </el-form>
    </section>

    <section class="sidebar-section conversation-list-section">
      <div v-if="props.conversations.length === 0" class="sidebar-empty">
        {{ t('lumo.sidebar.empty') }}
      </div>

      <button
        v-for="item in props.conversations"
        :key="item.conversationId"
        class="conversation-item"
        :class="{ active: item.conversationId === props.activeConversationId }"
        @click="void props.selectConversation(item.conversationId)"
      >
        <div class="conversation-top">
          <div class="conversation-title">{{ item.title }}</div>
          <el-tag v-if="item.archived" size="small" type="info">
            {{ t('lumo.sidebar.archived') }}
          </el-tag>
        </div>
        <div class="conversation-meta">
          <span>{{ props.resolveModelLabel(item.modelCode) }}</span>
          <span>{{ props.resolveProjectLabel(item.projectId) }}</span>
          <span>{{ t('lumo.sidebar.updatedAt', { value: item.updatedAt }) }}</span>
        </div>
      </button>
    </section>
  </aside>
</template>

<style scoped>
.lumo-sidebar {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  border: 1px solid rgba(93, 109, 156, 0.12);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 249, 255, 0.92));
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.sidebar-section {
  padding-top: 14px;
  border-top: 1px solid rgba(102, 117, 168, 0.12);
}

.sidebar-section:first-of-type {
  padding-top: 0;
  border-top: none;
}

.sidebar-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.conversation-list-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sidebar-empty {
  border: 1px dashed rgba(101, 117, 173, 0.28);
  border-radius: 14px;
  padding: 16px;
  color: #6b7591;
  background: rgba(247, 249, 255, 0.82);
}

.conversation-item {
  width: 100%;
  border: 1px solid rgba(86, 102, 153, 0.12);
  border-radius: 14px;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.9);
  text-align: left;
  cursor: pointer;
  transition:
    transform 160ms cubic-bezier(0.2, 0.8, 0.2, 1),
    border-color 160ms ease,
    box-shadow 160ms ease;
}

.conversation-item:hover {
  transform: translateY(-1px);
  border-color: rgba(92, 86, 216, 0.28);
  box-shadow: 0 14px 28px rgba(38, 57, 122, 0.08);
}

.conversation-item.active {
  border-color: rgba(92, 86, 216, 0.42);
  background: linear-gradient(135deg, rgba(235, 236, 255, 0.96), rgba(246, 251, 255, 0.96));
}

.conversation-top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
}

.conversation-title {
  font-weight: 700;
  color: #1d2746;
}

.conversation-meta {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  font-size: 12px;
  color: #667089;
}
</style>
