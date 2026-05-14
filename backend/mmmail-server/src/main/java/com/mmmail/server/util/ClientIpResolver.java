package com.mmmail.server.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ClientIpResolver {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";

    public String resolve(HttpServletRequest request) {
        String forwardedFor = firstForwardedFor(request.getHeader(HEADER_X_FORWARDED_FOR));
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor;
        }
        String realIp = normalizeHeaderAddress(request.getHeader(HEADER_X_REAL_IP));
        if (StringUtils.hasText(realIp)) {
            return realIp;
        }
        String remoteAddr = request.getRemoteAddr();
        if (!StringUtils.hasText(remoteAddr)) {
            throw new IllegalStateException("Client IP address is unavailable");
        }
        return remoteAddr;
    }

    private static String firstForwardedFor(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return normalizeHeaderAddress(value.split(",", 2)[0]);
    }

    private static String normalizeHeaderAddress(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim();
    }
}
