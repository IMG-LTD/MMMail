declare namespace Api {
  namespace Authenticator {
    interface Entry {
      periodSeconds: number;
    }

    interface EntryPayload {
      accountName: string;
      algorithm?: string;
      digits?: number;
      issuer: string;
      periodSeconds?: number;
      secretCiphertext: string;
    }

    interface ImportPayload {
      content: string;
      format?: string;
    }

    interface QrImagePayload {
      dataUrl: string;
    }

    interface BackupExportPayload {
      passphrase: string;
    }

    interface BackupImportPayload {
      content: string;
      passphrase: string;
    }

    interface SecurityPayload {
      syncEnabled: boolean;
      encryptedBackupEnabled: boolean;
      pinProtectionEnabled: boolean;
      lockTimeoutSeconds: number;
      pin?: string;
    }

    interface PinPayload {
      pin: string;
    }

    interface PinVerification {
      verified: boolean;
      lockTimeoutSeconds: number;
    }

    interface ImportResult {
      importedCount: number;
      totalCount: number;
      entries: Entry[];
    }

    interface ExportResult {
      format: string;
      fileName: string;
      content: string;
      entryCount: number;
      exportedAt: string;
    }

    interface Backup {
      fileName: string;
      content: string;
      entryCount: number;
      encryption: string;
      exportedAt: string;
    }
  }
}
