<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import Modal from '@/design-system/components/Modal.vue'
import { lt, useLocaleText } from '@/locales'
import { useShellStore } from '@/store/modules/shell'

const router = useRouter()
const shellStore = useShellStore()
const { tr } = useLocaleText()

const quickCreateActions = computed(() => [
  { label: tr(lt('写邮件', '寫郵件', 'Compose mail')), path: '/mail/compose', tone: 'mail' },
  { label: tr(lt('新建日程', '新增日程', 'New event')), path: '/calendar', tone: 'calendar' },
  { label: tr(lt('上传文件', '上傳檔案', 'Upload file')), path: '/drive', tone: 'drive' },
  { label: tr(lt('新建文档', '新增文件', 'New document')), path: '/docs', tone: 'docs' },
  { label: tr(lt('新建表格', '新增試算表', 'New sheet')), path: '/sheets', tone: 'sheets' },
  { label: tr(lt('运行命令', '執行命令', 'Run command')), path: '/command-center', tone: 'command' }
])

function updateVisibility(value: boolean) {
  if (!value) {
    shellStore.closeQuickCreate()
  }
}

function runAction(path: string) {
  void router.push(path)
  shellStore.closeQuickCreate()
}
</script>

<template>
  <Modal
    :show="shellStore.quickCreateOpen"
    :title="tr(lt('快速创建', '快速建立', 'Quick create'))"
    :description="tr(lt('打开现有模块入口，不伪造创建成功状态。', '開啟現有模組入口，不偽造建立成功狀態。', 'Open route-backed creation surfaces without faking success.'))"
    size="sm"
    @close="shellStore.closeQuickCreate()"
    @update:show="updateVisibility"
  >
    <div class="quick-create-modal">
      <button
        v-for="action in quickCreateActions"
        :key="action.path"
        class="quick-create-modal__action"
        :class="`quick-create-modal__action--${action.tone}`"
        type="button"
        @click="runAction(action.path)"
      >
        {{ action.label }}
      </button>
    </div>
  </Modal>
</template>

<style scoped>
.quick-create-modal {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.quick-create-modal__action {
  min-height: 48px;
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius-sm);
  background: var(--mm-surface-soft);
  color: var(--mm-text-primary);
  font-weight: 800;
}

.quick-create-modal__action--mail {
  color: var(--mm-product-mail);
}

.quick-create-modal__action--calendar {
  color: var(--mm-product-calendar);
}

.quick-create-modal__action--drive {
  color: var(--mm-product-drive);
}

.quick-create-modal__action--docs {
  color: var(--mm-product-docs);
}

.quick-create-modal__action--sheets {
  color: var(--mm-product-sheets);
}

.quick-create-modal__action--command {
  color: var(--mm-product-command);
}

@media (max-width: 520px) {
  .quick-create-modal {
    grid-template-columns: 1fr;
  }
}
</style>
