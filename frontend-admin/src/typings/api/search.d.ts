declare namespace Api {
  namespace Search {
    interface QueryParams {
      q: string;
      types?: string;
      page?: number;
      size?: number;
      from?: string;
      to?: string;
      orgId?: string | number;
    }

    type SuggestionParams = Pick<QueryParams, 'q'>;
    type FacetParams = Pick<QueryParams, 'q' | 'types' | 'from' | 'to' | 'orgId'>;

    interface Navigation {
      kind: string;
      path: string;
    }

    interface Item {
      moduleType: string;
      resourceId: string;
      title: string;
      snippet: string;
      score: number;
      updatedAt: string;
      navigation: Navigation;
    }

    interface Facets {
      byType: Record<string, number>;
    }

    interface SearchResult {
      total: number;
      items: Item[];
      facets: Facets;
      page: number;
      size: number;
    }

    interface Suggestion {
      moduleType: string;
      resourceId: string;
      title: string;
      path: string;
    }
  }
}
