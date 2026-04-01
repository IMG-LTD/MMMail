<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'

const { t } = useI18n()

useHead(() => ({
  title: t('nav.sent')
}))
</script>

<template>
  <FolderMailbox title-key="nav.sent" folder="sent" />
</template>
