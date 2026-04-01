<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'

const { t } = useI18n()

useHead(() => ({
  title: t('nav.archive')
}))
</script>

<template>
  <FolderMailbox title-key="nav.archive" folder="archive" />
</template>
