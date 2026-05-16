declare namespace Api {
  namespace OrgBusiness {
    interface OverviewVo {
      orgId: string;
      orgName: string;
      currentRole: string;
      memberCount: number;
      adminCount: number;
      pendingInviteCount: number;
      teamSpaceCount: number;
      storageBytes: number;
      storageLimitBytes: number;
      allowedEmailDomains: string[];
      governanceReviewSlaHours: number;
      requireDualReviewGovernance: boolean;
      generatedAt: string;
    }

    interface TeamSpaceVo {
      id: string;
      orgId: string;
      name: string;
      slug: string;
      description: string;
      rootItemId: string;
      storageBytes: number;
      storageLimitBytes: number;
      itemCount: number;
      createdBy: string;
      currentAccessRole: string;
      canWrite: boolean;
      canManage: boolean;
      updatedAt: string;
    }

    interface TeamSpaceItemVo {
      id: string;
      teamSpaceId: string;
      parentId: string | null;
      itemType: 'FOLDER' | 'FILE' | string;
      name: string;
      mimeType: string | null;
      sizeBytes: number;
      ownerEmail: string;
      updatedAt: string;
    }

    interface ListItemsParams {
      parentId?: string | number;
      keyword?: string;
      itemType?: 'FOLDER' | 'FILE' | string;
      limit?: number;
    }
  }
}
