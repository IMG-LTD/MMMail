import { request } from '../request';

type RelayPolicyPayload = {
  catchAllEnabled: boolean;
  customDomainId: number | null;
  defaultMailboxId: number | null;
  note?: string;
  subdomainMode: string;
};

export function readSimpleLoginOverview(params: { orgId?: string } = {}) {
  return request<Api.SimpleLogin.Overview>({ url: '/api/v1/simplelogin/overview', params });
}

export function listSimpleLoginRelayPolicies(orgId: string) {
  return request<Api.SimpleLogin.RelayPolicy[]>({ url: `/api/v1/simplelogin/orgs/${orgId}/relay-policies` });
}

export function createSimpleLoginRelayPolicy(orgId: string, data: RelayPolicyPayload) {
  return request<Api.SimpleLogin.RelayPolicy>({
    url: `/api/v1/simplelogin/orgs/${orgId}/relay-policies`,
    method: 'post',
    data
  });
}

export function updateSimpleLoginRelayPolicy(orgId: string, policyId: string, data: RelayPolicyPayload) {
  return request<Api.SimpleLogin.RelayPolicy>({
    url: `/api/v1/simplelogin/orgs/${orgId}/relay-policies/${policyId}`,
    method: 'put',
    data
  });
}

export function deleteSimpleLoginRelayPolicy(orgId: string, policyId: string) {
  return request<void>({
    url: `/api/v1/simplelogin/orgs/${orgId}/relay-policies/${policyId}`,
    method: 'delete'
  });
}
