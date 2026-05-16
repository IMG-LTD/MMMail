import { request } from '../request';

const BASE = '/api/v1/orgs';

export function readOrgBusinessOverview(orgId: string | number) {
  return request<Api.OrgBusiness.OverviewVo>({
    url: `${BASE}/${orgId}/business/overview`
  });
}

export function listOrgTeamSpaces(orgId: string | number) {
  return request<Api.OrgBusiness.TeamSpaceVo[]>({
    url: `${BASE}/${orgId}/team-spaces`
  });
}

export function listOrgTeamSpaceItems(
  orgId: string | number,
  teamSpaceId: string | number,
  params: Api.OrgBusiness.ListItemsParams = {}
) {
  return request<Api.OrgBusiness.TeamSpaceItemVo[]>({
    url: `${BASE}/${orgId}/team-spaces/${teamSpaceId}/items`,
    params
  });
}

export function buildOrgTeamSpaceFileDownloadUrl(
  orgId: string | number,
  teamSpaceId: string | number,
  itemId: string | number
) {
  return `${BASE}/${orgId}/team-spaces/${teamSpaceId}/files/${itemId}/download`;
}
