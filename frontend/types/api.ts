export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export type MailId = string

export interface UserProfile {
  id: string
  email: string
  displayName: string
  role: 'USER' | 'ADMIN'
  mailAddressMode: MailAddressMode
}

export interface AuthPayload {
  accessToken: string
  refreshToken: string
  user: UserProfile
}

export type DocsPermission = 'OWNER' | 'VIEW' | 'EDIT'
export type DocsScope = 'OWNED' | 'SHARED'
export type DocsPresenceMode = 'VIEW' | 'EDIT'
export type DocsSyncKind = 'BOOTSTRAP' | 'SYNC' | 'UPDATE'

export interface DocsNoteSummary {
  id: string
  title: string
  updatedAt: string
  permission: DocsPermission
  scope: DocsScope
  currentVersion: number
  ownerEmail: string
  ownerDisplayName: string
  collaboratorCount: number
}

export interface DocsNoteDetail {
  id: string
  title: string
  content: string
  createdAt: string
  updatedAt: string
  currentVersion: number
  permission: DocsPermission
  shared: boolean
  ownerEmail: string
  ownerDisplayName: string
  collaboratorCount: number
  syncCursor: number
  syncVersion: string
}

export interface DocsNoteShare {
  shareId: string
  collaboratorUserId: string
  collaboratorEmail: string
  collaboratorDisplayName: string
  permission: 'VIEW' | 'EDIT'
  createdAt: string
}

export interface DocsNoteComment {
  commentId: string
  authorUserId: string
  authorEmail: string
  authorDisplayName: string
  excerpt: string | null
  content: string
  resolved: boolean
  resolvedAt: string | null
  createdAt: string
}

export interface DocsNotePresence {
  presenceId: string
  userId: string
  email: string
  displayName: string
  sessionId: string
  activeMode: DocsPresenceMode
  permission: DocsPermission
  lastHeartbeatAt: string
}

export interface DocsNoteCollaborationOverview {
  generatedAt: string
  collaborators: DocsNoteShare[]
  comments: DocsNoteComment[]
  activeSessions: DocsNotePresence[]
  syncCursor: number
  syncVersion: string
}

export interface DocsNoteSyncEvent {
  eventId: number
  eventType: string
  noteId: string
  sessionId: string
  actorEmail: string
  createdAt: string
}

export interface DocsNoteSync {
  kind: DocsSyncKind
  generatedAt: string
  syncCursor: number
  syncVersion: string
  hasUpdates: boolean
  total: number
  items: DocsNoteSyncEvent[]
}

export interface CreateDocsNoteRequest {
  title: string
  content?: string
}

export interface UpdateDocsNoteRequest {
  title: string
  content?: string
  currentVersion: number
}

export interface CreateDocsNoteShareRequest {
  collaboratorEmail: string
  permission: 'VIEW' | 'EDIT'
}

export interface CreateDocsNoteCommentRequest {
  excerpt?: string
  content: string
}

export interface HeartbeatDocsNotePresenceRequest {
  activeMode: DocsPresenceMode
}

export interface PassItemSummary {
  id: string
  title: string
  website: string | null
  username: string | null
  favorite: boolean
  updatedAt: string
}

export interface PassItemDetail {
  id: string
  title: string
  website: string | null
  username: string | null
  secretCiphertext: string
  note: string
  favorite: boolean
  createdAt: string
  updatedAt: string
}

export interface CreatePassItemRequest {
  title: string
  website?: string
  username?: string
  secretCiphertext: string
  note?: string
}

export interface UpdatePassItemRequest {
  title: string
  website?: string
  username?: string
  secretCiphertext: string
  note?: string
}

export interface GeneratePasswordRequest {
  length?: number
  includeLowercase?: boolean
  includeUppercase?: boolean
  includeDigits?: boolean
  includeSymbols?: boolean
}

export interface PassGeneratedPassword {
  password: string
}

export type AuthenticatorAlgorithm = 'SHA1' | 'SHA256' | 'SHA512'
export type AuthenticatorImportFormat = 'AUTO' | 'OTPAUTH_URI' | 'MMMAIL_JSON'

export interface AuthenticatorEntrySummary {
  id: string
  issuer: string
  accountName: string
  algorithm: AuthenticatorAlgorithm
  digits: number
  periodSeconds: number
  updatedAt: string
}

export interface AuthenticatorEntryDetail extends AuthenticatorEntrySummary {
  secretCiphertext: string
  createdAt: string
}

export interface CreateAuthenticatorEntryRequest {
  issuer: string
  accountName: string
  secretCiphertext: string
  algorithm?: AuthenticatorAlgorithm
  digits?: number
  periodSeconds?: number
}

export interface UpdateAuthenticatorEntryRequest {
  issuer: string
  accountName: string
  secretCiphertext: string
  algorithm?: AuthenticatorAlgorithm
  digits?: number
  periodSeconds?: number
}

export interface AuthenticatorCodePayload {
  code: string
  expiresInSeconds: number
  periodSeconds: number
  digits: number
}

export interface ImportAuthenticatorEntriesRequest {
  content: string
  format?: AuthenticatorImportFormat
}

export interface AuthenticatorImportResult {
  importedCount: number
  totalCount: number
  entries: AuthenticatorEntrySummary[]
}

export interface AuthenticatorExportPayload {
  format: string
  fileName: string
  content: string
  entryCount: number
  exportedAt: string
}

export interface CreateAuthenticatorBackupRequest {
  passphrase: string
}

export interface ImportAuthenticatorBackupRequest {
  content: string
  passphrase: string
}

export interface AuthenticatorBackupPayload {
  fileName: string
  content: string
  entryCount: number
  encryption: string
  exportedAt: string
}

export type VpnServerTier = 'STANDARD' | 'SECURE_CORE'
export type VpnServerStatus = 'ONLINE' | 'MAINTENANCE'
export type VpnProtocol = 'WIREGUARD' | 'OPENVPN_UDP' | 'OPENVPN_TCP'
export type VpnSessionStatus = 'CONNECTED' | 'DISCONNECTED'

export interface VpnServerItem {
  serverId: string
  country: string
  city: string
  tier: VpnServerTier
  status: VpnServerStatus
  loadPercent: number
}

export interface VpnSessionItem {
  sessionId: string
  serverId: string
  serverCountry: string
  serverCity: string
  serverTier: VpnServerTier
  protocol: VpnProtocol
  status: VpnSessionStatus
  connectedAt: string
  disconnectedAt: string | null
  durationSeconds: number
}

