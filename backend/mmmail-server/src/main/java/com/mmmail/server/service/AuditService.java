package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.server.mapper.AuditEventMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.entity.AuditEvent;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.vo.AuditEventVo;
import com.mmmail.server.model.vo.OrgAuditEventVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AuditService {

    private final AuditEventMapper auditEventMapper;
    private final UserAccountMapper userAccountMapper;

    public AuditService(AuditEventMapper auditEventMapper, UserAccountMapper userAccountMapper) {
        this.auditEventMapper = auditEventMapper;
        this.userAccountMapper = userAccountMapper;
    }

    public void record(Long actorId, String eventType, String detail, String ipAddress) {
        recordEvent(actorId, eventType, detail, ipAddress, null);
    }

    public void record(Long actorId, String eventType, String detail, String ipAddress, Long orgId) {
        recordEvent(actorId, eventType, detail, ipAddress, orgId);
    }

    public AuditEventVo recordEvent(Long actorId, String eventType, String detail, String ipAddress) {
        return recordEvent(actorId, eventType, detail, ipAddress, null);
    }

    public AuditEventVo recordEvent(Long actorId, String eventType, String detail, String ipAddress, Long orgId) {
        AuditEvent event = new AuditEvent();
        event.setOrgId(orgId);
        event.setActorId(actorId);
        event.setEventType(eventType);
        event.setDetail(detail);
        event.setIpAddress(ipAddress == null || ipAddress.isBlank() ? "0.0.0.0" : ipAddress);
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        event.setDeleted(0);
        auditEventMapper.insert(event);
        return toAuditEventVo(event);
    }

    public List<AuditEventVo> list(Long userId, boolean admin) {
        LambdaQueryWrapper<AuditEvent> queryWrapper = new LambdaQueryWrapper<>();
        if (!admin) {
            queryWrapper.eq(AuditEvent::getActorId, userId);
        }
        queryWrapper.orderByDesc(AuditEvent::getCreatedAt).last("limit 100");

        return auditEventMapper.selectList(queryWrapper).stream()
                .map(this::toAuditEventVo)
                .toList();
    }

    public List<AuditEventVo> listActorEvents(
            Long userId,
            Set<String> eventTypes,
            Long afterEventId,
            int limit,
            boolean ascending
    ) {
        if (userId == null || eventTypes == null || eventTypes.isEmpty()) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 100));
        LambdaQueryWrapper<AuditEvent> query = new LambdaQueryWrapper<AuditEvent>()
                .eq(AuditEvent::getActorId, userId)
                .in(AuditEvent::getEventType, eventTypes);
        if (afterEventId != null && afterEventId > 0) {
            query.gt(AuditEvent::getId, afterEventId);
        }
        if (ascending) {
            query.orderByAsc(AuditEvent::getId);
        } else {
            query.orderByDesc(AuditEvent::getId);
        }
        query.last("limit " + safeLimit);
        return auditEventMapper.selectList(query).stream().map(this::toAuditEventVo).toList();
    }

    public AuditEventVo latestActorEvent(Long userId, Set<String> eventTypes) {
        List<AuditEventVo> events = listActorEvents(userId, eventTypes, null, 1, false);
        if (events.isEmpty()) {
            return null;
        }
        return events.getFirst();
    }

    public List<AuditEventVo> listEventsByDetail(
            Set<String> eventTypes,
            String detailToken,
            Long afterEventId,
            int limit,
            boolean ascending
    ) {
        if (eventTypes == null || eventTypes.isEmpty() || !StringUtils.hasText(detailToken)) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 100));
        LambdaQueryWrapper<AuditEvent> query = new LambdaQueryWrapper<AuditEvent>()
                .in(AuditEvent::getEventType, eventTypes)
                .like(AuditEvent::getDetail, detailToken.trim());
        if (afterEventId != null && afterEventId > 0) {
            query.gt(AuditEvent::getId, afterEventId);
        }
        if (ascending) {
            query.orderByAsc(AuditEvent::getId);
        } else {
            query.orderByDesc(AuditEvent::getId);
        }
        query.last("limit " + safeLimit);
        return auditEventMapper.selectList(query).stream().map(this::toAuditEventVo).toList();
    }

    public AuditEventVo latestEventByDetail(Set<String> eventTypes, String detailToken) {
        List<AuditEventVo> events = listEventsByDetail(eventTypes, detailToken, null, 1, false);
        if (events.isEmpty()) {
            return null;
        }
        return events.getFirst();
    }

    public List<OrgAuditEventVo> listByOrg(
            Long orgId,
            int limit,
            String eventType,
            String actorEmail,
            String keyword
    ) {
        return listByOrg(orgId, limit, eventType, actorEmail, keyword, null, null, false);
    }

    public List<OrgAuditEventVo> listByOrg(
            Long orgId,
            int limit,
            String eventType,
            String actorEmail,
            String keyword,
            LocalDateTime fromAt,
            LocalDateTime toAt,
            boolean ascending
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        LambdaQueryWrapper<AuditEvent> query = new LambdaQueryWrapper<AuditEvent>()
                .eq(AuditEvent::getOrgId, orgId);

        if (StringUtils.hasText(eventType)) {
            query.eq(AuditEvent::getEventType, eventType.trim().toUpperCase());
        }
        if (StringUtils.hasText(keyword)) {
            String safeKeyword = keyword.trim();
            query.and(wrapper -> wrapper
                    .like(AuditEvent::getDetail, safeKeyword)
                    .or()
                    .like(AuditEvent::getEventType, safeKeyword));
        }

        if (StringUtils.hasText(actorEmail)) {
            String normalizedActorEmail = actorEmail.trim().toLowerCase();
            Set<Long> actorIdSet = userAccountMapper.selectList(new LambdaQueryWrapper<UserAccount>()
                            .like(UserAccount::getEmail, normalizedActorEmail))
                    .stream()
                    .map(UserAccount::getId)
                    .collect(java.util.stream.Collectors.toSet());
            if (actorIdSet.isEmpty()) {
                return List.of();
            }
            query.in(AuditEvent::getActorId, actorIdSet);
        }

        applyDateRange(query, fromAt, toAt);
        applySort(query, ascending);
        query.last("limit " + safeLimit);
        List<AuditEvent> events = auditEventMapper.selectList(query);
        return toOrgAuditEvents(events);
    }

    public int countOrgEvents(Long orgId) {
        return Math.toIntExact(auditEventMapper.selectCount(new LambdaQueryWrapper<AuditEvent>()
                .eq(AuditEvent::getOrgId, orgId)));
    }

    public int countOrgEventTypes(Long orgId) {
        return (int) auditEventMapper.selectList(new LambdaQueryWrapper<AuditEvent>()
                        .select(AuditEvent::getEventType)
                        .eq(AuditEvent::getOrgId, orgId))
                .stream()
                .map(AuditEvent::getEventType)
                .filter(StringUtils::hasText)
                .distinct()
                .count();
    }

    public OrgAuditEventVo firstOrgEvent(Long orgId) {
        return findOrgEdgeEvent(orgId, true);
    }

    public OrgAuditEventVo latestOrgEvent(Long orgId) {
        return findOrgEdgeEvent(orgId, false);
    }

    private void applyDateRange(
            LambdaQueryWrapper<AuditEvent> query,
            LocalDateTime fromAt,
            LocalDateTime toAt
    ) {
        if (fromAt != null) {
            query.ge(AuditEvent::getCreatedAt, fromAt);
        }
        if (toAt != null) {
            query.le(AuditEvent::getCreatedAt, toAt);
        }
    }

    private void applySort(LambdaQueryWrapper<AuditEvent> query, boolean ascending) {
        if (ascending) {
            query.orderByAsc(AuditEvent::getCreatedAt).orderByAsc(AuditEvent::getId);
            return;
        }
        query.orderByDesc(AuditEvent::getCreatedAt).orderByDesc(AuditEvent::getId);
    }

    private OrgAuditEventVo findOrgEdgeEvent(Long orgId, boolean ascending) {
        LambdaQueryWrapper<AuditEvent> query = new LambdaQueryWrapper<AuditEvent>()
                .eq(AuditEvent::getOrgId, orgId);
        applySort(query, ascending);
        query.last("limit 1");
        List<AuditEvent> events = auditEventMapper.selectList(query);
        if (events.isEmpty()) {
            return null;
        }
        return toOrgAuditEvents(events).getFirst();
    }

    private List<OrgAuditEventVo> toOrgAuditEvents(List<AuditEvent> events) {
        if (events.isEmpty()) {
            return List.of();
        }

        Set<Long> actorIds = events.stream()
                .map(AuditEvent::getActorId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        Map<Long, String> actorEmailMap = actorIds.isEmpty()
                ? Map.of()
                : userAccountMapper.selectList(new LambdaQueryWrapper<UserAccount>().in(UserAccount::getId, actorIds))
                .stream()
                .collect(java.util.stream.Collectors.toMap(UserAccount::getId, UserAccount::getEmail));

        return events.stream()
                .map(event -> new OrgAuditEventVo(
                        String.valueOf(event.getId()),
                        event.getOrgId() == null ? null : String.valueOf(event.getOrgId()),
                        event.getActorId() == null ? null : String.valueOf(event.getActorId()),
                        actorEmailMap.get(event.getActorId()),
                        event.getEventType(),
                        event.getIpAddress(),
                        event.getDetail(),
                        event.getCreatedAt()
                ))
                .toList();
    }

    private AuditEventVo toAuditEventVo(AuditEvent event) {
        return new AuditEventVo(
                event.getId(),
                event.getEventType(),
                event.getIpAddress(),
                event.getDetail(),
                event.getCreatedAt()
        );
    }
}
