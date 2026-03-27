<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'

interface Props {
  disabled: boolean
  loading: boolean
  audioEnabled: boolean
  videoEnabled: boolean
  screenSharing: boolean
}

defineProps<Props>()
const emit = defineEmits<{
  toggleAudio: []
  toggleVideo: []
  toggleScreen: []
}>()

const { t } = useI18n()
</script>

<template>
  <section class="mm-card meet-media-card">
    <h2 class="mm-section-title">{{ t('meet.workspace.media.title') }}</h2>
    <div class="meet-media-actions">
      <el-button
        :disabled="disabled"
        :loading="loading"
        :type="audioEnabled ? 'success' : 'info'"
        @click="emit('toggleAudio')"
      >
        {{ t(`meet.workspace.media.audio.${audioEnabled}`) }}
      </el-button>
      <el-button
        :disabled="disabled"
        :loading="loading"
        :type="videoEnabled ? 'success' : 'info'"
        @click="emit('toggleVideo')"
      >
        {{ t(`meet.workspace.media.video.${videoEnabled}`) }}
      </el-button>
      <el-button
        :disabled="disabled"
        :loading="loading"
        :type="screenSharing ? 'warning' : 'info'"
        @click="emit('toggleScreen')"
      >
        {{ t(`meet.workspace.media.screen.${screenSharing}`) }}
      </el-button>
    </div>
  </section>
</template>

<style scoped>
.meet-media-card {
  padding: 16px;
}

.meet-media-actions {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
