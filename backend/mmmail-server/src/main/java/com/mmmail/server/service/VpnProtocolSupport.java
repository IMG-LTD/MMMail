package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

public final class VpnProtocolSupport {

    private static final Set<String> SUPPORTED_PROTOCOLS = Set.of("WIREGUARD", "OPENVPN_UDP", "OPENVPN_TCP");

    private VpnProtocolSupport() {
    }

    public static String normalizeProtocol(String protocol) {
        if (!StringUtils.hasText(protocol)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "VPN protocol is required");
        }
        String normalized = protocol.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_PROTOCOLS.contains(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported VPN protocol");
        }
        return normalized;
    }
}
