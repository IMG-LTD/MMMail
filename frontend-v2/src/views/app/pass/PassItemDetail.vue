<script setup lang="ts">
import type { PassDetailCard, PassSurfaceEntry } from './pass-types'

defineProps<{
  primaryCard: PassDetailCard
  revealed: boolean
  secondaryCard: PassDetailCard
  selectedEntry: PassSurfaceEntry | null
}>()

defineEmits<{
  confirmRevoke: []
  confirmRotate: []
  openShare: []
  toggleSecret: []
}>()
</script>

<template>
  <section class="pass-item-detail">
    <article class="pass-item-detail__card">
      <span class="section-label">{{ primaryCard.label }}</span>
      <strong>{{ primaryCard.title }}</strong>
      <p>{{ primaryCard.copy }}</p>
      <dl v-if="primaryCard.facts.length">
        <div v-for="fact in primaryCard.facts" :key="fact.label">
          <dt>{{ fact.label }}</dt>
          <dd>{{ fact.value }}</dd>
        </div>
      </dl>
    </article>
    <article class="pass-item-detail__card">
      <span class="section-label">{{ secondaryCard.label }}</span>
      <strong>{{ secondaryCard.title }}</strong>
      <p>{{ secondaryCard.copy }}</p>
      <dl v-if="secondaryCard.facts.length">
        <div v-for="fact in secondaryCard.facts" :key="fact.label">
          <dt>{{ fact.label }}</dt>
          <dd>{{ fact.value }}</dd>
        </div>
      </dl>
    </article>
    <div class="pass-item-detail__actions">
      <button class="pass-secret-reveal" type="button" @click="$emit('toggleSecret')">
        {{ revealed ? 'Hide secret' : 'Show secret' }}
      </button>
      <button class="pass-secure-link-trigger" type="button" @click="$emit('openShare')">Share link</button>
      <button class="pass-rotate-trigger" type="button" @click="$emit('confirmRotate')">Rotate</button>
      <button class="pass-revoke-trigger" type="button" @click="$emit('confirmRevoke')">Revoke</button>
    </div>
    <p class="pass-item-detail__secret">{{ revealed ? 'sk_live_masked_for_local_ui' : '••••••••••••••••' }}</p>
  </section>
</template>
