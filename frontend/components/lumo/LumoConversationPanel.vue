<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from '~/composables/useI18n'
import LumoKnowledgePanel from '~/components/lumo/LumoKnowledgePanel.vue'
import type { LumoModelCode } from '~/utils/lumo-workspace'
import type {
  LumoConversation,
  LumoMessage,
  LumoProjectKnowledgeParity as LumoProjectKnowledge,
  LumoTranslateLocale
} from '~/types/suite-lumo'

interface CreateKnowledgePayload {
  title: string
  content: string
}

interface Props {
  activeConversation: LumoConversation | null
  activeProjectLabel: string
  messages: LumoMessage[]
  projectKnowledge: LumoProjectKnowledge[]
  selectedKnowledgeIds: string[]
  webSearchEnabled: boolean
  citationsEnabled: boolean
  translateLocale: LumoTranslateLocale
  modelOptions: readonly LumoModelCode[]
  translateLocaleOptions: readonly LumoTranslateLocale[]
  sending: boolean
  updatingModel: boolean
  archiving: boolean
  creatingKnowledge: boolean
  deletingKnowledgeId: string
  resolveModelLabel: (modelCode: LumoModelCode) => string
  resolveTranslateLocaleLabel: (value: LumoTranslateLocale) => string
  updateModel: (modelCode: LumoModelCode) => Promise<void>
  toggleArchive: (archived: boolean) => Promise<void>
  sendMessage: (content: string) => Promise<boolean>
  createKnowledge: (payload: CreateKnowledgePayload) => Promise<boolean>
  deleteKnowledge: (item: LumoProjectKnowledge) => Promise<void>
  changeSelectedKnowledgeIds: (value: string[]) => void
  changeWebSearchEnabled: (value: boolean) => void
  changeCitationsEnabled: (value: boolean) => void
  changeTranslateLocale: (value: LumoTranslateLocale) => void
}

const props = defineProps<Props>()
const { t } = useI18n()
const messageDraft = ref('')

async function onModelChange(value: string | number | boolean): Promise<void> {
  await props.updateModel(String(value) as LumoModelCode)
}

async function onSend(): Promise<void> {
  const success = await props.sendMessage(messageDraft.value)
  if (!success) {
    return
  }
  messageDraft.value = ''
}

function onWebSearchChange(value: string | number | boolean): void {
  props.changeWebSearchEnabled(Boolean(value))
}

function onCitationsChange(value: string | number | boolean): void {
  props.changeCitationsEnabled(Boolean(value))
}

function onTranslateLocaleChange(value: string | number | boolean): void {
  props.changeTranslateLocale(String(value) as LumoTranslateLocale)
}

function isHttpCitation(url: string): boolean {
  return url.startsWith('http://') || url.startsWith('https://')
}
</script>

