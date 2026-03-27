package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.model.entity.VpnConnectionProfile;
import com.mmmail.server.model.enums.VpnProfileRoutingMode;
import com.mmmail.server.model.vo.VpnServerVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class VpnServerCatalogService {

    private static final String STATUS_ONLINE = "ONLINE";
    private static final String TIER_SECURE_CORE = "SECURE_CORE";
    private static final Map<String, ServerDefinition> SERVERS = buildServers();

    public List<VpnServerVo> listServers() {
        return SERVERS.values().stream().map(this::toVo).toList();
    }

    public ServerDefinition requireServer(String serverId) {
        if (!StringUtils.hasText(serverId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "VPN serverId is required");
        }
        ServerDefinition server = SERVERS.get(serverId.trim().toUpperCase(Locale.ROOT));
        if (server == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "VPN server is not found");
        }
        return server;
    }

    public ServerDefinition requireOnlineServer(String serverId) {
        ServerDefinition server = requireServer(serverId);
        if (!STATUS_ONLINE.equals(server.status())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "VPN server is unavailable");
        }
        return server;
    }

    public ServerDefinition selectFastest(boolean secureCoreOnly) {
        return eligibleServers(secureCoreOnly).stream()
                .min(Comparator.comparingInt(ServerDefinition::loadPercent))
                .orElseThrow(() -> new BizException(ErrorCode.INVALID_ARGUMENT, "No VPN server is available"));
    }

    public ServerDefinition selectRandom(boolean secureCoreOnly, Long userId) {
        List<ServerDefinition> eligible = eligibleServers(secureCoreOnly);
        if (eligible.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "No VPN server is available");
        }
        int seed = Math.floorMod(Long.hashCode(userId + LocalDate.now().toEpochDay()), eligible.size());
        return eligible.get(seed);
    }

    public ServerDefinition selectCountry(String country, boolean secureCoreOnly) {
        if (!StringUtils.hasText(country)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "VPN country is required");
        }
        return eligibleServers(secureCoreOnly).stream()
                .filter(item -> item.country().equalsIgnoreCase(country.trim()))
                .min(Comparator.comparingInt(ServerDefinition::loadPercent))
                .orElseThrow(() -> new BizException(ErrorCode.INVALID_ARGUMENT, "VPN country is unavailable"));
    }

    public ServerDefinition resolveProfileTarget(VpnConnectionProfile profile, Long userId) {
        VpnProfileRoutingMode routingMode = VpnProfileRoutingMode.resolve(profile.getRoutingMode(), VpnProfileRoutingMode.FASTEST);
        boolean secureCoreEnabled = profile.getSecureCoreEnabled() != null && profile.getSecureCoreEnabled() == 1;
        return switch (routingMode) {
            case SERVER -> requireOnlineServer(profile.getTargetServerId());
            case COUNTRY -> selectCountry(profile.getTargetCountry(), secureCoreEnabled);
            case FASTEST -> selectFastest(secureCoreEnabled);
        };
    }

    public boolean isCountryDefined(String country) {
        if (!StringUtils.hasText(country)) {
            return false;
        }
        return SERVERS.values().stream().anyMatch(item -> item.country().equalsIgnoreCase(country.trim()));
    }

    private List<ServerDefinition> eligibleServers(boolean secureCoreOnly) {
        return SERVERS.values().stream()
                .filter(item -> STATUS_ONLINE.equals(item.status()))
                .filter(item -> !secureCoreOnly || TIER_SECURE_CORE.equals(item.tier()))
                .toList();
    }

    private VpnServerVo toVo(ServerDefinition definition) {
        return new VpnServerVo(
                definition.serverId(),
                definition.country(),
                definition.city(),
                definition.tier(),
                definition.status(),
                definition.loadPercent()
        );
    }

    private static Map<String, ServerDefinition> buildServers() {
        Map<String, ServerDefinition> servers = new LinkedHashMap<>();
        servers.put("US-NYC-01", new ServerDefinition("US-NYC-01", "United States", "New York", "STANDARD", STATUS_ONLINE, 38));
        servers.put("NL-AMS-02", new ServerDefinition("NL-AMS-02", "Netherlands", "Amsterdam", "STANDARD", STATUS_ONLINE, 46));
        servers.put("SE-STO-04", new ServerDefinition("SE-STO-04", "Sweden", "Stockholm", "STANDARD", STATUS_ONLINE, 29));
        servers.put("CH-GVA-SC1", new ServerDefinition("CH-GVA-SC1", "Switzerland", "Geneva", TIER_SECURE_CORE, STATUS_ONLINE, 61));
        servers.put("IS-REK-SC2", new ServerDefinition("IS-REK-SC2", "Iceland", "Reykjavik", TIER_SECURE_CORE, STATUS_ONLINE, 57));
        servers.put("JP-TYO-03", new ServerDefinition("JP-TYO-03", "Japan", "Tokyo", "STANDARD", "MAINTENANCE", 0));
        return Map.copyOf(servers);
    }

    public record ServerDefinition(
            String serverId,
            String country,
            String city,
            String tier,
            String status,
            int loadPercent
    ) {
    }
}
