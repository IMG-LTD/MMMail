import { request } from '../request';

interface UnifiedSearchParams {
  keyword: string;
  limit: number;
}

export function readUnifiedSearch(params: UnifiedSearchParams) {
  return request<Api.Suite.UnifiedSearchResult>({
    url: '/api/v1/suite/unified-search',
    params
  });
}
