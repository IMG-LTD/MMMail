package com.mmmail.server.observability;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RequestRouteModuleResolver {

    public String resolve(String path) {
        if (!StringUtils.hasText(path)) {
            return "unknown";
        }
        if (path.startsWith("/api/v1/auth")) {
            return "auth";
        }
        if (path.startsWith("/api/v2/platform")) {
            return "platform";
        }
        if (path.startsWith("/api/v1/mails")
                || path.startsWith("/api/v1/mail-")
                || path.startsWith("/api/v1/conversations")
                || path.startsWith("/api/v1/public/mail")) {
            return "mail";
        }
        if (path.startsWith("/api/v1/calendar")) {
            return "calendar";
        }
        if (path.startsWith("/api/v1/drive") || path.startsWith("/api/v1/public/drive")) {
            return "drive";
        }
        if (path.startsWith("/api/v1/system") || path.startsWith("/actuator")) {
            return "system";
        }
        if (path.startsWith("/api/v1/orgs") || path.startsWith("/api/v1/audit") || path.startsWith("/api/v1/settings")) {
            return "admin";
        }
        if (!path.startsWith("/api/v1/")) {
            return "public";
        }
        String[] segments = path.substring("/api/v1/".length()).split("/");
        return segments.length == 0 || !StringUtils.hasText(segments[0]) ? "unknown" : segments[0];
    }
}
