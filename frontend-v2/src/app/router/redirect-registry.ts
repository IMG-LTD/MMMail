export interface RedirectRule {
  from: string
  to: string
}

export const redirectRegistry: RedirectRule[] = [
  { from: '/pass-monitor', to: '/pass/monitor' },
  { from: '/conversations', to: '/inbox' },
  { from: '/labels', to: '/inbox' },
  { from: '/settings/system-health', to: '/settings?panel=system-health' }
]
