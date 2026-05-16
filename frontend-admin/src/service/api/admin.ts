import { request } from '../request';

export function readAdminSummary(orgId: string) {
  return request<Api.Admin.Summary>({ url: `/api/v1/orgs/${orgId}/admin-console/summary` });
}

export function listAdminDomains(_orgId: string) {
  return request<Api.Admin.Domain[]>({ url: '/api/v1/domains' });
}

export function createAdminDomain(_orgId: string, data: { domain: string }) {
  return request<Api.Admin.Domain>({
    url: '/api/v1/domains',
    method: 'post',
    data
  });
}

export function verifyAdminDomain(_orgId: string, domainId: string) {
  return request<Api.Admin.Domain>({
    url: `/api/v1/domains/${domainId}/verify`,
    method: 'post'
  });
}

export function listAdminDomainDnsRecords(domainId: string) {
  return request<Api.Admin.DomainDnsRecords>({ url: `/api/v1/domains/${domainId}/dns-records` });
}

export function readAdminDomainDiagnostics(domainId: string) {
  return request<Api.Admin.DomainDnsDiagnostics>({ url: `/api/v1/domains/${domainId}/diagnostics` });
}

export function deleteAdminDomain(_orgId: string, domainId: string) {
  return request<void>({
    url: `/api/v1/domains/${domainId}`,
    method: 'delete'
  });
}

export function listAdminProductAccess(orgId: string) {
  return request<Api.Admin.ProductAccess[]>({ url: `/api/v1/orgs/${orgId}/admin-console/product-access` });
}

export function listAdminMemberSessions(orgId: string) {
  return request<Api.Admin.MemberSession[]>({ url: `/api/v1/orgs/${orgId}/admin-console/member-sessions` });
}
