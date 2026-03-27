package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.CalendarEventMapper;
import com.mmmail.server.mapper.CalendarEventShareMapper;
import com.mmmail.server.mapper.ContactEntryMapper;
import com.mmmail.server.mapper.DriveItemMapper;
import com.mmmail.server.mapper.MailMessageMapper;
import com.mmmail.server.mapper.SuiteSubscriptionMapper;
import com.mmmail.server.model.dto.ChangeSuitePlanRequest;
import com.mmmail.server.model.entity.CalendarEvent;
import com.mmmail.server.model.entity.CalendarEventShare;
import com.mmmail.server.model.entity.ContactEntry;
import com.mmmail.server.model.entity.DriveItem;
import com.mmmail.server.model.entity.MailMessage;
import com.mmmail.server.model.entity.SuiteSubscription;
import com.mmmail.server.model.vo.SuitePlanVo;
import com.mmmail.server.model.vo.SuiteProductStatusVo;
import com.mmmail.server.model.vo.SuiteSubscriptionVo;
import com.mmmail.server.model.vo.SuiteUsageVo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SuiteService {

    private static final String SUBSCRIPTION_STATUS_ACTIVE = "ACTIVE";

    private final SuiteSubscriptionMapper suiteSubscriptionMapper;
    private final MailMessageMapper mailMessageMapper;
    private final ContactEntryMapper contactEntryMapper;
    private final CalendarEventMapper calendarEventMapper;
    private final CalendarEventShareMapper calendarEventShareMapper;
    private final DriveItemMapper driveItemMapper;
    private final AuditService auditService;
    private final SuiteCatalogService suiteCatalogService;

    public SuiteService(
            SuiteSubscriptionMapper suiteSubscriptionMapper,
            MailMessageMapper mailMessageMapper,
            ContactEntryMapper contactEntryMapper,
            CalendarEventMapper calendarEventMapper,
            CalendarEventShareMapper calendarEventShareMapper,
            DriveItemMapper driveItemMapper,
            AuditService auditService,
            SuiteCatalogService suiteCatalogService
    ) {
        this.suiteSubscriptionMapper = suiteSubscriptionMapper;
        this.mailMessageMapper = mailMessageMapper;
        this.contactEntryMapper = contactEntryMapper;
        this.calendarEventMapper = calendarEventMapper;
        this.calendarEventShareMapper = calendarEventShareMapper;
        this.driveItemMapper = driveItemMapper;
        this.auditService = auditService;
        this.suiteCatalogService = suiteCatalogService;
    }

    public List<SuitePlanVo> listPlans(Long userId, String ipAddress) {
        List<SuitePlanVo> plans = suiteCatalogService.listPlans();
        auditService.record(userId, "SUITE_PLAN_LIST", "count=" + plans.size(), ipAddress);
        return plans;
    }

    public SuiteSubscriptionVo getSubscription(Long userId, String ipAddress) {
        SuiteSubscription subscription = ensureSubscription(userId);
        SuiteSubscriptionVo result = buildSubscriptionVo(userId, subscription);
        auditService.record(userId, "SUITE_SUBSCRIPTION_QUERY", "plan=" + result.planCode(), ipAddress);
        return result;
    }

    public List<SuiteProductStatusVo> listProducts(Long userId, String ipAddress) {
        SuiteSubscription subscription = ensureSubscription(userId);
        List<SuiteProductStatusVo> items = suiteCatalogService.listProductsForPlan(subscription.getPlanCode());
        auditService.record(userId, "SUITE_PRODUCT_LIST", "count=" + items.size(), ipAddress);
        return items;
    }

    @Transactional
    public SuiteSubscriptionVo changePlan(Long userId, ChangeSuitePlanRequest request, String ipAddress) {
        if (!StringUtils.hasText(request.planCode())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Plan code is required");
        }
        SuiteCatalogService.PlanDefinition targetPlan = suiteCatalogService.requirePlan(request.planCode());
        SuiteSubscription subscription = ensureSubscription(userId);

        subscription.setPlanCode(targetPlan.code());
        subscription.setStatus(SUBSCRIPTION_STATUS_ACTIVE);
        subscription.setUpdatedAt(LocalDateTime.now());
        suiteSubscriptionMapper.updateById(subscription);

        auditService.record(userId, "SUITE_PLAN_CHANGE", "plan=" + targetPlan.code(), ipAddress);
        return buildSubscriptionVo(userId, subscription);
    }

    public void assertCalendarEventQuota(Long userId, String ipAddress) {
        SuiteSubscription subscription = ensureSubscription(userId);
        SuiteCatalogService.PlanDefinition plan = suiteCatalogService.requirePlan(subscription.getPlanCode());

        long count = safeCount(calendarEventMapper.selectCount(new LambdaQueryWrapper<CalendarEvent>()
                .eq(CalendarEvent::getOwnerId, userId)));
        if (count >= plan.calendarEventLimit()) {
            auditService.record(
                    userId,
                    "CAL_QUOTA_REJECT",
                    "kind=EVENT_CREATE,plan=" + plan.code() + ",limit=" + plan.calendarEventLimit(),
                    ipAddress
            );
            throw new BizException(ErrorCode.QUOTA_EXCEEDED, "Calendar event quota exceeded for current plan");
        }
    }

    public void assertCalendarShareQuota(Long userId, String ipAddress) {
        SuiteSubscription subscription = ensureSubscription(userId);
        SuiteCatalogService.PlanDefinition plan = suiteCatalogService.requirePlan(subscription.getPlanCode());

        long count = safeCount(calendarEventShareMapper.selectCount(new LambdaQueryWrapper<CalendarEventShare>()
                .eq(CalendarEventShare::getOwnerId, userId)));
        if (count >= plan.calendarShareLimit()) {
            auditService.record(
                    userId,
                    "CAL_QUOTA_REJECT",
                    "kind=SHARE_CREATE,plan=" + plan.code() + ",limit=" + plan.calendarShareLimit(),
                    ipAddress
            );
            throw new BizException(ErrorCode.QUOTA_EXCEEDED, "Calendar share quota exceeded for current plan");
        }
    }

    public void assertDriveStorageQuota(Long userId, long deltaBytes, String ipAddress) {
        if (deltaBytes <= 0) {
            return;
        }
        SuiteSubscription subscription = ensureSubscription(userId);
        SuiteCatalogService.PlanDefinition plan = suiteCatalogService.requirePlan(subscription.getPlanCode());
        long storageLimitBytes = plan.driveStorageMb() * 1024L * 1024L;
        long currentStorageBytes = safeCount(driveItemMapper.selectStorageBytesByOwner(userId));
        long targetStorageBytes = currentStorageBytes + deltaBytes;
        if (targetStorageBytes > storageLimitBytes) {
            auditService.record(
                    userId,
                    "DRIVE_QUOTA_REJECT",
                    "plan=" + plan.code() + ",current=" + currentStorageBytes + ",delta=" + deltaBytes + ",limit=" + storageLimitBytes,
                    ipAddress
            );
            throw new BizException(ErrorCode.QUOTA_EXCEEDED, "Drive storage quota exceeded for current plan");
        }
    }

    public long resolveDriveStorageLimitBytes(Long userId) {
        SuiteSubscription subscription = ensureSubscription(userId);
        SuiteCatalogService.PlanDefinition plan = suiteCatalogService.requirePlan(subscription.getPlanCode());
        return plan.driveStorageMb() * 1024L * 1024L;
    }

    private SuiteSubscriptionVo buildSubscriptionVo(Long userId, SuiteSubscription subscription) {
        SuiteCatalogService.PlanDefinition plan = suiteCatalogService.requirePlan(subscription.getPlanCode());
        SuiteUsageVo usage = buildUsage(userId);
        return new SuiteSubscriptionVo(
                plan.code(),
                plan.name(),
                subscription.getStatus(),
                subscription.getUpdatedAt(),
                usage,
                suiteCatalogService.toPlanVo(plan)
        );
    }

    private SuiteUsageVo buildUsage(Long userId) {
        long mailCount = safeCount(mailMessageMapper.selectCount(new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getOwnerId, userId)));
        long contactCount = safeCount(contactEntryMapper.selectCount(new LambdaQueryWrapper<ContactEntry>()
                .eq(ContactEntry::getOwnerId, userId)));
        long calendarEventCount = safeCount(calendarEventMapper.selectCount(new LambdaQueryWrapper<CalendarEvent>()
                .eq(CalendarEvent::getOwnerId, userId)));
        long calendarShareCount = safeCount(calendarEventShareMapper.selectCount(new LambdaQueryWrapper<CalendarEventShare>()
                .eq(CalendarEventShare::getOwnerId, userId)));
        long driveFileCount = safeCount(driveItemMapper.selectCount(new LambdaQueryWrapper<DriveItem>()
                .eq(DriveItem::getOwnerId, userId)
                .eq(DriveItem::getItemType, "FILE")));
        long driveFolderCount = safeCount(driveItemMapper.selectCount(new LambdaQueryWrapper<DriveItem>()
                .eq(DriveItem::getOwnerId, userId)
                .eq(DriveItem::getItemType, "FOLDER")));
        long driveStorageBytes = safeCount(driveItemMapper.selectStorageBytesByOwner(userId));
        return new SuiteUsageVo(
                mailCount,
                contactCount,
                calendarEventCount,
                calendarShareCount,
                driveFileCount,
                driveFolderCount,
                driveStorageBytes
        );
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

    private long safeCount(Long value) {
        return value == null ? 0L : value;
    }
}
