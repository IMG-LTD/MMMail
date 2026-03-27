package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.VpnConnectionSessionMapper;
import com.mmmail.server.model.entity.VpnConnectionProfile;
import com.mmmail.server.model.entity.VpnConnectionSession;
import com.mmmail.server.model.enums.VpnConnectionSource;
import com.mmmail.server.model.enums.VpnDefaultConnectionMode;
import com.mmmail.server.model.vo.VpnServerVo;
import com.mmmail.server.model.vo.VpnSessionVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VpnService {

    private static final String STATUS_CONNECTED = "CONNECTED";
    private static final String STATUS_DISCONNECTED = "DISCONNECTED";
    private static final int DEFAULT_HISTORY_LIMIT = 20;
    private static final int MAX_HISTORY_LIMIT = 100;

    private final VpnConnectionSessionMapper vpnConnectionSessionMapper;
    private final AuditService auditService;
    private final VpnServerCatalogService vpnServerCatalogService;
    private final VpnSettingsService vpnSettingsService;
    private final VpnProfileService vpnProfileService;

    public VpnService(
            VpnConnectionSessionMapper vpnConnectionSessionMapper,
            AuditService auditService,
            VpnServerCatalogService vpnServerCatalogService,
            VpnSettingsService vpnSettingsService,
            VpnProfileService vpnProfileService
    ) {
        this.vpnConnectionSessionMapper = vpnConnectionSessionMapper;
        this.auditService = auditService;
        this.vpnServerCatalogService = vpnServerCatalogService;
        this.vpnSettingsService = vpnSettingsService;
        this.vpnProfileService = vpnProfileService;
    }

    public List<VpnServerVo> listServers(Long userId, String ipAddress) {
        List<VpnServerVo> servers = vpnServerCatalogService.listServers();
        auditService.record(userId, "VPN_SERVERS_LIST", "count=" + servers.size(), ipAddress);
        return servers;
    }

    public VpnSessionVo current(Long userId, String ipAddress) {
        VpnConnectionSession current = loadCurrent(userId);
        auditService.record(userId, "VPN_SESSION_CURRENT", "hasCurrent=" + (current != null), ipAddress);
        return toSessionVo(current, LocalDateTime.now());
    }

    public List<VpnSessionVo> history(Long userId, Integer limit, String ipAddress) {
        int safeLimit = limit == null ? DEFAULT_HISTORY_LIMIT : Math.max(1, Math.min(limit, MAX_HISTORY_LIMIT));
        List<VpnSessionVo> sessions = vpnConnectionSessionMapper.selectList(new LambdaQueryWrapper<VpnConnectionSession>()
                        .eq(VpnConnectionSession::getOwnerId, userId)
                        .orderByDesc(VpnConnectionSession::getConnectedAt, VpnConnectionSession::getId)
                        .last("limit " + safeLimit))
                .stream()
                .map(item -> toSessionVo(item, LocalDateTime.now()))
                .toList();
        auditService.record(userId, "VPN_SESSION_HISTORY", "count=" + sessions.size(), ipAddress);
        return sessions;
    }

    @Transactional
    public VpnSessionVo connect(Long userId, String serverId, String protocol, String ipAddress) {
        VpnSettingsService.ResolvedVpnSettings settings = vpnSettingsService.resolveSettings(userId);
        VpnServerCatalogService.ServerDefinition server = vpnServerCatalogService.requireOnlineServer(serverId);
        ConnectionPlan plan = new ConnectionPlan(
                server,
                VpnProtocolSupport.normalizeProtocol(protocol),
                null,
                null,
                settings.netshieldMode().name(),
                settings.killSwitchEnabled(),
                VpnConnectionSource.MANUAL.name()
        );
        return activateSession(userId, plan, ipAddress);
    }

    @Transactional
    public VpnSessionVo quickConnect(Long userId, String profileId, String ipAddress) {
        ConnectionPlan plan = buildQuickConnectPlan(userId, profileId);
        return activateSession(userId, plan, ipAddress);
    }

    @Transactional
    public VpnSessionVo disconnect(Long userId, String ipAddress) {
        VpnConnectionSession current = loadCurrent(userId);
        if (current == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "VPN session is not connected");
        }
        LocalDateTime now = LocalDateTime.now();
        current.setStatus(STATUS_DISCONNECTED);
        current.setDisconnectedAt(now);
        current.setDurationSeconds(Duration.between(current.getConnectedAt(), now).toSeconds());
        current.setUpdatedAt(now);
        vpnConnectionSessionMapper.updateById(current);
        auditService.record(userId, "VPN_SESSION_DISCONNECT", "sessionId=" + current.getId(), ipAddress);
        return toSessionVo(current, now);
    }

    private ConnectionPlan buildQuickConnectPlan(Long userId, String profileId) {
        Long requestedProfileId = VpnProfileIdSupport.parseNullable(profileId, "VPN profile id is invalid");
        if (requestedProfileId != null) {
            return buildProfilePlan(userId, vpnProfileService.requireProfile(userId, requestedProfileId), VpnConnectionSource.PROFILE);
        }
        VpnSettingsService.ResolvedVpnSettings settings = vpnSettingsService.resolveSettings(userId);
        return switch (settings.defaultConnectionMode()) {
            case FASTEST -> new ConnectionPlan(
                    vpnServerCatalogService.selectFastest(false),
                    "WIREGUARD",
                    null,
                    null,
                    settings.netshieldMode().name(),
                    settings.killSwitchEnabled(),
                    VpnConnectionSource.QUICK_CONNECT.name()
            );
            case RANDOM -> new ConnectionPlan(
                    vpnServerCatalogService.selectRandom(false, userId),
                    "WIREGUARD",
                    null,
                    null,
                    settings.netshieldMode().name(),
                    settings.killSwitchEnabled(),
                    VpnConnectionSource.QUICK_CONNECT.name()
            );
            case LAST_CONNECTION -> buildLastConnectionPlan(userId);
            case PROFILE -> buildProfilePlan(userId, vpnProfileService.requireProfile(userId, settings.defaultProfileId()), VpnConnectionSource.QUICK_CONNECT);
        };
    }

    private ConnectionPlan buildLastConnectionPlan(Long userId) {
        VpnConnectionSession lastSession = vpnConnectionSessionMapper.selectOne(new LambdaQueryWrapper<VpnConnectionSession>()
                .eq(VpnConnectionSession::getOwnerId, userId)
                .orderByDesc(VpnConnectionSession::getConnectedAt, VpnConnectionSession::getId)
                .last("limit 1"));
        if (lastSession == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "VPN last connection is not available");
        }
        VpnServerCatalogService.ServerDefinition server = vpnServerCatalogService.requireOnlineServer(lastSession.getServerId());
        return new ConnectionPlan(
                server,
                VpnProtocolSupport.normalizeProtocol(lastSession.getProtocol()),
                lastSession.getProfileId(),
                lastSession.getProfileName(),
                lastSession.getNetshieldMode(),
                lastSession.getKillSwitchEnabled() != null && lastSession.getKillSwitchEnabled() == 1,
                VpnConnectionSource.QUICK_CONNECT.name()
        );
    }

    private ConnectionPlan buildProfilePlan(Long userId, VpnConnectionProfile profile, VpnConnectionSource source) {
        VpnServerCatalogService.ServerDefinition server = vpnServerCatalogService.resolveProfileTarget(profile, userId);
        return new ConnectionPlan(
                server,
                profile.getProtocol(),
                profile.getId(),
                profile.getName(),
                profile.getNetshieldMode(),
                profile.getKillSwitchEnabled() != null && profile.getKillSwitchEnabled() == 1,
                source.name()
        );
    }

    private VpnSessionVo activateSession(Long userId, ConnectionPlan plan, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        disconnectCurrentSessions(userId, now);
        VpnConnectionSession session = new VpnConnectionSession();
        session.setOwnerId(userId);
        session.setServerId(plan.server().serverId());
        session.setServerCountry(plan.server().country());
        session.setServerCity(plan.server().city());
        session.setServerTier(plan.server().tier());
        session.setProtocol(plan.protocol());
        session.setStatus(STATUS_CONNECTED);
        session.setProfileId(plan.profileId());
        session.setProfileName(plan.profileName());
        session.setNetshieldMode(plan.netshieldMode());
        session.setKillSwitchEnabled(plan.killSwitchEnabled() ? 1 : 0);
        session.setConnectionSource(plan.connectionSource());
        session.setConnectedAt(now);
        session.setDisconnectedAt(null);
        session.setDurationSeconds(0L);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        session.setDeleted(0);
        vpnConnectionSessionMapper.insert(session);
        auditService.record(
                userId,
                "VPN_SESSION_CONNECT",
                "sessionId=" + session.getId() + ",serverId=" + session.getServerId() + ",source=" + plan.connectionSource(),
                ipAddress
        );
        return toSessionVo(session, now);
    }

    private void disconnectCurrentSessions(Long userId, LocalDateTime now) {
        List<VpnConnectionSession> connected = vpnConnectionSessionMapper.selectList(new LambdaQueryWrapper<VpnConnectionSession>()
                .eq(VpnConnectionSession::getOwnerId, userId)
                .eq(VpnConnectionSession::getStatus, STATUS_CONNECTED));
        for (VpnConnectionSession session : connected) {
            session.setStatus(STATUS_DISCONNECTED);
            session.setDisconnectedAt(now);
            session.setDurationSeconds(Duration.between(session.getConnectedAt(), now).toSeconds());
            session.setUpdatedAt(now);
            vpnConnectionSessionMapper.updateById(session);
        }
    }

    private VpnConnectionSession loadCurrent(Long userId) {
        return vpnConnectionSessionMapper.selectOne(new LambdaQueryWrapper<VpnConnectionSession>()
                .eq(VpnConnectionSession::getOwnerId, userId)
                .eq(VpnConnectionSession::getStatus, STATUS_CONNECTED)
                .orderByDesc(VpnConnectionSession::getConnectedAt, VpnConnectionSession::getId)
                .last("limit 1"));
    }

    private VpnSessionVo toSessionVo(VpnConnectionSession session, LocalDateTime now) {
        if (session == null) {
            return null;
        }
        long durationSeconds = resolveDurationSeconds(session, now);
        return new VpnSessionVo(
                String.valueOf(session.getId()),
                session.getServerId(),
                session.getServerCountry(),
                session.getServerCity(),
                session.getServerTier(),
                session.getProtocol(),
                session.getStatus(),
                session.getProfileId() == null ? null : String.valueOf(session.getProfileId()),
                session.getProfileName(),
                session.getNetshieldMode(),
                session.getKillSwitchEnabled() != null && session.getKillSwitchEnabled() == 1,
                session.getConnectionSource(),
                session.getConnectedAt(),
                session.getDisconnectedAt(),
                durationSeconds
        );
    }

    private long resolveDurationSeconds(VpnConnectionSession session, LocalDateTime now) {
        if (STATUS_CONNECTED.equals(session.getStatus())) {
            return Math.max(0, Duration.between(session.getConnectedAt(), now).toSeconds());
        }
        return session.getDurationSeconds() == null ? 0 : Math.max(0, session.getDurationSeconds());
    }

    private record ConnectionPlan(
            VpnServerCatalogService.ServerDefinition server,
            String protocol,
            Long profileId,
            String profileName,
            String netshieldMode,
            boolean killSwitchEnabled,
            String connectionSource
    ) {
    }
}
