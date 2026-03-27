package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.AuthenticatorEntryMapper;
import com.mmmail.server.mapper.BlockedDomainMapper;
import com.mmmail.server.mapper.BlockedSenderMapper;
import com.mmmail.server.mapper.CalendarEventMapper;
import com.mmmail.server.mapper.DocsNoteMapper;
import com.mmmail.server.mapper.DriveItemMapper;
import com.mmmail.server.mapper.OrgCustomDomainMapper;
import com.mmmail.server.mapper.OrgMemberMapper;
import com.mmmail.server.mapper.LumoMessageMapper;
import com.mmmail.server.mapper.LumoProjectKnowledgeMapper;
import com.mmmail.server.mapper.LumoProjectMapper;
import com.mmmail.server.mapper.MailMessageMapper;
import com.mmmail.server.mapper.SheetsWorkbookMapper;
import com.mmmail.server.mapper.StandardNoteProfileMapper;
import com.mmmail.server.mapper.StandardNoteFolderMapper;
import com.mmmail.server.mapper.MeetQualitySnapshotMapper;
import com.mmmail.server.mapper.PassAliasContactMapper;
import com.mmmail.server.mapper.PassMailAliasMapper;
import com.mmmail.server.mapper.PassMailboxMapper;
import com.mmmail.server.mapper.SimpleLoginRelayPolicyMapper;
import com.mmmail.server.mapper.MeetRoomSessionMapper;
import com.mmmail.server.mapper.PassVaultItemMapper;
import com.mmmail.server.mapper.SuiteGovernanceRequestMapper;
import com.mmmail.server.mapper.TrustedDomainMapper;
import com.mmmail.server.mapper.TrustedSenderMapper;
import com.mmmail.server.mapper.UserSessionMapper;
import com.mmmail.server.mapper.VpnConnectionSessionMapper;
import com.mmmail.server.mapper.WalletAccountMapper;
import com.mmmail.server.mapper.WalletTransactionMapper;
import com.mmmail.server.model.entity.AuthenticatorEntry;
import com.mmmail.server.model.entity.BlockedDomain;
import com.mmmail.server.model.entity.BlockedSender;
import com.mmmail.server.model.entity.CalendarEvent;
import com.mmmail.server.model.entity.DocsNote;
import com.mmmail.server.model.entity.DriveItem;
import com.mmmail.server.model.entity.OrgCustomDomain;
import com.mmmail.server.model.entity.OrgMember;
import com.mmmail.server.model.entity.LumoMessage;
import com.mmmail.server.model.entity.LumoProject;
import com.mmmail.server.model.entity.LumoProjectKnowledge;
import com.mmmail.server.model.entity.MailMessage;
import com.mmmail.server.model.entity.SheetsWorkbook;
import com.mmmail.server.model.entity.StandardNoteProfile;
import com.mmmail.server.model.entity.StandardNoteFolder;
import com.mmmail.server.model.entity.MeetQualitySnapshot;
import com.mmmail.server.model.entity.MeetRoomSession;
import com.mmmail.server.model.entity.PassAliasContact;
import com.mmmail.server.model.entity.PassMailAlias;
import com.mmmail.server.model.entity.PassMailbox;
import com.mmmail.server.model.entity.SimpleLoginRelayPolicy;
import com.mmmail.server.model.entity.PassVaultItem;
import com.mmmail.server.model.entity.SuiteGovernanceRequest;
import com.mmmail.server.model.entity.TrustedDomain;
import com.mmmail.server.model.entity.TrustedSender;
import com.mmmail.server.model.entity.UserSession;
import com.mmmail.server.model.entity.VpnConnectionSession;
import com.mmmail.server.model.entity.WalletAccount;
import com.mmmail.server.model.entity.WalletTransaction;
import com.mmmail.server.model.vo.PassItemSummaryVo;
import com.mmmail.server.model.vo.SuiteGovernanceChangeRequestVo;
import com.mmmail.server.model.vo.SuiteGovernanceOverviewVo;
import com.mmmail.server.model.vo.SuiteGovernancePolicyTemplateVo;
import com.mmmail.server.model.vo.SuiteProductStatusVo;
import com.mmmail.server.model.vo.SuiteReadinessItemVo;
import com.mmmail.server.model.vo.SuiteReadinessReportVo;
import com.mmmail.server.model.vo.SuiteReadinessSignalVo;
import com.mmmail.server.model.vo.SuiteRemediationActionVo;
import com.mmmail.server.model.vo.SuiteRemediationExecutionResultVo;
import com.mmmail.server.model.vo.SuiteSecurityPostureVo;
import com.mmmail.server.model.vo.SuiteSubscriptionVo;
import com.mmmail.server.model.vo.SuiteUnifiedSearchItemVo;
import com.mmmail.server.model.vo.SuiteUnifiedSearchResultVo;
import com.mmmail.server.model.vo.VpnServerVo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class SuiteInsightService {

    private static final String RISK_LOW = "LOW";
    private static final String RISK_MEDIUM = "MEDIUM";
    private static final String RISK_HIGH = "HIGH";
    private static final String RISK_CRITICAL = "CRITICAL";
    private static final String PRODUCT_SECURITY = "SECURITY";
    private static final String ACTION_MAIL_ADD_BLOCKED_DOMAIN_BASELINE = "MAIL_ADD_BLOCKED_DOMAIN_BASELINE";
    private static final String ACTION_MAIL_ADD_BLOCKED_SENDER_BASELINE = "MAIL_ADD_BLOCKED_SENDER_BASELINE";
    private static final String ACTION_MAIL_ADD_TRUSTED_DOMAIN_BASELINE = "MAIL_ADD_TRUSTED_DOMAIN_BASELINE";
    private static final String ACTION_MAIL_ADD_TRUSTED_SENDER_BASELINE = "MAIL_ADD_TRUSTED_SENDER_BASELINE";
    private static final String ACTION_MAIL_REMOVE_BLOCKED_DOMAIN_BASELINE = "MAIL_REMOVE_BLOCKED_DOMAIN_BASELINE";
    private static final String ACTION_MAIL_REMOVE_BLOCKED_SENDER_BASELINE = "MAIL_REMOVE_BLOCKED_SENDER_BASELINE";
    private static final String ACTION_MAIL_REMOVE_TRUSTED_DOMAIN_BASELINE = "MAIL_REMOVE_TRUSTED_DOMAIN_BASELINE";
    private static final String ACTION_MAIL_REMOVE_TRUSTED_SENDER_BASELINE = "MAIL_REMOVE_TRUSTED_SENDER_BASELINE";
    private static final String ACTION_PASS_CREATE_BASELINE_ITEM = "PASS_CREATE_BASELINE_ITEM";
    private static final String ACTION_PASS_DELETE_BASELINE_ITEM = "PASS_DELETE_BASELINE_ITEM";
    private static final String ACTION_VPN_CONNECT_SECURE_CORE_BASELINE = "VPN_CONNECT_SECURE_CORE_BASELINE";
    private static final String ACTION_VPN_DISCONNECT_CURRENT = "VPN_DISCONNECT_CURRENT";
    private static final String ACTION_WALLET_BATCH_ADVANCE = "WALLET_BATCH_ADVANCE";
    private static final String ACTION_WALLET_BATCH_RECONCILE = "WALLET_BATCH_RECONCILE";
    private static final String EXECUTION_SUCCESS = "SUCCESS";
    private static final String EXECUTION_NO_OP = "NO_OP";
    private static final String EXECUTION_FAILED = "FAILED";
    private static final String GOVERNANCE_STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    private static final String GOVERNANCE_STATUS_PENDING_SECOND_REVIEW = "PENDING_SECOND_REVIEW";
    private static final String GOVERNANCE_STATUS_APPROVED_PENDING_EXECUTION = "APPROVED_PENDING_EXECUTION";
    private static final String GOVERNANCE_STATUS_REJECTED = "REJECTED";
    private static final String GOVERNANCE_STATUS_EXECUTED = "EXECUTED";
    private static final String GOVERNANCE_STATUS_EXECUTED_WITH_FAILURE = "EXECUTED_WITH_FAILURE";
    private static final String GOVERNANCE_STATUS_ROLLED_BACK = "ROLLED_BACK";
    private static final String GOVERNANCE_STATUS_ROLLBACK_WITH_FAILURE = "ROLLBACK_WITH_FAILURE";
    private static final String GOVERNANCE_EXECUTION_SESSION_CONFLICT_MESSAGE =
            "Execution requires a different authenticated session than reviewer";
    private static final String GOVERNANCE_REVIEW_DECISION_APPROVE = "APPROVE";
    private static final String GOVERNANCE_REVIEW_DECISION_REJECT = "REJECT";
    private static final String GOVERNANCE_SECOND_REVIEW_USER_CONFLICT_MESSAGE =
            "Second review must be completed by a different user";
    private static final String GOVERNANCE_SECOND_REVIEW_ASSIGNMENT_REQUIRED_MESSAGE =
            "Only designated second reviewer can complete second review";
    private static final String GOVERNANCE_SECOND_REVIEW_ASSIGNMENT_FIRST_REVIEW_CONFLICT_MESSAGE =
            "Designated second reviewer cannot perform first review";
    private static final String GOVERNANCE_SECOND_REVIEW_ASSIGNMENT_INVALID_MESSAGE =
            "Designated second reviewer must be OWNER or governance-enabled ADMIN in the organization";
    private static final String ORG_ROLE_OWNER = "OWNER";
    private static final String ORG_ROLE_ADMIN = "ADMIN";
    private static final String ORG_GOVERNANCE_REVIEW_FORBIDDEN_MESSAGE = "No permission to review organization governance request";
    private static final String ORG_GOVERNANCE_EXECUTION_FORBIDDEN_MESSAGE = "No permission to execute organization governance request";
    private static final String ORG_GOVERNANCE_CREATE_FORBIDDEN_MESSAGE = "Only OWNER or ADMIN can create organization governance request";
    private static final String GOVERNANCE_TEMPLATE_SECURITY_BASELINE_HARDENING = "SECURITY_BASELINE_HARDENING";
    private static final String GOVERNANCE_TEMPLATE_ACCOUNT_ACCESS_CONTAINMENT = "ACCOUNT_ACCESS_CONTAINMENT";
    private static final String BASELINE_BLOCKED_DOMAIN = "phishing-simulation.local";
    private static final String BASELINE_BLOCKED_SENDER = "suspicious@phishing-simulation.local";
    private static final String BASELINE_TRUSTED_DOMAIN = "trusted-partner.local";
    private static final String BASELINE_TRUSTED_SENDER = "security-team@trusted-partner.local";
    private static final String BASELINE_PASS_TITLE = "MMMail Security Baseline";
    private static final String BASELINE_PASS_WEBSITE = "https://security.mmmail.local";
    private static final String BASELINE_PASS_USERNAME = "security-admin";
    private static final int REMEDIATION_WALLET_MAX_ITEMS = 5;
    private static final int DEFAULT_UNIFIED_SEARCH_LIMIT = 20;
    private static final int MAX_UNIFIED_SEARCH_LIMIT = 80;
    private static final int UNIFIED_SEARCH_SUMMARY_MAX_LENGTH = 80;
    private static final int UNIFIED_SEARCH_TITLE_MAX_LENGTH = 100;
    private static final int BOOLEAN_TRUE = 1;
    private static final int AUDIT_KEYWORD_MAX_LENGTH = 36;
    private static final Map<String, GovernanceTemplateDefinition> GOVERNANCE_TEMPLATE_DEFINITIONS = buildGovernanceTemplates();

    private final SuiteService suiteService;
    private final OrgService orgService;
    private final OrgMemberMapper orgMemberMapper;
    private final OrgCustomDomainMapper orgCustomDomainMapper;
    private final MailMessageMapper mailMessageMapper;
    private final SheetsWorkbookMapper sheetsWorkbookMapper;
    private final DocsNoteMapper docsNoteMapper;
    private final StandardNoteProfileMapper standardNoteProfileMapper;
    private final StandardNoteFolderMapper standardNoteFolderMapper;
    private final DriveItemMapper driveItemMapper;
    private final PassVaultItemMapper passVaultItemMapper;
    private final PassMailAliasMapper passMailAliasMapper;
    private final PassMailboxMapper passMailboxMapper;
    private final SimpleLoginRelayPolicyMapper simpleLoginRelayPolicyMapper;
    private final PassAliasContactMapper passAliasContactMapper;
    private final CalendarEventMapper calendarEventMapper;
    private final AuthenticatorEntryMapper authenticatorEntryMapper;
    private final VpnConnectionSessionMapper vpnConnectionSessionMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final LumoProjectMapper lumoProjectMapper;
    private final LumoMessageMapper lumoMessageMapper;
    private final LumoProjectKnowledgeMapper lumoProjectKnowledgeMapper;
    private final MeetRoomSessionMapper meetRoomSessionMapper;
    private final MeetQualitySnapshotMapper meetQualitySnapshotMapper;
    private final UserSessionMapper userSessionMapper;
    private final SuiteGovernanceRequestMapper suiteGovernanceRequestMapper;
    private final BlockedSenderMapper blockedSenderMapper;
    private final TrustedSenderMapper trustedSenderMapper;
    private final BlockedDomainMapper blockedDomainMapper;
    private final TrustedDomainMapper trustedDomainMapper;
    private final WalletAccountMapper walletAccountMapper;
    private final BlockedDomainService blockedDomainService;
    private final BlockedSenderService blockedSenderService;
    private final TrustedDomainService trustedDomainService;
    private final TrustedSenderService trustedSenderService;
    private final PassService passService;
    private final VpnService vpnService;
    private final WalletService walletService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final StandardNotesChecklistCodec standardNotesChecklistCodec;

    public SuiteInsightService(
            SuiteService suiteService,
            OrgService orgService,
            OrgMemberMapper orgMemberMapper,
            OrgCustomDomainMapper orgCustomDomainMapper,
            MailMessageMapper mailMessageMapper,
            SheetsWorkbookMapper sheetsWorkbookMapper,
            DocsNoteMapper docsNoteMapper,
            StandardNoteProfileMapper standardNoteProfileMapper,
            StandardNoteFolderMapper standardNoteFolderMapper,
            DriveItemMapper driveItemMapper,
            PassVaultItemMapper passVaultItemMapper,
            PassMailAliasMapper passMailAliasMapper,
            PassMailboxMapper passMailboxMapper,
            SimpleLoginRelayPolicyMapper simpleLoginRelayPolicyMapper,
            PassAliasContactMapper passAliasContactMapper,
            CalendarEventMapper calendarEventMapper,
            AuthenticatorEntryMapper authenticatorEntryMapper,
            VpnConnectionSessionMapper vpnConnectionSessionMapper,
            WalletTransactionMapper walletTransactionMapper,
            LumoProjectMapper lumoProjectMapper,
            LumoMessageMapper lumoMessageMapper,
            LumoProjectKnowledgeMapper lumoProjectKnowledgeMapper,
            MeetRoomSessionMapper meetRoomSessionMapper,
            MeetQualitySnapshotMapper meetQualitySnapshotMapper,
            UserSessionMapper userSessionMapper,
            SuiteGovernanceRequestMapper suiteGovernanceRequestMapper,
            BlockedSenderMapper blockedSenderMapper,
            TrustedSenderMapper trustedSenderMapper,
            BlockedDomainMapper blockedDomainMapper,
            TrustedDomainMapper trustedDomainMapper,
            WalletAccountMapper walletAccountMapper,
            BlockedDomainService blockedDomainService,
            BlockedSenderService blockedSenderService,
            TrustedDomainService trustedDomainService,
            TrustedSenderService trustedSenderService,
            PassService passService,
            VpnService vpnService,
            WalletService walletService,
            AuditService auditService,
            ObjectMapper objectMapper,
            StandardNotesChecklistCodec standardNotesChecklistCodec
    ) {
        this.suiteService = suiteService;
        this.orgService = orgService;
        this.orgMemberMapper = orgMemberMapper;
        this.orgCustomDomainMapper = orgCustomDomainMapper;
        this.mailMessageMapper = mailMessageMapper;
        this.sheetsWorkbookMapper = sheetsWorkbookMapper;
        this.docsNoteMapper = docsNoteMapper;
        this.standardNoteProfileMapper = standardNoteProfileMapper;
        this.standardNoteFolderMapper = standardNoteFolderMapper;
        this.driveItemMapper = driveItemMapper;
        this.passVaultItemMapper = passVaultItemMapper;
        this.passMailAliasMapper = passMailAliasMapper;
        this.passMailboxMapper = passMailboxMapper;
        this.simpleLoginRelayPolicyMapper = simpleLoginRelayPolicyMapper;
        this.passAliasContactMapper = passAliasContactMapper;
        this.calendarEventMapper = calendarEventMapper;
        this.authenticatorEntryMapper = authenticatorEntryMapper;
        this.vpnConnectionSessionMapper = vpnConnectionSessionMapper;
        this.walletTransactionMapper = walletTransactionMapper;
        this.lumoProjectMapper = lumoProjectMapper;
        this.lumoMessageMapper = lumoMessageMapper;
        this.lumoProjectKnowledgeMapper = lumoProjectKnowledgeMapper;
        this.meetRoomSessionMapper = meetRoomSessionMapper;
        this.meetQualitySnapshotMapper = meetQualitySnapshotMapper;
        this.userSessionMapper = userSessionMapper;
        this.suiteGovernanceRequestMapper = suiteGovernanceRequestMapper;
        this.blockedSenderMapper = blockedSenderMapper;
        this.trustedSenderMapper = trustedSenderMapper;
        this.blockedDomainMapper = blockedDomainMapper;
        this.trustedDomainMapper = trustedDomainMapper;
        this.walletAccountMapper = walletAccountMapper;
        this.blockedDomainService = blockedDomainService;
        this.blockedSenderService = blockedSenderService;
        this.trustedDomainService = trustedDomainService;
        this.trustedSenderService = trustedSenderService;
        this.passService = passService;
        this.vpnService = vpnService;
        this.walletService = walletService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.standardNotesChecklistCodec = standardNotesChecklistCodec;
    }

    public SuiteReadinessReportVo getReadinessReport(Long userId, String ipAddress) {
        return getReadinessReport(userId, ipAddress, Set.of());
    }

    public SuiteReadinessReportVo getReadinessReport(Long userId, String ipAddress, Set<String> visibleProductCodes) {
        SuiteReadinessReportVo report = buildReadinessReport(userId, ipAddress, visibleProductCodes);
        auditService.record(
                userId,
                "SUITE_READINESS_QUERY",
                "score=" + report.overallScore() + ",risk=" + report.overallRiskLevel(),
                normalizeIp(ipAddress)
        );
        return report;
    }

    public SuiteSecurityPostureVo getSecurityPosture(Long userId, String ipAddress) {
        return getSecurityPosture(userId, ipAddress, Set.of());
    }

    public SuiteSecurityPostureVo getSecurityPosture(Long userId, String ipAddress, Set<String> visibleProductCodes) {
        SuiteReadinessReportVo readinessReport = buildReadinessReport(userId, ipAddress, visibleProductCodes);
        boolean mailVisible = isProductVisible(visibleProductCodes, "MAIL");
        boolean walletVisible = isProductVisible(visibleProductCodes, "WALLET");

        long activeSessionCount = countActiveSessions(userId);
        long blockedSenderCount = mailVisible ? countBlockedSenders(userId) : 0;
        long trustedSenderCount = mailVisible ? countTrustedSenders(userId) : 0;
        long blockedDomainCount = mailVisible ? countBlockedDomains(userId) : 0;
        long trustedDomainCount = mailVisible ? countTrustedDomains(userId) : 0;
        long walletBlockedCount = walletVisible ? countWalletBlockedTransactions(userId) : 0;
        long walletFailedCount = walletVisible ? countWalletFailedTransactions(userId) : 0;

        List<String> alerts = new ArrayList<>();
        List<SuiteRemediationActionVo> actions = new ArrayList<>();
        int score = 100;

        if (activeSessionCount > 5) {
            score -= 22;
            alerts.add("Detected " + activeSessionCount + " active sessions; session spread is high.");
            actions.add(action("P0", PRODUCT_SECURITY, "Revoke stale sessions and keep <= 3 active sessions."));
        } else if (activeSessionCount > 3) {
            score -= 10;
            alerts.add("Active sessions exceed recommended baseline.");
            actions.add(action("P1", PRODUCT_SECURITY, "Review active sessions and revoke unknown devices."));
        }

        if (mailVisible && blockedDomainCount == 0) {
            score -= 16;
            alerts.add("No blocked domains configured; phishing domain protection is weak.");
            actions.add(action("P0", "MAIL", "Add at least one blocked domain for known spam/phishing origins.", ACTION_MAIL_ADD_BLOCKED_DOMAIN_BASELINE));
        }

        if (mailVisible && blockedSenderCount == 0) {
            score -= 10;
            alerts.add("No blocked senders configured.");
            actions.add(action("P1", "MAIL", "Add blocked senders from recent suspicious traffic.", ACTION_MAIL_ADD_BLOCKED_SENDER_BASELINE));
        }

        if (mailVisible && trustedDomainCount == 0) {
            score -= 8;
            actions.add(action("P2", "MAIL", "Configure trusted business domains to reduce false positives.", ACTION_MAIL_ADD_TRUSTED_DOMAIN_BASELINE));
        }

        if (mailVisible && trustedSenderCount == 0) {
            score -= 6;
            actions.add(action("P2", "MAIL", "Add trusted senders for core partners and internal routing.", ACTION_MAIL_ADD_TRUSTED_SENDER_BASELINE));
        }

        if (walletVisible && walletBlockedCount > 0) {
            score -= Math.min(20, 8 + (int) (walletBlockedCount * 3));
            alerts.add("Wallet execution has " + walletBlockedCount + " blocked mid-stage transactions.");
            actions.add(action("P0", "WALLET", "Use wallet execution queue to advance/remediate blocked transactions.", ACTION_WALLET_BATCH_ADVANCE));
        }

        if (walletVisible && walletFailedCount > 0) {
            score -= Math.min(20, 6 + (int) (walletFailedCount * 3));
            alerts.add("Wallet has " + walletFailedCount + " failed transactions requiring remediation.");
            actions.add(action("P0", "WALLET", "Remediate failed wallet transactions and keep retry evidence.", ACTION_WALLET_BATCH_RECONCILE));
        }

        if (readinessReport.criticalRiskProductCount() > 0) {
            score -= 22;
            alerts.add("Critical readiness risk exists in product portfolio.");
        }

        if (readinessReport.highRiskProductCount() > 2) {
            score -= 12;
            alerts.add("High-risk product count is above governance threshold.");
        }

        for (SuiteReadinessItemVo item : readinessReport.items()) {
            if (isHighOrCritical(item.riskLevel())) {
                if (!item.blockers().isEmpty()) {
                    alerts.add(item.productCode() + ": " + item.blockers().get(0));
                }
                if (!item.actions().isEmpty()) {
                    actions.add(item.actions().get(0));
                }
            }
        }

        List<SuiteRemediationActionVo> normalizedActions = deduplicateAndSortActions(actions);
        int finalScore = clamp(score);
        SuiteSecurityPostureVo posture = new SuiteSecurityPostureVo(
                LocalDateTime.now(),
                finalScore,
                toRiskLevel(finalScore),
                activeSessionCount,
                blockedSenderCount,
                trustedSenderCount,
                blockedDomainCount,
                trustedDomainCount,
                readinessReport.highRiskProductCount(),
                readinessReport.criticalRiskProductCount(),
                deduplicateAlerts(alerts),
                normalizedActions
        );
        auditService.record(
                userId,
                "SUITE_SECURITY_POSTURE_QUERY",
                "score=" + posture.securityScore() + ",risk=" + posture.overallRiskLevel(),
                normalizeIp(ipAddress)
        );
        return posture;
    }

    public SuiteUnifiedSearchResultVo unifiedSearch(Long userId, String keyword, Integer limit, String ipAddress) {
        return unifiedSearch(userId, keyword, limit, ipAddress, Set.of());
    }

    public SuiteUnifiedSearchResultVo unifiedSearch(
            Long userId,
            String keyword,
            Integer limit,
            String ipAddress,
            Set<String> visibleProductCodes
    ) {
        String normalizedKeyword = normalizeSearchKeyword(keyword);
        int normalizedLimit = normalizeUnifiedSearchLimit(limit);
        List<SuiteUnifiedSearchItemVo> mergedItems = new ArrayList<>();
        if (StringUtils.hasText(normalizedKeyword)) {
            mergedItems.addAll(searchMailItems(userId, normalizedKeyword, normalizedLimit));
            mergedItems.addAll(searchDocsItems(userId, normalizedKeyword, normalizedLimit));
            mergedItems.addAll(searchSheetsItems(userId, normalizedKeyword, normalizedLimit));
            mergedItems.addAll(searchDriveItems(userId, normalizedKeyword, normalizedLimit));
            mergedItems.addAll(searchSimpleLoginItems(userId, normalizedKeyword, normalizedLimit));
            mergedItems.addAll(searchStandardNotesItems(userId, normalizedKeyword, normalizedLimit));
            mergedItems.addAll(searchPassItems(userId, normalizedKeyword, normalizedLimit));
            mergedItems.addAll(searchCalendarItems(userId, normalizedKeyword, normalizedLimit));
            mergedItems.addAll(searchLumoItems(userId, normalizedKeyword, normalizedLimit));
        }
        List<SuiteUnifiedSearchItemVo> items = mergedItems.stream()
                .filter(item -> isProductVisible(visibleProductCodes, item.productCode()))
                .sorted(Comparator
                        .comparing(SuiteUnifiedSearchItemVo::updatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(SuiteUnifiedSearchItemVo::productCode)
                        .thenComparing(SuiteUnifiedSearchItemVo::title))
                .limit(normalizedLimit)
                .toList();
        SuiteUnifiedSearchResultVo result = new SuiteUnifiedSearchResultVo(
                LocalDateTime.now(),
                normalizedKeyword,
                normalizedLimit,
                items.size(),
                items
        );
        auditService.record(
                userId,
                "SUITE_UNIFIED_SEARCH_QUERY",
                "keyword=" + safeAuditKeyword(normalizedKeyword) + ",limit=" + normalizedLimit + ",total=" + result.total(),
                normalizeIp(ipAddress)
        );
        return result;
    }

    public SuiteGovernanceOverviewVo getGovernanceOverview(Long userId, String ipAddress) {
        List<Long> managedOrgIds = orgService.listGovernanceManagedOrgIds(userId);
        List<SuiteGovernanceRequest> requests = suiteGovernanceRequestMapper.selectList(
                buildGovernanceChangeRequestQuery(userId, managedOrgIds)
        );
        SuiteGovernanceOverviewVo overview = buildGovernanceOverview(requests);
        auditService.record(
                userId,
                "SUITE_GOVERNANCE_OVERVIEW_QUERY",
                "total=" + overview.totalRequests() + ",slaBreached=" + overview.slaBreachedCount(),
                normalizeIp(ipAddress)
        );
        return overview;
    }

    public List<SuiteGovernancePolicyTemplateVo> listGovernanceTemplates(Long userId, String ipAddress) {
        List<SuiteGovernancePolicyTemplateVo> templates = GOVERNANCE_TEMPLATE_DEFINITIONS.values().stream()
                .map(this::toGovernanceTemplateVo)
                .toList();
        auditService.record(
                userId,
                "SUITE_GOVERNANCE_TEMPLATE_LIST",
                "count=" + templates.size(),
                normalizeIp(ipAddress)
        );
        return templates;
    }

    public List<SuiteGovernanceChangeRequestVo> listGovernanceChangeRequests(Long userId, String ipAddress) {
        List<Long> managedOrgIds = orgService.listGovernanceManagedOrgIds(userId);
        LambdaQueryWrapper<SuiteGovernanceRequest> query = buildGovernanceChangeRequestQuery(userId, managedOrgIds);

        List<SuiteGovernanceRequest> entities = suiteGovernanceRequestMapper.selectList(query);
        if (entities.isEmpty()) {
            auditService.record(userId, "SUITE_GOVERNANCE_CHANGE_REQUEST_LIST", "count=0", normalizeIp(ipAddress));
            return List.of();
        }
        List<SuiteGovernanceChangeRequestVo> requests = entities.stream()
                .map(this::toGovernanceChangeRequestVo)
                .toList();
        auditService.record(
                userId,
                "SUITE_GOVERNANCE_CHANGE_REQUEST_LIST",
                "count=" + requests.size(),
                normalizeIp(ipAddress)
        );
        return requests;
    }

    public SuiteGovernanceChangeRequestVo createGovernanceChangeRequest(
            Long userId,
            String templateCode,
            String reason,
            Long orgId,
            Long secondReviewerUserId,
            String ipAddress
    ) {
        GovernanceTemplateDefinition template = requireGovernanceTemplate(templateCode);
        String safeReason = sanitizeFreeText(reason, "Governance reason is required", 300);
        Long safeOrgId = normalizeOptionalOrgId(orgId);
        Long safeSecondReviewerUserId = normalizeOptionalUserId(secondReviewerUserId);
        boolean requireDualReview = false;
        LocalDateTime reviewDueAt = null;
        if (safeOrgId != null) {
            OrgService.OrgGovernanceAccess access = assertCanCreateOrgGovernanceRequest(userId, safeOrgId);
            requireDualReview = access.requireDualReviewGovernance();
            reviewDueAt = LocalDateTime.now().plusHours(access.governanceReviewSlaHours());
            if (!requireDualReview && safeSecondReviewerUserId != null) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "Designated second reviewer requires dual-review policy");
            }
            if (safeSecondReviewerUserId != null) {
                assertCanBeDesignatedSecondReviewer(safeOrgId, safeSecondReviewerUserId);
            }
        } else if (safeSecondReviewerUserId != null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Designated second reviewer is only supported for organization scope");
        }
        LocalDateTime now = LocalDateTime.now();
        SuiteGovernanceRequest entity = new SuiteGovernanceRequest();
        entity.setOwnerId(userId);
        entity.setOrgId(safeOrgId);
        entity.setRequestId(generateGovernanceRequestId());
        entity.setTemplateCode(template.templateCode());
        entity.setTemplateName(template.name());
        entity.setStatus(GOVERNANCE_STATUS_PENDING_REVIEW);
        entity.setReason(safeReason);
        entity.setRequireDualReview(requireDualReview ? 1 : 0);
        entity.setSecondReviewerUserId(safeSecondReviewerUserId);
        entity.setRequestedAt(now);
        entity.setReviewDueAt(reviewDueAt);
        entity.setActionCodesJson(writeAsJson(template.actionCodes()));
        entity.setRollbackActionCodesJson(writeAsJson(template.rollbackActionCodes()));
        entity.setExecutionResultsJson(writeAsJson(List.of()));
        entity.setRollbackResultsJson(writeAsJson(List.of()));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setDeleted(0);
        suiteGovernanceRequestMapper.insert(entity);
        String safeIp = normalizeIp(ipAddress);
        auditService.record(
                userId,
                "SUITE_GOVERNANCE_CHANGE_REQUEST_CREATE",
                "requestId=" + entity.getRequestId()
                        + ",template=" + entity.getTemplateCode()
                        + ",scope=" + (safeOrgId == null ? "PERSONAL" : "ORG")
                        + ",orgId=" + (safeOrgId == null ? "-" : safeOrgId)
                        + ",dualReview=" + requireDualReview
                        + ",secondReviewerUserId=" + (safeSecondReviewerUserId == null ? "-" : safeSecondReviewerUserId)
                        + ",reviewDueAt=" + (reviewDueAt == null ? "-" : reviewDueAt),
                safeIp,
                safeOrgId
        );
        return toGovernanceChangeRequestVo(entity);
    }

    public SuiteGovernanceChangeRequestVo reviewGovernanceChangeRequest(
            Long userId,
            Long sessionId,
            String requestId,
            String decision,
            String reviewNote,
            String ipAddress
    ) {
        GovernanceRequestAccess access = loadGovernanceRequestAccess(userId, requestId);
        SuiteGovernanceRequest entity = access.request();
        assertGovernanceRequestReviewable(entity);
        assertCanReviewGovernance(access);
        String safeDecision = sanitizeReviewDecision(decision);
        Long currentSessionId = requireSessionId(sessionId);
        String safeReviewNote = normalizeOptionalText(
                reviewNote,
                GOVERNANCE_REVIEW_DECISION_APPROVE.equals(safeDecision)
                        ? "Reviewed and approved for execution"
                        : "Reviewed and rejected",
                300
        );
        LocalDateTime now = LocalDateTime.now();
        String status = applyReviewDecision(entity, userId, currentSessionId, safeDecision, safeReviewNote, now);
        entity.setUpdatedAt(now);
        suiteGovernanceRequestMapper.updateById(entity);
        String safeIp = normalizeIp(ipAddress);
        auditService.record(
                userId,
                "SUITE_GOVERNANCE_CHANGE_REQUEST_REVIEW",
                "requestId=" + entity.getRequestId()
                        + ",decision=" + safeDecision
                        + ",status=" + status
                        + ",stage=" + resolveReviewStage(entity),
                safeIp,
                access.orgId()
        );
        return toGovernanceChangeRequestVo(entity);
    }

    public SuiteGovernanceChangeRequestVo approveGovernanceChangeRequest(
            Long userId,
            Long sessionId,
            String requestId,
            String approvalNote,
            String ipAddress
    ) {
        GovernanceRequestAccess access = loadGovernanceRequestAccess(userId, requestId);
        SuiteGovernanceRequest entity = access.request();
        if (!GOVERNANCE_STATUS_APPROVED_PENDING_EXECUTION.equals(entity.getStatus())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Governance request is not ready for execution");
        }
        assertCanExecuteGovernance(access);
        Long currentSessionId = requireSessionId(sessionId);
        if (entity.getReviewedBySessionId() != null && entity.getReviewedBySessionId().equals(currentSessionId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, GOVERNANCE_EXECUTION_SESSION_CONFLICT_MESSAGE);
        }

        String safeIp = normalizeIp(ipAddress);
        String safeApprovalNote = normalizeOptionalText(approvalNote, "Approved by suite governance", 300);
        List<String> actionCodes = readStringList(entity.getActionCodesJson());
        List<SuiteRemediationExecutionResultVo> executionResults = executeActionBatch(entity.getOwnerId(), actionCodes, safeIp);
        String status = hasExecutionFailure(executionResults)
                ? GOVERNANCE_STATUS_EXECUTED_WITH_FAILURE
                : GOVERNANCE_STATUS_EXECUTED;

        LocalDateTime now = LocalDateTime.now();
        entity.setStatus(status);
        entity.setApprovalNote(safeApprovalNote);
        entity.setApprovedAt(now);
        entity.setExecutedAt(now);
        entity.setExecutedByUserId(userId);
        entity.setExecutedBySessionId(currentSessionId);
        entity.setExecutionResultsJson(writeAsJson(executionResults));
        entity.setUpdatedAt(now);
        suiteGovernanceRequestMapper.updateById(entity);
        auditService.record(
                userId,
                "SUITE_GOVERNANCE_CHANGE_REQUEST_APPROVE",
                "requestId=" + entity.getRequestId()
                        + ",status=" + status
                        + ",actions=" + executionResults.size()
                        + ",ownerId=" + entity.getOwnerId(),
                safeIp,
                access.orgId()
        );
        return toGovernanceChangeRequestVo(entity);
    }

    public SuiteGovernanceChangeRequestVo rollbackGovernanceChangeRequest(
            Long userId,
            String requestId,
            String rollbackReason,
            String ipAddress
    ) {
        GovernanceRequestAccess access = loadGovernanceRequestAccess(userId, requestId);
        SuiteGovernanceRequest entity = access.request();
        if (!GOVERNANCE_STATUS_EXECUTED.equals(entity.getStatus())
                && !GOVERNANCE_STATUS_EXECUTED_WITH_FAILURE.equals(entity.getStatus())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Governance request cannot be rolled back in current status");
        }
        assertCanExecuteGovernance(access);
        List<String> rollbackActionCodes = readStringList(entity.getRollbackActionCodesJson());
        if (rollbackActionCodes.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Governance template does not support rollback");
        }

        String safeIp = normalizeIp(ipAddress);
        String safeRollbackReason = sanitizeFreeText(rollbackReason, "Rollback reason is required", 300);
        List<SuiteRemediationExecutionResultVo> rollbackResults = executeActionBatch(entity.getOwnerId(), rollbackActionCodes, safeIp);
        String status = hasExecutionFailure(rollbackResults)
                ? GOVERNANCE_STATUS_ROLLBACK_WITH_FAILURE
                : GOVERNANCE_STATUS_ROLLED_BACK;

        LocalDateTime now = LocalDateTime.now();
        entity.setStatus(status);
        entity.setRollbackReason(safeRollbackReason);
        entity.setRolledBackAt(now);
        entity.setRollbackResultsJson(writeAsJson(rollbackResults));
        entity.setUpdatedAt(now);
        suiteGovernanceRequestMapper.updateById(entity);
        auditService.record(
                userId,
                "SUITE_GOVERNANCE_CHANGE_REQUEST_ROLLBACK",
                "requestId=" + entity.getRequestId()
                        + ",status=" + status
                        + ",actions=" + rollbackResults.size()
                        + ",ownerId=" + entity.getOwnerId(),
                safeIp,
                access.orgId()
        );
        return toGovernanceChangeRequestVo(entity);
    }

    public SuiteRemediationExecutionResultVo executeRemediationAction(Long userId, String actionCode, String ipAddress) {
        String safeIp = normalizeIp(ipAddress);
        String safeActionCode = StringUtils.hasText(actionCode) ? actionCode.trim().toUpperCase() : "UNKNOWN";
        try {
            safeActionCode = normalizeActionCode(actionCode);
            SuiteRemediationExecutionResultVo result = switch (safeActionCode) {
                case ACTION_MAIL_ADD_BLOCKED_DOMAIN_BASELINE -> executeMailAddBlockedDomainBaseline(userId);
                case ACTION_MAIL_ADD_BLOCKED_SENDER_BASELINE -> executeMailAddBlockedSenderBaseline(userId);
                case ACTION_MAIL_ADD_TRUSTED_DOMAIN_BASELINE -> executeMailAddTrustedDomainBaseline(userId);
                case ACTION_MAIL_ADD_TRUSTED_SENDER_BASELINE -> executeMailAddTrustedSenderBaseline(userId);
                case ACTION_MAIL_REMOVE_BLOCKED_DOMAIN_BASELINE -> executeMailRemoveBlockedDomainBaseline(userId);
                case ACTION_MAIL_REMOVE_BLOCKED_SENDER_BASELINE -> executeMailRemoveBlockedSenderBaseline(userId);
                case ACTION_MAIL_REMOVE_TRUSTED_DOMAIN_BASELINE -> executeMailRemoveTrustedDomainBaseline(userId);
                case ACTION_MAIL_REMOVE_TRUSTED_SENDER_BASELINE -> executeMailRemoveTrustedSenderBaseline(userId);
                case ACTION_PASS_CREATE_BASELINE_ITEM -> executePassCreateBaselineItem(userId, safeIp);
                case ACTION_PASS_DELETE_BASELINE_ITEM -> executePassDeleteBaselineItem(userId, safeIp);
                case ACTION_VPN_CONNECT_SECURE_CORE_BASELINE -> executeVpnConnectSecureCoreBaseline(userId, safeIp);
                case ACTION_VPN_DISCONNECT_CURRENT -> executeVpnDisconnectCurrent(userId, safeIp);
                case ACTION_WALLET_BATCH_ADVANCE -> executeWalletBatchAdvance(userId, safeIp);
                case ACTION_WALLET_BATCH_RECONCILE -> executeWalletBatchReconcile(userId, safeIp);
                default -> throw new BizException(ErrorCode.INVALID_ARGUMENT, "Suite remediation actionCode is invalid");
            };
            auditService.record(
                    userId,
                    "SUITE_REMEDIATION_ACTION_EXECUTE",
                    "actionCode=" + safeActionCode + ",product=" + result.productCode() + ",status=" + result.status(),
                    safeIp
            );
            return result;
        } catch (BizException exception) {
            auditService.record(
                    userId,
                    "SUITE_REMEDIATION_ACTION_FAILED",
                    "actionCode=" + safeActionCode + ",error=" + safeErrorMessage(exception.getMessage()),
                    safeIp
            );
            throw exception;
        } catch (Exception exception) {
            auditService.record(
                    userId,
                    "SUITE_REMEDIATION_ACTION_FAILED",
                    "actionCode=" + safeActionCode + ",error=" + safeErrorMessage(exception.getMessage()),
                    safeIp
            );
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Suite remediation execution failed");
        }
    }

    private LambdaQueryWrapper<SuiteGovernanceRequest> buildGovernanceChangeRequestQuery(Long userId, List<Long> managedOrgIds) {
        return new LambdaQueryWrapper<SuiteGovernanceRequest>()
                .and(wrapper -> {
                    wrapper.eq(SuiteGovernanceRequest::getOwnerId, userId);
                    if (!managedOrgIds.isEmpty()) {
                        wrapper.or().in(SuiteGovernanceRequest::getOrgId, managedOrgIds);
                    }
                })
                .orderByDesc(SuiteGovernanceRequest::getRequestedAt);
    }

    private SuiteGovernanceOverviewVo buildGovernanceOverview(List<SuiteGovernanceRequest> requests) {
        long pendingReviewCount = 0;
        long pendingSecondReviewCount = 0;
        long approvedPendingExecutionCount = 0;
        long rejectedCount = 0;
        long executedCount = 0;
        long executedWithFailureCount = 0;
        long rolledBackCount = 0;
        long rollbackWithFailureCount = 0;
        long slaBreachedCount = 0;
        LocalDateTime now = LocalDateTime.now();
        for (SuiteGovernanceRequest request : requests) {
            String status = request.getStatus();
            if (GOVERNANCE_STATUS_PENDING_REVIEW.equals(status)) {
                pendingReviewCount++;
            } else if (GOVERNANCE_STATUS_PENDING_SECOND_REVIEW.equals(status)) {
                pendingSecondReviewCount++;
            } else if (GOVERNANCE_STATUS_APPROVED_PENDING_EXECUTION.equals(status)) {
                approvedPendingExecutionCount++;
            } else if (GOVERNANCE_STATUS_REJECTED.equals(status)) {
                rejectedCount++;
            } else if (GOVERNANCE_STATUS_EXECUTED.equals(status)) {
                executedCount++;
            } else if (GOVERNANCE_STATUS_EXECUTED_WITH_FAILURE.equals(status)) {
                executedWithFailureCount++;
            } else if (GOVERNANCE_STATUS_ROLLED_BACK.equals(status)) {
                rolledBackCount++;
            } else if (GOVERNANCE_STATUS_ROLLBACK_WITH_FAILURE.equals(status)) {
                rollbackWithFailureCount++;
            }
            if (isReviewSlaBreached(request, now)) {
                slaBreachedCount++;
            }
        }
        return new SuiteGovernanceOverviewVo(
                now,
                requests.size(),
                pendingReviewCount,
                pendingSecondReviewCount,
                approvedPendingExecutionCount,
                rejectedCount,
                executedCount,
                executedWithFailureCount,
                rolledBackCount,
                rollbackWithFailureCount,
                slaBreachedCount
        );
    }

    private boolean isReviewSlaBreached(SuiteGovernanceRequest request, LocalDateTime now) {
        if (!isReviewPendingStatus(request.getStatus())) {
            return false;
        }
        if (request.getReviewDueAt() == null) {
            return false;
        }
        return request.getReviewDueAt().isBefore(now);
    }

    private boolean isReviewPendingStatus(String status) {
        return GOVERNANCE_STATUS_PENDING_REVIEW.equals(status)
                || GOVERNANCE_STATUS_PENDING_SECOND_REVIEW.equals(status);
    }

    private String normalizeSearchKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return "";
        }
        return keyword.trim();
    }

    private int normalizeUnifiedSearchLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_UNIFIED_SEARCH_LIMIT;
        }
        if (limit < 1 || limit > MAX_UNIFIED_SEARCH_LIMIT) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "limit must be between 1 and " + MAX_UNIFIED_SEARCH_LIMIT);
        }
        return limit;
    }

    private String safeAuditKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return "-";
        }
        return shorten(keyword, AUDIT_KEYWORD_MAX_LENGTH);
    }

    private List<SuiteUnifiedSearchItemVo> searchMailItems(Long userId, String keyword, int limit) {
        List<MailMessage> entities = mailMessageMapper.selectList(new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getOwnerId, userId)
                .and(query -> query.like(MailMessage::getSubject, keyword)
                        .or().like(MailMessage::getPeerEmail, keyword)
                        .or().like(MailMessage::getBodyCiphertext, keyword))
                .orderByDesc(MailMessage::getUpdatedAt)
                .last("limit " + limit));
        return entities.stream()
                .map(entity -> new SuiteUnifiedSearchItemVo(
                        "MAIL",
                        "MESSAGE",
                        String.valueOf(entity.getId()),
                        defaultTitle(entity.getSubject(), "(No Subject)"),
                        shorten(defaultText(entity.getPeerEmail(), entity.getFolderType()), UNIFIED_SEARCH_SUMMARY_MAX_LENGTH),
                        "/mail/" + entity.getId(),
                        firstNonNullTime(entity.getUpdatedAt(), entity.getSentAt(), entity.getCreatedAt())
                ))
                .toList();
    }

    private List<SuiteUnifiedSearchItemVo> searchDocsItems(Long userId, String keyword, int limit) {
        List<DocsNote> entities = docsNoteMapper.selectList(new LambdaQueryWrapper<DocsNote>()
                .eq(DocsNote::getOwnerId, userId)
                .eq(DocsNote::getWorkspaceType, "DOCS")
                .and(query -> query.like(DocsNote::getTitle, keyword).or().like(DocsNote::getContent, keyword))
                .orderByDesc(DocsNote::getUpdatedAt)
                .last("limit " + limit));
        return entities.stream()
                .map(entity -> new SuiteUnifiedSearchItemVo(
                        "DOCS",
                        "NOTE",
                        String.valueOf(entity.getId()),
                        defaultTitle(entity.getTitle(), "Untitled note"),
                        shorten(defaultText(entity.getContent(), "-"), UNIFIED_SEARCH_SUMMARY_MAX_LENGTH),
                        "/docs",
                        firstNonNullTime(entity.getUpdatedAt(), entity.getCreatedAt())
                ))
                .toList();
    }

    private List<SuiteUnifiedSearchItemVo> searchStandardNotesItems(Long userId, String keyword, int limit) {
        List<SuiteUnifiedSearchItemVo> items = new ArrayList<>();
        items.addAll(searchStandardNoteFolderItems(userId, keyword, limit));
        int noteLimit = Math.max(1, limit - items.size());
        List<DocsNote> entities = docsNoteMapper.selectList(new LambdaQueryWrapper<DocsNote>()
                .eq(DocsNote::getOwnerId, userId)
                .eq(DocsNote::getWorkspaceType, "STANDARD_NOTES")
                .and(query -> query.like(DocsNote::getTitle, keyword).or().like(DocsNote::getContent, keyword))
                .orderByDesc(DocsNote::getUpdatedAt)
                .last("limit " + noteLimit));
        items.addAll(entities.stream()
                .map(entity -> new SuiteUnifiedSearchItemVo(
                        "STANDARD_NOTES",
                        "NOTE",
                        String.valueOf(entity.getId()),
                        defaultTitle(entity.getTitle(), "Untitled note"),
                        shorten(defaultText(entity.getContent(), "-"), UNIFIED_SEARCH_SUMMARY_MAX_LENGTH),
                        "/standard-notes?noteId=" + entity.getId(),
                        firstNonNullTime(entity.getUpdatedAt(), entity.getCreatedAt())
                ))
                .toList());
        return items.stream()
                .sorted(Comparator.comparing(SuiteUnifiedSearchItemVo::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    private List<SuiteUnifiedSearchItemVo> searchStandardNoteFolderItems(Long userId, String keyword, int limit) {
        List<StandardNoteFolder> folders = standardNoteFolderMapper.selectList(new LambdaQueryWrapper<StandardNoteFolder>()
                .eq(StandardNoteFolder::getOwnerId, userId)
                .and(query -> query.like(StandardNoteFolder::getName, keyword).or().like(StandardNoteFolder::getDescription, keyword))
                .orderByDesc(StandardNoteFolder::getUpdatedAt)
                .last("limit " + limit));
        return folders.stream()
                .map(folder -> new SuiteUnifiedSearchItemVo(
                        "STANDARD_NOTES",
                        "FOLDER",
                        String.valueOf(folder.getId()),
                        defaultTitle(folder.getName(), "Untitled folder"),
                        shorten(defaultText(folder.getDescription(), folder.getColor()), UNIFIED_SEARCH_SUMMARY_MAX_LENGTH),
                        "/standard-notes?folderId=" + folder.getId(),
                        firstNonNullTime(folder.getUpdatedAt(), folder.getCreatedAt())
                ))
                .toList();
    }

    private List<SuiteUnifiedSearchItemVo> searchDriveItems(Long userId, String keyword, int limit) {
        List<DriveItem> entities = driveItemMapper.selectList(new LambdaQueryWrapper<DriveItem>()
                .eq(DriveItem::getOwnerId, userId)
                .and(query -> query.like(DriveItem::getName, keyword).or().like(DriveItem::getMimeType, keyword))
                .orderByDesc(DriveItem::getUpdatedAt)
                .last("limit " + limit));
        return entities.stream()
                .map(entity -> new SuiteUnifiedSearchItemVo(
                        "DRIVE",
                        defaultText(entity.getItemType(), "ITEM"),
                        String.valueOf(entity.getId()),
                        defaultTitle(entity.getName(), "Unnamed drive item"),
                        shorten(defaultText(entity.getMimeType(), entity.getItemType()), UNIFIED_SEARCH_SUMMARY_MAX_LENGTH),
                        "/drive",
                        firstNonNullTime(entity.getUpdatedAt(), entity.getCreatedAt())
                ))
                .toList();
    }

    private List<SuiteUnifiedSearchItemVo> searchSheetsItems(Long userId, String keyword, int limit) {
        List<SheetsWorkbook> entities = sheetsWorkbookMapper.selectList(new LambdaQueryWrapper<SheetsWorkbook>()
                .eq(SheetsWorkbook::getOwnerId, userId)
                .and(query -> query.like(SheetsWorkbook::getTitle, keyword)
                        .or().like(SheetsWorkbook::getGridJson, keyword)
                        .or().like(SheetsWorkbook::getSheetsJson, keyword))
                .orderByDesc(SheetsWorkbook::getUpdatedAt)
                .last("limit " + limit));
        return entities.stream()
                .map(entity -> new SuiteUnifiedSearchItemVo(
                        "SHEETS",
                        "WORKBOOK",
                        String.valueOf(entity.getId()),
                        defaultTitle(entity.getTitle(), "Untitled workbook"),
                        entity.getRowCount() + "×" + entity.getColCount() + " workbook",
                        "/sheets?workbookId=" + entity.getId(),
                        firstNonNullTime(entity.getUpdatedAt(), entity.getLastOpenedAt(), entity.getCreatedAt())
                ))
                .toList();
    }

    private List<SuiteUnifiedSearchItemVo> searchSimpleLoginItems(Long userId, String keyword, int limit) {
        List<SuiteUnifiedSearchItemVo> items = new ArrayList<>();
        items.addAll(searchSimpleLoginAliasItems(userId, keyword, limit));
        items.addAll(searchSimpleLoginPolicyItems(loadActiveOrgIds(userId), keyword, limit));
        return items.stream()
                .sorted(Comparator.comparing(SuiteUnifiedSearchItemVo::updatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(limit)
                .toList();
    }

    private List<SuiteUnifiedSearchItemVo> searchSimpleLoginAliasItems(Long userId, String keyword, int limit) {
        List<PassMailAlias> entities = passMailAliasMapper.selectList(new LambdaQueryWrapper<PassMailAlias>()
                .eq(PassMailAlias::getOwnerId, userId)
                .and(query -> query.like(PassMailAlias::getAliasEmail, keyword)
                        .or().like(PassMailAlias::getTitle, keyword)
                        .or().like(PassMailAlias::getForwardToEmail, keyword)
                        .or().like(PassMailAlias::getNote, keyword))
                .orderByDesc(PassMailAlias::getUpdatedAt)
                .last("limit " + limit));
        return entities.stream()
                .map(entity -> new SuiteUnifiedSearchItemVo(
                        "SIMPLELOGIN",
                        "ALIAS",
                        String.valueOf(entity.getId()),
                        defaultTitle(entity.getTitle(), entity.getAliasEmail()),
                        shorten(defaultText(entity.getAliasEmail(), entity.getForwardToEmail()), UNIFIED_SEARCH_SUMMARY_MAX_LENGTH),
                        "/simplelogin?aliasId=" + entity.getId(),
                        firstNonNullTime(entity.getUpdatedAt(), entity.getCreatedAt())
                ))
                .toList();
    }

    private List<SuiteUnifiedSearchItemVo> searchSimpleLoginPolicyItems(List<Long> orgIds, String keyword, int limit) {
        if (orgIds.isEmpty()) {
            return List.of();
        }
        int fetchLimit = Math.max(limit * 4, DEFAULT_UNIFIED_SEARCH_LIMIT);
        List<SimpleLoginRelayPolicy> policies = simpleLoginRelayPolicyMapper.selectList(new LambdaQueryWrapper<SimpleLoginRelayPolicy>()
                .in(SimpleLoginRelayPolicy::getOrgId, orgIds)
                .orderByDesc(SimpleLoginRelayPolicy::getUpdatedAt)
                .last("limit " + fetchLimit));
        if (policies.isEmpty()) {
            return List.of();
        }
        Map<Long, OrgCustomDomain> domainMap = loadSimpleLoginPolicyDomainMap(policies);
        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        return policies.stream()
                .filter(policy -> simpleLoginPolicyMatchesKeyword(policy, domainMap.get(policy.getCustomDomainId()), normalizedKeyword))
                .limit(limit)
                .map(policy -> toSimpleLoginPolicySearchItem(policy, domainMap.get(policy.getCustomDomainId())))
                .toList();
    }

    private Map<Long, OrgCustomDomain> loadSimpleLoginPolicyDomainMap(List<SimpleLoginRelayPolicy> policies) {
        Set<Long> domainIds = policies.stream()
                .map(SimpleLoginRelayPolicy::getCustomDomainId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (domainIds.isEmpty()) {
            return Map.of();
        }
        List<OrgCustomDomain> domains = orgCustomDomainMapper.selectList(new LambdaQueryWrapper<OrgCustomDomain>()
                .in(OrgCustomDomain::getId, domainIds));
        Map<Long, OrgCustomDomain> domainMap = new LinkedHashMap<>();
        for (OrgCustomDomain domain : domains) {
            domainMap.put(domain.getId(), domain);
        }
        return domainMap;
    }

    private boolean simpleLoginPolicyMatchesKeyword(SimpleLoginRelayPolicy policy, OrgCustomDomain domain, String keyword) {
        return containsIgnoreCase(domain == null ? null : domain.getDomain(), keyword)
                || containsIgnoreCase(policy.getDefaultMailboxEmail(), keyword)
                || containsIgnoreCase(policy.getNote(), keyword)
                || containsIgnoreCase(policy.getSubdomainMode(), keyword);
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private SuiteUnifiedSearchItemVo toSimpleLoginPolicySearchItem(SimpleLoginRelayPolicy policy, OrgCustomDomain domain) {
        String title = domain == null ? "Relay policy" : domain.getDomain();
        String summary = policy.getDefaultMailboxEmail();
        if (policy.getCatchAllEnabled() != null && policy.getCatchAllEnabled() == BOOLEAN_TRUE) {
            summary = "Catch-all · " + summary;
        }
        return new SuiteUnifiedSearchItemVo(
                "SIMPLELOGIN",
                "POLICY",
                String.valueOf(policy.getId()),
                defaultTitle(title, "Relay policy"),
                shorten(defaultText(summary, policy.getSubdomainMode()), UNIFIED_SEARCH_SUMMARY_MAX_LENGTH),
                "/simplelogin?orgId=" + policy.getOrgId(),
                firstNonNullTime(policy.getUpdatedAt(), policy.getCreatedAt())
        );
    }

    private List<SuiteUnifiedSearchItemVo> searchPassItems(Long userId, String keyword, int limit) {
        List<PassVaultItem> entities = passVaultItemMapper.selectList(new LambdaQueryWrapper<PassVaultItem>()
                .eq(PassVaultItem::getOwnerId, userId)
                .and(query -> query.like(PassVaultItem::getTitle, keyword)
                        .or().like(PassVaultItem::getWebsite, keyword)
                        .or().like(PassVaultItem::getUsername, keyword))
                .orderByDesc(PassVaultItem::getUpdatedAt)
                .last("limit " + limit));
        return entities.stream()
                .map(entity -> new SuiteUnifiedSearchItemVo(
                        "PASS",
                        "VAULT_ITEM",
                        String.valueOf(entity.getId()),
                        defaultTitle(entity.getTitle(), "Untitled credential"),
                        shorten(defaultText(entity.getWebsite(), entity.getUsername()), UNIFIED_SEARCH_SUMMARY_MAX_LENGTH),
                        "/pass",
                        firstNonNullTime(entity.getUpdatedAt(), entity.getCreatedAt())
                ))
                .toList();
    }

    private List<SuiteUnifiedSearchItemVo> searchCalendarItems(Long userId, String keyword, int limit) {
        List<CalendarEvent> entities = calendarEventMapper.selectList(new LambdaQueryWrapper<CalendarEvent>()
                .eq(CalendarEvent::getOwnerId, userId)
                .and(query -> query.like(CalendarEvent::getTitle, keyword)
                        .or().like(CalendarEvent::getDescription, keyword)
                        .or().like(CalendarEvent::getLocation, keyword))
                .orderByDesc(CalendarEvent::getUpdatedAt)
                .last("limit " + limit));
        return entities.stream()
                .map(entity -> new SuiteUnifiedSearchItemVo(
                        "CALENDAR",
                        "EVENT",
                        String.valueOf(entity.getId()),
                        defaultTitle(entity.getTitle(), "Untitled event"),
                        shorten(defaultText(entity.getLocation(), entity.getDescription()), UNIFIED_SEARCH_SUMMARY_MAX_LENGTH),
                        "/calendar",
                        firstNonNullTime(entity.getUpdatedAt(), entity.getStartAt(), entity.getCreatedAt())
                ))
                .toList();
    }

    private List<SuiteUnifiedSearchItemVo> searchLumoItems(Long userId, String keyword, int limit) {
        List<SuiteUnifiedSearchItemVo> items = new ArrayList<>();
        List<LumoProject> projects = lumoProjectMapper.selectList(new LambdaQueryWrapper<LumoProject>()
                .eq(LumoProject::getOwnerId, userId)
                .and(query -> query.like(LumoProject::getName, keyword).or().like(LumoProject::getDescription, keyword))
                .orderByDesc(LumoProject::getUpdatedAt)
                .last("limit " + limit));
        for (LumoProject project : projects) {
            items.add(new SuiteUnifiedSearchItemVo(
                    "LUMO",
                    "PROJECT",
                    String.valueOf(project.getId()),
                    defaultTitle(project.getName(), "Untitled project"),
                    shorten(defaultText(project.getDescription(), "-"), UNIFIED_SEARCH_SUMMARY_MAX_LENGTH),
                    "/lumo",
                    firstNonNullTime(project.getUpdatedAt(), project.getCreatedAt())
            ));
        }
        List<LumoMessage> messages = lumoMessageMapper.selectList(new LambdaQueryWrapper<LumoMessage>()
                .eq(LumoMessage::getOwnerId, userId)
                .like(LumoMessage::getContent, keyword)
                .orderByDesc(LumoMessage::getUpdatedAt)
                .last("limit " + limit));
        for (LumoMessage message : messages) {
            items.add(new SuiteUnifiedSearchItemVo(
                    "LUMO",
                    "MESSAGE",
                    String.valueOf(message.getId()),
                    defaultTitle(message.getContent(), "Lumo message"),
                    "conversationId=" + (message.getConversationId() == null ? "-" : message.getConversationId()),
                    "/lumo",
                    firstNonNullTime(message.getUpdatedAt(), message.getCreatedAt())
            ));
        }
        return items;
    }

    private String defaultTitle(String value, String fallback) {
        String normalized = defaultText(value, fallback);
        return shorten(normalized, UNIFIED_SEARCH_TITLE_MAX_LENGTH);
    }

    private String defaultText(String value, String fallback) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return fallback;
    }

    private String shorten(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        if (maxLength <= 3) {
            return normalized.substring(0, maxLength);
        }
        return normalized.substring(0, maxLength - 3) + "...";
    }

    private LocalDateTime firstNonNullTime(LocalDateTime first, LocalDateTime second) {
        if (first != null) {
            return first;
        }
        return second;
    }

    private LocalDateTime firstNonNullTime(LocalDateTime first, LocalDateTime second, LocalDateTime third) {
        LocalDateTime resolved = firstNonNullTime(first, second);
        if (resolved != null) {
            return resolved;
        }
        return third;
    }

    private SuiteRemediationExecutionResultVo executeMailAddBlockedDomainBaseline(Long userId) {
        boolean created = blockedDomainService.addBlockedDomainIfAbsent(userId, BASELINE_BLOCKED_DOMAIN);
        Map<String, Object> details = Map.of(
                "domain", BASELINE_BLOCKED_DOMAIN,
                "created", created
        );
        if (!created) {
            return executionResult(
                    ACTION_MAIL_ADD_BLOCKED_DOMAIN_BASELINE,
                    "MAIL",
                    EXECUTION_NO_OP,
                    "Blocked domain baseline is already configured.",
                    details
            );
        }
        return executionResult(
                ACTION_MAIL_ADD_BLOCKED_DOMAIN_BASELINE,
                "MAIL",
                EXECUTION_SUCCESS,
                "Blocked domain baseline configured successfully.",
                details
        );
    }

    private SuiteRemediationExecutionResultVo executeMailAddBlockedSenderBaseline(Long userId) {
        boolean created = blockedSenderService.addBlockedSenderIfAbsent(userId, BASELINE_BLOCKED_SENDER);
        Map<String, Object> details = Map.of(
                "email", BASELINE_BLOCKED_SENDER,
                "created", created
        );
        if (!created) {
            return executionResult(
                    ACTION_MAIL_ADD_BLOCKED_SENDER_BASELINE,
                    "MAIL",
                    EXECUTION_NO_OP,
                    "Blocked sender baseline is already configured.",
                    details
            );
        }
        return executionResult(
                ACTION_MAIL_ADD_BLOCKED_SENDER_BASELINE,
                "MAIL",
                EXECUTION_SUCCESS,
                "Blocked sender baseline configured successfully.",
                details
        );
    }

    private SuiteRemediationExecutionResultVo executeMailAddTrustedDomainBaseline(Long userId) {
        boolean created = trustedDomainService.addTrustedDomainIfAbsent(userId, BASELINE_TRUSTED_DOMAIN);
        Map<String, Object> details = Map.of(
                "domain", BASELINE_TRUSTED_DOMAIN,
                "created", created
        );
        if (!created) {
            return executionResult(
                    ACTION_MAIL_ADD_TRUSTED_DOMAIN_BASELINE,
                    "MAIL",
                    EXECUTION_NO_OP,
                    "Trusted domain baseline is already configured.",
                    details
            );
        }
        return executionResult(
                ACTION_MAIL_ADD_TRUSTED_DOMAIN_BASELINE,
                "MAIL",
                EXECUTION_SUCCESS,
                "Trusted domain baseline configured successfully.",
                details
        );
    }

    private SuiteRemediationExecutionResultVo executeMailAddTrustedSenderBaseline(Long userId) {
        boolean created = trustedSenderService.addTrustedSenderIfAbsent(userId, BASELINE_TRUSTED_SENDER);
        Map<String, Object> details = Map.of(
                "email", BASELINE_TRUSTED_SENDER,
                "created", created
        );
        if (!created) {
            return executionResult(
                    ACTION_MAIL_ADD_TRUSTED_SENDER_BASELINE,
                    "MAIL",
                    EXECUTION_NO_OP,
                    "Trusted sender baseline is already configured.",
                    details
            );
        }
        return executionResult(
                ACTION_MAIL_ADD_TRUSTED_SENDER_BASELINE,
                "MAIL",
                EXECUTION_SUCCESS,
                "Trusted sender baseline configured successfully.",
                details
        );
    }

    private SuiteRemediationExecutionResultVo executeMailRemoveBlockedDomainBaseline(Long userId) {
        boolean removed = blockedDomainService.removeBlockedDomainIfPresent(userId, BASELINE_BLOCKED_DOMAIN);
        Map<String, Object> details = Map.of(
                "domain", BASELINE_BLOCKED_DOMAIN,
                "removed", removed
        );
        if (!removed) {
            return executionResult(
                    ACTION_MAIL_REMOVE_BLOCKED_DOMAIN_BASELINE,
                    "MAIL",
                    EXECUTION_NO_OP,
                    "Blocked domain baseline is not present.",
                    details
            );
        }
        return executionResult(
                ACTION_MAIL_REMOVE_BLOCKED_DOMAIN_BASELINE,
                "MAIL",
                EXECUTION_SUCCESS,
                "Blocked domain baseline removed.",
                details
        );
    }

    private SuiteRemediationExecutionResultVo executeMailRemoveBlockedSenderBaseline(Long userId) {
        boolean removed = blockedSenderService.removeBlockedSenderIfPresent(userId, BASELINE_BLOCKED_SENDER);
        Map<String, Object> details = Map.of(
                "email", BASELINE_BLOCKED_SENDER,
                "removed", removed
        );
        if (!removed) {
            return executionResult(
                    ACTION_MAIL_REMOVE_BLOCKED_SENDER_BASELINE,
                    "MAIL",
                    EXECUTION_NO_OP,
                    "Blocked sender baseline is not present.",
                    details
            );
        }
        return executionResult(
                ACTION_MAIL_REMOVE_BLOCKED_SENDER_BASELINE,
                "MAIL",
                EXECUTION_SUCCESS,
                "Blocked sender baseline removed.",
                details
        );
    }

    private SuiteRemediationExecutionResultVo executeMailRemoveTrustedDomainBaseline(Long userId) {
        boolean removed = trustedDomainService.removeTrustedDomainIfPresent(userId, BASELINE_TRUSTED_DOMAIN);
        Map<String, Object> details = Map.of(
                "domain", BASELINE_TRUSTED_DOMAIN,
                "removed", removed
        );
        if (!removed) {
            return executionResult(
                    ACTION_MAIL_REMOVE_TRUSTED_DOMAIN_BASELINE,
                    "MAIL",
                    EXECUTION_NO_OP,
                    "Trusted domain baseline is not present.",
                    details
            );
        }
        return executionResult(
                ACTION_MAIL_REMOVE_TRUSTED_DOMAIN_BASELINE,
                "MAIL",
                EXECUTION_SUCCESS,
                "Trusted domain baseline removed.",
                details
        );
    }

    private SuiteRemediationExecutionResultVo executeMailRemoveTrustedSenderBaseline(Long userId) {
        boolean removed = trustedSenderService.removeTrustedSenderIfPresent(userId, BASELINE_TRUSTED_SENDER);
        Map<String, Object> details = Map.of(
                "email", BASELINE_TRUSTED_SENDER,
                "removed", removed
        );
        if (!removed) {
            return executionResult(
                    ACTION_MAIL_REMOVE_TRUSTED_SENDER_BASELINE,
                    "MAIL",
                    EXECUTION_NO_OP,
                    "Trusted sender baseline is not present.",
                    details
            );
        }
        return executionResult(
                ACTION_MAIL_REMOVE_TRUSTED_SENDER_BASELINE,
                "MAIL",
                EXECUTION_SUCCESS,
                "Trusted sender baseline removed.",
                details
        );
    }

    private SuiteRemediationExecutionResultVo executePassCreateBaselineItem(Long userId, String ipAddress) {
        List<PassItemSummaryVo> existing = passService.list(userId, BASELINE_PASS_TITLE, false, 20);
        boolean hasBaseline = existing.stream().anyMatch(item -> BASELINE_PASS_TITLE.equalsIgnoreCase(item.title()));
        if (hasBaseline) {
            return executionResult(
                    ACTION_PASS_CREATE_BASELINE_ITEM,
                    "PASS",
                    EXECUTION_NO_OP,
                    "Pass baseline credential already exists.",
                    Map.of("title", BASELINE_PASS_TITLE, "created", false)
            );
        }
        var generatedPassword = passService.generatePassword(userId, 24, true, true, true, true, ipAddress);
        var createdItem = passService.create(
                userId,
                BASELINE_PASS_TITLE,
                BASELINE_PASS_WEBSITE,
                BASELINE_PASS_USERNAME,
                generatedPassword.password(),
                "Auto-generated by suite remediation action",
                ipAddress
        );
        return executionResult(
                ACTION_PASS_CREATE_BASELINE_ITEM,
                "PASS",
                EXECUTION_SUCCESS,
                "Pass baseline credential created.",
                Map.of(
                        "title", BASELINE_PASS_TITLE,
                        "itemId", createdItem.id(),
                        "created", true
                )
        );
    }

    private SuiteRemediationExecutionResultVo executePassDeleteBaselineItem(Long userId, String ipAddress) {
        List<PassItemSummaryVo> existing = passService.list(userId, BASELINE_PASS_TITLE, false, 50).stream()
                .filter(item -> BASELINE_PASS_TITLE.equalsIgnoreCase(item.title()))
                .toList();
        if (existing.isEmpty()) {
            return executionResult(
                    ACTION_PASS_DELETE_BASELINE_ITEM,
                    "PASS",
                    EXECUTION_NO_OP,
                    "Pass baseline credential is not present.",
                    Map.of("removedCount", 0)
            );
        }
        for (PassItemSummaryVo item : existing) {
            passService.delete(userId, Long.parseLong(item.id()), ipAddress);
        }
        return executionResult(
                ACTION_PASS_DELETE_BASELINE_ITEM,
                "PASS",
                EXECUTION_SUCCESS,
                "Pass baseline credential removed.",
                Map.of("removedCount", existing.size())
        );
    }

    private SuiteRemediationExecutionResultVo executeVpnConnectSecureCoreBaseline(Long userId, String ipAddress) {
        List<VpnServerVo> servers = vpnService.listServers(userId, ipAddress);
        VpnServerVo targetServer = servers.stream()
                .filter(server -> "SECURE_CORE".equals(server.tier()) && "ONLINE".equals(server.status()))
                .findFirst()
                .orElse(servers.stream()
                        .filter(server -> "ONLINE".equals(server.status()))
                        .findFirst()
                        .orElse(null));
        if (targetServer == null) {
            return executionResult(
                    ACTION_VPN_CONNECT_SECURE_CORE_BASELINE,
                    "VPN",
                    EXECUTION_NO_OP,
                    "No online VPN server is available.",
                    Map.of("serverFound", false)
            );
        }

        var currentSession = vpnService.current(userId, ipAddress);
        if (currentSession != null
                && "CONNECTED".equals(currentSession.status())
                && targetServer.serverId().equals(currentSession.serverId())) {
            return executionResult(
                    ACTION_VPN_CONNECT_SECURE_CORE_BASELINE,
                    "VPN",
                    EXECUTION_NO_OP,
                    "VPN is already connected to the target server.",
                    Map.of(
                            "serverId", targetServer.serverId(),
                            "sessionId", currentSession.sessionId(),
                            "created", false
                    )
            );
        }
        var connectedSession = vpnService.connect(userId, targetServer.serverId(), "WIREGUARD", ipAddress);
        return executionResult(
                ACTION_VPN_CONNECT_SECURE_CORE_BASELINE,
                "VPN",
                EXECUTION_SUCCESS,
                "VPN secure connection established.",
                Map.of(
                        "serverId", connectedSession.serverId(),
                        "sessionId", connectedSession.sessionId(),
                        "created", true
                )
        );
    }

    private SuiteRemediationExecutionResultVo executeVpnDisconnectCurrent(Long userId, String ipAddress) {
        var currentSession = vpnService.current(userId, ipAddress);
        if (currentSession == null || !"CONNECTED".equals(currentSession.status())) {
            return executionResult(
                    ACTION_VPN_DISCONNECT_CURRENT,
                    "VPN",
                    EXECUTION_NO_OP,
                    "No active VPN session is connected.",
                    Map.of("disconnected", false)
            );
        }
        var disconnected = vpnService.disconnect(userId, ipAddress);
        return executionResult(
                ACTION_VPN_DISCONNECT_CURRENT,
                "VPN",
                EXECUTION_SUCCESS,
                "VPN session disconnected.",
                Map.of(
                        "disconnected", true,
                        "sessionId", disconnected.sessionId(),
                        "serverId", disconnected.serverId()
                )
        );
    }

    private SuiteRemediationExecutionResultVo executeWalletBatchAdvance(Long userId, String ipAddress) {
        WalletAccount account = loadLatestWalletAccount(userId);
        if (account == null) {
            return executionResult(
                    ACTION_WALLET_BATCH_ADVANCE,
                    "WALLET",
                    EXECUTION_NO_OP,
                    "No wallet account is available for batch advance.",
                    Map.of("accountFound", false)
            );
        }
        var batchResult = walletService.batchAdvanceTransactions(
                userId,
                account.getId(),
                REMEDIATION_WALLET_MAX_ITEMS,
                "suite-remediation",
                ipAddress
        );
        String status = resolveBatchExecutionStatus(batchResult.processedCount(), batchResult.successCount(), batchResult.failedCount());
        return executionResult(
                ACTION_WALLET_BATCH_ADVANCE,
                "WALLET",
                status,
                "Wallet batch advance executed.",
                Map.of(
                        "accountId", batchResult.accountId(),
                        "processedCount", batchResult.processedCount(),
                        "successCount", batchResult.successCount(),
                        "failedCount", batchResult.failedCount(),
                        "skippedCount", batchResult.skippedCount()
                )
        );
    }

    private SuiteRemediationExecutionResultVo executeWalletBatchReconcile(Long userId, String ipAddress) {
        WalletAccount account = loadLatestWalletAccount(userId);
        if (account == null) {
            return executionResult(
                    ACTION_WALLET_BATCH_RECONCILE,
                    "WALLET",
                    EXECUTION_NO_OP,
                    "No wallet account is available for batch reconcile.",
                    Map.of("accountFound", false)
            );
        }
        var batchResult = walletService.batchReconcileTransactions(
                userId,
                account.getId(),
                REMEDIATION_WALLET_MAX_ITEMS,
                "AUTO",
                ipAddress
        );
        String status = resolveBatchExecutionStatus(batchResult.processedCount(), batchResult.successCount(), batchResult.failedCount());
        return executionResult(
                ACTION_WALLET_BATCH_RECONCILE,
                "WALLET",
                status,
                "Wallet batch reconcile executed.",
                Map.of(
                        "accountId", batchResult.accountId(),
                        "processedCount", batchResult.processedCount(),
                        "successCount", batchResult.successCount(),
                        "failedCount", batchResult.failedCount(),
                        "skippedCount", batchResult.skippedCount()
                )
        );
    }

    private WalletAccount loadLatestWalletAccount(Long userId) {
        return walletAccountMapper.selectOne(new LambdaQueryWrapper<WalletAccount>()
                .eq(WalletAccount::getOwnerId, userId)
                .orderByDesc(WalletAccount::getUpdatedAt)
                .last("limit 1"));
    }

    private String resolveBatchExecutionStatus(int processedCount, int successCount, int failedCount) {
        if (processedCount == 0) {
            return EXECUTION_NO_OP;
        }
        if (successCount > 0) {
            return EXECUTION_SUCCESS;
        }
        if (failedCount > 0) {
            return EXECUTION_FAILED;
        }
        return EXECUTION_NO_OP;
    }

    private String normalizeActionCode(String actionCode) {
        if (!StringUtils.hasText(actionCode)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Suite remediation actionCode is required");
        }
        String normalized = actionCode.trim().toUpperCase();
        if (!isSupportedActionCode(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Suite remediation actionCode is invalid");
        }
        return normalized;
    }

    private boolean isSupportedActionCode(String actionCode) {
        return ACTION_MAIL_ADD_BLOCKED_DOMAIN_BASELINE.equals(actionCode)
                || ACTION_MAIL_ADD_BLOCKED_SENDER_BASELINE.equals(actionCode)
                || ACTION_MAIL_ADD_TRUSTED_DOMAIN_BASELINE.equals(actionCode)
                || ACTION_MAIL_ADD_TRUSTED_SENDER_BASELINE.equals(actionCode)
                || ACTION_MAIL_REMOVE_BLOCKED_DOMAIN_BASELINE.equals(actionCode)
                || ACTION_MAIL_REMOVE_BLOCKED_SENDER_BASELINE.equals(actionCode)
                || ACTION_MAIL_REMOVE_TRUSTED_DOMAIN_BASELINE.equals(actionCode)
                || ACTION_MAIL_REMOVE_TRUSTED_SENDER_BASELINE.equals(actionCode)
                || ACTION_PASS_CREATE_BASELINE_ITEM.equals(actionCode)
                || ACTION_PASS_DELETE_BASELINE_ITEM.equals(actionCode)
                || ACTION_VPN_CONNECT_SECURE_CORE_BASELINE.equals(actionCode)
                || ACTION_VPN_DISCONNECT_CURRENT.equals(actionCode)
                || ACTION_WALLET_BATCH_ADVANCE.equals(actionCode)
                || ACTION_WALLET_BATCH_RECONCILE.equals(actionCode);
    }

    private String safeErrorMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "unknown";
        }
        return message.length() > 160 ? message.substring(0, 160) : message;
    }

    private SuiteRemediationExecutionResultVo executionResult(
            String actionCode,
            String productCode,
            String status,
            String message,
            Map<String, Object> details
    ) {
        return new SuiteRemediationExecutionResultVo(
                actionCode,
                productCode,
                status,
                message,
                LocalDateTime.now(),
                details
        );
    }

    private GovernanceTemplateDefinition requireGovernanceTemplate(String templateCode) {
        if (!StringUtils.hasText(templateCode)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Governance templateCode is required");
        }
        GovernanceTemplateDefinition template = GOVERNANCE_TEMPLATE_DEFINITIONS.get(templateCode.trim().toUpperCase());
        if (template == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Governance templateCode is invalid");
        }
        return template;
    }

    private GovernanceRequestAccess loadGovernanceRequestAccess(Long userId, String requestId) {
        if (!StringUtils.hasText(requestId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Governance requestId is required");
        }
        SuiteGovernanceRequest entity = suiteGovernanceRequestMapper.selectOne(
                new LambdaQueryWrapper<SuiteGovernanceRequest>()
                        .eq(SuiteGovernanceRequest::getRequestId, requestId.trim())
                        .last("limit 1")
        );
        if (entity == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Governance request is not found");
        }
        if (entity.getOrgId() == null) {
            if (!entity.getOwnerId().equals(userId)) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "Governance request is not found");
            }
            return new GovernanceRequestAccess(entity, null, null);
        }
        OrgService.OrgGovernanceAccess access = orgService.resolveGovernanceAccess(userId, entity.getOrgId());
        return new GovernanceRequestAccess(entity, entity.getOrgId(), access);
    }

    private OrgService.OrgGovernanceAccess assertCanCreateOrgGovernanceRequest(Long userId, Long orgId) {
        OrgService.OrgGovernanceAccess access = orgService.resolveGovernanceAccess(userId, orgId);
        if (ORG_ROLE_OWNER.equals(access.role()) || ORG_ROLE_ADMIN.equals(access.role())) {
            return access;
        }
        throw new BizException(ErrorCode.ORG_FORBIDDEN, ORG_GOVERNANCE_CREATE_FORBIDDEN_MESSAGE);
    }

    private void assertCanReviewGovernance(GovernanceRequestAccess access) {
        if (!access.orgScoped()) {
            return;
        }
        OrgService.OrgGovernanceAccess orgAccess = access.orgAccess();
        if (ORG_ROLE_OWNER.equals(orgAccess.role())) {
            return;
        }
        if (ORG_ROLE_ADMIN.equals(orgAccess.role()) && orgAccess.adminCanReviewGovernance()) {
            return;
        }
        throw new BizException(ErrorCode.ORG_FORBIDDEN, ORG_GOVERNANCE_REVIEW_FORBIDDEN_MESSAGE);
    }

    private void assertCanExecuteGovernance(GovernanceRequestAccess access) {
        if (!access.orgScoped()) {
            return;
        }
        OrgService.OrgGovernanceAccess orgAccess = access.orgAccess();
        if (ORG_ROLE_OWNER.equals(orgAccess.role())) {
            return;
        }
        if (ORG_ROLE_ADMIN.equals(orgAccess.role()) && orgAccess.adminCanExecuteGovernance()) {
            return;
        }
        throw new BizException(ErrorCode.ORG_FORBIDDEN, ORG_GOVERNANCE_EXECUTION_FORBIDDEN_MESSAGE);
    }

    private void assertGovernanceRequestReviewable(SuiteGovernanceRequest entity) {
        String status = entity.getStatus();
        if (GOVERNANCE_STATUS_PENDING_REVIEW.equals(status)) {
            return;
        }
        if (isDualReviewEnabled(entity) && GOVERNANCE_STATUS_PENDING_SECOND_REVIEW.equals(status)) {
            return;
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, "Governance request is not pending review");
    }

    private String applyReviewDecision(
            SuiteGovernanceRequest entity,
            Long userId,
            Long sessionId,
            String decision,
            String reviewNote,
            LocalDateTime reviewedAt
    ) {
        if (isDualReviewEnabled(entity) && GOVERNANCE_STATUS_PENDING_REVIEW.equals(entity.getStatus())) {
            assertFirstReviewerNotAssignedSecondReviewer(entity, userId);
        }
        if (isDualReviewEnabled(entity) && GOVERNANCE_STATUS_PENDING_SECOND_REVIEW.equals(entity.getStatus())) {
            assertDesignatedSecondReviewer(entity, userId);
            assertSecondReviewerDistinct(entity, userId);
        }
        if (GOVERNANCE_REVIEW_DECISION_REJECT.equals(decision)) {
            applyFinalReviewFields(entity, userId, sessionId, reviewNote, reviewedAt);
            entity.setStatus(GOVERNANCE_STATUS_REJECTED);
            return GOVERNANCE_STATUS_REJECTED;
        }
        if (isDualReviewEnabled(entity) && GOVERNANCE_STATUS_PENDING_REVIEW.equals(entity.getStatus())) {
            applyFirstReviewFields(entity, userId, sessionId, reviewNote, reviewedAt);
            entity.setStatus(GOVERNANCE_STATUS_PENDING_SECOND_REVIEW);
            return GOVERNANCE_STATUS_PENDING_SECOND_REVIEW;
        }
        applyFinalReviewFields(entity, userId, sessionId, reviewNote, reviewedAt);
        entity.setStatus(GOVERNANCE_STATUS_APPROVED_PENDING_EXECUTION);
        return GOVERNANCE_STATUS_APPROVED_PENDING_EXECUTION;
    }

    private void applyFirstReviewFields(
            SuiteGovernanceRequest entity,
            Long userId,
            Long sessionId,
            String reviewNote,
            LocalDateTime reviewedAt
    ) {
        entity.setFirstReviewNote(reviewNote);
        entity.setFirstReviewedAt(reviewedAt);
        entity.setFirstReviewedByUserId(userId);
        entity.setFirstReviewedBySessionId(sessionId);
        entity.setReviewNote(null);
        entity.setReviewedAt(null);
        entity.setReviewedByUserId(null);
        entity.setReviewedBySessionId(null);
    }

    private void applyFinalReviewFields(
            SuiteGovernanceRequest entity,
            Long userId,
            Long sessionId,
            String reviewNote,
            LocalDateTime reviewedAt
    ) {
        entity.setReviewNote(reviewNote);
        entity.setReviewedAt(reviewedAt);
        entity.setReviewedByUserId(userId);
        entity.setReviewedBySessionId(sessionId);
    }

    private void assertSecondReviewerDistinct(SuiteGovernanceRequest entity, Long userId) {
        if (entity.getFirstReviewedByUserId() != null && entity.getFirstReviewedByUserId().equals(userId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, GOVERNANCE_SECOND_REVIEW_USER_CONFLICT_MESSAGE);
        }
    }

    private void assertFirstReviewerNotAssignedSecondReviewer(SuiteGovernanceRequest entity, Long userId) {
        if (entity.getSecondReviewerUserId() == null) {
            return;
        }
        if (entity.getSecondReviewerUserId().equals(userId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, GOVERNANCE_SECOND_REVIEW_ASSIGNMENT_FIRST_REVIEW_CONFLICT_MESSAGE);
        }
    }

    private void assertDesignatedSecondReviewer(SuiteGovernanceRequest entity, Long userId) {
        if (entity.getSecondReviewerUserId() == null) {
            return;
        }
        if (!entity.getSecondReviewerUserId().equals(userId)) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, GOVERNANCE_SECOND_REVIEW_ASSIGNMENT_REQUIRED_MESSAGE);
        }
    }

    private void assertCanBeDesignatedSecondReviewer(Long orgId, Long secondReviewerUserId) {
        OrgService.OrgGovernanceAccess access = orgService.resolveGovernanceAccess(secondReviewerUserId, orgId);
        if (ORG_ROLE_OWNER.equals(access.role())) {
            return;
        }
        if (ORG_ROLE_ADMIN.equals(access.role()) && access.adminCanReviewGovernance()) {
            return;
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, GOVERNANCE_SECOND_REVIEW_ASSIGNMENT_INVALID_MESSAGE);
    }

    private boolean isDualReviewEnabled(SuiteGovernanceRequest entity) {
        return entity.getRequireDualReview() != null && entity.getRequireDualReview() == 1;
    }

    private String resolveReviewStage(SuiteGovernanceRequest entity) {
        if (!isDualReviewEnabled(entity)) {
            return "SINGLE_REVIEW";
        }
        if (GOVERNANCE_STATUS_PENDING_REVIEW.equals(entity.getStatus())) {
            return "FIRST_REVIEW_PENDING";
        }
        if (GOVERNANCE_STATUS_PENDING_SECOND_REVIEW.equals(entity.getStatus())) {
            return "SECOND_REVIEW_PENDING";
        }
        return "REVIEW_COMPLETED";
    }

    private Long normalizeOptionalOrgId(Long orgId) {
        if (orgId == null) {
            return null;
        }
        if (orgId <= 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "orgId must be greater than 0");
        }
        return orgId;
    }

    private Long normalizeOptionalUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        if (userId <= 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "secondReviewerUserId must be greater than 0");
        }
        return userId;
    }

    private String sanitizeReviewDecision(String decision) {
        if (!StringUtils.hasText(decision)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Review decision is required");
        }
        String normalized = decision.trim().toUpperCase();
        if (!GOVERNANCE_REVIEW_DECISION_APPROVE.equals(normalized)
                && !GOVERNANCE_REVIEW_DECISION_REJECT.equals(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Review decision is invalid");
        }
        return normalized;
    }

    private String sanitizeFreeText(String value, String requiredMessage, int maxLength) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, requiredMessage);
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            return normalized.substring(0, maxLength);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value, String fallback, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            return normalized.substring(0, maxLength);
        }
        return normalized;
    }

    private String writeAsJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to serialize governance payload");
        }
    }

    private List<String> readStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
            return list == null ? List.of() : list;
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to parse governance string list");
        }
    }

    private List<SuiteRemediationExecutionResultVo> readExecutionResults(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<SuiteRemediationExecutionResultVo> list = objectMapper.readValue(
                    json,
                    new TypeReference<List<SuiteRemediationExecutionResultVo>>() {
                    }
            );
            return list == null ? List.of() : list;
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to parse governance execution results");
        }
    }

    private List<SuiteRemediationExecutionResultVo> executeActionBatch(Long userId, List<String> actionCodes, String ipAddress) {
        List<SuiteRemediationExecutionResultVo> results = new ArrayList<>();
        for (String actionCode : actionCodes) {
            try {
                results.add(executeRemediationAction(userId, actionCode, ipAddress));
            } catch (BizException exception) {
                results.add(executionResult(
                        actionCode,
                        productCodeForAction(actionCode),
                        EXECUTION_FAILED,
                        "Execution failed: " + safeErrorMessage(exception.getMessage()),
                        Map.of("error", safeErrorMessage(exception.getMessage()))
                ));
            } catch (Exception exception) {
                results.add(executionResult(
                        actionCode,
                        productCodeForAction(actionCode),
                        EXECUTION_FAILED,
                        "Execution failed: internal error",
                        Map.of("error", "internal-error")
                ));
            }
        }
        return results;
    }

    private String productCodeForAction(String actionCode) {
        if (actionCode == null) {
            return "SECURITY";
        }
        if (actionCode.startsWith("MAIL_")) {
            return "MAIL";
        }
        if (actionCode.startsWith("PASS_")) {
            return "PASS";
        }
        if (actionCode.startsWith("VPN_")) {
            return "VPN";
        }
        if (actionCode.startsWith("WALLET_")) {
            return "WALLET";
        }
        return "SECURITY";
    }

    private boolean hasExecutionFailure(List<SuiteRemediationExecutionResultVo> results) {
        return results.stream().anyMatch(result -> EXECUTION_FAILED.equals(result.status()));
    }

    private SuiteGovernancePolicyTemplateVo toGovernanceTemplateVo(GovernanceTemplateDefinition template) {
        return new SuiteGovernancePolicyTemplateVo(
                template.templateCode(),
                template.name(),
                template.riskLevel(),
                template.description(),
                template.actionCodes(),
                template.rollbackActionCodes(),
                template.approvalRequired()
        );
    }

    private SuiteGovernanceChangeRequestVo toGovernanceChangeRequestVo(SuiteGovernanceRequest entity) {
        boolean reviewSlaBreached = entity.getReviewDueAt() != null
                && entity.getReviewedAt() == null
                && LocalDateTime.now().isAfter(entity.getReviewDueAt());
        return new SuiteGovernanceChangeRequestVo(
                entity.getRequestId(),
                entity.getOrgId() == null ? null : String.valueOf(entity.getOrgId()),
                String.valueOf(entity.getOwnerId()),
                entity.getTemplateCode(),
                entity.getTemplateName(),
                entity.getStatus(),
                entity.getReason(),
                isDualReviewEnabled(entity),
                resolveReviewStage(entity),
                entity.getFirstReviewNote(),
                entity.getFirstReviewedAt(),
                entity.getFirstReviewedByUserId(),
                entity.getFirstReviewedBySessionId(),
                entity.getSecondReviewerUserId(),
                entity.getReviewNote(),
                entity.getApprovalNote(),
                entity.getRollbackReason(),
                entity.getRequestedAt(),
                entity.getReviewDueAt(),
                reviewSlaBreached,
                entity.getReviewedAt(),
                entity.getReviewedByUserId(),
                entity.getReviewedBySessionId(),
                entity.getApprovedAt(),
                entity.getExecutedAt(),
                entity.getExecutedByUserId(),
                entity.getExecutedBySessionId(),
                entity.getRolledBackAt(),
                readStringList(entity.getActionCodesJson()),
                readStringList(entity.getRollbackActionCodesJson()),
                readExecutionResults(entity.getExecutionResultsJson()),
                readExecutionResults(entity.getRollbackResultsJson())
        );
    }

    private Long requireSessionId(Long sessionId) {
        if (sessionId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Session context is missing");
        }
        return sessionId;
    }

    private String generateGovernanceRequestId() {
        return "GCR-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static Map<String, GovernanceTemplateDefinition> buildGovernanceTemplates() {
        LinkedHashMap<String, GovernanceTemplateDefinition> templates = new LinkedHashMap<>();
        templates.put(
                GOVERNANCE_TEMPLATE_SECURITY_BASELINE_HARDENING,
                new GovernanceTemplateDefinition(
                        GOVERNANCE_TEMPLATE_SECURITY_BASELINE_HARDENING,
                        "Security Baseline Hardening",
                        RISK_HIGH,
                        "Apply baseline sender/domain protections and trust rules for mail security.",
                        List.of(
                                ACTION_MAIL_ADD_BLOCKED_DOMAIN_BASELINE,
                                ACTION_MAIL_ADD_BLOCKED_SENDER_BASELINE,
                                ACTION_MAIL_ADD_TRUSTED_DOMAIN_BASELINE,
                                ACTION_MAIL_ADD_TRUSTED_SENDER_BASELINE
                        ),
                        List.of(
                                ACTION_MAIL_REMOVE_BLOCKED_DOMAIN_BASELINE,
                                ACTION_MAIL_REMOVE_BLOCKED_SENDER_BASELINE,
                                ACTION_MAIL_REMOVE_TRUSTED_DOMAIN_BASELINE,
                                ACTION_MAIL_REMOVE_TRUSTED_SENDER_BASELINE
                        ),
                        true
                )
        );
        templates.put(
                GOVERNANCE_TEMPLATE_ACCOUNT_ACCESS_CONTAINMENT,
                new GovernanceTemplateDefinition(
                        GOVERNANCE_TEMPLATE_ACCOUNT_ACCESS_CONTAINMENT,
                        "Account Access Containment",
                        RISK_CRITICAL,
                        "Create secure baseline credentials and enforce VPN secure-core connectivity.",
                        List.of(
                                ACTION_PASS_CREATE_BASELINE_ITEM,
                                ACTION_VPN_CONNECT_SECURE_CORE_BASELINE
                        ),
                        List.of(
                                ACTION_PASS_DELETE_BASELINE_ITEM,
                                ACTION_VPN_DISCONNECT_CURRENT
                        ),
                        true
                )
        );
        return Map.copyOf(templates);
    }

    private SuiteReadinessReportVo buildReadinessReport(Long userId, String ipAddress, Set<String> visibleProductCodes) {
        String normalizedIp = normalizeIp(ipAddress);
        SuiteSubscriptionVo subscription = suiteService.getSubscription(userId, normalizedIp);
        List<SuiteProductStatusVo> products = suiteService.listProducts(userId, normalizedIp).stream()
                .filter(product -> isProductVisible(visibleProductCodes, product.code()))
                .toList();
        ProductSnapshot snapshot = buildSnapshot(userId, subscription);

        List<SuiteReadinessItemVo> items = products.stream()
                .map(product -> assessProduct(product, snapshot))
                .toList();

        int overallScore = items.isEmpty()
                ? 0
                : (int) Math.round(items.stream().mapToInt(SuiteReadinessItemVo::score).average().orElse(0));
        int highRiskProductCount = (int) items.stream().filter(item -> RISK_HIGH.equals(item.riskLevel())).count();
        int criticalRiskProductCount = (int) items.stream().filter(item -> RISK_CRITICAL.equals(item.riskLevel())).count();

        return new SuiteReadinessReportVo(
                LocalDateTime.now(),
                overallScore,
                toRiskLevel(overallScore),
                highRiskProductCount,
                criticalRiskProductCount,
                items
        );
    }

    private boolean isProductVisible(Set<String> visibleProductCodes, String productCode) {
        if (visibleProductCodes == null || visibleProductCodes.isEmpty()) {
            return true;
        }
        if (!StringUtils.hasText(productCode)) {
            return true;
        }
        return visibleProductCodes.contains(productCode.trim().toUpperCase(Locale.ROOT));
    }

    private ProductSnapshot buildSnapshot(Long userId, SuiteSubscriptionVo subscription) {
        long docsCount = safeCount(docsNoteMapper.selectCount(new LambdaQueryWrapper<DocsNote>()
                .eq(DocsNote::getOwnerId, userId)
                .eq(DocsNote::getWorkspaceType, "DOCS")));
        long standardNotesCount = safeCount(docsNoteMapper.selectCount(new LambdaQueryWrapper<DocsNote>()
                .eq(DocsNote::getOwnerId, userId)
                .eq(DocsNote::getWorkspaceType, "STANDARD_NOTES")));
        long standardNotesPinnedCount = safeCount(standardNoteProfileMapper.selectCount(new LambdaQueryWrapper<StandardNoteProfile>()
                .eq(StandardNoteProfile::getOwnerId, userId)
                .eq(StandardNoteProfile::getPinned, 1)));
        long standardNotesTaggedCount = safeCount(standardNoteProfileMapper.selectCount(new LambdaQueryWrapper<StandardNoteProfile>()
                .eq(StandardNoteProfile::getOwnerId, userId)
                .and(query -> query.isNotNull(StandardNoteProfile::getTagsJson).ne(StandardNoteProfile::getTagsJson, "").ne(StandardNoteProfile::getTagsJson, "[]"))));
        long standardNotesFolderCount = safeCount(standardNoteFolderMapper.selectCount(new LambdaQueryWrapper<StandardNoteFolder>()
                .eq(StandardNoteFolder::getOwnerId, userId)));
        long standardNotesChecklistTaskCount = countStandardNotesChecklistTasks(userId);
        long sheetsWorkbookCount = safeCount(sheetsWorkbookMapper.selectCount(new LambdaQueryWrapper<SheetsWorkbook>()
                .eq(SheetsWorkbook::getOwnerId, userId)));
        long sheetsFormulaCellCount = countSheetsFormulaCells(userId);
        long passItemCount = safeCount(passVaultItemMapper.selectCount(new LambdaQueryWrapper<PassVaultItem>()
                .eq(PassVaultItem::getOwnerId, userId)));
        long simpleLoginAliasCount = safeCount(passMailAliasMapper.selectCount(new LambdaQueryWrapper<PassMailAlias>()
                .eq(PassMailAlias::getOwnerId, userId)));
        long simpleLoginVerifiedMailboxCount = safeCount(passMailboxMapper.selectCount(new LambdaQueryWrapper<PassMailbox>()
                .eq(PassMailbox::getOwnerId, userId)
                .eq(PassMailbox::getStatus, "VERIFIED")));
        long simpleLoginReverseAliasCount = safeCount(passAliasContactMapper.selectCount(new LambdaQueryWrapper<PassAliasContact>()
                .eq(PassAliasContact::getOwnerId, userId)));
        List<Long> activeOrgIds = loadActiveOrgIds(userId);
        long simpleLoginRelayPolicyCount = countSimpleLoginRelayPolicies(activeOrgIds);
        long simpleLoginCatchAllDomainCount = countSimpleLoginCatchAllPolicies(activeOrgIds);
        long authenticatorEntryCount = safeCount(authenticatorEntryMapper.selectCount(new LambdaQueryWrapper<AuthenticatorEntry>()
                .eq(AuthenticatorEntry::getOwnerId, userId)));
        long vpnConnectedCount = safeCount(vpnConnectionSessionMapper.selectCount(new LambdaQueryWrapper<VpnConnectionSession>()
                .eq(VpnConnectionSession::getOwnerId, userId)
                .eq(VpnConnectionSession::getStatus, "CONNECTED")));
        long walletTxCount = safeCount(walletTransactionMapper.selectCount(new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getOwnerId, userId)));
        long walletSignedCount = safeCount(walletTransactionMapper.selectCount(new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getOwnerId, userId)
                .eq(WalletTransaction::getStatus, "SIGNED")));
        long walletPendingCount = safeCount(walletTransactionMapper.selectCount(new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getOwnerId, userId)
                .eq(WalletTransaction::getStatus, "PENDING")));
        long walletBroadcastedCount = safeCount(walletTransactionMapper.selectCount(new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getOwnerId, userId)
                .eq(WalletTransaction::getStatus, "BROADCASTED")));
        long walletFailedCount = safeCount(walletTransactionMapper.selectCount(new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getOwnerId, userId)
                .eq(WalletTransaction::getStatus, "FAILED")));
        long walletBlockedCount = countWalletBlockedTransactions(userId);
        long lumoProjectCount = safeCount(lumoProjectMapper.selectCount(new LambdaQueryWrapper<LumoProject>()
                .eq(LumoProject::getOwnerId, userId)));
        long lumoMessageCount = safeCount(lumoMessageMapper.selectCount(new LambdaQueryWrapper<LumoMessage>()
                .eq(LumoMessage::getOwnerId, userId)));
        long lumoKnowledgeCount = safeCount(lumoProjectKnowledgeMapper.selectCount(new LambdaQueryWrapper<LumoProjectKnowledge>()
                .eq(LumoProjectKnowledge::getOwnerId, userId)));
        long meetRoomCount = safeCount(meetRoomSessionMapper.selectCount(new LambdaQueryWrapper<MeetRoomSession>()
                .eq(MeetRoomSession::getOwnerId, userId)));
        long meetQualityCount = safeCount(meetQualitySnapshotMapper.selectCount(new LambdaQueryWrapper<MeetQualitySnapshot>()
                .eq(MeetQualitySnapshot::getOwnerId, userId)));
        long blockedSenderCount = countBlockedSenders(userId);
        long blockedDomainCount = countBlockedDomains(userId);

        return new ProductSnapshot(
                subscription.usage().mailCount(),
                subscription.usage().contactCount(),
                subscription.usage().calendarEventCount(),
                subscription.usage().calendarShareCount(),
                subscription.usage().driveFileCount(),
                subscription.usage().driveStorageBytes(),
                subscription.plan().driveStorageMb() * 1024L * 1024L,
                docsCount,
                standardNotesCount,
                standardNotesPinnedCount,
                standardNotesTaggedCount,
                standardNotesFolderCount,
                standardNotesChecklistTaskCount,
                sheetsWorkbookCount,
                sheetsFormulaCellCount,
                passItemCount,
                simpleLoginAliasCount,
                simpleLoginVerifiedMailboxCount,
                simpleLoginReverseAliasCount,
                simpleLoginRelayPolicyCount,
                simpleLoginCatchAllDomainCount,
                authenticatorEntryCount,
                vpnConnectedCount,
                walletTxCount,
                walletSignedCount,
                walletPendingCount,
                walletBroadcastedCount,
                walletFailedCount,
                walletBlockedCount,
                lumoProjectCount,
                lumoMessageCount,
                lumoKnowledgeCount,
                meetRoomCount,
                meetQualityCount,
                blockedSenderCount,
                blockedDomainCount
        );
    }

    private SuiteReadinessItemVo assessProduct(SuiteProductStatusVo product, ProductSnapshot snapshot) {
        if (!product.enabledByPlan()) {
            return new SuiteReadinessItemVo(
                    product.code(),
                    product.name(),
                    product.category(),
                    false,
                    62,
                    RISK_MEDIUM,
                    List.of(
                            signal("plan_locked", 1, "Current subscription does not enable this product")
                    ),
                    List.of("Current subscription plan does not enable this product."),
                    List.of(action("P1", product.code(), "Upgrade suite plan to unlock this product workflow."))
            );
        }

        int score = 90;
        List<SuiteReadinessSignalVo> signals = new ArrayList<>();
        List<String> blockers = new ArrayList<>();
        List<SuiteRemediationActionVo> actions = new ArrayList<>();

        switch (product.code()) {
            case "MAIL" -> {
                signals.add(signal("mail_count", snapshot.mailCount(), "Mailbox message count"));
                signals.add(signal("blocked_sender_count", snapshot.blockedSenderCount(), "Blocked sender rules"));
                signals.add(signal("blocked_domain_count", snapshot.blockedDomainCount(), "Blocked domain rules"));
                if (snapshot.mailCount() == 0) {
                    score -= 28;
                    blockers.add("Mailbox has no activity signal.");
                    actions.add(action("P1", "MAIL", "Send and receive seed emails to verify delivery path."));
                }
                if (snapshot.blockedDomainCount() == 0) {
                    score -= 12;
                    blockers.add("No blocked domain policy configured.");
                    actions.add(action("P0", "MAIL", "Configure blocked domains for phishing baseline.", ACTION_MAIL_ADD_BLOCKED_DOMAIN_BASELINE));
                }
            }
            case "CALENDAR" -> {
                signals.add(signal("event_count", snapshot.calendarEventCount(), "Calendar events created"));
                signals.add(signal("share_count", snapshot.calendarShareCount(), "Calendar shares created"));
                if (snapshot.calendarEventCount() == 0) {
                    score -= 24;
                    blockers.add("No calendar event has been created.");
                    actions.add(action("P1", "CALENDAR", "Create baseline operational events for readiness checks."));
                }
                if (snapshot.calendarShareCount() == 0) {
                    score -= 8;
                    actions.add(action("P2", "CALENDAR", "Share one event to validate collaboration workflow."));
                }
            }
            case "CONTACTS" -> {
                signals.add(signal("contact_count", snapshot.contactCount(), "Contacts in workspace"));
                if (snapshot.contactCount() < 5) {
                    score -= 22;
                    blockers.add("Contact directory coverage is below baseline (<5)." );
                    actions.add(action("P1", "CONTACTS", "Import or create contacts to complete communication graph."));
                }
            }
            case "DRIVE" -> {
                signals.add(signal("file_count", snapshot.driveFileCount(), "Drive files"));
                signals.add(signal("storage_bytes", snapshot.driveStorageBytes(), "Drive storage bytes"));
                if (snapshot.driveFileCount() == 0) {
                    score -= 20;
                    blockers.add("No file in Drive workspace.");
                    actions.add(action("P1", "DRIVE", "Upload baseline files to validate storage workflow."));
                }
                if (snapshot.driveStorageLimitBytes() > 0
                        && snapshot.driveStorageBytes() * 100 / snapshot.driveStorageLimitBytes() > 90) {
                    score -= 14;
                    blockers.add("Drive storage usage exceeds 90% threshold.");
                    actions.add(action("P0", "DRIVE", "Archive or clean files to recover storage headroom."));
                }
            }
            case "DOCS" -> {
                signals.add(signal("docs_note_count", snapshot.docsCount(), "Docs notes"));
                if (snapshot.docsCount() == 0) {
                    score -= 30;
                    blockers.add("Docs collaborative content is empty.");
                    actions.add(action("P1", "DOCS", "Create at least one collaborative doc to validate editing flow."));
                }
            }
            case "STANDARD_NOTES" -> {
                signals.add(signal("standard_note_count", snapshot.standardNotesCount(), "Standard Notes items"));
                signals.add(signal("pinned_note_count", snapshot.standardNotesPinnedCount(), "Pinned notes"));
                signals.add(signal("tagged_note_count", snapshot.standardNotesTaggedCount(), "Tagged notes"));
                signals.add(signal("folder_count", snapshot.standardNotesFolderCount(), "Folders"));
                signals.add(signal("checklist_task_count", snapshot.standardNotesChecklistTaskCount(), "Checklist tasks"));
                if (snapshot.standardNotesCount() == 0) {
                    score -= 30;
                    blockers.add("Standard Notes workspace has no notes.");
                    actions.add(action("P1", "STANDARD_NOTES", "Create at least one private note and validate metadata flow."));
                }
                if (snapshot.standardNotesTaggedCount() == 0) {
                    score -= 10;
                    actions.add(action("P2", "STANDARD_NOTES", "Add tags to notes so personal knowledge can be organized."));
                }
                if (snapshot.standardNotesPinnedCount() == 0) {
                    score -= 6;
                    actions.add(action("P2", "STANDARD_NOTES", "Pin one note for quick access to core reference material."));
                }
                if (snapshot.standardNotesCount() > 0 && snapshot.standardNotesFolderCount() == 0) {
                    score -= 8;
                    actions.add(action("P2", "STANDARD_NOTES", "Group notes into at least one folder for calmer organization."));
                }
                if (snapshot.standardNotesCount() > 0 && snapshot.standardNotesChecklistTaskCount() == 0) {
                    score -= 6;
                    actions.add(action("P2", "STANDARD_NOTES", "Create one checklist note so task execution can be validated."));
                }
            }
            case "SHEETS" -> {
                signals.add(signal("sheets_workbook_count", snapshot.sheetsWorkbookCount(), "Sheets workbooks"));
                signals.add(signal("sheets_formula_cell_count", snapshot.sheetsFormulaCellCount(), "Formula cells"));
                if (snapshot.sheetsWorkbookCount() == 0) {
                    score -= 30;
                    blockers.add("Sheets workspace has no workbooks.");
                    actions.add(action("P1", "SHEETS", "Create at least one workbook and validate the grid editing flow."));
                } else if (snapshot.sheetsWorkbookCount() < 3) {
                    score -= 14;
                    actions.add(action("P2", "SHEETS", "Expand workbook coverage for recurring planning and reporting workflows."));
                }
                if (snapshot.sheetsWorkbookCount() > 0 && snapshot.sheetsFormulaCellCount() == 0) {
                    score -= 10;
                    actions.add(action("P2", "SHEETS", "Add formula-driven cells so workbook automation and reporting flows are exercised."));
                }
            }
            case "PASS" -> {
                signals.add(signal("vault_item_count", snapshot.passItemCount(), "Pass vault items"));
                if (snapshot.passItemCount() == 0) {
                    score -= 34;
                    blockers.add("Pass vault has no credentials.");
                    actions.add(action("P0", "PASS", "Create credential vault entries and run health baseline.", ACTION_PASS_CREATE_BASELINE_ITEM));
                } else if (snapshot.passItemCount() < 5) {
                    score -= 16;
                    actions.add(action("P2", "PASS", "Expand vault coverage for key business systems."));
                }
            }
            case "SIMPLELOGIN" -> {
                signals.add(signal("alias_count", snapshot.simpleLoginAliasCount(), "SimpleLogin aliases"));
                signals.add(signal("verified_mailbox_count", snapshot.simpleLoginVerifiedMailboxCount(), "Verified mailboxes"));
                signals.add(signal("reverse_alias_contact_count", snapshot.simpleLoginReverseAliasCount(), "Reverse alias contacts"));
                signals.add(signal("relay_policy_count", snapshot.simpleLoginRelayPolicyCount(), "Relay domain policies"));
                signals.add(signal("catch_all_domain_count", snapshot.simpleLoginCatchAllDomainCount(), "Catch-all domains"));
                if (snapshot.simpleLoginAliasCount() == 0) {
                    score -= 28;
                    blockers.add("SimpleLogin workspace has no aliases.");
                    actions.add(action("P1", "SIMPLELOGIN", "Create at least one alias and validate relay routing."));
                }
                if (snapshot.simpleLoginVerifiedMailboxCount() == 0) {
                    score -= 24;
                    blockers.add("SimpleLogin workspace has no verified mailbox targets.");
                    actions.add(action("P1", "SIMPLELOGIN", "Verify mailbox targets before enabling relay workflows."));
                }
                if (snapshot.simpleLoginReverseAliasCount() == 0) {
                    score -= 8;
                    actions.add(action("P2", "SIMPLELOGIN", "Create reverse alias contacts to unlock sender privacy flow."));
                }
                if (snapshot.simpleLoginRelayPolicyCount() == 0) {
                    score -= 14;
                    actions.add(action("P1", "SIMPLELOGIN", "Attach a relay policy to at least one verified custom domain."));
                }
                if (snapshot.simpleLoginRelayPolicyCount() > 0 && snapshot.simpleLoginCatchAllDomainCount() == 0) {
                    score -= 4;
                    actions.add(action("P2", "SIMPLELOGIN", "Evaluate catch-all coverage for verified relay domains."));
                }
            }
            case "AUTHENTICATOR" -> {
                signals.add(signal("totp_entry_count", snapshot.authenticatorEntryCount(), "Authenticator entries"));
                if (snapshot.authenticatorEntryCount() == 0) {
                    score -= 34;
                    blockers.add("No authenticator entry configured.");
                    actions.add(action("P0", "AUTHENTICATOR", "Enroll at least one TOTP account and verify code lifecycle."));
                }
            }
            case "VPN" -> {
                signals.add(signal("connected_session_count", snapshot.vpnConnectedCount(), "Connected VPN sessions"));
                if (snapshot.vpnConnectedCount() == 0) {
                    score -= 28;
                    blockers.add("VPN has no active connectivity signal.");
                    actions.add(action("P1", "VPN", "Establish one VPN session to validate secure routing.", ACTION_VPN_CONNECT_SECURE_CORE_BASELINE));
                }
            }
            case "WALLET" -> {
                signals.add(signal("transaction_count", snapshot.walletTxCount(), "Wallet transaction count"));
                signals.add(signal("signed_tx_count", snapshot.walletSignedCount(), "Signed transactions"));
                signals.add(signal("pending_tx_count", snapshot.walletPendingCount(), "Pending transactions"));
                signals.add(signal("broadcasted_tx_count", snapshot.walletBroadcastedCount(), "Broadcasted transactions"));
                signals.add(signal("failed_tx_count", snapshot.walletFailedCount(), "Failed transactions"));
                signals.add(signal("blocked_mid_stage_count", snapshot.walletBlockedCount(), "Blocked mid-stage transactions"));
                if (snapshot.walletTxCount() == 0) {
                    score -= 28;
                    blockers.add("Wallet has no transaction lifecycle evidence.");
                    actions.add(action("P1", "WALLET", "Execute receive/send flow to generate wallet telemetry."));
                }
                if (snapshot.walletSignedCount() == 0) {
                    score -= 12;
                    actions.add(action("P1", "WALLET", "Run transaction signing to validate signature governance."));
                }
                if (snapshot.walletFailedCount() > 0) {
                    score -= Math.min(22, 8 + (int) (snapshot.walletFailedCount() * 4));
                    blockers.add("Wallet contains failed transactions requiring remediation.");
                    actions.add(action("P0", "WALLET", "Investigate and resolve failed wallet transactions.", ACTION_WALLET_BATCH_RECONCILE));
                }
                if (snapshot.walletBlockedCount() > 0) {
                    score -= Math.min(20, 6 + (int) (snapshot.walletBlockedCount() * 3));
                    blockers.add("Wallet has blocked mid-stage transactions (pending/signed/broadcasted).");
                    actions.add(action("P0", "WALLET", "Use execution command center to advance or remediate blocked transactions.", ACTION_WALLET_BATCH_ADVANCE));
                }
            }
            case "LUMO" -> {
                signals.add(signal("project_count", snapshot.lumoProjectCount(), "Lumo projects"));
                signals.add(signal("message_count", snapshot.lumoMessageCount(), "Lumo messages"));
                signals.add(signal("knowledge_count", snapshot.lumoKnowledgeCount(), "Lumo knowledge sources"));
                if (snapshot.lumoProjectCount() == 0) {
                    score -= 30;
                    blockers.add("Lumo has no project context.");
                    actions.add(action("P1", "LUMO", "Create projects to anchor privacy AI workflows."));
                }
                if (snapshot.lumoKnowledgeCount() == 0) {
                    score -= 18;
                    blockers.add("Lumo knowledge base is empty.");
                    actions.add(action("P1", "LUMO", "Add project knowledge to improve context grounding."));
                }
                if (snapshot.lumoMessageCount() < 3) {
                    score -= 8;
                    actions.add(action("P2", "LUMO", "Run multi-turn conversations to validate response quality."));
                }
            }
            case "MEET" -> {
                signals.add(signal("room_count", snapshot.meetRoomCount(), "Meet rooms"));
                signals.add(signal("quality_snapshot_count", snapshot.meetQualityCount(), "Quality snapshots"));
                if (snapshot.meetRoomCount() == 0) {
                    score -= 28;
                    blockers.add("Meet has no room activity.");
                    actions.add(action("P1", "MEET", "Create and join meeting rooms to validate session control."));
                }
                if (snapshot.meetQualityCount() == 0) {
                    score -= 16;
                    blockers.add("Meet quality diagnostics has no sample.");
                    actions.add(action("P1", "MEET", "Report participant quality metrics to activate diagnostics."));
                }
            }
            default -> {
                signals.add(signal("fallback", 0, "No specialized readiness evaluator"));
                score -= 8;
            }
        }

        int finalScore = clamp(score);
        return new SuiteReadinessItemVo(
                product.code(),
                product.name(),
                product.category(),
                true,
                finalScore,
                toRiskLevel(finalScore),
                signals,
                blockers,
                deduplicateAndSortActions(actions)
        );
    }

    private long countActiveSessions(Long userId) {
        return safeCount(userSessionMapper.selectCount(new LambdaQueryWrapper<UserSession>()
                .eq(UserSession::getOwnerId, userId)
                .eq(UserSession::getRevoked, 0)
                .gt(UserSession::getExpiresAt, LocalDateTime.now())));
    }

    private long countBlockedSenders(Long userId) {
        return safeCount(blockedSenderMapper.selectCount(new LambdaQueryWrapper<BlockedSender>()
                .eq(BlockedSender::getOwnerId, userId)));
    }

    private long countTrustedSenders(Long userId) {
        return safeCount(trustedSenderMapper.selectCount(new LambdaQueryWrapper<TrustedSender>()
                .eq(TrustedSender::getOwnerId, userId)));
    }

    private long countBlockedDomains(Long userId) {
        return safeCount(blockedDomainMapper.selectCount(new LambdaQueryWrapper<BlockedDomain>()
                .eq(BlockedDomain::getOwnerId, userId)));
    }

    private long countTrustedDomains(Long userId) {
        return safeCount(trustedDomainMapper.selectCount(new LambdaQueryWrapper<TrustedDomain>()
                .eq(TrustedDomain::getOwnerId, userId)));
    }

    private long countWalletBlockedTransactions(Long userId) {
        return safeCount(walletTransactionMapper.selectCount(new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getOwnerId, userId)
                .lt(WalletTransaction::getUpdatedAt, LocalDateTime.now().minusMinutes(30))
                .and(query -> query.eq(WalletTransaction::getStatus, "PENDING")
                        .or()
                        .eq(WalletTransaction::getStatus, "SIGNED")
                        .or()
                        .eq(WalletTransaction::getStatus, "BROADCASTED"))));
    }

    private long countWalletFailedTransactions(Long userId) {
        return safeCount(walletTransactionMapper.selectCount(new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getOwnerId, userId)
                .eq(WalletTransaction::getStatus, "FAILED")));
    }

    private List<String> deduplicateAlerts(List<String> alerts) {
        return new ArrayList<>(new LinkedHashSet<>(alerts));
    }

    private List<SuiteRemediationActionVo> deduplicateAndSortActions(List<SuiteRemediationActionVo> actions) {
        Map<String, SuiteRemediationActionVo> uniqueActions = new LinkedHashMap<>();
        for (SuiteRemediationActionVo action : actions) {
            String key = action.priority()
                    + "|" + action.productCode()
                    + "|" + action.action()
                    + "|" + action.actionCode();
            uniqueActions.putIfAbsent(key, action);
        }
        return uniqueActions.values().stream()
                .sorted(Comparator
                        .comparingInt((SuiteRemediationActionVo action) -> priorityRank(action.priority()))
                        .thenComparing(SuiteRemediationActionVo::productCode)
                        .thenComparing(SuiteRemediationActionVo::action))
                .limit(12)
                .toList();
    }

    private boolean isHighOrCritical(String riskLevel) {
        return RISK_HIGH.equals(riskLevel) || RISK_CRITICAL.equals(riskLevel);
    }

    private int priorityRank(String priority) {
        return switch (priority) {
            case "P0" -> 0;
            case "P1" -> 1;
            case "P2" -> 2;
            default -> 3;
        };
    }

    private String normalizeIp(String ipAddress) {
        return StringUtils.hasText(ipAddress) ? ipAddress : "0.0.0.0";
    }

    private SuiteReadinessSignalVo signal(String key, long value, String note) {
        return new SuiteReadinessSignalVo(key, value, note);
    }

    private SuiteRemediationActionVo action(String priority, String productCode, String action) {
        return new SuiteRemediationActionVo(priority, productCode, action, null);
    }

    private SuiteRemediationActionVo action(String priority, String productCode, String action, String actionCode) {
        return new SuiteRemediationActionVo(priority, productCode, action, actionCode);
    }

    private String toRiskLevel(int score) {
        if (score < 45) {
            return RISK_CRITICAL;
        }
        if (score < 65) {
            return RISK_HIGH;
        }
        if (score < 80) {
            return RISK_MEDIUM;
        }
        return RISK_LOW;
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private long safeCount(Long value) {
        return value == null ? 0L : value;
    }

    private record GovernanceRequestAccess(
            SuiteGovernanceRequest request,
            Long orgId,
            OrgService.OrgGovernanceAccess orgAccess
    ) {
        private boolean orgScoped() {
            return orgId != null;
        }
    }

    private record GovernanceTemplateDefinition(
            String templateCode,
            String name,
            String riskLevel,
            String description,
            List<String> actionCodes,
            List<String> rollbackActionCodes,
            boolean approvalRequired
    ) {
    }

    private List<Long> loadActiveOrgIds(Long userId) {
        return orgMemberMapper.selectList(new LambdaQueryWrapper<OrgMember>()
                        .eq(OrgMember::getUserId, userId)
                        .eq(OrgMember::getStatus, "ACTIVE"))
                .stream()
                .map(OrgMember::getOrgId)
                .distinct()
                .toList();
    }

    private long countSimpleLoginRelayPolicies(List<Long> orgIds) {
        return countSimpleLoginPolicies(orgIds, false);
    }

    private long countSimpleLoginCatchAllPolicies(List<Long> orgIds) {
        return countSimpleLoginPolicies(orgIds, true);
    }

    private long countSimpleLoginPolicies(List<Long> orgIds, boolean catchAllOnly) {
        if (orgIds.isEmpty()) {
            return 0L;
        }
        LambdaQueryWrapper<SimpleLoginRelayPolicy> query = new LambdaQueryWrapper<SimpleLoginRelayPolicy>()
                .in(SimpleLoginRelayPolicy::getOrgId, orgIds);
        if (catchAllOnly) {
            query.eq(SimpleLoginRelayPolicy::getCatchAllEnabled, BOOLEAN_TRUE);
        }
        return safeCount(simpleLoginRelayPolicyMapper.selectCount(query));
    }

    private long countStandardNotesChecklistTasks(Long userId) {
        List<StandardNoteProfile> profiles = standardNoteProfileMapper.selectList(new LambdaQueryWrapper<StandardNoteProfile>()
                .eq(StandardNoteProfile::getOwnerId, userId)
                .eq(StandardNoteProfile::getNoteType, "CHECKLIST"));
        List<Long> noteIds = profiles.stream().map(StandardNoteProfile::getNoteId).toList();
        if (noteIds.isEmpty()) {
            return 0L;
        }
        List<DocsNote> notes = docsNoteMapper.selectList(new LambdaQueryWrapper<DocsNote>()
                .eq(DocsNote::getOwnerId, userId)
                .eq(DocsNote::getWorkspaceType, "STANDARD_NOTES")
                .in(DocsNote::getId, noteIds));
        long total = 0L;
        for (DocsNote note : notes) {
            total += standardNotesChecklistCodec.summarize(note.getContent()).taskCount();
        }
        return total;
    }

    private long countSheetsFormulaCells(Long userId) {
        List<SheetsWorkbook> workbooks = sheetsWorkbookMapper.selectList(new LambdaQueryWrapper<SheetsWorkbook>()
                .eq(SheetsWorkbook::getOwnerId, userId)
                .select(SheetsWorkbook::getGridJson, SheetsWorkbook::getSheetsJson));
        long total = 0L;
        for (SheetsWorkbook workbook : workbooks) {
            total += countFormulaCells(workbook);
        }
        return total;
    }

    private long countFormulaCells(SheetsWorkbook workbook) {
        if (StringUtils.hasText(workbook.getSheetsJson())) {
            return countFormulaCellsInSheetsJson(workbook.getSheetsJson());
        }
        return countFormulaCellsInGridJson(workbook.getGridJson());
    }

    private long countFormulaCellsInSheetsJson(String sheetsJson) {
        try {
            List<java.util.Map<String, Object>> sheets = objectMapper.readValue(sheetsJson, new TypeReference<List<java.util.Map<String, Object>>>() {
            });
            long total = 0L;
            for (java.util.Map<String, Object> sheet : sheets) {
                Object grid = sheet.get("grid");
                total += countFormulaCellsInGridObject(grid);
            }
            return total;
        } catch (Exception ex) {
            return 0L;
        }
    }

    private long countFormulaCellsInGridObject(Object gridObject) {
        if (!(gridObject instanceof List<?> rows)) {
            return 0L;
        }
        long total = 0L;
        for (Object rowObject : rows) {
            if (!(rowObject instanceof List<?> cells)) {
                continue;
            }
            for (Object cellObject : cells) {
                if (!(cellObject instanceof String value)) {
                    continue;
                }
                if (StringUtils.hasText(value) && value.trim().startsWith("=") && value.trim().length() > 1) {
                    total++;
                }
            }
        }
        return total;
    }

    private long countFormulaCellsInGridJson(String gridJson) {
        if (!StringUtils.hasText(gridJson)) {
            return 0L;
        }
        try {
            List<List<String>> grid = objectMapper.readValue(gridJson, new TypeReference<List<List<String>>>() {
            });
            return countFormulaCellsInGridObject(grid);
        } catch (JsonProcessingException ex) {
            return 0L;
        }
    }

    private record ProductSnapshot(
            long mailCount,
            long contactCount,
            long calendarEventCount,
            long calendarShareCount,
            long driveFileCount,
            long driveStorageBytes,
            long driveStorageLimitBytes,
            long docsCount,
            long standardNotesCount,
            long standardNotesPinnedCount,
            long standardNotesTaggedCount,
            long standardNotesFolderCount,
            long standardNotesChecklistTaskCount,
            long sheetsWorkbookCount,
            long sheetsFormulaCellCount,
            long passItemCount,
            long simpleLoginAliasCount,
            long simpleLoginVerifiedMailboxCount,
            long simpleLoginReverseAliasCount,
            long simpleLoginRelayPolicyCount,
            long simpleLoginCatchAllDomainCount,
            long authenticatorEntryCount,
            long vpnConnectedCount,
            long walletTxCount,
            long walletSignedCount,
            long walletPendingCount,
            long walletBroadcastedCount,
            long walletFailedCount,
            long walletBlockedCount,
            long lumoProjectCount,
            long lumoMessageCount,
            long lumoKnowledgeCount,
            long meetRoomCount,
            long meetQualityCount,
            long blockedSenderCount,
            long blockedDomainCount
    ) {
    }
}
