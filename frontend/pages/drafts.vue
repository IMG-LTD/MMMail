<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'

const { t } = useI18n()

useHead(() => ({
  title: t('nav.drafts')
}))
</script>

<template>
  <FolderMailbox title-key="nav.drafts" folder="drafts" />
</template>
