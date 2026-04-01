<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'

const { t } = useI18n()

useHead(() => ({
  title: t('nav.snoozed')
}))
</script>

<template>
  <FolderMailbox title-key="nav.snoozed" folder="snoozed" />
</template>
