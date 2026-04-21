import { ref } from 'vue'

export type AsyncActionPhase = 'idle' | 'loading' | 'success'

export function useAsyncActionState() {
  const phase = ref<AsyncActionPhase>('idle')

  async function run<T>(work: () => Promise<T>) {
    phase.value = 'loading'
    try {
      const result = await work()
      phase.value = 'success'
      return result
    } catch (error) {
      phase.value = 'idle'
      throw error
    }
  }

  function reset() {
    phase.value = 'idle'
  }

  return {
    phase,
    reset,
    run
  }
}