<template>
  <section class="mm-card conversation-panel">
    <header class="conversation-header">
      <div v-if="props.activeConversation" class="conversation-copy">
        <span class="conversation-eyebrow">
          {{ t('lumo.conversation.project') }} · {{ props.activeProjectLabel }}
        </span>
        <h2 class="conversation-title">{{ props.activeConversation.title }}</h2>
        <div class="conversation-meta-row">
          <el-tag size="small" effect="plain">{{ props.resolveModelLabel(props.activeConversation.modelCode) }}</el-tag>
          <el-tag
            size="small"
            :type="props.activeConversation.archived ? 'info' : 'success'"
          >
            {{ t(props.activeConversation.archived ? 'lumo.conversation.archivedState' : 'lumo.conversation.liveState') }}
          </el-tag>
          <span class="mm-muted">{{ t('lumo.conversation.updatedAt', { value: props.activeConversation.updatedAt }) }}</span>
        </div>
      </div>

      <div v-else class="conversation-copy">
        <span class="conversation-eyebrow">{{ t('lumo.hero.badge') }}</span>
        <h2 class="conversation-title">{{ t('lumo.conversation.emptyTitle') }}</h2>
        <p class="mm-muted">{{ t('lumo.conversation.emptySubtitle') }}</p>
      </div>

      <div v-if="props.activeConversation" class="conversation-actions">
        <el-select
          :model-value="props.activeConversation.modelCode"
          style="width: 180px"
          :disabled="props.updatingModel"
          @change="onModelChange"
        >
          <el-option
            v-for="item in props.modelOptions"
            :key="item"
            :label="props.resolveModelLabel(item)"
            :value="item"
          />
        </el-select>
        <el-button
          :type="props.activeConversation.archived ? 'success' : 'warning'"
          :loading="props.archiving"
          @click="void props.toggleArchive(!props.activeConversation.archived)"
        >
          {{ t(props.activeConversation.archived ? 'lumo.conversation.restore' : 'lumo.conversation.archive') }}
        </el-button>
      </div>
    </header>

    <section class="message-section">
      <div class="message-section-head">
        <h3 class="mm-section-subtitle">{{ t('lumo.conversation.messagesTitle') }}</h3>
      </div>

      <div v-if="props.messages.length === 0" class="message-empty">
        {{ t('lumo.conversation.noMessages') }}
      </div>

      <article
        v-for="item in props.messages"
        :key="item.messageId"
        class="message-item"
        :class="item.role === 'USER' ? 'role-user' : 'role-assistant'"
      >
        <header class="message-item-head">
          <strong>{{ t(`lumo.role.${item.role}`) }}</strong>
          <div class="message-item-meta">
            <span>{{ item.createdAt }}</span>
            <span>{{ t('lumo.message.tokens', { count: item.tokenCount }) }}</span>
          </div>
        </header>
        <p class="message-item-content">{{ item.content }}</p>
        <div v-if="item.role === 'ASSISTANT'" class="message-capability-row">
          <el-tag size="small" effect="plain">
            {{ t(`lumo.capability.mode.${item.capabilityMode}`) }}
          </el-tag>
          <el-tag v-if="item.webSearchEnabled" size="small" type="warning" effect="plain">
            {{ t('lumo.capability.webSearch') }}
          </el-tag>
          <el-tag v-if="item.responseLocale" size="small" type="success" effect="plain">
            {{ t('lumo.translate.output', { locale: props.resolveTranslateLocaleLabel(item.responseLocale as LumoTranslateLocale) }) }}
          </el-tag>
          <el-tag v-if="item.citationsEnabled" size="small" type="info" effect="plain">
            {{ t('lumo.capability.citations') }}
          </el-tag>
        </div>
        <section v-if="item.citations.length > 0" class="citation-panel">
          <div class="citation-title">{{ t('lumo.citations.title') }}</div>
          <article
            v-for="citation in item.citations"
            :key="`${item.messageId}-${citation.url}`"
            class="citation-item"
          >
            <div class="citation-item-head">
              <strong>{{ citation.title }}</strong>
              <el-tag size="small" effect="plain">
                {{ t(`lumo.citations.source.${citation.sourceType}`) }}
              </el-tag>
            </div>
            <p class="citation-note">{{ citation.note }}</p>
            <el-link
              v-if="isHttpCitation(citation.url)"
              :href="citation.url"
              target="_blank"
              type="primary"
            >
              {{ citation.url }}
            </el-link>
            <span v-else class="citation-inline">{{ citation.url }}</span>
          </article>
        </section>
      </article>
    </section>

    <LumoKnowledgePanel
      :active-project-id="props.activeConversation?.projectId || ''"
      :project-knowledge="props.projectKnowledge"
      :selected-knowledge-ids="props.selectedKnowledgeIds"
      :creating-knowledge="props.creatingKnowledge"
      :deleting-knowledge-id="props.deletingKnowledgeId"
      :create-knowledge="props.createKnowledge"
      :delete-knowledge="props.deleteKnowledge"
      @update-selected-knowledge-ids="props.changeSelectedKnowledgeIds"
    />

    <section class="composer-card">
      <div class="composer-capabilities">
        <div class="composer-capability">
          <span class="composer-capability-label">{{ t('lumo.capability.webSearch') }}</span>
          <el-switch :model-value="props.webSearchEnabled" @change="onWebSearchChange" />
        </div>
        <div class="composer-capability">
          <span class="composer-capability-label">{{ t('lumo.capability.citations') }}</span>
          <el-switch :model-value="props.citationsEnabled" @change="onCitationsChange" />
        </div>
        <div class="composer-capability locale-picker">
          <span class="composer-capability-label">{{ t('lumo.translate.title') }}</span>
          <el-select
            :model-value="props.translateLocale"
            style="width: 180px"
            @change="onTranslateLocaleChange"
          >
            <el-option
              v-for="item in props.translateLocaleOptions"
              :key="item"
              :label="props.resolveTranslateLocaleLabel(item)"
              :value="item"
            />
          </el-select>
        </div>
      </div>
      <el-input
        v-model="messageDraft"
        type="textarea"
        :rows="4"
        maxlength="4000"
        show-word-limit
        :disabled="!props.activeConversation || props.activeConversation.archived"
        :placeholder="t('lumo.composer.placeholder')"
      />
      <div class="composer-actions">
        <el-button
          type="primary"
          :disabled="!props.activeConversation || props.activeConversation.archived"
          :loading="props.sending"
          @click="onSend"
        >
          {{ t('lumo.composer.send') }}
        </el-button>
      </div>
    </section>
  </section>
