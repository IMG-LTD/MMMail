import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from '~/composables/useI18n'
import { useSuiteApi } from '~/composables/useSuiteApi'
import { useOrgAccessStore } from '~/stores/org-access'
import type { SuiteCollaborationCenter } from '~/types/api'
import {
  filterMainlineCollaborationItems,
  type MainlineCollaborationEvent
} from '~/utils/collaboration'
import { filterSuiteCollaborationCenterByAccess } from '~/utils/org-product-surface-filter'

function resolveErrorMessage(error: unknown, fallbackMessage: string): string {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message
  }
  return fallbackMessage
}

function normalizeCollaborationItems(
  center: SuiteCollaborationCenter | null
): MainlineCollaborationEvent[] {
  if (!center) {
    return []
  }
  return filterMainlineCollaborationItems(center.items)
}

export function useSuiteOverviewWorkspace() {
  const { t } = useI18n()
  const { getCollaborationCenter } = useSuiteApi()
  const orgAccessStore = useOrgAccessStore()

  const loading = ref(false)
  const collaborationCenter = ref<SuiteCollaborationCenter | null>(null)

  const collaborationItems = computed(() => normalizeCollaborationItems(
    filterSuiteCollaborationCenterByAccess(collaborationCenter.value, orgAccessStore.isProductEnabled)
  ))

  async function loadCollaborationCenter(): Promise<void> {
    loading.value = true
    try {
      collaborationCenter.value = await getCollaborationCenter(24)
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, t('suite.sectionOverview.handoff.loadFailed')))
    } finally {
      loading.value = false
    }
  }

  onMounted(() => {
    void loadCollaborationCenter()
  })

  watch(
    () => orgAccessStore.activeOrgId,
    (nextOrgId, previousOrgId) => {
      if (nextOrgId !== previousOrgId) {
        void loadCollaborationCenter()
      }
    }
  )

  return {
    loading,
    collaborationItems,
    loadCollaborationCenter
  }
}
