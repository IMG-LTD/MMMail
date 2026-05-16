declare namespace Api {
  /**
   * namespace Auth
   *
   * backend api module: "auth"
   */
  namespace Auth {
    interface LoginToken {
      token: string;
      refreshToken: string;
    }

    interface AuthPayload {
      accessToken: string;
      refreshToken: string;
      user: UserInfo;
      currentOrgId?: string;
      entitlements: string[];
      featureFlags: string[];
      risk: LoginRiskLevel;
      riskReasons: string[];
      secondFactorRequired: boolean;
      securityEventId?: string;
    }

    type LoginRiskLevel = 'low' | 'medium' | 'high';

    interface LoginRisk {
      risk: LoginRiskLevel;
      riskReasons: string[];
      secondFactorRequired: boolean;
      securityEventId?: string;
    }

    interface AccessDecisionMeta {
      roles?: string[];
      role?: string;
      requires?: string[];
      anyOf?: string[];
      featureFlag?: string;
      orgRequired?: boolean;
      fallback?: AccessFallback;
    }

    type AccessFallback = 'upgrade' | 'contact-sales' | 'trial' | 'forbidden';

    interface UserInfo {
      id?: string | number;
      userId: string;
      userName: string;
      roles: string[];
      buttons: string[];
      email?: string;
      displayName?: string;
      role?: string;
      mailAddressMode?: string;
      currentOrgId?: string;
      entitlements?: string[];
      featureFlags?: string[];
      avatar?: string;
      orgs?: OrgSummary[];
    }

    interface OrgSummary {
      id: string;
      name: string;
      role: string;
    }
  }
}