</template>

<style scoped>
.conversation-panel {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  border: 1px solid rgba(90, 106, 161, 0.12);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 255, 0.94));
}

.conversation-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.conversation-copy {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.conversation-eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #5a54d7;
}

.conversation-title {
  margin: 0;
  font-size: 28px;
  line-height: 1.12;
  color: #192241;
}

.conversation-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 10px;
  align-items: center;
}

.conversation-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.message-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.message-section-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.message-empty {
  border: 1px dashed rgba(95, 111, 164, 0.24);
  border-radius: 14px;
  padding: 18px;
  color: #67728f;
  background: rgba(248, 250, 255, 0.68);
}

.message-item {
  border-radius: 16px;
  padding: 14px 16px;
  border: 1px solid rgba(93, 107, 160, 0.12);
  box-shadow: 0 12px 24px rgba(37, 51, 98, 0.05);
}

.role-user {
  background: linear-gradient(135deg, rgba(232, 241, 255, 0.95), rgba(246, 249, 255, 0.96));
}

.role-assistant {
  background: linear-gradient(135deg, rgba(247, 243, 255, 0.96), rgba(251, 250, 255, 0.98));
}

.message-item-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 10px;
}

.message-item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  font-size: 12px;
  color: #66718b;
}

.message-item-content {
  margin: 0;
  color: #24304f;
  line-height: 1.68;
  white-space: pre-wrap;
}

.message-capability-row {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.citation-panel {
  margin-top: 14px;
  display: grid;
  gap: 10px;
}

.citation-title {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #69739a;
}

.citation-item {
  border-radius: 14px;
  padding: 12px;
  border: 1px solid rgba(96, 112, 167, 0.16);
  background: rgba(255, 255, 255, 0.82);
}

.citation-item-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
}

.citation-note {
  margin: 8px 0;
  color: #4f5b79;
  line-height: 1.55;
}

.citation-inline {
  font-size: 12px;
  color: #6b7694;
}

.composer-card {
  border-radius: 18px;
  padding: 16px;
  background: linear-gradient(135deg, rgba(243, 245, 255, 0.96), rgba(255, 255, 255, 0.98));
  border: 1px solid rgba(98, 112, 164, 0.12);
}

.composer-capabilities {
  margin-bottom: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.composer-capability {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(100, 114, 166, 0.12);
}

.composer-capability-label {
  font-size: 13px;
  color: #44506d;
}

.locale-picker {
  margin-left: auto;
}

.composer-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 920px) {
  .conversation-header {
    flex-direction: column;
  }

  .conversation-actions {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
  }

  .locale-picker {
    margin-left: 0;
  }
}
</style>
