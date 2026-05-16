declare namespace Api {
  namespace Security {
    type SecurityAction = 'block' | 'force-logout' | 'mark-safe';
    type SecurityRisk = 'low' | 'medium' | 'high';

    interface SecurityEvent {
      id: string;
      type: string;
      severity: 'LOW' | 'MEDIUM' | 'HIGH';
      risk: SecurityRisk;
      reasons: string[];
      email?: string;
      ipAddress?: string;
      city?: string;
      country?: string;
      source?: string;
      detail?: string;
      lockedUntil?: string;
      acknowledgedAt?: string;
      actionStatus?: string;
      actionTaken?: string;
      actionAt?: string;
      createdAt: string;
    }
  }
}
