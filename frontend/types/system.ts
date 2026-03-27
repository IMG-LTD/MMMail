export interface SystemHealthComponentStatus {
  name: string
  status: string
  details: string
}

export interface SystemHealthRequestMetric {
  module: string
  totalRequests: number
  failedRequests: number
}

export interface SystemHealthMetricSummary {
  totalRequests: number
  failedRequests: number
  processCpuUsage: number | null
  systemCpuUsage: number | null
  usedMemoryMb: number | null
  maxMemoryMb: number | null
  liveThreads: number | null
  activeDbConnections: number | null
  maxDbConnections: number | null
  modules: SystemHealthRequestMetric[]
}

export interface SystemHealthErrorTrackingSummary {
  totalEvents: number
  serverEvents: number
  clientEvents: number
  lastOccurredAt: string | null
}

export interface SystemHealthErrorEvent {
  eventId: string
  source: string
  category: string
  severity: string
  message: string
  detail: string | null
  path: string | null
  method: string | null
  status: number | null
  errorCode: number | null
  requestId: string | null
  userId: string | null
  sessionId: string | null
  orgId: string | null
  occurredAt: string
}

export interface SystemHealthJobSummary {
  activeRuns: number
  totalRuns: number
  failedRuns: number
  lastCompletedAt: string | null
}

export interface SystemHealthJobRun {
  runId: string
  jobName: string
  trigger: string
  status: string
  detail: string
  actorId: string | null
  orgId: string | null
  durationMs: number | null
  startedAt: string
  completedAt: string | null
}

export interface SystemHealthOverview {
  status: string
  applicationName: string
  applicationVersion: string
  activeProfiles: string[]
  uptimeSeconds: number
  generatedAt: string
  components: SystemHealthComponentStatus[]
  metrics: SystemHealthMetricSummary
  errorTracking: SystemHealthErrorTrackingSummary
  recentErrors: SystemHealthErrorEvent[]
  jobs: SystemHealthJobSummary
  recentJobs: SystemHealthJobRun[]
  prometheusPath: string
}

export interface CreateClientErrorEventRequest {
  message: string
  category: string
  severity: string
  detail?: string
  path?: string
  method?: string
  requestId?: string
}
