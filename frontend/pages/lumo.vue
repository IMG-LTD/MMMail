<script setup lang="ts">
import LumoConversationPanel from '~/components/lumo/LumoConversationPanel.vue'
import LumoSidebarPanel from '~/components/lumo/LumoSidebarPanel.vue'
import LumoWorkspaceHero from '~/components/lumo/LumoWorkspaceHero.vue'
import { useI18n } from '~/composables/useI18n'
import { useLumoWorkspace } from '~/composables/useLumoWorkspace'

definePageMeta({
  layout: 'default'
})

const { t } = useI18n()
const workspace = useLumoWorkspace()

useHead(() => ({
  title: t('page.lumo.title'),
  meta: [
    {
      name: 'description',
      content: t('lumo.hero.subtitle')
    }
  ]
}))
</script>

<template>
  <div class="mm-page" v-loading="workspace.loading.value">
    <section class="lumo-page-shell">
      <LumoWorkspaceHero
        :include-archived="workspace.includeArchived.value"
        :summary="workspace.summary.value"
        @refresh="workspace.loadData"
        @update-include-archived="workspace.onToggleIncludeArchived"
      />

      <section class="lumo-grid">
        <LumoSidebarPanel
          :projects="workspace.projects.value"
          :conversations="workspace.conversations.value"
          :selected-project-id="workspace.selectedProjectId.value"
          :active-conversation-id="workspace.activeConversationId.value"
          :creating-project="workspace.creatingProject.value"
          :creating-conversation="workspace.creatingConversation.value"
          :model-options="workspace.modelOptions"
          :resolve-project-label="workspace.resolveProjectLabel"
          :resolve-model-label="workspace.resolveModelLabel"
          :create-project="workspace.onCreateProject"
          :create-conversation="workspace.onCreateConversation"
          :change-project-filter="workspace.onChangeProjectFilter"
          :select-conversation="workspace.onSelectConversation"
        />

        <LumoConversationPanel
          :active-conversation="workspace.activeConversation.value"
          :active-project-label="workspace.activeProjectLabel.value"
          :messages="workspace.messages.value"
          :project-knowledge="workspace.projectKnowledge.value"
          :selected-knowledge-ids="workspace.selectedKnowledgeIds.value"
          :web-search-enabled="workspace.webSearchEnabled.value"
          :citations-enabled="workspace.citationsEnabled.value"
          :translate-locale="workspace.translateLocale.value"
          :model-options="workspace.modelOptions"
          :translate-locale-options="workspace.translateLocaleOptions"
          :sending="workspace.sending.value"
          :updating-model="workspace.updatingModel.value"
          :archiving="workspace.archiving.value"
          :creating-knowledge="workspace.creatingKnowledge.value"
          :deleting-knowledge-id="workspace.deletingKnowledgeId.value"
          :resolve-model-label="workspace.resolveModelLabel"
          :resolve-translate-locale-label="workspace.resolveTranslateLocaleLabel"
          :update-model="workspace.onUpdateModel"
          :toggle-archive="workspace.onToggleArchive"
          :send-message="workspace.onSendMessage"
          :create-knowledge="workspace.onCreateKnowledge"
          :delete-knowledge="workspace.onDeleteKnowledge"
          :change-selected-knowledge-ids="workspace.setSelectedKnowledgeIds"
          :change-web-search-enabled="workspace.setWebSearchEnabled"
          :change-citations-enabled="workspace.setCitationsEnabled"
          :change-translate-locale="workspace.setTranslateLocale"
        />
      </section>
    </section>
  </div>
</template>

<style scoped>
.lumo-page-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.lumo-grid {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 16px;
}

@media (max-width: 1120px) {
  .lumo-grid {
    grid-template-columns: 1fr;
  }
}
</style>
