import {
  COMMUNITY_V1_MAINLINE_JOURNEY_STAGES,
  type CommunityMainlineJourneyStageDefinition,
  type CommunityMainlineProductCode
} from '~/constants/module-maturity'
import {
  isMainlineCollaborationProductCode,
  type MainlineCollaborationEvent
} from '~/utils/collaboration'

export type MainlineHandoffStageStatus = 'DONE' | 'ACTIVE' | 'NEXT' | 'PENDING'

export interface MainlineHandoffStageState extends CommunityMainlineJourneyStageDefinition {
  status: MainlineHandoffStageStatus
  evidence: MainlineCollaborationEvent | null
}

export interface MainlineHandoffRun {
  started: boolean
  completed: boolean
  currentStage: CommunityMainlineProductCode | null
  nextStage: CommunityMainlineProductCode | null
  anchorEvent: MainlineCollaborationEvent | null
  latestEvent: MainlineCollaborationEvent | null
  completedCount: number
  stages: ReadonlyArray<MainlineHandoffStageState>
  recentItems: ReadonlyArray<MainlineCollaborationEvent>
}

const MAX_RECENT_ITEMS = 4
const NEXT_STAGE_OFFSET = 1

function toEventTimeValue(event: Pick<MainlineCollaborationEvent, 'createdAt' | 'eventId'>): number {
  const parsed = Date.parse(event.createdAt)
  if (Number.isNaN(parsed)) {
    return event.eventId
  }
  return parsed
}

function compareByNewest(left: MainlineCollaborationEvent, right: MainlineCollaborationEvent): number {
  const timeDifference = toEventTimeValue(right) - toEventTimeValue(left)
  if (timeDifference !== 0) {
    return timeDifference
  }
  return right.eventId - left.eventId
}

function sortByNewest(items: readonly MainlineCollaborationEvent[]): MainlineCollaborationEvent[] {
  return [...items].sort(compareByNewest)
}

function resolveVisibleStages(
  visibleProductCodes?: readonly string[]
): CommunityMainlineJourneyStageDefinition[] {
  if (!visibleProductCodes || visibleProductCodes.length === 0) {
    return [...COMMUNITY_V1_MAINLINE_JOURNEY_STAGES]
  }
  const visibleCodes = new Set(visibleProductCodes.filter(isMainlineCollaborationProductCode))
  return COMMUNITY_V1_MAINLINE_JOURNEY_STAGES.filter(stage => visibleCodes.has(stage.productCode))
}

function filterVisibleMainlineEvents(
  items: readonly MainlineCollaborationEvent[],
  visibleStages: readonly CommunityMainlineJourneyStageDefinition[]
): MainlineCollaborationEvent[] {
  const visibleCodes = new Set(visibleStages.map(stage => stage.productCode))
  return sortByNewest(items.filter(item => visibleCodes.has(item.productCode)))
}

function findAnchorEvent(
  items: readonly MainlineCollaborationEvent[],
  visibleStages: readonly CommunityMainlineJourneyStageDefinition[]
): MainlineCollaborationEvent | null {
  const startProductCode = visibleStages[0]?.productCode
  if (!startProductCode) {
    return null
  }
  return items.find(item => item.productCode === startProductCode) ?? null
}

function filterCurrentRunItems(
  items: readonly MainlineCollaborationEvent[],
  anchorEvent: MainlineCollaborationEvent | null
): MainlineCollaborationEvent[] {
  if (!anchorEvent) {
    return []
  }
  const anchorTime = toEventTimeValue(anchorEvent)
  return items.filter(item => toEventTimeValue(item) >= anchorTime)
}

function buildEvidenceMap(
  items: readonly MainlineCollaborationEvent[]
): Partial<Record<CommunityMainlineProductCode, MainlineCollaborationEvent>> {
  const evidenceMap: Partial<Record<CommunityMainlineProductCode, MainlineCollaborationEvent>> = {}
  for (const item of items) {
    if (!evidenceMap[item.productCode]) {
      evidenceMap[item.productCode] = item
    }
  }
  return evidenceMap
}

function resolveCurrentStageIndex(
  stages: readonly CommunityMainlineJourneyStageDefinition[],
  evidenceMap: Partial<Record<CommunityMainlineProductCode, MainlineCollaborationEvent>>
): number {
  return stages.reduce((highestIndex, stage, index) => {
    if (evidenceMap[stage.productCode]) {
      return index
    }
    return highestIndex
  }, -1)
}

function resolveStageStatus(index: number, currentStageIndex: number): MainlineHandoffStageStatus {
  if (currentStageIndex < 0) {
    return index === 0 ? 'NEXT' : 'PENDING'
  }
  if (index < currentStageIndex) {
    return 'DONE'
  }
  if (index === currentStageIndex) {
    return 'ACTIVE'
  }
  if (index === currentStageIndex + NEXT_STAGE_OFFSET) {
    return 'NEXT'
  }
  return 'PENDING'
}

function resolveNextStage(
  currentStageIndex: number,
  stages: readonly CommunityMainlineJourneyStageDefinition[]
): CommunityMainlineProductCode | null {
  if (stages.length === 0) {
    return null
  }
  if (currentStageIndex < 0) {
    return stages[0]?.productCode ?? null
  }
  return stages[currentStageIndex + NEXT_STAGE_OFFSET]?.productCode ?? null
}

function resolveRecentItems(
  currentRunItems: readonly MainlineCollaborationEvent[],
  visibleItems: readonly MainlineCollaborationEvent[]
): MainlineCollaborationEvent[] {
  const sourceItems = currentRunItems.length > 0 ? currentRunItems : visibleItems
  return sourceItems.slice(0, MAX_RECENT_ITEMS)
}

export function buildMainlineHandoffRun(
  items: readonly MainlineCollaborationEvent[],
  visibleProductCodes?: readonly string[]
): MainlineHandoffRun {
  const visibleStages = resolveVisibleStages(visibleProductCodes)
  const visibleItems = filterVisibleMainlineEvents(items, visibleStages)
  const anchorEvent = findAnchorEvent(visibleItems, visibleStages)
  const currentRunItems = filterCurrentRunItems(visibleItems, anchorEvent)
  const evidenceMap = buildEvidenceMap(currentRunItems)
  const currentStageIndex = resolveCurrentStageIndex(visibleStages, evidenceMap)
  const stages = visibleStages.map((stage, index) => ({
    ...stage,
    status: resolveStageStatus(index, currentStageIndex),
    evidence: evidenceMap[stage.productCode] ?? null
  }))
  const recentItems = resolveRecentItems(currentRunItems, visibleItems)
  const currentStage = currentStageIndex >= 0 ? visibleStages[currentStageIndex]?.productCode ?? null : null

  return {
    started: currentStageIndex >= 0,
    completed: currentStageIndex >= 0 && currentStageIndex === visibleStages.length - 1,
    currentStage,
    nextStage: resolveNextStage(currentStageIndex, visibleStages),
    anchorEvent,
    latestEvent: currentRunItems[0] ?? recentItems[0] ?? null,
    completedCount: currentStageIndex >= 0 ? currentStageIndex + 1 : 0,
    stages,
    recentItems
  }
}
