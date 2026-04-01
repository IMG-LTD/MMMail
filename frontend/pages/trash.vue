<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'

const { t } = useI18n()

useHead(() => ({
  title: t('nav.trash')
}))
</script>

<template>
  <FolderMailbox title-key="nav.trash" folder="trash" :show-trash-bulk-actions="true" />
</template>
