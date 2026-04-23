export interface SystemHealthMetricSummary {
  totalRequests: number
  failedRequests: number
}

export interface SystemHealthErrorTrackingSummary {
  totalEvents: number
  serverEvents: number
  clientEvents: number
}

export interface SystemHealthJobSummary {
  activeRuns: number
  totalRuns: number
  failedRuns: number
}

export interface SystemHealthOverview {
  status: string
  applicationName: string
  applicationVersion: string
  metrics: SystemHealthMetricSummary
  errorTracking: SystemHealthErrorTrackingSummary
  jobs: SystemHealthJobSummary
  prometheusPath: string
}