export interface ConnectVpnSessionRequest {
  serverId: string
  protocol: VpnProtocol
}

export type MeetAccessLevel = 'PRIVATE' | 'PUBLIC'
export type MeetRoomStatus = 'ACTIVE' | 'ENDED'
export type MeetAccessState = 'LOCKED' | 'WAITLISTED' | 'GRANTED' | 'CONTACT_REQUESTED'
export type MeetAccessRecommendedAction = 'JOIN_WAITLIST' | 'ACTIVATE' | 'OPEN_WORKSPACE' | 'CONTACT_SALES' | 'CONTACT_REQUESTED'

export interface MeetAccessOverview {
  planCode: string
  planName: string
  eligibleForInstantAccess: boolean
  accessGranted: boolean
  waitlistRequested: boolean
  salesContactRequested: boolean
  accessState: MeetAccessState
  recommendedAction: MeetAccessRecommendedAction
  companyName: string | null
  requestedSeats: number | null
  requestNote: string | null
  waitlistRequestedAt: string | null
  accessGrantedAt: string | null
  salesContactRequestedAt: string | null
}

export interface MeetRoomItem {
  roomId: string
  roomCode: string
  topic: string
  accessLevel: MeetAccessLevel
  maxParticipants: number
  joinCode: string | null
  status: MeetRoomStatus
  startedAt: string
  endedAt: string | null
  durationSeconds: number
}

export interface CreateMeetRoomRequest {
  topic: string
  accessLevel?: MeetAccessLevel
  maxParticipants?: number
}

export interface JoinMeetWaitlistRequest {
  note?: string
}

export interface RequestMeetEnterpriseAccessRequest {
  companyName: string
  requestedSeats: number
  note?: string
}

export type MeetParticipantRole = 'HOST' | 'CO_HOST' | 'PARTICIPANT'
export type MeetParticipantStatus = 'ACTIVE' | 'LEFT' | 'REMOVED'

export interface MeetParticipantItem {
  participantId: string
  roomId: string
  userId: string
  displayName: string
  role: MeetParticipantRole
  status: MeetParticipantStatus
  audioEnabled: boolean
  videoEnabled: boolean
  screenSharing: boolean
  joinedAt: string
  leftAt: string | null
  lastHeartbeatAt: string
  self: boolean
  canManageParticipants: boolean
  canTransferHost: boolean
}

export interface JoinMeetRoomRequest {
  displayName: string
}

export interface MeetGuestJoinOverview {
  roomId: string
  roomCode: string
  topic: string
  joinCode: string
  accessLevel: MeetAccessLevel
  roomStatus: MeetRoomStatus
  guestJoinEnabled: boolean
  lobbyEnabled: boolean
  activeParticipants: number
  maxParticipants: number
}

export interface SubmitMeetGuestRequestRequest {
  displayName: string
  audioEnabled?: boolean
  videoEnabled?: boolean
}

export type MeetGuestRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'LEFT'
export type MeetGuestSessionStatus = 'WAITING' | 'ACTIVE' | 'REJECTED' | 'LEFT' | 'REMOVED' | 'ROOM_ENDED'

export interface MeetGuestRequestItem {
  requestId: string
  roomId: string
  roomCode: string
  roomStatus: MeetRoomStatus
  displayName: string
  audioEnabled: boolean
  videoEnabled: boolean
  status: MeetGuestRequestStatus
  requestToken: string
  guestSessionToken: string | null
  participantId: string | null
  requestedAt: string
  approvedAt: string | null
  rejectedAt: string | null
}

export interface MeetGuestParticipantView {
  participantId: string
  displayName: string
  role: MeetParticipantRole
  status: MeetParticipantStatus
  audioEnabled: boolean
  videoEnabled: boolean
  screenSharing: boolean
  self: boolean
}

export interface MeetGuestSession {
  roomId: string
  roomCode: string
  topic: string
  sessionStatus: MeetGuestSessionStatus
  selfParticipant: MeetGuestParticipantView | null
  participants: MeetGuestParticipantView[]
}

export interface UpdateMeetParticipantRoleRequest {
  role: MeetParticipantRole
}

export interface TransferMeetHostRequest {
  targetParticipantId: string
}

export type MeetSignalType = 'OFFER' | 'ANSWER' | 'ICE'

export interface SendMeetSignalRequest {
  fromParticipantId: string
  toParticipantId?: string
  payload: string
}

export interface MeetSignalEventItem {
  eventSeq: string
  roomId: string
  signalType: MeetSignalType
  fromParticipantId: string
  toParticipantId: string | null
  payload: string
  createdAt: string
}

export interface PruneMeetInactiveParticipantsRequest {
  inactiveSeconds: number
}

export interface MeetPruneInactiveResult {
  roomId: string
  inactiveSeconds: number
  removedCount: number
  removedParticipantIds: string[]
  executedAt: string
}

export interface UpdateMeetParticipantMediaRequest {
  audioEnabled: boolean
  videoEnabled: boolean
  screenSharing: boolean
}

export interface ReportMeetQualityRequest {
  jitterMs: number
  packetLossPercent: number
  roundTripMs: number
}

export interface MeetQualitySnapshotItem {
  snapshotId: string
  roomId: string
  participantId: string
  jitterMs: number
  packetLossPercent: number
  roundTripMs: number
  qualityScore: number
  createdAt: string
}

export interface WalletAccount {
  accountId: string
  walletName: string
  assetSymbol: string
  address: string
  balanceMinor: number
  createdAt: string
  updatedAt: string
}

export type WalletTransactionType = 'SEND' | 'RECEIVE'
export type WalletTransactionStatus = 'CONFIRMED' | 'PENDING' | 'SIGNED' | 'BROADCASTED' | 'FAILED'
export type WalletExecutionRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type WalletRemediationStrategy = 'RETRY_SIGN' | 'RETRY_BROADCAST' | 'ROLLBACK_FAIL'
export type WalletReconcileStrategy = 'AUTO' | WalletRemediationStrategy

export interface WalletTransaction {
  transactionId: string
  accountId: string
  txType: WalletTransactionType
  counterpartyAddress: string
  amountMinor: number
  assetSymbol: string
  memo: string
  status: WalletTransactionStatus
  confirmations: number
  signatureHash: string | null
  networkTxHash: string | null
  createdAt: string
}

