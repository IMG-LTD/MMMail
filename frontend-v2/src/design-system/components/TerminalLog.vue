<script setup lang="ts">
import { computed, watch } from 'vue'
import { useSrLive } from '@/shared/composables/useSrLive'

type TerminalLogLevel = 'info' | 'success' | 'warning' | 'error'
type TerminalLogStream = 'stdout' | 'stderr' | 'system'

export interface TerminalLogLine {
  commandId?: string
  id: string
  level: TerminalLogLevel
  stream: TerminalLogStream
  text: string
  timestamp: string
}

const props = withDefaults(
  defineProps<{
    autoFollow?: boolean
    copyable?: boolean
    filter?: string
    lines: readonly TerminalLogLine[]
    running?: boolean
  }>(),
  {
    autoFollow: true,
    copyable: true,
    filter: '',
    running: false
  }
)

const emit = defineEmits<{
  clear: []
  copy: [text: string]
  pauseFollow: []
  resumeFollow: []
  'update:filter': [filter: string]
}>()

const srLive = useSrLive()

const visibleLines = computed(() => {
  const filter = props.filter.trim().toLowerCase()
  return filter ? props.lines.filter(line => line.text.toLowerCase().includes(filter)) : props.lines
})

function announce(message: string, urgent = false) {
  if (urgent) {
    srLive.assertive(message)
    return
  }
  srLive.polite(message)
}

function copyLog() {
  const text = visibleLines.value.map(line => line.text).join('\n')
  emit('copy', text)
  if (typeof navigator !== 'undefined' && navigator.clipboard) {
    void navigator.clipboard.writeText(text)
  }
}

function toggleFollow() {
  if (props.autoFollow) {
    emit('pauseFollow')
    return
  }
  emit('resumeFollow')
}

watch(
  () => props.lines.at(-1)?.id,
  () => {
    const line = props.lines.at(-1)
    if (!line) {
      return
    }
    if (line.level === 'error') {
      announce(`Command failed: ${line.text}`, true)
      return
    }
    if (line.level === 'success') {
      announce(`Command completed: ${line.text}`)
    }
  }
)

watch(
  () => props.running,
  value => {
    announce(value ? 'Command running' : 'Command stopped')
  }
)
</script>

<template>
  <section class="terminal-log" :class="{ 'terminal-log--running': running, 'terminal-log--follow': autoFollow }" aria-label="Terminal log">
    <header class="terminal-log__toolbar">
      <input :value="filter" placeholder="Filter logs" type="search" @input="emit('update:filter', ($event.target as HTMLInputElement).value)" />
      <button v-if="copyable" type="button" @click="copyLog">Copy</button>
      <button type="button" @click="toggleFollow">
        {{ autoFollow ? 'Pause follow' : 'Resume follow' }}
      </button>
      <button type="button" @click="emit('clear')">Clear</button>
    </header>
    <ol class="terminal-log__lines" role="log" aria-live="polite">
      <li
        v-for="line in visibleLines"
        :key="line.id"
        class="terminal-log__line"
        :class="[`terminal-log__line--${line.level}`, `terminal-log__line--${line.stream}`]"
      >
        <time>{{ line.timestamp }}</time>
        <span>{{ line.stream }}</span>
        <code>{{ line.text }}</code>
      </li>
    </ol>
  </section>
</template>

<style scoped>
.terminal-log {
  overflow: hidden;
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius-md);
  background: #111827;
  color: #d1d5db;
}

.terminal-log__toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  background: #0b1120;
}

.terminal-log__toolbar input {
  flex: 1 1 180px;
  min-height: 34px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: var(--mm-radius-sm);
  padding: 0 10px;
  background: #111827;
  color: white;
}

.terminal-log__toolbar button {
  min-height: 34px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: var(--mm-radius-sm);
  background: #1f2937;
  color: white;
  font-size: 12px;
  font-weight: 800;
}

.terminal-log__lines {
  max-height: 320px;
  margin: 0;
  overflow: auto;
  padding: 10px;
  list-style: none;
}

.terminal-log__line {
  display: grid;
  grid-template-columns: 88px 64px 1fr;
  gap: 10px;
  min-height: 26px;
  align-items: start;
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", monospace;
  font-size: 12px;
}

.terminal-log__line time,
.terminal-log__line span {
  color: #9ca3af;
}

.terminal-log__line code {
  white-space: pre-wrap;
}

.terminal-log__line--success code {
  color: #86efac;
}

.terminal-log__line--warning code {
  color: #fde68a;
}

.terminal-log__line--error code {
  color: #fecaca;
}
</style>
