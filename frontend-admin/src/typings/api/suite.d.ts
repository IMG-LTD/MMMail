declare namespace Api {
  namespace Suite {
    interface UnifiedSearchItem {
      productCode: string;
      itemType: string;
      entityId: string;
      title: string;
      summary: string;
      routePath: string;
      updatedAt: string;
    }

    interface UnifiedSearchResult {
      generatedAt: string;
      keyword: string;
      limit: number;
      total: number;
      items: UnifiedSearchItem[];
    }
  }
}
