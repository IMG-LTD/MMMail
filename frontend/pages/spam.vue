<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'

const { t } = useI18n()

useHead(() => ({
  title: t('nav.spam')
}))
</script>

<template>
  <FolderMailbox title-key="nav.spam" folder="spam" show-spam-bulk-actions />
</template>