export interface WalletStageCounts {
  pendingCount: number
  signedCount: number
  broadcastedCount: number
  confirmedCount: number
  failedCount: number
}

export interface WalletPriorityTransaction {
  transactionId: string
  status: WalletTransactionStatus
  ageMinutes: number
  reason: string
  recommendedActions: WalletRemediationStrategy[]
}

export interface WalletExecutionOverview {
  accountId: string
  generatedAt: string
  executionHealthScore: number
  riskLevel: WalletExecutionRiskLevel
  stageCounts: WalletStageCounts
  blockedCount: number
  priorityTransactions: WalletPriorityTransaction[]
}

export type WalletExecutionRecommendedOperation = 'ADVANCE' | WalletRemediationStrategy

export interface WalletExecutionPlanItem {
  transactionId: string
  status: WalletTransactionStatus
  reason: string
  recommendedOperation: WalletExecutionRecommendedOperation
  priority: number
}

export interface WalletExecutionPlan {
  accountId: string
  generatedAt: string
  recommendedAdvanceCount: number
  recommendedRemediationCount: number
  estimatedRiskDelta: number
  items: WalletExecutionPlanItem[]
}

export interface WalletExecutionTraceStageEvent {
  stage: string
  at: string
  source: string
  message: string
}

export interface WalletExecutionTrace {
  transactionId: string
  currentStatus: WalletTransactionStatus
  integrityScore: number
  warnings: string[]
  stageEvents: WalletExecutionTraceStageEvent[]
}

export interface WalletReconciliationOverview {
  accountId: string
  generatedAt: string
  integrityScore: number
  riskLevel: WalletExecutionRiskLevel
  mismatchCount: number
  blockedCount: number
  failedCount: number
  recommendedActions: string[]
}

export interface WalletActionResult {
  transaction: WalletTransaction
  fromStatus: WalletTransactionStatus
  toStatus: WalletTransactionStatus
  operation: string
  message: string
}

export interface WalletBatchActionResult {
  accountId: string
  operation: string
  requestedCount: number
  processedCount: number
  successCount: number
  failedCount: number
  skippedCount: number
  results: WalletActionResult[]
}

export interface CreateWalletAccountRequest {
  walletName: string
  assetSymbol: string
  address: string
}

export interface ReceiveWalletTransactionRequest {
  accountId: string
  amountMinor: number
  assetSymbol: string
  sourceAddress: string
  memo?: string
}

export interface SendWalletTransactionRequest {
  accountId: string
  amountMinor: number
  assetSymbol: string
  targetAddress: string
  memo?: string
}

export interface ConfirmWalletTransactionRequest {
  confirmations: number
  networkTxHash: string
}

export interface SignWalletTransactionRequest {
  signerHint: string
}

export interface BroadcastWalletTransactionRequest {
  networkTxHash: string
}

export interface FailWalletTransactionRequest {
  reason?: string
}

export interface AdvanceWalletTransactionRequest {
  operatorHint?: string
}

export interface RemediateWalletTransactionRequest {
  strategy: WalletRemediationStrategy
  reason?: string
}

export interface BatchAdvanceWalletTransactionsRequest {
  accountId: string
  maxItems?: number
  operatorHint?: string
}

export interface BatchRemediateWalletTransactionsRequest {
  accountId: string
  maxItems?: number
  strategy: WalletRemediationStrategy
  reason?: string
}

export interface BatchReconcileWalletTransactionsRequest {
  accountId: string
  maxItems?: number
  strategy?: WalletReconcileStrategy
}

export interface LumoProject {
  projectId: string
  name: string
  description: string
  conversationCount: number
  createdAt: string
  updatedAt: string
}

export interface CreateLumoProjectRequest {
  name: string
  description?: string
}

export interface LumoProjectKnowledge {
  knowledgeId: string
  projectId: string
  title: string
  content: string
  createdAt: string
  updatedAt: string
}

export interface CreateLumoProjectKnowledgeRequest {
  title: string
  content: string
}

export interface LumoConversation {
  conversationId: string
  projectId: string | null
  title: string
  pinned: boolean
  modelCode: 'LUMO-BASE' | 'LUMO-PLUS' | 'LUMO-BIZ'
  archived: boolean
  createdAt: string
  updatedAt: string
}

export type LumoMessageRole = 'USER' | 'ASSISTANT'

export interface LumoMessage {
  messageId: string
  conversationId: string
  role: LumoMessageRole
  content: string
  tokenCount: number
  createdAt: string
}

export interface CreateLumoConversationRequest {
  title: string
  modelCode?: 'LUMO-BASE' | 'LUMO-PLUS' | 'LUMO-BIZ'
  projectId?: string
}

export interface SendLumoMessageRequest {
  content: string
  knowledgeIds?: string[]
}

export interface UpdateLumoConversationModelRequest {
  modelCode: 'LUMO-BASE' | 'LUMO-PLUS' | 'LUMO-BIZ'
}

export interface UpdateLumoConversationArchiveRequest {
  archived: boolean
}

export type SystemMailFolder = 'INBOX' | 'SENT' | 'DRAFTS' | 'OUTBOX' | 'ARCHIVE' | 'SPAM' | 'TRASH' | 'SCHEDULED' | 'SNOOZED'
export type MailFolder = SystemMailFolder | 'CUSTOM'
export type MailIdentitySource = 'PRIMARY' | 'ORG_CUSTOM_DOMAIN' | 'PASS_ALIAS'
export type MailIdentityStatus = 'ENABLED' | 'DISABLED'

export interface MailSummary {
  id: MailId
  ownerId: string
  senderEmail: string | null
  peerEmail: string
  folderType: MailFolder
  customFolderId: string | null
  customFolderName: string | null
  subject: string
  preview: string
  isRead: boolean
  isStarred: boolean
  isDraft: boolean
  sentAt: string
  labels: string[]
}

export interface MailDetail extends MailSummary {
  body: string
  attachments: MailAttachment[]
  e2ee?: MailBodyE2ee | null
}

export interface MailBodyE2ee {
  enabled: boolean
  algorithm: string | null
  recipientFingerprints: string[]
}

export interface MailAttachment {
  id: string
  mailId: string
  fileName: string
  contentType: string
  fileSize: number
  e2ee?: MailAttachmentE2ee | null
}

export interface MailPage {
  items: MailSummary[]
  total: number
  page: number
  size: number
  unread: number
}

