<script setup lang="ts">
import { computed } from 'vue'
import HostedBadge from './HostedBadge.vue'
import PremiumBadge from './PremiumBadge.vue'
import StatusBadge from './StatusBadge.vue'
import { useShellStore } from '@/store/modules/shell'

type CommandRestriction = 'premium' | 'hosted' | 'permission' | 'preview'

export interface CommandPaletteItem {
  description?: string
  id: string
  label: string
  path?: string
  restriction?: CommandRestriction
  shortcut?: string
}

export interface CommandPaletteGroup {
  id: string
  items: readonly CommandPaletteItem[]
  label: string
}

const props = withDefaults(
  defineProps<{
    groups: readonly CommandPaletteGroup[]
    placeholder?: string
    query: string
    recentItems?: readonly CommandPaletteItem[]
    show: boolean
  }>(),
  {
    placeholder: 'Search commands',
    recentItems: () => []
  }
)

const emit = defineEmits<{
  close: []
  execute: [item: CommandPaletteItem]
  search: [query: string]
  select: [item: CommandPaletteItem]
  'update:query': [query: string]
  'update:show': [value: boolean]
}>()

const shellStore = useShellStore()

const visibleGroups = computed(() => {
  const query = props.query.trim().toLowerCase()
  if (!query) {
    return props.groups
  }
  return props.groups
    .map(group => ({ ...group, items: group.items.filter(item => item.label.toLowerCase().includes(query)) }))
    .filter(group => group.items.length > 0)
})

function updateQuery(value: string) {
  emit('update:query', value)
  emit('search', value)
}

function closePalette() {
  shellStore.closeCommandPalette()
  emit('update:show', false)
  emit('close')
}

function selectItem(item: CommandPaletteItem) {
  emit('select', item)
  emit('execute', item)
}
</script>

<template>
  <div v-if="show" class="command-palette" role="dialog" aria-modal="true" aria-label="Command palette">
    <div class="command-palette__panel">
      <label class="command-palette__search" role="combobox" aria-expanded="true" aria-controls="command-palette-results">
        <span class="sr-only">{{ placeholder }}</span>
        <input :placeholder="placeholder" :value="query" type="search" @input="updateQuery(($event.target as HTMLInputElement).value)" />
      </label>
      <div id="command-palette-results" class="command-palette__results" role="listbox">
        <section v-if="recentItems.length && !query" class="command-palette__group">
          <h3>Recent</h3>
          <button v-for="item in recentItems" :key="item.id" role="option" type="button" @click="selectItem(item)">
            <span>{{ item.label }}</span>
            <small v-if="item.shortcut">{{ item.shortcut }}</small>
          </button>
        </section>
        <section v-for="group in visibleGroups" :key="group.id" class="command-palette__group">
          <h3>{{ group.label }}</h3>
          <button v-for="item in group.items" :key="item.id" role="option" type="button" @click="selectItem(item)">
            <span>
              {{ item.label }}
              <em v-if="item.description">{{ item.description }}</em>
            </span>
            <PremiumBadge v-if="item.restriction === 'premium'" compact />
            <HostedBadge v-else-if="item.restriction === 'hosted'" compact />
            <StatusBadge v-else-if="item.restriction === 'permission'" compact label="Permission" tone="warning" />
            <StatusBadge v-else-if="item.restriction === 'preview'" compact label="Preview" tone="preview" />
            <small v-if="item.shortcut">{{ item.shortcut }}</small>
          </button>
        </section>
      </div>
      <footer class="command-palette__footer">
        <button type="button" @click="closePalette">Close</button>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.command-palette {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: grid;
  place-items: start center;
  padding: 12vh 16px 16px;
  background: var(--mm-overlay);
}

.command-palette__panel {
  width: min(720px, 100%);
  overflow: hidden;
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius-lg);
  background: var(--mm-surface);
  box-shadow: var(--mm-shadow-lg);
}

.command-palette__search {
  display: block;
  padding: 14px;
  border-bottom: 1px solid var(--mm-border);
}

.command-palette__search input {
  width: 100%;
  min-height: 42px;
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius-sm);
  padding: 0 12px;
  background: var(--mm-surface-soft);
}

.command-palette__results {
  max-height: 420px;
  overflow: auto;
}

.command-palette__group {
  display: grid;
  gap: 4px;
  padding: 12px 14px;
}

.command-palette__group h3 {
  margin: 0 0 4px;
  color: var(--mm-text-secondary);
  font-size: 11px;
  text-transform: uppercase;
}

.command-palette__group button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 44px;
  border: 0;
  border-radius: var(--mm-radius-sm);
  background: transparent;
  color: var(--mm-text-primary);
  text-align: left;
}

.command-palette__group button:hover {
  background: var(--mm-surface-soft);
}

.command-palette__group em,
.command-palette__group small {
  display: block;
  color: var(--mm-text-secondary);
  font-size: 12px;
  font-style: normal;
}

.command-palette__footer {
  display: flex;
  justify-content: flex-end;
  padding: 12px 14px;
  border-top: 1px solid var(--mm-border);
}

.command-palette__footer button {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius-sm);
  background: var(--mm-surface);
}
</style>
