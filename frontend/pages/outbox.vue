<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'

const { t } = useI18n()

useHead(() => ({
  title: t('nav.outbox')
}))
</script>

<template>
  <FolderMailbox title-key="nav.outbox" folder="outbox" />
</template>
