package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mmmail.server.mapper.CalendarEventAttendeeMapper;
import com.mmmail.server.mapper.CalendarEventShareMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.entity.CalendarEventAttendee;
import com.mmmail.server.model.entity.CalendarEventShare;
import com.mmmail.server.model.entity.UserAccount;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CalendarInvitationOrchestrationService {

    private static final String SHARE_PERMISSION_VIEW = "VIEW";
    private static final String SHARE_STATUS_NEEDS_ACTION = "NEEDS_ACTION";
    public static final String SHARE_SOURCE_MANUAL = "MANUAL";
    public static final String SHARE_SOURCE_ATTENDEE = "ATTENDEE";

    private final CalendarEventAttendeeMapper calendarEventAttendeeMapper;
    private final CalendarEventShareMapper calendarEventShareMapper;
    private final UserAccountMapper userAccountMapper;
    private final SuiteService suiteService;

    public CalendarInvitationOrchestrationService(
            CalendarEventAttendeeMapper calendarEventAttendeeMapper,
            CalendarEventShareMapper calendarEventShareMapper,
            UserAccountMapper userAccountMapper,
            SuiteService suiteService
    ) {
        this.calendarEventAttendeeMapper = calendarEventAttendeeMapper;
        this.calendarEventShareMapper = calendarEventShareMapper;
        this.userAccountMapper = userAccountMapper;
        this.suiteService = suiteService;
    }

    public void syncInternalInvitations(Long ownerId, Long eventId, String ipAddress, LocalDateTime now) {
        List<CalendarEventAttendee> attendees = loadAttendees(eventId);
        Map<Long, CalendarEventShare> sharesByTargetUserId = loadSharesByTargetUserId(ownerId, eventId);
        Map<String, UserAccount> localUsersByEmail = loadLocalUsersByEmail(attendees);
        Set<Long> activeManagedUserIds = new LinkedHashSet<>();

        for (CalendarEventAttendee attendee : attendees) {
            UserAccount localUser = localUsersByEmail.get(attendee.getEmail());
            if (shouldSkipManagedInvite(ownerId, localUser)) {
                continue;
            }
            activeManagedUserIds.add(localUser.getId());
            syncManagedInvite(ownerId, eventId, attendee, localUser, sharesByTargetUserId.get(localUser.getId()), ipAddress, now);
        }

        removeStaleManagedShares(ownerId, eventId, sharesByTargetUserId.values(), activeManagedUserIds);
    }

    public void syncAttendeeResponseStatus(Long eventId, String attendeeEmail, String responseStatus, LocalDateTime now) {
        if (attendeeEmail == null || responseStatus == null) {
            return;
        }
        calendarEventAttendeeMapper.update(
                null,
                new LambdaUpdateWrapper<CalendarEventAttendee>()
                        .eq(CalendarEventAttendee::getEventId, eventId)
                        .eq(CalendarEventAttendee::getEmail, attendeeEmail)
                        .set(CalendarEventAttendee::getResponseStatus, responseStatus)
                        .set(CalendarEventAttendee::getUpdatedAt, now)
        );
    }

    private List<CalendarEventAttendee> loadAttendees(Long eventId) {
        return calendarEventAttendeeMapper.selectList(new LambdaQueryWrapper<CalendarEventAttendee>()
                .eq(CalendarEventAttendee::getEventId, eventId)
                .orderByAsc(CalendarEventAttendee::getEmail));
    }

    private Map<Long, CalendarEventShare> loadSharesByTargetUserId(Long ownerId, Long eventId) {
        Map<Long, CalendarEventShare> sharesByTargetUserId = new LinkedHashMap<>();
        List<CalendarEventShare> shares = calendarEventShareMapper.selectList(new LambdaQueryWrapper<CalendarEventShare>()
                .eq(CalendarEventShare::getOwnerId, ownerId)
                .eq(CalendarEventShare::getEventId, eventId));
        for (CalendarEventShare share : shares) {
            sharesByTargetUserId.put(share.getTargetUserId(), share);
        }
        return sharesByTargetUserId;
    }

    private Map<String, UserAccount> loadLocalUsersByEmail(List<CalendarEventAttendee> attendees) {
        Set<String> emails = new LinkedHashSet<>();
        for (CalendarEventAttendee attendee : attendees) {
            emails.add(attendee.getEmail());
        }
        if (emails.isEmpty()) {
            return Map.of();
        }

        Map<String, UserAccount> localUsersByEmail = new LinkedHashMap<>();
        List<UserAccount> localUsers = userAccountMapper.selectList(new LambdaQueryWrapper<UserAccount>()
                .in(UserAccount::getEmail, emails));
        for (UserAccount localUser : localUsers) {
            localUsersByEmail.put(localUser.getEmail(), localUser);
        }
        return localUsersByEmail;
    }

    private boolean shouldSkipManagedInvite(Long ownerId, UserAccount localUser) {
        return localUser == null || localUser.getId().equals(ownerId);
    }

    private void syncManagedInvite(
            Long ownerId,
            Long eventId,
            CalendarEventAttendee attendee,
            UserAccount localUser,
            CalendarEventShare existingShare,
            String ipAddress,
            LocalDateTime now
    ) {
        if (existingShare == null) {
            createManagedShare(ownerId, eventId, attendee, localUser, ipAddress, now);
            return;
        }
        if (SHARE_SOURCE_ATTENDEE.equals(existingShare.getSource())) {
            refreshManagedShare(existingShare, attendee, now);
            return;
        }
        syncAttendeeResponseStatus(eventId, attendee.getEmail(), existingShare.getResponseStatus(), now);
    }

    private void createManagedShare(
            Long ownerId,
            Long eventId,
            CalendarEventAttendee attendee,
            UserAccount localUser,
            String ipAddress,
            LocalDateTime now
    ) {
        suiteService.assertCalendarShareQuota(ownerId, ipAddress);

        CalendarEventShare share = new CalendarEventShare();
        share.setOwnerId(ownerId);
        share.setEventId(eventId);
        share.setTargetUserId(localUser.getId());
        share.setTargetEmail(attendee.getEmail());
        share.setPermission(SHARE_PERMISSION_VIEW);
        share.setResponseStatus(SHARE_STATUS_NEEDS_ACTION);
        share.setSource(SHARE_SOURCE_ATTENDEE);
        share.setCreatedAt(now);
        share.setUpdatedAt(now);
        share.setDeleted(0);
        calendarEventShareMapper.insert(share);
        syncAttendeeResponseStatus(eventId, attendee.getEmail(), share.getResponseStatus(), now);
    }

    private void refreshManagedShare(CalendarEventShare share, CalendarEventAttendee attendee, LocalDateTime now) {
        boolean changed = false;
        if (!SHARE_PERMISSION_VIEW.equals(share.getPermission())) {
            share.setPermission(SHARE_PERMISSION_VIEW);
            changed = true;
        }
        if (!attendee.getEmail().equals(share.getTargetEmail())) {
            share.setTargetEmail(attendee.getEmail());
            changed = true;
        }
        if (changed) {
            share.setUpdatedAt(now);
            calendarEventShareMapper.updateById(share);
        }
        syncAttendeeResponseStatus(share.getEventId(), attendee.getEmail(), share.getResponseStatus(), now);
    }

    private void removeStaleManagedShares(
            Long ownerId,
            Long eventId,
            Iterable<CalendarEventShare> shares,
            Set<Long> activeManagedUserIds
    ) {
        for (CalendarEventShare share : shares) {
            if (!SHARE_SOURCE_ATTENDEE.equals(share.getSource())) {
                continue;
            }
            if (activeManagedUserIds.contains(share.getTargetUserId())) {
                continue;
            }
            calendarEventShareMapper.purgeByOwnerEventAndId(ownerId, eventId, share.getId());
        }
    }
}
