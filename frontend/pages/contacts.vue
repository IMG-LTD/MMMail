<script setup lang="ts">
import ContactsWorkspace from '~/components/contacts/ContactsWorkspace.vue'
import { useI18n } from '~/composables/useI18n'

const { t } = useI18n()

useHead(() => ({
  title: t('contacts.pageTitle')
}))
</script>

<template>
  <ContactsWorkspace />
</template>
