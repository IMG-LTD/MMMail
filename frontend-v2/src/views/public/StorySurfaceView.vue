<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { lt, useLocaleText } from '@/locales'
import { failureModes, findSurface, onboardingStories } from '@/shared/content/route-surfaces'

type StoryGroup = 'onboarding' | 'failure'

const route = useRoute()
const { tr } = useLocaleText()

const storyGroup = computed<StoryGroup>(() => {
  return route.meta.storyGroup === 'failure' ? 'failure' : 'onboarding'
})
const storyKey = computed(() => String(route.params.storyKey ?? route.meta.storyKey ?? 'invitation-landing'))

const story = computed(() => {
  if (storyGroup.value === 'failure') {
    return findSurface(failureModes, storyKey.value, 'f01')
  }

  return findSurface(onboardingStories, storyKey.value, 'invitation-landing')
})

const storyCards = computed(() => {
  return storyGroup.value === 'failure' ? failureModes : onboardingStories
})
</script>

<template>
  <section class="story-surface">
    <article class="story-surface__hero surface-card">
      <span class="section-label">{{ tr(story.eyebrow) }}</span>
      <h1>{{ tr(story.title) }}</h1>
      <p class="page-subtitle">{{ tr(story.description) }}</p>
      <div class="story-surface__actions">
        <button v-for="(action, index) in story.actions" :key="`${story.key}-action-${index}`" type="button">{{ tr(action) }}</button>
      </div>
    </article>

    <div class="story-surface__layout">
      <article class="story-surface__body surface-card">
        <p v-for="(paragraph, index) in story.body" :key="`${story.key}-body-${index}`">{{ tr(paragraph) }}</p>
      </article>
      <aside class="story-surface__index surface-card">
        <span class="section-label">
          {{ storyGroup === 'failure' ? tr(lt('失败清单', '失敗清單', 'Failure board')) : tr(lt('引导包', '引導包', 'Onboarding pack')) }}
        </span>
        <button
          v-for="item in storyCards"
          :key="item.key"
          type="button"
          :class="{ 'story-surface__index--active': item.key === story.key }"
        >
          {{ tr(item.title) }}
        </button>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.story-surface {
  display: grid;
  gap: 16px;
  min-height: 100vh;
  padding: 24px;
  background: linear-gradient(180deg, #f6f7f9 0%, #ffffff 100%);
}

.story-surface__hero,
.story-surface__body,
.story-surface__index {
  display: grid;
  gap: 12px;
  padding: 20px;
}

.story-surface__hero h1 {
  margin: 0;
  font-size: clamp(36px, 4vw, 54px);
  line-height: 0.96;
  letter-spacing: -0.05em;
}

.story-surface__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.story-surface__actions button,
.story-surface__index button {
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid var(--mm-border);
  border-radius: 12px;
  background: var(--mm-card);
}

.story-surface__layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 16px;
}

.story-surface__body p {
  margin: 0;
  color: var(--mm-text-secondary);
  line-height: 1.7;
}

.story-surface__index--active {
  border-color: var(--mm-accent-border) !important;
  background: var(--mm-accent-soft) !important;
  color: var(--mm-primary);
}

@media (max-width: 920px) {
  .story-surface {
    padding: 16px;
  }

  .story-surface__layout {
    grid-template-columns: 1fr;
  }
}
</style>
