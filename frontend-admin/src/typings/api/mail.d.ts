declare namespace Api {
  namespace Mail {
    interface Folder {
      key: string;
      label: string;
      unreadCount: number;
    }

    interface Label {
      id: number;
      name: string;
      color: string;
    }

    interface Attachment {
      id: string;
      fileName: string;
      fileSize: number;
      contentType?: string;
    }

    interface MessageSummary {
      id: string;
      senderEmail: string | null;
      senderDisplayName?: string | null;
      peerEmail: string;
      subject: string;
      preview: string;
      sentAt: string;
      isRead: boolean;
      unread: boolean;
      folderType: string;
      customFolderId?: string | null;
      customFolderName?: string | null;
      isStarred?: boolean;
      isDraft?: boolean;
      labels: string[];
      hasAttachments?: boolean;
    }

    interface MessageDetail extends MessageSummary {
      body: string;
      attachments: Attachment[];
    }

    interface MessagePage {
      items: MessageSummary[];
      total?: number;
      page?: number;
      size?: number;
    }

    interface DraftPayload {
      body?: string;
      fromEmail?: string;
      subject?: string;
      toEmail?: string;
    }

    interface SendPayload extends DraftPayload {
      idempotencyKey?: string;
      labels?: string[];
      toEmail: string;
    }

    interface BulkActionPayload {
      action: string;
      messageIds: string[];
    }

    interface UpdateLabelsPayload {
      labels: string[];
    }

    interface ExternalServer {
      host: string;
      port: number;
      ssl?: boolean;
      starttls?: boolean;
    }

    interface ExternalAccount {
      accountId: string;
      provider: string;
      authMode: string;
      email: string;
      username: string;
      imap: ExternalServer;
      smtp: ExternalServer;
      syncStatus: string;
      lastSyncAt?: string | null;
      lastError?: string | null;
      createdAt: string;
      updatedAt: string;
    }

    interface ExternalAccountPayload {
      provider?: string;
      authMode?: string;
      email?: string;
      username?: string;
      password?: string;
      oauthRefreshToken?: string;
      imap?: ExternalServer;
      smtp?: ExternalServer;
    }

    interface ExternalAccountTest {
      imapOk: boolean;
      smtpOk: boolean;
      latencyMs: number;
      message: string;
    }

    interface ExternalAccountSync {
      accountId: string;
      syncStatus: string;
      imported: number;
      skipped: number;
      lastSyncAt?: string | null;
      lastError?: string | null;
    }
  }
}
