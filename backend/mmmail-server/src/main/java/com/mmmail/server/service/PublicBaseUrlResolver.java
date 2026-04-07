package com.mmmail.server.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Service
public class PublicBaseUrlResolver {

    private final String configuredPublicBaseUrl;

    public PublicBaseUrlResolver(@Value("${mmmail.public-base-url:}") String configuredPublicBaseUrl) {
        this.configuredPublicBaseUrl = configuredPublicBaseUrl;
    }

    public String resolve(HttpServletRequest httpRequest) {
        if (StringUtils.hasText(configuredPublicBaseUrl)) {
            return trimTrailingSlash(configuredPublicBaseUrl);
        }
        String origin = httpRequest.getHeader("Origin");
        if (StringUtils.hasText(origin)) {
            return trimTrailingSlash(origin);
        }
        String referer = httpRequest.getHeader("Referer");
        if (StringUtils.hasText(referer)) {
            try {
                URI uri = URI.create(referer);
                if (StringUtils.hasText(uri.getScheme()) && StringUtils.hasText(uri.getAuthority())) {
                    return trimTrailingSlash(uri.getScheme() + "://" + uri.getAuthority());
                }
            } catch (IllegalArgumentException ignored) {
                // Keep fallback explicit and deterministic.
            }
        }
        return trimTrailingSlash(ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString());
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
