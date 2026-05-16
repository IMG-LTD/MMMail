import { request } from '../request';

export function readSearchResults(params: Api.Search.QueryParams) {
  return request<Api.Search.SearchResult>({
    url: '/api/v1/search',
    params
  });
}

export function readSearchSuggestions(params: Api.Search.SuggestionParams) {
  return request<Api.Search.Suggestion[]>({
    url: '/api/v1/search/suggestions',
    params
  });
}

export function readSearchFacets(params: Api.Search.FacetParams) {
  return request<Api.Search.Facets>({
    url: '/api/v1/search/facets',
    params
  });
}
