import { computed, ref } from 'vue'

export interface DialogStackEntry {
  id: string
  kind: 'dialog' | 'drawer' | 'sheet'
}

const stack = ref<DialogStackEntry[]>([])

export function useDialogStack() {
  function push(entry: DialogStackEntry) {
    stack.value = [...stack.value, entry]
  }

  function pop() {
    stack.value = stack.value.slice(0, -1)
  }

  return {
    depth: computed(() => stack.value.length),
    top: computed(() => stack.value.at(-1) || null),
    pop,
    push,
    stack: computed(() => stack.value)
  }
}
