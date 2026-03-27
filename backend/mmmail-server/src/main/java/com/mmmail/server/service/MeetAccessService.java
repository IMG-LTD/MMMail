package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.MeetAccessEnrollmentMapper;
import com.mmmail.server.mapper.SuiteSubscriptionMapper;
import com.mmmail.server.model.entity.MeetAccessEnrollment;
import com.mmmail.server.model.entity.SuiteSubscription;
import com.mmmail.server.model.vo.MeetAccessOverviewVo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class MeetAccessService {

    private static final String PLAN_FREE = "FREE";
    private static final String SUBSCRIPTION_STATUS_ACTIVE = "ACTIVE";
    private static final String ACCESS_STATE_LOCKED = "LOCKED";
    private static final String ACCESS_STATE_WAITLISTED = "WAITLISTED";
    private static final String ACCESS_STATE_GRANTED = "GRANTED";
    private static final String ACCESS_STATE_CONTACT_REQUESTED = "CONTACT_REQUESTED";
    private static final String ACTION_JOIN_WAITLIST = "JOIN_WAITLIST";
    private static final String ACTION_ACTIVATE = "ACTIVATE";
    private static final String ACTION_OPEN_WORKSPACE = "OPEN_WORKSPACE";
    private static final String ACTION_CONTACT_SALES = "CONTACT_SALES";
    private static final String ACTION_CONTACT_REQUESTED = "CONTACT_REQUESTED";
    private static final int FLAG_ON = 1;
    private static final int FLAG_OFF = 0;
    private static final int MAX_NOTE_LENGTH = 512;

    private final MeetAccessEnrollmentMapper meetAccessEnrollmentMapper;
    private final SuiteSubscriptionMapper suiteSubscriptionMapper;
    private final AuditService auditService;
    private final SuiteCatalogService suiteCatalogService;

    public MeetAccessService(
            MeetAccessEnrollmentMapper meetAccessEnrollmentMapper,
            SuiteSubscriptionMapper suiteSubscriptionMapper,
            AuditService auditService,
            SuiteCatalogService suiteCatalogService
    ) {
        this.meetAccessEnrollmentMapper = meetAccessEnrollmentMapper;
        this.suiteSubscriptionMapper = suiteSubscriptionMapper;
        this.auditService = auditService;
        this.suiteCatalogService = suiteCatalogService;
    }

    public MeetAccessOverviewVo getOverview(Long userId, String ipAddress) {
        SuiteSubscription subscription = ensureSubscription(userId);
        MeetAccessOverviewVo overview = toOverview(subscription, findEnrollment(userId));
        auditService.record(userId, "MEET_ACCESS_OVERVIEW", "state=" + overview.accessState(), ipAddress);
        return overview;
    }

    @Transactional
    public MeetAccessOverviewVo joinWaitlist(Long userId, String note, String ipAddress) {
        SuiteSubscription subscription = ensureSubscription(userId);
        LocalDateTime now = LocalDateTime.now();
        MeetAccessEnrollment enrollment = ensureEnrollment(userId, subscription.getPlanCode(), now);
        enrollment.setWaitlistRequested(FLAG_ON);
        if (enrollment.getWaitlistRequestedAt() == null) {
            enrollment.setWaitlistRequestedAt(now);
        }
        enrollment.setRequestNote(normalizeNote(note));
        enrollment.setUpdatedAt(now);
        meetAccessEnrollmentMapper.updateById(enrollment);
        MeetAccessOverviewVo overview = toOverview(subscription, enrollment);
        auditService.record(userId, "MEET_ACCESS_WAITLIST_JOIN", "state=" + overview.accessState(), ipAddress);
        return overview;
    }

    @Transactional
    public MeetAccessOverviewVo requestEnterpriseAccess(
            Long userId,
            String companyName,
            Integer requestedSeats,
            String note,
            String ipAddress
    ) {
        SuiteSubscription subscription = ensureSubscription(userId);
        LocalDateTime now = LocalDateTime.now();
        MeetAccessEnrollment enrollment = ensureEnrollment(userId, subscription.getPlanCode(), now);
        enrollment.setSalesContactRequested(FLAG_ON);
        enrollment.setCompanyName(companyName.trim());
        enrollment.setRequestedSeats(requestedSeats);
        enrollment.setRequestNote(normalizeNote(note));
        if (enrollment.getSalesContactRequestedAt() == null) {
            enrollment.setSalesContactRequestedAt(now);
        }
        enrollment.setUpdatedAt(now);
        meetAccessEnrollmentMapper.updateById(enrollment);
        MeetAccessOverviewVo overview = toOverview(subscription, enrollment);
        auditService.record(
                userId,
                "MEET_ACCESS_CONTACT_SALES",
                "company=" + enrollment.getCompanyName() + ",seats=" + enrollment.getRequestedSeats(),
                ipAddress
        );
        return overview;
    }

    @Transactional
    public MeetAccessOverviewVo activate(Long userId, String ipAddress) {
        SuiteSubscription subscription = ensureSubscription(userId);
        if (!eligibleForInstantAccess(subscription.getPlanCode())) {
            auditService.record(userId, "MEET_ACCESS_ACTIVATE_REJECT", "plan=" + subscription.getPlanCode(), ipAddress);
            throw new BizException(ErrorCode.FORBIDDEN, "Current suite plan is not eligible for Meet early access");
        }
        LocalDateTime now = LocalDateTime.now();
        MeetAccessEnrollment enrollment = ensureEnrollment(userId, subscription.getPlanCode(), now);
        enrollment.setAccessGranted(FLAG_ON);
        if (enrollment.getAccessGrantedAt() == null) {
            enrollment.setAccessGrantedAt(now);
        }
        enrollment.setUpdatedAt(now);
        meetAccessEnrollmentMapper.updateById(enrollment);
        MeetAccessOverviewVo overview = toOverview(subscription, enrollment);
        auditService.record(userId, "MEET_ACCESS_ACTIVATE", "state=" + overview.accessState(), ipAddress);
        return overview;
    }

    public void assertAccessGranted(Long userId, String ipAddress, String action) {
        MeetAccessEnrollment enrollment = findEnrollment(userId);
        if (enrollment != null && enrollment.getAccessGranted() == FLAG_ON) {
            return;
        }
        auditService.record(userId, "MEET_ACCESS_DENIED", "action=" + action, ipAddress);
        throw new BizException(ErrorCode.FORBIDDEN, "Meet is currently limited to early access members");
    }

    private SuiteSubscription ensureSubscription(Long userId) {
        SuiteSubscription existing = findSubscription(userId);
        if (existing != null) {
            return existing;
        }
        LocalDateTime now = LocalDateTime.now();
        SuiteSubscription created = new SuiteSubscription();
        created.setOwnerId(userId);
        created.setPlanCode(suiteCatalogService.defaultPlanCode());
        created.setStatus(SUBSCRIPTION_STATUS_ACTIVE);
        created.setCreatedAt(now);
        created.setUpdatedAt(now);
        created.setDeleted(0);
        try {
            suiteSubscriptionMapper.insert(created);
            return created;
        } catch (DuplicateKeyException ignored) {
            SuiteSubscription current = findSubscription(userId);
            if (current != null) {
                return current;
            }
            throw ignored;
        }
    }

    private SuiteSubscription findSubscription(Long userId) {
        return suiteSubscriptionMapper.selectOne(new LambdaQueryWrapper<SuiteSubscription>()
                .eq(SuiteSubscription::getOwnerId, userId));
    }

    private MeetAccessEnrollment ensureEnrollment(Long userId, String planCode, LocalDateTime now) {
        MeetAccessEnrollment existing = findEnrollment(userId);
        if (existing != null) {
            existing.setPlanCodeSnapshot(normalizePlanCode(planCode));
            return existing;
        }
        MeetAccessEnrollment created = new MeetAccessEnrollment();
        created.setOwnerId(userId);
        created.setPlanCodeSnapshot(normalizePlanCode(planCode));
        created.setWaitlistRequested(FLAG_OFF);
        created.setAccessGranted(FLAG_OFF);
        created.setSalesContactRequested(FLAG_OFF);
        created.setCreatedAt(now);
        created.setUpdatedAt(now);
        created.setDeleted(0);
        meetAccessEnrollmentMapper.insert(created);
        return created;
    }

    private MeetAccessEnrollment findEnrollment(Long userId) {
        return meetAccessEnrollmentMapper.selectOne(new LambdaQueryWrapper<MeetAccessEnrollment>()
                .eq(MeetAccessEnrollment::getOwnerId, userId));
    }

    private MeetAccessOverviewVo toOverview(SuiteSubscription subscription, MeetAccessEnrollment enrollment) {
        boolean accessGranted = enrollment != null && enrollment.getAccessGranted() == FLAG_ON;
        boolean waitlistRequested = enrollment != null && enrollment.getWaitlistRequested() == FLAG_ON;
        boolean salesContactRequested = enrollment != null && enrollment.getSalesContactRequested() == FLAG_ON;
        boolean eligibleForInstantAccess = eligibleForInstantAccess(subscription.getPlanCode());
        String accessState = resolveAccessState(accessGranted, waitlistRequested, salesContactRequested);
        String recommendedAction = resolveRecommendedAction(accessGranted, waitlistRequested, eligibleForInstantAccess, salesContactRequested);
        return new MeetAccessOverviewVo(
                normalizePlanCode(subscription.getPlanCode()),
                resolvePlanName(subscription.getPlanCode()),
                eligibleForInstantAccess,
                accessGranted,
                waitlistRequested,
                salesContactRequested,
                accessState,
                recommendedAction,
                enrollment == null ? null : enrollment.getCompanyName(),
                enrollment == null ? null : enrollment.getRequestedSeats(),
                enrollment == null ? null : enrollment.getRequestNote(),
                enrollment == null ? null : enrollment.getWaitlistRequestedAt(),
                enrollment == null ? null : enrollment.getAccessGrantedAt(),
                enrollment == null ? null : enrollment.getSalesContactRequestedAt()
        );
    }

    private String resolveAccessState(boolean accessGranted, boolean waitlistRequested, boolean salesContactRequested) {
        if (accessGranted) {
            return ACCESS_STATE_GRANTED;
        }
        if (salesContactRequested) {
            return ACCESS_STATE_CONTACT_REQUESTED;
        }
        if (waitlistRequested) {
            return ACCESS_STATE_WAITLISTED;
        }
        return ACCESS_STATE_LOCKED;
    }

    private String resolveRecommendedAction(
            boolean accessGranted,
            boolean waitlistRequested,
            boolean eligibleForInstantAccess,
            boolean salesContactRequested
    ) {
        if (accessGranted) {
            return ACTION_OPEN_WORKSPACE;
        }
        if (eligibleForInstantAccess) {
            return ACTION_ACTIVATE;
        }
        if (salesContactRequested) {
            return ACTION_CONTACT_REQUESTED;
        }
        if (waitlistRequested) {
            return ACTION_CONTACT_SALES;
        }
        return ACTION_JOIN_WAITLIST;
    }

    private boolean eligibleForInstantAccess(String planCode) {
        return suiteCatalogService.eligibleForMeetInstantAccess(planCode);
    }

    private String normalizePlanCode(String planCode) {
        if (!StringUtils.hasText(planCode)) {
            return PLAN_FREE;
        }
        return planCode.trim().toUpperCase();
    }

    private String resolvePlanName(String planCode) {
        return suiteCatalogService.resolvePlanName(planCode);
    }

    private String normalizeNote(String note) {
        if (!StringUtils.hasText(note)) {
            return null;
        }
        String trimmed = note.trim();
        if (trimmed.length() > MAX_NOTE_LENGTH) {
            return trimmed.substring(0, MAX_NOTE_LENGTH);
        }
        return trimmed;
    }
}
