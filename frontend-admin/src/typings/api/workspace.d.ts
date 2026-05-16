declare namespace Api {
  namespace Workspace {
    interface ProductCard {
      key: string;
      label: string;
      value: string;
      state: string;
      updatedAt: string;
    }

    interface Summary {
      productCards: ProductCard[];
      recommendationCount: number;
      systemStatus: string;
    }

    interface ActivityItem {
      id: string;
      product: string;
      title: string;
      occurredAt: string;
      actor: string;
    }

    interface Task {
      id: string;
      title: string;
      completed: boolean;
      dueAt: string;
      product: string;
    }

    interface PatchTaskPayload {
      completed?: boolean;
      title?: string;
    }
  }
}
