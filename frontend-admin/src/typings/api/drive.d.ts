declare namespace Api {
  namespace Drive {
    interface Item {
      id: string;
      parentId: string | null;
      itemType: string;
      name: string;
      mimeType: string | null;
      sizeBytes: number;
      shareCount: number;
      createdAt: string;
      updatedAt: string;
    }

    interface UploadPayload {
      fileName: string;
      parentId?: string | null;
      sizeBytes: number;
    }

    interface SharePayload {
      permission: 'VIEW' | 'EDIT';
      expiresAt?: string | null;
      password?: string;
    }

    interface EncryptedSharePayload {
      permission: 'VIEW';
      expiresAt?: string | null;
      password: string;
      e2eeAlgorithm: string;
      encryptedFile: File;
    }

    interface ShareReadableE2ee {
      enabled: boolean;
      algorithm: string;
    }

    interface ShareLink {
      id: string;
      itemId: string;
      token: string;
      permission: string;
      expiresAt: string | null;
      status: string;
      passwordProtected: boolean;
      e2ee: ShareReadableE2ee | null;
      createdAt: string;
      updatedAt: string;
    }

    interface Usage {
      fileCount: number;
      folderCount: number;
      storageBytes: number;
      storageLimitBytes: number;
    }

    interface FileE2ee {
      enabled: boolean;
      algorithm: string;
      fingerprints: string[];
    }

    interface Version {
      id: string;
      itemId: string;
      versionNo: number;
      authorUserId?: string;
      changeSummary?: string;
      mimeType: string;
      sizeBytes: number;
      checksum: string;
      e2ee: FileE2ee;
      createdAt: string;
    }

    interface VersionCleanup {
      deletedVersions: number;
      remainingVersions: number;
      appliedRetentionCount: number;
      appliedRetentionDays: number;
    }

    interface PublicShareCapabilities {
      auditedActions: string[];
      passwordHeader: string;
      states: string[];
      supportsAudit: boolean;
      supportsPasswordUnlock: boolean;
    }

    interface PublicShareMetadata {
      shareId: string;
      token: string;
      itemId: string;
      itemType: string;
      itemName: string;
      mimeType: string | null;
      sizeBytes: number;
      permission: string;
      status: string;
      expiresAt: string | null;
      passwordProtected: boolean;
      e2ee: ShareReadableE2ee | null;
    }
  }
}
