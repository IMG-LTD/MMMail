<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'

defineProps<{
  testId: string
  title: string
  message: string
}>()

const emit = defineEmits<{
  retry: []
  dismiss: []
}>()

const { t } = useI18n()
</script>

<template>
  <el-alert :title="title" type="error" :closable="false" show-icon>
    <div class="drive-error-banner" :data-testid="testId">
      <p class="drive-error-banner__message">{{ message }}</p>
      <div class="drive-error-banner__actions">
        <el-button size="small" type="danger" plain :data-testid="`${testId}-retry`" @click="emit('retry')">
          {{ t('common.actions.retry') }}
        </el-button>
        <el-button size="small" :data-testid="`${testId}-dismiss`" @click="emit('dismiss')">
          {{ t('common.actions.dismiss') }}
        </el-button>
      </div>
    </div>
  </el-alert>
</template>

<style scoped>
.drive-error-banner {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.drive-error-banner__message {
  margin: 0;
  color: var(--mm-text-primary);
}

.drive-error-banner__actions {
  display: inline-flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