export interface MailboxStats {
  folderCounts: Record<SystemMailFolder, number>
  unreadCount: number
  starredCount: number
}

export interface MailActionResult {
  affected: number
  stats: MailboxStats
}

export type ConversationAction = 'MARK_READ' | 'MARK_UNREAD' | 'MOVE_ARCHIVE' | 'MOVE_TRASH'

export interface ConversationSummary {
  conversationId: string
  subject: string
  participants: string[]
  messageCount: number
  unreadCount: number
  latestAt: string
}

export interface ConversationPage {
  items: ConversationSummary[]
  total: number
  page: number
  size: number
}

export interface ConversationDetail {
  conversationId: string
  subject: string
  messages: MailSummary[]
}

export interface ContactSuggestion {
  email: string
  displayName: string | null
  isFavorite: boolean
  source: 'CONTACT' | 'HISTORY'
  lastContactAt: string
  messageCount: number
}

export interface ContactItem {
  id: string
  displayName: string
  email: string
  note: string | null
  isFavorite: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateContactRequest {
  displayName: string
  email: string
  note?: string
}

export interface UpdateContactRequest {
  displayName: string
  email: string
  note?: string
}

export interface ContactGroup {
  id: string
  name: string
  description: string | null
  memberCount: number
  createdAt: string
  updatedAt: string
}

export interface CreateContactGroupRequest {
  name: string
  description?: string
}

export interface UpdateContactGroupRequest {
  name: string
  description?: string
}

export interface UpdateContactGroupMembersRequest {
  contactIds: string[]
}

export interface ContactImportCsvRequest {
  content: string
  mergeDuplicates?: boolean
}

export interface ContactImportResult {
  totalRows: number
  created: number
  updated: number
  skipped: number
  invalid: number
}

export interface ContactDuplicateGroup {
  signature: string
  count: number
  contacts: ContactItem[]
}

export interface CalendarAttendee {
  id: string
  email: string
  displayName: string | null
  responseStatus: string
}

export type CalendarSharePermission = 'VIEW' | 'EDIT' | 'OWNER'
export type CalendarShareResponseStatus = 'NEEDS_ACTION' | 'ACCEPTED' | 'DECLINED'

export interface CalendarEvent {
  id: string
  title: string
  location: string | null
  startAt: string
  endAt: string
  allDay: boolean
  timezone: string
  reminderMinutes: number | null
  attendeeCount: number
  updatedAt: string
  shared: boolean
  ownerEmail: string | null
  sharePermission: CalendarSharePermission
  canEdit: boolean
  canDelete: boolean
}

export interface CalendarEventDetail {
  id: string
  title: string
  description: string | null
  location: string | null
  startAt: string
  endAt: string
  allDay: boolean
  timezone: string
  reminderMinutes: number | null
  attendees: CalendarAttendee[]
  createdAt: string
  updatedAt: string
  shared: boolean
  ownerEmail: string | null
  sharePermission: CalendarSharePermission
  canEdit: boolean
  canDelete: boolean
}

export interface CalendarAgendaItem {
  id: string
  title: string
  location: string | null
  startAt: string
  endAt: string
  attendeeCount: number
  shared: boolean
  ownerEmail: string | null
  sharePermission: CalendarSharePermission
}

export interface ImportCalendarIcsRequest {
  content: string
  timezone?: string
  reminderMinutes?: number | null
}

export interface CalendarImportResult {
  totalCount: number
  importedCount: number
  eventIds: string[]
}

export interface CalendarAttendeeInput {
  email: string
  displayName?: string
}

export interface CreateCalendarEventRequest {
  title: string
  description?: string
  location?: string
  startAt: string
  endAt: string
  allDay?: boolean
  timezone?: string
  reminderMinutes?: number | null
  attendees?: CalendarAttendeeInput[]
}

export interface UpdateCalendarEventRequest {
  title: string
  description?: string
  location?: string
  startAt: string
  endAt: string
  allDay?: boolean
  timezone?: string
  reminderMinutes?: number | null
  attendees?: CalendarAttendeeInput[]
}

export interface CalendarEventShare {
  id: string
  eventId: string
  targetUserId: string
  targetEmail: string
  permission: Exclude<CalendarSharePermission, 'OWNER'>
  responseStatus: CalendarShareResponseStatus
  createdAt: string
  updatedAt: string
}

export interface CalendarIncomingShare {
  shareId: string
  eventId: string
  eventTitle: string
  ownerEmail: string | null
  permission: Exclude<CalendarSharePermission, 'OWNER'>
  responseStatus: CalendarShareResponseStatus
  updatedAt: string
}

export interface CreateCalendarShareRequest {
  targetEmail: string
  permission: Exclude<CalendarSharePermission, 'OWNER'>
}

export interface UpdateCalendarShareRequest {
  permission: Exclude<CalendarSharePermission, 'OWNER'>
}

export type CalendarAvailabilityStatus = 'BUSY' | 'FREE' | 'UNKNOWN'

export interface CalendarAvailabilitySlot {
  startAt: string
  endAt: string
  allDay: boolean
}

export interface CalendarParticipantAvailability {
  email: string
  availability: CalendarAvailabilityStatus
  overlapCount: number
  busySlots: CalendarAvailabilitySlot[]
}

export interface CalendarAvailabilitySummary {
  attendeeCount: number
  busyCount: number
  freeCount: number
  unknownCount: number
  hasConflicts: boolean
}

export interface CalendarAvailability {
  startAt: string
  endAt: string
  summary: CalendarAvailabilitySummary
  attendees: CalendarParticipantAvailability[]
}

export interface QueryCalendarAvailabilityRequest {
  startAt: string
  endAt: string
  attendeeEmails: string[]
  excludeEventId?: string | null
}

export interface RespondCalendarShareRequest {
  response: 'ACCEPT' | 'DECLINE'
}

export type SuitePlanCode = 'FREE' | 'MAIL_PLUS' | 'UNLIMITED'

export interface SuitePlan {
  code: SuitePlanCode
  name: string
  description: string
  mailDailySendLimit: number
  contactLimit: number
  calendarEventLimit: number
  calendarShareLimit: number
  driveStorageMb: number
  enabledProducts: string[]
}

export interface SuiteUsage {
  mailCount: number
  contactCount: number
  calendarEventCount: number
  calendarShareCount: number
  driveFileCount: number
  driveFolderCount: number
  driveStorageBytes: number
}

export interface SuiteSubscription {
  planCode: SuitePlanCode
  planName: string
  status: string
  updatedAt: string
  usage: SuiteUsage
  plan: SuitePlan
}

export interface ChangeSuitePlanRequest {
  planCode: SuitePlanCode
}

export type SuiteProductStatus = 'ENABLED' | 'COMING_SOON' | 'ROADMAP' | 'PREVIEW'
export type SuiteRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type SuiteActionPriority = 'P0' | 'P1' | 'P2'

export interface SuiteProductItem {
  code: string
  name: string
  status: SuiteProductStatus
  category: string
  description: string
  enabledByPlan: boolean
  highlights: string[]
}

export interface SuiteRemediationAction {
  priority: SuiteActionPriority
  productCode: string
  action: string
  actionCode?: string | null
}

export interface SuiteReadinessSignal {
  key: string
  value: number
  note: string
}

export interface SuiteReadinessItem {
  productCode: string
  productName: string
  category: string
  enabledByPlan: boolean
  score: number
  riskLevel: SuiteRiskLevel
  signals: SuiteReadinessSignal[]
  blockers: string[]
  actions: SuiteRemediationAction[]
}

export interface SuiteReadinessReport {
  generatedAt: string
  overallScore: number
  overallRiskLevel: SuiteRiskLevel
  highRiskProductCount: number
  criticalRiskProductCount: number
  items: SuiteReadinessItem[]
}

export interface SuiteSecurityPosture {
  generatedAt: string
  securityScore: number
  overallRiskLevel: SuiteRiskLevel
  activeSessionCount: number
  blockedSenderCount: number
  trustedSenderCount: number
  blockedDomainCount: number
  trustedDomainCount: number
  highRiskProductCount: number
  criticalRiskProductCount: number
  alerts: string[]
  recommendedActions: SuiteRemediationAction[]
}

export interface SuiteUnifiedSearchItem {
  productCode: string
  itemType: string
  entityId: string
  title: string
  summary: string
  routePath: string
  updatedAt: string | null
}

export interface SuiteUnifiedSearchResult {
  generatedAt: string
  keyword: string
  limit: number
  total: number
  items: SuiteUnifiedSearchItem[]
}

export interface SuiteGovernanceOverview {
  generatedAt: string
  totalRequests: number
  pendingReviewCount: number
  pendingSecondReviewCount: number
  approvedPendingExecutionCount: number
  rejectedCount: number
  executedCount: number
  executedWithFailureCount: number
  rolledBackCount: number
  rollbackWithFailureCount: number
  slaBreachedCount: number
}

export interface SuiteCommandItem {
  commandType: string
  label: string
  description: string
  routePath: string
  actionCode: string | null
  productCode: string | null
  priority: string | null
}

export interface SuiteCommandCenter {
  generatedAt: string
  quickRoutes: SuiteCommandItem[]
  pinnedSearches: SuiteCommandItem[]
  recentKeywords: string[]
  recommendedActions: SuiteRemediationAction[]
  pendingGovernanceCount: number
  securityAlertCount: number
}

export interface SuiteCommandFeedItem {
  eventId: number
  eventType: string
  category: string
  title: string
  detail: string
  productCode: string | null
  routePath: string
  ipAddress: string
  createdAt: string
}

export interface SuiteCommandFeed {
  generatedAt: string
  limit: number
  total: number
  items: SuiteCommandFeedItem[]
}

export interface SuiteNotificationItem {
  notificationId: string
  channel: string
  severity: string
  title: string
  message: string
  routePath: string
  actionCode: string | null
  productCode: string | null
  createdAt: string
  read: boolean
  readAt: string | null
  workflowStatus: 'ACTIVE' | 'ARCHIVED' | 'IGNORED' | 'SNOOZED'
  snoozedUntil: string | null
  assignedToUserId: number | null
  assignedToDisplayName: string | null
}

export type SuiteCollaborationProductCode = 'DOCS' | 'DRIVE' | 'SHEETS' | 'MEET'

export interface SuiteCollaborationEvent {
  eventId: number
  productCode: SuiteCollaborationProductCode
  eventType: string
  title: string
  summary: string
  routePath: string
  actorEmail: string | null
  sessionId: string | null
  createdAt: string
}

export interface SuiteCollaborationCenter {
  generatedAt: string
  limit: number
  total: number
  productCounts: Record<string, number>
  syncCursor: number
  syncVersion: string
  items: SuiteCollaborationEvent[]
}

export interface SuiteCollaborationSync {
  kind: 'BOOTSTRAP' | 'SYNC' | 'UPDATE'
  generatedAt: string
  syncCursor: number
  syncVersion: string
  hasUpdates: boolean
  total: number
  items: SuiteCollaborationEvent[]
}

export interface SuiteNotificationCenter {
  generatedAt: string
  limit: number
  total: number
  criticalCount: number
  unreadCount: number
  syncCursor: number
  syncVersion: string
  items: SuiteNotificationItem[]
}

export interface SuiteNotificationOperationHistoryItem {
  operationId: string
  operation: string
  requestedCount: number
  affectedCount: number
  executedAt: string
  undoAvailable: boolean
}

export interface SuiteNotificationOperationHistory {
  generatedAt: string
  limit: number
  total: number
  syncCursor: number
  syncVersion: string
  items: SuiteNotificationOperationHistoryItem[]
}

export interface SuiteNotificationSyncEvent {
  eventId: number
  eventType: string
  operation: string
  operationId: string | null
  requestedCount: number
  affectedCount: number
  sessionId: string | null
  createdAt: string
}

export interface SuiteNotificationSync {
  kind: 'BOOTSTRAP' | 'SYNC' | 'UPDATE'
  generatedAt: string
  syncCursor: number
  syncVersion: string
  hasUpdates: boolean
  total: number
  items: SuiteNotificationSyncEvent[]
}

export interface SuiteWebPushStatus {
  enabled: boolean
  deliveryScope: string
  vapidPublicKey: string | null
  message: string | null
}

export interface SuiteWebPushSubscribeRequest {
  endpoint: string
  p256dh: string
  auth: string
  contentEncoding: string
  userAgent?: string
}

export interface SuiteWebPushUnsubscribeRequest {
  endpoint: string
}

export interface MarkSuiteNotificationsReadRequest {
  notificationIds: string[]
}

export interface SnoozeSuiteNotificationsRequest {
  notificationIds: string[]
  snoozedUntil: string
}

export interface AssignSuiteNotificationsRequest {
  notificationIds: string[]
  assigneeUserId: number
  assigneeDisplayName: string
}

export interface UndoSuiteNotificationWorkflowRequest {
  operationId: string
}

export interface SuiteNotificationMarkReadResult {
  executedAt: string
  requestedCount: number
  affectedCount: number
  syncCursor: number
  syncVersion: string
}

export interface SuiteNotificationWorkflowResult {
  executedAt: string
  operation: string
  requestedCount: number
  affectedCount: number
  operationId: string
  syncCursor: number
  syncVersion: string
}

export interface SuiteRemediationExecutionResult {
  actionCode: string
  productCode: string
  status: 'SUCCESS' | 'NO_OP' | 'FAILED'
  message: string
  executedAt: string
  details: Record<string, unknown>
}

export interface BatchExecuteSuiteRemediationActionsRequest {
  actionCodes: string[]
}

export interface SuiteBatchRemediationExecutionItem {
  actionCode: string
  success: boolean
  errorCode: number | null
  message: string
  executionResult: SuiteRemediationExecutionResult | null
}

export interface SuiteBatchRemediationExecutionResult {
  generatedAt: string
  totalCount: number
  successCount: number
  failedCount: number
  items: SuiteBatchRemediationExecutionItem[]
}

export type SuiteGovernanceRequestStatus =
  | 'PENDING_REVIEW'
  | 'PENDING_SECOND_REVIEW'
  | 'APPROVED_PENDING_EXECUTION'
  | 'REJECTED'
  | 'EXECUTED'
  | 'EXECUTED_WITH_FAILURE'
  | 'ROLLED_BACK'
  | 'ROLLBACK_WITH_FAILURE'

export type SuiteGovernanceReviewStage =
  | 'SINGLE_REVIEW'
  | 'FIRST_REVIEW_PENDING'
  | 'SECOND_REVIEW_PENDING'
  | 'REVIEW_COMPLETED'

export interface BatchReviewSuiteGovernanceChangeRequestsRequest {
  requestIds: string[]
  decision: 'APPROVE' | 'REJECT'
  reviewNote?: string
}

export interface SuiteBatchGovernanceReviewItem {
  requestId: string
  success: boolean
  errorCode: number | null
  message: string
  result: SuiteGovernanceChangeRequest | null
}

export interface SuiteBatchGovernanceReviewResult {
  generatedAt: string
  decision: 'APPROVE' | 'REJECT'
  totalCount: number
  successCount: number
  failedCount: number
  items: SuiteBatchGovernanceReviewItem[]
}

export interface SuiteGovernancePolicyTemplate {
  templateCode: string
  name: string
  riskLevel: SuiteRiskLevel
  description: string
  actionCodes: string[]
  rollbackActionCodes: string[]
  approvalRequired: boolean
}

export interface SuiteGovernanceChangeRequest {
  requestId: string
  orgId: string | null
  ownerId: string
  templateCode: string
  templateName: string
  status: SuiteGovernanceRequestStatus
  reason: string
  requireDualReview: boolean
  reviewStage: SuiteGovernanceReviewStage
  firstReviewNote: string | null
  firstReviewedAt: string | null
  firstReviewedByUserId: string | null
  firstReviewedBySessionId: string | null
  secondReviewerUserId: string | null
  reviewNote: string | null
  approvalNote: string | null
  rollbackReason: string | null
  requestedAt: string
  reviewDueAt: string | null
  reviewSlaBreached: boolean
  reviewedAt: string | null
  reviewedByUserId: string | null
  reviewedBySessionId: string | null
  approvedAt: string | null
  executedAt: string | null
  executedByUserId: string | null
  executedBySessionId: string | null
  rolledBackAt: string | null
  actionCodes: string[]
  rollbackActionCodes: string[]
  executionResults: SuiteRemediationExecutionResult[]
  rollbackResults: SuiteRemediationExecutionResult[]
}

export interface CreateSuiteGovernanceChangeRequestRequest {
  templateCode: string
  reason: string
  orgId?: string
  secondReviewerUserId?: string
}

export interface ApproveSuiteGovernanceChangeRequestRequest {
  requestId: string
  approvalNote?: string
}

export interface ReviewSuiteGovernanceChangeRequestRequest {
  requestId: string
  decision: 'APPROVE' | 'REJECT'
  reviewNote?: string
}

export interface RollbackSuiteGovernanceChangeRequestRequest {
  requestId: string
  rollbackReason: string
}

export type DriveItemType = 'FOLDER' | 'FILE'
export type DriveSharePermission = 'VIEW' | 'EDIT'
export type DriveShareStatus = 'ACTIVE' | 'REVOKED'
export type DriveSavedShareStatus = 'ACTIVE' | 'REVOKED' | 'EXPIRED' | 'UNAVAILABLE'
export type DriveCollaboratorShareStatus = 'NEEDS_ACTION' | 'ACCEPTED' | 'DECLINED' | 'REVOKED'
export type DrivePreviewKind = 'TEXT' | 'IMAGE' | 'PDF' | 'UNSUPPORTED'

export interface DriveFileE2ee {
  enabled: boolean
  algorithm: string
  recipientFingerprints: string[]
}

export interface DriveShareE2ee {
  enabled: boolean
  algorithm: string
  mode?: 'PASSWORD'
}

export interface DriveUploadE2eePayload {
  enabled: true
  algorithm: string
  recipientFingerprints: string[]
  fileName: string
  contentType: string
  fileSize: number
}

export interface DriveItem {
  id: string
  parentId: string | null
  itemType: DriveItemType
  name: string
  mimeType: string | null
  sizeBytes: number
  shareCount: number
  e2ee?: DriveFileE2ee | null
  createdAt: string
  updatedAt: string
}

export interface DriveShareLink {
  id: string
  itemId: string
  token: string
  permission: DriveSharePermission
  expiresAt: string | null
  status: DriveShareStatus
  passwordProtected: boolean
  e2ee?: DriveShareE2ee | null
  createdAt: string
  updatedAt: string
}

export interface DriveCollaboratorShare {
  shareId: string
  collaboratorUserId: string
  collaboratorEmail: string
  collaboratorDisplayName: string
  permission: DriveSharePermission
  responseStatus: DriveCollaboratorShareStatus
  createdAt: string
  updatedAt: string
}

export interface DriveIncomingCollaboratorShare {
  shareId: string
  itemId: string
  itemType: DriveItemType
  itemName: string
  ownerEmail: string
  ownerDisplayName: string | null
  permission: DriveSharePermission
  responseStatus: DriveCollaboratorShareStatus
  updatedAt: string
}

export interface DriveCollaboratorSharedItem {
  shareId: string
  itemId: string
  itemType: DriveItemType
  itemName: string
  ownerEmail: string
  ownerDisplayName: string | null
  permission: DriveSharePermission
  status: DriveCollaboratorShareStatus
  updatedAt: string
  available: boolean
}

export interface DriveSavedShare {
  id: string
  shareId: string
  token: string
  itemId: string
  itemType: DriveItemType
  itemName: string
  ownerEmail: string
  ownerDisplayName: string | null
  permission: DriveSharePermission
  status: DriveSavedShareStatus
  expiresAt: string | null
  e2ee?: DriveShareE2ee | null
  savedAt: string
  available: boolean
}

export interface DriveTrashItem {
  id: string
  parentId: string | null
  itemType: DriveItemType
  name: string
  mimeType: string | null
  sizeBytes: number
  trashedAt: string | null
  purgeAfterAt: string | null
  updatedAt: string
}

export interface DriveShareAccessLog {
  id: string
  shareId: string | null
  itemId: string | null
  action: 'METADATA' | 'DOWNLOAD' | 'PREVIEW'
  accessStatus: string
  ipAddress: string | null
  userAgent: string | null
  createdAt: string
}

export interface DriveFileVersion {
  id: string
  itemId: string
  versionNo: number
  mimeType: string | null
  sizeBytes: number
  checksum: string | null
  e2ee?: DriveFileE2ee | null
  createdAt: string
}

export interface DriveVersionCleanupResult {
  deletedVersions: number
  remainingVersions: number
  appliedRetentionCount: number
  appliedRetentionDays: number
}

export interface DriveBatchActionFailedItem {
  itemId: string
  reason: string
}

export interface DriveBatchActionResult {
  requestedCount: number
  successCount: number
  failedCount: number
  failedItems: DriveBatchActionFailedItem[]
}

export interface DriveBatchShareResult {
  requestedCount: number
  successCount: number
  failedCount: number
  createdShares: DriveShareLink[]
  failedItems: DriveBatchActionFailedItem[]
}

export interface DriveUsage {
  fileCount: number
  folderCount: number
  storageBytes: number
  storageLimitBytes: number
}

export interface ListDriveItemsParams {
  parentId?: string | null
  keyword?: string
  itemType?: DriveItemType | ''
  limit?: number
}

export interface CreateDriveFolderRequest {
  name: string
  parentId?: string | null
}

export interface BatchCreateDriveShareRequest {
  itemIds: string[]
  permission: DriveSharePermission
  expiresAt?: string
  password?: string
}

export interface CreateDriveCollaboratorShareRequest {
  targetEmail: string
  permission: DriveSharePermission
}

export interface UpdateDriveCollaboratorShareRequest {
  permission: DriveSharePermission
}

export interface RespondDriveCollaboratorShareRequest {
  response: 'ACCEPT' | 'DECLINE'
}

export interface CreateDriveFileRequest {
  name: string
  parentId?: string | null
  mimeType?: string
  sizeBytes: number
  storagePath?: string
  checksum?: string
}

export interface MoveDriveItemRequest {
  parentId?: string | null
}

export interface CreateDriveShareRequest {
  permission: DriveSharePermission
  expiresAt?: string
  password?: string
}

export interface DriveEncryptedPublicShareE2eePayload {
  enabled: true
  algorithm: string
  mode: 'PASSWORD'
  fileName: string
  contentType: string
  fileSize: number
}

export interface CreateEncryptedPublicShareRequest {
  permission: Extract<DriveSharePermission, 'VIEW'>
  expiresAt?: string
  password: string
  encryptedFile: File
  e2ee: DriveEncryptedPublicShareE2eePayload
}

export interface UpdateDriveShareRequest {
  permission: DriveSharePermission
  expiresAt?: string | null
  password?: string
  clearPassword?: boolean
}

export interface PublicDriveShareMetadata {
  shareId: string
  token: string
  itemId: string
  itemType: DriveItemType
  itemName: string
  mimeType: string | null
  sizeBytes: number
  permission: DriveSharePermission
  status: DriveShareStatus
  expiresAt: string | null
  passwordProtected: boolean
  e2ee?: DriveShareE2ee | null
}

export type OrgRole = 'OWNER' | 'ADMIN' | 'MEMBER'
export type OrgMemberStatus = 'INVITED' | 'ACTIVE' | 'DECLINED' | 'DISABLED'

export interface OrgWorkspace {
  id: string
  name: string
  slug: string
  role: OrgRole
  status: OrgMemberStatus
  memberCount: number
  updatedAt: string
}

export interface OrgMember {
  id: string
  orgId: string
  userId: string | null
  userEmail: string
  role: OrgRole
  status: OrgMemberStatus
  invitedBy: string | null
  joinedAt: string | null
  updatedAt: string
}

export interface OrgIncomingInvite {
  inviteId: string
  orgId: string
  orgName: string
  orgSlug: string
  role: Exclude<OrgRole, 'OWNER'>
  status: OrgMemberStatus
  invitedByEmail: string | null
  updatedAt: string
}

export interface CreateOrgRequest {
  name: string
}

export interface InviteOrgMemberRequest {
  email: string
  role: Exclude<OrgRole, 'OWNER'>
}

export interface RespondOrgInviteRequest {
  response: 'ACCEPT' | 'DECLINE'
}

export interface UpdateOrgMemberRoleRequest {
  role: Exclude<OrgRole, 'OWNER'>
}

export interface UpdateOrgMemberStatusRequest {
  status: Extract<OrgMemberStatus, 'ACTIVE' | 'DISABLED'>
}

export interface OrgPolicy {
  orgId: string
  allowedEmailDomains: string[]
  memberLimit: number
  governanceReviewSlaHours: number
  adminCanInviteAdmin: boolean
  adminCanRemoveAdmin: boolean
  adminCanReviewGovernance: boolean
  adminCanExecuteGovernance: boolean
  requireDualReviewGovernance: boolean
  updatedAt: string | null
}

export interface UpdateOrgPolicyRequest {
  allowedEmailDomains?: string[]
  memberLimit?: number
  governanceReviewSlaHours?: number
  adminCanInviteAdmin?: boolean
  adminCanRemoveAdmin?: boolean
  adminCanReviewGovernance?: boolean
  adminCanExecuteGovernance?: boolean
  requireDualReviewGovernance?: boolean
}

export interface BatchUpdateOrgMemberRoleRequest {
  memberIds: string[]
  role: Exclude<OrgRole, 'OWNER'>
}

export interface BatchRemoveOrgMembersRequest {
  memberIds: string[]
}

export interface OrgBatchFailure {
  memberId: string
  reason: string
}

export interface OrgBatchActionResult {
  requestedCount: number
  successIds: string[]
  failedItems: OrgBatchFailure[]
}

export interface OrgAuditEvent {
  id: string
  orgId: string | null
  actorId: string | null
  actorEmail: string | null
  eventType: string
  ipAddress: string
  detail: string
  createdAt: string
}

export interface MailSenderIdentity {
  identityId: string | null
  orgId: string | null
  orgName: string | null
  memberId: string | null
  emailAddress: string
  displayName: string | null
  source: MailIdentitySource
  status: MailIdentityStatus
  defaultIdentity: boolean
}

export type MailE2eeRecipientReadiness = 'READY' | 'NOT_READY' | 'UNDELIVERABLE'

export interface MailE2eeRecipientRouteStatus {
  targetEmail: string
  forwardToEmail: string
  keyAvailable: boolean
  fingerprint: string | null
  algorithm: string | null
  publicKeyArmored: string | null
}

export interface MailE2eeRecipientStatus {
  toEmail: string
  fromEmail: string
  deliverable: boolean
  encryptionReady: boolean
  readiness: MailE2eeRecipientReadiness
  routeCount: number
  routes: MailE2eeRecipientRouteStatus[]
}

export interface SendMailRequest {
  draftId?: MailId
  toEmail: string
  fromEmail?: string
  subject: string
  body?: string
  idempotencyKey: string
  labels: string[]
  scheduledAt?: string
  e2ee?: MailBodyE2eePayload
}

export interface MailBodyE2eePayload {
  encryptedBody: string
  algorithm: string
  recipientFingerprints: string[]
}

export interface MailAttachmentE2ee {
  enabled: boolean
  algorithm: string | null
  recipientFingerprints: string[]
}

export interface MailAttachmentE2eePayload {
  algorithm: string
  recipientFingerprints: string[]
}

export interface UploadDraftAttachmentOptions {
  file: Blob | File
  fileName: string
  contentType: string
  fileSize: number
  e2ee?: MailAttachmentE2eePayload
}

export interface DraftRequest {
  draftId?: MailId
  toEmail: string
  fromEmail?: string
  subject: string
  body?: string
  e2ee?: MailBodyE2eePayload
}

export interface LabelItem {
  id: number
  name: string
  color: string
}

export interface SearchMailParams {
  keyword?: string
  folder?: SystemMailFolder | ''
  unread?: boolean | null
  starred?: boolean | null
  from?: string
  to?: string
  label?: string
  page?: number
  size?: number
}

export interface SearchPreset {
  id: string
  name: string
  keyword: string | null
  folder: SystemMailFolder | null
  unread: boolean | null
  starred: boolean | null
  from: string | null
  to: string | null
  label: string | null
  isPinned: boolean
  pinnedAt: string | null
  usageCount: number
  lastUsedAt: string | null
}

export interface CreateSearchPresetRequest {
  name: string
  keyword?: string
  folder?: SystemMailFolder | ''
  unread?: boolean | null
  starred?: boolean | null
  from?: string
  to?: string
  label?: string
}

export interface UpdateSearchPresetRequest {
  name: string
  keyword?: string
  folder?: SystemMailFolder | ''
  unread?: boolean | null
  starred?: boolean | null
  from?: string
  to?: string
  label?: string
}

export interface SearchHistoryItem {
  id: string
  keyword: string
  usageCount: number
  lastUsedAt: string
}

export type PreferredLocale = 'en' | 'zh-CN' | 'zh-TW'
export type MailAddressMode = 'PROTON_ADDRESS' | 'EXTERNAL_ACCOUNT'

export interface UserPreference {
  displayName: string
  signature: string
  timezone: string
  preferredLocale: PreferredLocale
  mailAddressMode: MailAddressMode
  autoSaveSeconds: number
  undoSendSeconds: number
  driveVersionRetentionCount: number
  driveVersionRetentionDays: number
}

export interface UpdateUserPreferenceRequest {
  displayName: string
  signature: string
  timezone: string
  preferredLocale?: PreferredLocale
  mailAddressMode?: MailAddressMode
  autoSaveSeconds: number
  undoSendSeconds: number
  driveVersionRetentionCount?: number
  driveVersionRetentionDays?: number
}

export interface MailE2eeKeyProfile {
  enabled: boolean
  fingerprint: string | null
  algorithm: string | null
  publicKeyArmored: string | null
  encryptedPrivateKeyArmored: string | null
  keyCreatedAt: string | null
}

export interface MailE2eeRecoveryPackage {
  enabled: boolean
  encryptedPrivateKeyArmored: string | null
  updatedAt: string | null
}

export interface UpdateMailE2eeKeyProfileRequest {
  enabled: boolean
  publicKeyArmored?: string
  encryptedPrivateKeyArmored?: string
  fingerprint?: string
  algorithm?: string
  keyCreatedAt?: string
}

export interface UpdateMailE2eeRecoveryRequest {
  enabled: boolean
  encryptedPrivateKeyArmored?: string
}

export interface AuditEvent {
  id: number
  eventType: string
  ipAddress: string
  detail: string
  createdAt: string
}

export interface UserSession {
  id: string
  createdAt: string
  expiresAt: string
  current: boolean
}

export interface BlockedSender {
  id: string
  email: string
  createdAt: string
}

export interface TrustedSender {
  id: string
  email: string
  createdAt: string
}

export interface BlockedDomain {
  id: string
  domain: string
  createdAt: string
}

export interface TrustedDomain {
  id: string
  domain: string
  createdAt: string
}

export interface RuleResolution {
  senderEmail: string
  senderDomain: string
  trustedSender: boolean
  blockedSender: boolean
  trustedDomain: boolean
  blockedDomain: boolean
  effectiveFolder: MailFolder
  reason: 'TRUSTED_SENDER' | 'BLOCKED_SENDER' | 'TRUSTED_DOMAIN' | 'BLOCKED_DOMAIN' | 'DEFAULT'
  matchedRule: string | null
}
