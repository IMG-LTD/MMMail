<script setup lang="ts">
import type { PassSurfaceEntry } from './pass-types'

defineProps<{
  emptyCopy: string
  entries: PassSurfaceEntry[]
  selectedKey: string
  statusCopy: string
  title: string
}>()

defineEmits<{
  select: [entryKey: string]
}>()
</script>

<template>
  <section class="pass-item-list">
    <header>
      <span class="section-label">Vault section</span>
      <h1>{{ title }}</h1>
      <p>{{ statusCopy }}</p>
    </header>
    <button
      v-for="entry in entries"
      :key="entry.key"
      class="pass-item-list__row"
      :class="{ 'pass-item-list__row--active': entry.key === selectedKey }"
      type="button"
      @click="$emit('select', entry.key)"
    >
      <span class="pass-item-list__avatar">{{ entry.avatar }}</span>
      <span>
        <strong>{{ entry.title }}</strong>
        <small>{{ entry.subtitle }} · {{ entry.meta }}</small>
      </span>
      <em>{{ entry.badge }}</em>
    </button>
    <article v-if="!entries.length" class="pass-item-list__empty">
      <strong>No data</strong>
      <p>{{ emptyCopy }}</p>
    </article>
  </section>
</template>
