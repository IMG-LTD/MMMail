package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.AuthLoginAttemptMapper;
import com.mmmail.server.mapper.AuthLoginLockMapper;
import com.mmmail.server.mapper.SecurityEventMapper;
import com.mmmail.server.model.entity.AuthLoginAttempt;
import com.mmmail.server.model.entity.AuthLoginLock;
import com.mmmail.server.model.entity.SecurityEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LoginRiskService {

    private static final int BRUTE_FORCE_THRESHOLD = 5;
    private static final int FAILURE_WINDOW_MINUTES = 10;
    private static final int LOCK_MINUTES = 15;
    private static final int HISTORY_DAYS = 30;
    private static final int HISTORY_LIMIT = 100;
    private static final double IMPOSSIBLE_TRAVEL_KM = 1000.0;

    private final AuthLoginAttemptMapper attemptMapper;
    private final AuthLoginLockMapper lockMapper;
    private final SecurityEventMapper securityEventMapper;
    private final LoginGeoResolver geoResolver;

    public LoginRiskService(
            AuthLoginAttemptMapper attemptMapper,
            AuthLoginLockMapper lockMapper,
            SecurityEventMapper securityEventMapper,
            LoginGeoResolver geoResolver
    ) {
        this.attemptMapper = attemptMapper;
        this.lockMapper = lockMapper;
        this.securityEventMapper = securityEventMapper;
        this.geoResolver = geoResolver;
    }

    public void ensureLoginAllowed(String email, String ipAddress) {
        AuthLoginLock lock = activeLock(email, ipAddress, LocalDateTime.now());
        if (lock == null) {
            return;
        }
        throw new BizException(
                ErrorCode.RATE_LIMITED,
                "Account temporarily locked until " + lock.getLockedUntil()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String email, Long userId, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        LoginGeoPoint geo = geoResolver.resolve(ipAddress);
        insertAttempt(email, userId, geo, false, now);
        long failureCount = recentFailureCount(email, geo.ipAddress(), now);
        if (failureCount < BRUTE_FORCE_THRESHOLD) {
            return;
        }
        LocalDateTime lockedUntil = now.plusMinutes(LOCK_MINUTES);
        upsertLock(email, geo.ipAddress(), failureCount, lockedUntil, now);
        createSecurityEvent(userId, email, "BRUTE_FORCE_LOCK", "HIGH", "high",
                List.of("brute_force"), geo, lockedUntil, now);
    }

    @Transactional
    public LoginRiskAssessment recordSuccess(Long userId, String email, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        LoginGeoPoint geo = geoResolver.resolve(ipAddress);
        LoginRiskAssessment assessment = assess(userId, email, geo, now);
        insertAttempt(email, userId, geo, true, now);
        clearExpiredLocks(email, now);
        if ("low".equals(assessment.risk())) {
            return assessment;
        }
        SecurityEvent event = createRiskEvent(userId, email, assessment, geo, now);
        return assessment.withSecurityEventId(event.getId());
    }

    private LoginRiskAssessment assess(Long userId, String email, LoginGeoPoint current, LocalDateTime now) {
        if (!isKnownCity(current)) {
            return LoginRiskAssessment.low();
        }
        List<AuthLoginAttempt> history = loadSuccessHistory(userId, email, now);
        if (history.isEmpty()) {
            return LoginRiskAssessment.low();
        }
        List<String> reasons = new ArrayList<>();
        addGeoChangeReason(history, current, reasons);
        addImpossibleTravelReason(history, current, now, reasons);
        if (reasons.contains("impossible_travel")) {
            return new LoginRiskAssessment("high", reasons, true, null);
        }
        if (!reasons.isEmpty()) {
            return new LoginRiskAssessment("medium", reasons, true, null);
        }
        return LoginRiskAssessment.low();
    }

    private List<AuthLoginAttempt> loadSuccessHistory(Long userId, String email, LocalDateTime now) {
        LambdaQueryWrapper<AuthLoginAttempt> query = new LambdaQueryWrapper<AuthLoginAttempt>()
                .eq(AuthLoginAttempt::getSuccess, 1)
                .ge(AuthLoginAttempt::getCreatedAt, now.minusDays(HISTORY_DAYS));
        if (userId != null) {
            query.eq(AuthLoginAttempt::getUserId, userId);
        } else {
            query.eq(AuthLoginAttempt::getEmail, email);
        }
        query.orderByDesc(AuthLoginAttempt::getCreatedAt).last("limit " + HISTORY_LIMIT);
        return attemptMapper.selectList(query);
    }

    private void addGeoChangeReason(List<AuthLoginAttempt> history, LoginGeoPoint current, List<String> reasons) {
        String mainCity = mainCity(history);
        if (StringUtils.hasText(mainCity) && !mainCity.equalsIgnoreCase(current.city())) {
            reasons.add("geo_change");
        }
    }

    private void addImpossibleTravelReason(
            List<AuthLoginAttempt> history,
            LoginGeoPoint current,
            LocalDateTime now,
            List<String> reasons
    ) {
        AuthLoginAttempt latest = latestKnownAttempt(history);
        if (latest == null || Duration.between(latest.getCreatedAt(), now).toHours() >= 1) {
            return;
        }
        if (distanceKm(latest.getLatitude(), latest.getLongitude(), current) > IMPOSSIBLE_TRAVEL_KM) {
            reasons.add("impossible_travel");
        }
    }

    private String mainCity(List<AuthLoginAttempt> history) {
        return history.stream()
                .filter(this::hasKnownCity)
                .collect(Collectors.groupingBy(AuthLoginAttempt::getCity, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private AuthLoginAttempt latestKnownAttempt(List<AuthLoginAttempt> history) {
        return history.stream()
                .filter(this::hasCoordinates)
                .max(Comparator.comparing(AuthLoginAttempt::getCreatedAt))
                .orElse(null);
    }

    private void insertAttempt(
            String email,
            Long userId,
            LoginGeoPoint geo,
            boolean success,
            LocalDateTime now
    ) {
        AuthLoginAttempt attempt = new AuthLoginAttempt();
        attempt.setEmail(email);
        attempt.setUserId(userId);
        attempt.setIpAddress(geo.ipAddress());
        attempt.setSuccess(success ? 1 : 0);
        attempt.setCity(geo.city());
        attempt.setCountry(geo.country());
        attempt.setLatitude(geo.latitude());
        attempt.setLongitude(geo.longitude());
        attempt.setGeoSource(geo.source());
        attempt.setCreatedAt(now);
        attempt.setDeleted(0);
        attemptMapper.insert(attempt);
    }

    private long recentFailureCount(String email, String ipAddress, LocalDateTime now) {
        return attemptMapper.selectCount(new LambdaQueryWrapper<AuthLoginAttempt>()
                .eq(AuthLoginAttempt::getEmail, email)
                .eq(AuthLoginAttempt::getIpAddress, ipAddress)
                .eq(AuthLoginAttempt::getSuccess, 0)
                .ge(AuthLoginAttempt::getCreatedAt, now.minusMinutes(FAILURE_WINDOW_MINUTES)));
    }

    private void upsertLock(
            String email,
            String ipAddress,
            long failureCount,
            LocalDateTime lockedUntil,
            LocalDateTime now
    ) {
        AuthLoginLock lock = findLock(email, ipAddress);
        if (lock == null) {
            insertLock(email, ipAddress, failureCount, lockedUntil, now);
            return;
        }
        lockMapper.update(null, new LambdaUpdateWrapper<AuthLoginLock>()
                .eq(AuthLoginLock::getId, lock.getId())
                .set(AuthLoginLock::getFailureCount, (int) failureCount)
                .set(AuthLoginLock::getLockedUntil, lockedUntil)
                .set(AuthLoginLock::getUpdatedAt, now));
    }

    private void insertLock(
            String email,
            String ipAddress,
            long failureCount,
            LocalDateTime lockedUntil,
            LocalDateTime now
    ) {
        AuthLoginLock lock = new AuthLoginLock();
        lock.setEmail(email);
        lock.setIpAddress(ipAddress);
        lock.setFailureCount((int) failureCount);
        lock.setLockedUntil(lockedUntil);
        lock.setCreatedAt(now);
        lock.setUpdatedAt(now);
        lock.setDeleted(0);
        lockMapper.insert(lock);
    }

    private SecurityEvent createRiskEvent(
            Long userId,
            String email,
            LoginRiskAssessment assessment,
            LoginGeoPoint geo,
            LocalDateTime now
    ) {
        String type = assessment.reasons().contains("impossible_travel")
                ? "LOGIN_IMPOSSIBLE_TRAVEL"
                : "LOGIN_GEO_CHANGE";
        String severity = "high".equals(assessment.risk()) ? "HIGH" : "MEDIUM";
        return createSecurityEvent(userId, email, type, severity, assessment.risk(),
                assessment.reasons(), geo, null, now);
    }

    private SecurityEvent createSecurityEvent(
            Long userId,
            String email,
            String type,
            String severity,
            String risk,
            List<String> reasons,
            LoginGeoPoint geo,
            LocalDateTime lockedUntil,
            LocalDateTime now
    ) {
        SecurityEvent event = new SecurityEvent();
        event.setUserId(userId);
        event.setEmail(email);
        event.setType(type);
        event.setSeverity(severity);
        event.setRisk(risk);
        event.setReasons(String.join(",", reasons));
        event.setIpAddress(geo.ipAddress());
        event.setCity(geo.city());
        event.setCountry(geo.country());
        event.setSource(geo.source());
        event.setDetail("ip=" + geo.ipAddress() + ",city=" + geo.city());
        event.setLockedUntil(lockedUntil);
        event.setActionStatus("OPEN");
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        event.setDeleted(0);
        securityEventMapper.insert(event);
        return event;
    }

    private AuthLoginLock activeLock(String email, String ipAddress, LocalDateTime now) {
        AuthLoginLock lock = findLock(email, ipAddress);
        if (lock == null || lock.getLockedUntil() == null || !lock.getLockedUntil().isAfter(now)) {
            return null;
        }
        return lock;
    }

    private AuthLoginLock findLock(String email, String ipAddress) {
        return lockMapper.selectOne(new LambdaQueryWrapper<AuthLoginLock>()
                .eq(AuthLoginLock::getEmail, email)
                .eq(AuthLoginLock::getIpAddress, ipAddress)
                .last("limit 1"));
    }

    private void clearExpiredLocks(String email, LocalDateTime now) {
        lockMapper.update(null, new LambdaUpdateWrapper<AuthLoginLock>()
                .eq(AuthLoginLock::getEmail, email)
                .le(AuthLoginLock::getLockedUntil, now)
                .set(AuthLoginLock::getDeleted, 1)
                .set(AuthLoginLock::getUpdatedAt, now));
    }

    private boolean hasKnownCity(AuthLoginAttempt attempt) {
        return StringUtils.hasText(attempt.getCity()) && !"Unknown".equalsIgnoreCase(attempt.getCity());
    }

    private boolean hasCoordinates(AuthLoginAttempt attempt) {
        return hasKnownCity(attempt) && attempt.getLatitude() != null && attempt.getLongitude() != null;
    }

    private boolean isKnownCity(LoginGeoPoint point) {
        return StringUtils.hasText(point.city()) && !"Unknown".equalsIgnoreCase(point.city());
    }

    private double distanceKm(Double latitude, Double longitude, LoginGeoPoint current) {
        if (latitude == null || longitude == null) {
            return 0.0;
        }
        double latDistance = Math.toRadians(current.latitude() - latitude);
        double lonDistance = Math.toRadians(current.longitude() - longitude);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(current.latitude()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
