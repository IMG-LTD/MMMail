declare namespace Api {
  namespace Docs {
    interface NoteSummary {
      id: string;
      title: string;
      updatedAt: string;
      currentVersion: number;
      permission: string;
    }

    interface NoteDetail extends NoteSummary {
      content: string;
    }

    interface CollabPresence {
      userId: string;
      email: string;
      displayName: string;
      status: string;
      lastSeenAt: string;
    }

    interface CollabSnapshot {
      resourceType: string;
      resourceId: string;
      version: number;
      snapshotBase64: string;
      updatedAt: string | null;
    }

    interface CollabSnapshotPayload {
      version: number;
      snapshotBase64: string;
    }

    interface CollabAwareness {
      resourceType: string;
      resourceId: string;
      activeUsers: CollabPresence[];
    }
  }

  namespace Community {
    interface Topic {
      id: string;
      slug: string;
      title: string;
      description: string | null;
      sortOrder: number;
      createdAt: string;
    }

    interface TopicDelete {
      id: string;
      deleted: boolean;
    }

    interface Post {
      id: string;
      authorUserId: number;
      topicId: string;
      title: string;
      bodyMd: string;
      bodyHtml: string;
      tags: string[];
      likeCount: number;
      commentCount: number;
      viewCount: number;
      pinned: boolean;
      locked: boolean;
      status: string;
      createdAt: string;
      updatedAt: string;
    }

    interface PostPage {
      items: Post[];
      total: number;
      page: number;
      size: number;
    }

    interface Comment {
      id: string;
      postId: string;
      parentCommentId: string | null;
      authorUserId: number;
      bodyMd: string;
      bodyHtml: string;
      status: string;
      createdAt: string;
      replies: Comment[];
    }

    interface Reaction {
      liked: boolean;
      likeCount: number;
    }

    interface Bookmark {
      bookmarked: boolean;
    }

    interface ViewCount {
      viewCount: number;
    }

    interface Report {
      id: string;
      targetType: string;
      targetId: string;
      reporterUserId: number;
      reason: string;
      detail: string | null;
      status: string;
      assigneeUserId: number | null;
      action: string | null;
      actionNote: string | null;
      createdAt: string;
      actionedAt: string | null;
    }
  }

  namespace Sheets {
    interface WorkbookSummary {
      id: string;
      title: string;
      rowCount: number;
      colCount: number;
      filledCellCount: number;
      currentVersion: number;
      updatedAt: string;
      canEdit: boolean;
    }

    interface WorkbookDetail extends WorkbookSummary {
      activeSheetId: string;
      grid: string[][];
      computedGrid: string[][];
    }

    interface FormulaCellInput {
      ref: string;
      formula: string;
    }

    interface FormulaEvaluationPayload {
      cells: FormulaCellInput[];
    }

    type FormulaCellValue = string | number | boolean | null;

    interface FormulaCellResult {
      ref: string;
      value: FormulaCellValue;
      type: string;
      dependsOn: string[];
    }

    interface FormulaEvaluation {
      results: FormulaCellResult[];
    }

    interface FormulaGraphNode {
      ref: string;
      formula: string;
      dependsOn: string[];
      dependents: string[];
    }

    interface DependencyGraph {
      nodes: FormulaGraphNode[];
      topologicalOrder: string[];
    }
  }

  namespace Pass {
    interface Vault {
      id: string;
      name: string;
      scopeType: string;
      ownerEmail: string;
      itemCount: number;
      updatedAt: string;
    }

    interface Item {
      id: string;
      title: string;
      website: string;
      username: string;
      favorite: boolean;
      updatedAt: string;
      itemType: string;
      secureLinkCount: number;
    }

    interface Monitor {
      totalItemCount: number;
      weakPasswordCount: number;
      reusedPasswordCount: number;
      inactiveTwoFactorCount: number;
    }
  }

  namespace Collaboration {
    interface Project {
      id: string;
      name: string;
      product: string;
      status: string;
      taskCount: number;
      updatedAt: string;
    }

    interface Task {
      id: string;
      projectId: string;
      title: string;
      product: string;
      status: string;
      columnId: string;
      position: string;
      assigneeEmail: string;
      dueAt: string;
    }

    interface BoardColumn {
      columnId: string;
      title: string;
      tasks: Task[];
    }

    interface Board {
      projectId: string;
      columns: BoardColumn[];
    }

    interface TaskMovePayload {
      columnId: string;
      afterTaskId?: string;
      beforeTaskId?: string;
    }

    interface TaskMoveResult {
      taskId: string;
      projectId: string;
      columnId: string;
      position: string;
    }
  }

  namespace CommandCenter {
    interface Command {
      id: string;
      name: string;
      description: string;
      product: string;
      enabled: boolean;
      parameterCount: number;
    }

    interface CatalogParams {
      context?: string;
    }

    interface RecentParams {
      limit?: number;
    }

    interface QuickSearchParams {
      q: string;
      limit?: number;
    }

    interface PinPayload {
      commandId: string;
      pinned: boolean;
    }

    interface CommandAction {
      kind: string;
      payload: Record<string, string | number | boolean | null | undefined>;
    }

    interface CatalogItem {
      id: string;
      title: string;
      description: string | null;
      group: string;
      icon: string;
      shortcut: string | null;
      action: CommandAction;
      requires: string[];
      pinned: boolean;
      lastUsedAt: string | null;
    }

    interface Preference {
      commandId: string;
      pinned: boolean;
      usageCount: number;
      lastUsedAt: string | null;
      pinnedAt: string | null;
    }

    interface Recent {
      commandId: string;
      title: string;
      group: string;
      routePath: string;
      lastUsedAt: string;
      usageCount: number;
    }

    interface QuickSearchItem {
      sourceType: string;
      id: string;
      title: string;
      summary: string;
      routePath: string;
      productCode: string;
    }
  }

  namespace Admin {
    interface Summary {
      orgId: string;
      orgName: string;
      memberCount: number;
      adminCount: number;
      domainCount: number;
      enabledProductCount: number;
      generatedAt: string;
    }

    interface Domain {
      id: string;
      orgId: string;
      domain: string;
      verificationToken: string;
      status: string;
      defaultDomain: boolean;
      verifiedAt: string | null;
      updatedAt: string;
    }

    interface DomainDnsRecord {
      type: string;
      host: string;
      expected: string;
    }

    interface DomainDnsRecords {
      records: DomainDnsRecord[];
    }

    interface DomainDnsDiagnosticRecord extends DomainDnsRecord {
      actual: string[];
      matched: boolean;
    }

    interface DomainDnsDiagnostics {
      domainId: string;
      domain: string;
      status: string;
      records: DomainDnsDiagnosticRecord[];
    }

    interface ProductAccess {
      memberId: string;
      userEmail: string;
      role: string;
      enabledProductCount: number;
    }

    interface MemberSession {
      sessionId: string;
      memberEmail: string;
      role: string;
      expiresAt: string;
      current: boolean;
    }
  }

  namespace Contacts {
    interface Contact {
      id: string;
      displayName: string;
      email: string;
      note: string;
      isFavorite: boolean;
      createdAt: string;
      updatedAt: string;
    }

    interface Group {
      id: string;
      name: string;
      description: string;
      memberCount: number;
      createdAt: string;
      updatedAt: string;
    }

    interface Suggestion {
      email: string;
      displayName: string;
      source: string;
      favorite: boolean;
    }

    interface SuggestionParams {
      keyword?: string;
      limit?: number;
    }

    interface ImportCsvPayload {
      content: string;
      mergeDuplicates: boolean;
    }

    interface ImportResult {
      totalRows: number;
      created: number;
      updated: number;
      skipped: number;
      invalid: number;
    }

    interface ExportParams {
      format?: string;
    }

    interface DuplicateGroup {
      signature: string;
      count: number;
      contacts: Contact[];
    }

    interface MergeDuplicatesPayload {
      primaryContactId: string;
      duplicateContactIds: string[];
    }
  }

  namespace Wallet {
    interface Account {
      accountId: string;
      walletName: string;
      assetSymbol: string;
      address: string;
      balanceMinor: number;
      createdAt: string;
      updatedAt: string;
    }

    interface Transaction {
      transactionId: string;
      accountId: string;
      txType: string;
      counterpartyAddress: string;
      amountMinor: number;
      assetSymbol: string;
      memo: string;
      status: string;
      confirmations: number;
      signatureHash: string;
      networkTxHash: string;
      createdAt: string;
    }

    interface ExecutionOverview {
      accountId: string;
      generatedAt: string;
      executionHealthScore: number;
      riskLevel: string;
      blockedCount: number;
    }
  }

  namespace Vpn {
    interface Server {
      serverId: string;
      country: string;
      city: string;
      tier: string;
      status: string;
      loadPercent: number;
    }

    interface Profile {
      profileId: string;
      name: string;
      protocol: string;
      routingMode: string;
      targetServerId: string;
      targetCountry: string;
      secureCoreEnabled: boolean;
      netshieldMode: string;
      killSwitchEnabled: boolean;
      createdAt: string;
      updatedAt: string;
    }

    interface Session {
      sessionId: string;
      serverId: string;
      serverCountry: string;
      serverCity: string;
      serverTier: string;
      protocol: string;
      status: string;
      profileId: string;
      profileName: string;
      netshieldMode: string;
      killSwitchEnabled: boolean;
      connectionSource: string;
      connectedAt: string;
      disconnectedAt: string;
      durationSeconds: number;
    }
  }

  namespace Meet {
    interface AccessOverview {
      planCode: string;
      planName: string;
      eligibleForInstantAccess: boolean;
      accessGranted: boolean;
      waitlistRequested: boolean;
      salesContactRequested: boolean;
      accessState: string;
      recommendedAction: string;
      companyName: string;
      requestedSeats: number | null;
      requestNote: string;
      waitlistRequestedAt: string;
      accessGrantedAt: string;
      salesContactRequestedAt: string;
    }

    interface Room {
      roomId: string;
      roomCode: string;
      topic: string;
      accessLevel: string;
      maxParticipants: number;
      joinCode: string;
      status: string;
      startedAt: string;
      endedAt: string;
      durationSeconds: number;
    }
  }

  namespace Authenticator {
    interface Entry {
      id: string;
      issuer: string;
      accountName: string;
      algorithm: string;
      digits: number;
      updatedAt: string;
    }

    interface Code {
      code: string;
      expiresInSeconds: number;
      periodSeconds: number;
      digits: number;
    }

    interface Security {
      syncEnabled: boolean;
      encryptedBackupEnabled: boolean;
      pinProtectionEnabled: boolean;
      pinConfigured: boolean;
      lockTimeoutSeconds: number;
      lastSyncedAt: string;
      lastBackupAt: string;
    }
  }

  namespace SimpleLogin {
    interface Overview {
      orgId: string;
      aliasCount: number;
      enabledAliasCount: number;
      disabledAliasCount: number;
      mailboxCount: number;
      verifiedMailboxCount: number;
      defaultMailboxEmail: string;
      reverseAliasContactCount: number;
      customDomainCount: number;
      verifiedCustomDomainCount: number;
      defaultDomain: string;
      relayPolicyCount: number;
      catchAllDomainCount: number;
      subdomainPolicyCount: number;
      defaultRelayMailboxEmail: string;
      generatedAt: string;
    }

    interface RelayPolicy {
      id: string;
      orgId: string;
      customDomainId: string;
      domain: string;
      catchAllEnabled: boolean;
      subdomainMode: string;
      defaultMailboxId: string;
      defaultMailboxEmail: string;
      note: string;
      createdAt: string;
      updatedAt: string;
    }
  }

  namespace StandardNotes {
    interface Overview {
      totalNoteCount: number;
      activeNoteCount: number;
      pinnedNoteCount: number;
      archivedNoteCount: number;
      uniqueTagCount: number;
      folderCount: number;
      checklistNoteCount: number;
      checklistTaskCount: number;
      completedChecklistTaskCount: number;
      exportReady: boolean;
      generatedAt: string;
    }

    interface Folder {
      id: string;
      name: string;
      color: string;
      description: string;
      noteCount: number;
      checklistTaskCount: number;
      completedChecklistTaskCount: number;
      updatedAt: string;
    }

    interface Summary {
      id: string;
      title: string;
      preview: string;
      noteType: string;
      tags: string[];
      pinned: boolean;
      archived: boolean;
      currentVersion: number;
      folderId: string;
      folderName: string;
      checklistTaskCount: number;
      completedChecklistTaskCount: number;
      updatedAt: string;
    }

    interface ChecklistItem {
      itemIndex: number;
      text: string;
      completed: boolean;
    }

    interface Detail extends Summary {
      content: string;
      checklistItems: ChecklistItem[];
      createdAt: string;
    }

    interface Export {
      exportedAt: string;
      overview: Overview;
      folders: Folder[];
      notes: Detail[];
    }
  }

  namespace MailFilters {
    interface Filter {
      id: string;
      name: string;
      enabled: boolean;
      senderContains: string;
      subjectContains: string;
      keywordContains: string;
      targetFolder: string;
      targetCustomFolderId: string;
      targetCustomFolderName: string;
      labels: string[];
      markRead: boolean;
      createdAt: string;
      updatedAt: string;
    }

    interface Preview {
      senderEmail: string;
      subject: string;
      baseFolder: string;
      effectiveFolder: string;
      effectiveCustomFolderId: string;
      effectiveCustomFolderName: string;
      effectiveLabels: string[];
      markRead: boolean;
      blockedBySecurityRule: boolean;
      securityReason: string;
      securityMatchedRule: string;
      matchedFilterId: string;
      matchedFilterName: string;
    }
  }

  namespace Billing {
    interface Overview {
      activePlanCode: string;
      activePlanName: string;
      latestDraft: CheckoutDraft | null;
      selfServeOfferCodes: string[];
      contactSalesOfferCodes: string[];
    }

    interface PricingOffer {
      code: string;
      name: string;
      description: string;
      segment: string;
      linkedPlanCode: string;
      checkoutMode: string;
      priceMode: string;
      currencyCode: string;
      priceValue: string;
      defaultBillingCycle: string;
      recommended: boolean;
    }

    interface Center {
      subscriptionSummary: SubscriptionSummary;
      paymentMethods: PaymentMethod[];
      invoices: Invoice[];
      availableActions: SubscriptionAction[];
    }

    interface SubscriptionSummary {
      activePlanCode: string;
      activePlanName: string;
      billingCycle: string;
      seatCount: number;
      autoRenew: boolean;
      currentPeriodEndsAt: string;
      pendingActionCode: string;
      pendingOfferCode: string;
      pendingOfferName: string;
      pendingEffectiveAt: string;
    }

    interface PaymentMethod {
      id: number;
      methodType: string;
      displayLabel: string;
      brand: string;
      lastFour: string;
      expiresAt: string;
      defaultMethod: boolean;
      status: string;
    }

    interface Invoice {
      invoiceNumber: string;
      offerCode: string;
      offerName: string;
      invoiceStatus: string;
      currencyCode: string;
      totalCents: number;
      billingCycle: string;
      seatCount: number;
      issuedAt: string;
      dueAt: string;
      downloadCode: string;
    }

    interface SubscriptionAction {
      actionCode: string;
      actionStatus: string;
      enabled: boolean;
      targetOfferCode: string;
      targetOfferName: string;
      effectiveAt: string;
      reasonCode: string;
    }

    interface CheckoutDraft {
      id: string;
      offerCode: string;
      billingCycle: string;
      seatCount: number;
      updatedAt: string;
    }
  }
}
