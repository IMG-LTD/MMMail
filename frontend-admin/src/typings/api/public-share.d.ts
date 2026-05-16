declare namespace Api {
  namespace PublicShare {
    interface MailAttachment {
      id: string;
      fileName: string;
      contentType: string;
      fileSize: number;
      algorithm: string;
    }

    interface MailShare {
      mailId: string;
      subject: string;
      senderEmail: string;
      recipientEmail: string;
      bodyCiphertext: string;
      algorithm: string;
      passwordHint: string;
      expiresAt: string | null;
      attachments: MailAttachment[];
    }

    interface PassShare {
      itemId: string;
      sharedVaultId: string | null;
      sharedVaultName: string | null;
      itemType: string;
      title: string;
      website: string | null;
      username: string;
      secretCiphertext: string;
      note: string | null;
      maxViews: number;
      currentViews: number;
      remainingViews: number | null;
      expiresAt: string | null;
    }
  }
}
