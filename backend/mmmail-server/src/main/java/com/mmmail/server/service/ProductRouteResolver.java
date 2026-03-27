package com.mmmail.server.service;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductRouteResolver {

    private static final List<RouteProductBinding> ROUTE_BINDINGS = List.of(
            new RouteProductBinding("/api/v1/authenticator", "AUTHENTICATOR"),
            new RouteProductBinding("/api/v1/calendar", "CALENDAR"),
            new RouteProductBinding("/api/v1/contact-groups", "MAIL"),
            new RouteProductBinding("/api/v1/contacts", "MAIL"),
            new RouteProductBinding("/api/v1/docs", "DOCS"),
            new RouteProductBinding("/api/v1/drive", "DRIVE"),
            new RouteProductBinding("/api/v1/labels", "MAIL"),
            new RouteProductBinding("/api/v1/lumo", "LUMO"),
            new RouteProductBinding("/api/v1/mail-easy-switch", "MAIL"),
            new RouteProductBinding("/api/v1/mail-filters", "MAIL"),
            new RouteProductBinding("/api/v1/mail-folders", "MAIL"),
            new RouteProductBinding("/api/v1/mails", "MAIL"),
            new RouteProductBinding("/api/v1/meet", "MEET"),
            new RouteProductBinding("/api/v1/search-history", "MAIL"),
            new RouteProductBinding("/api/v1/search-presets", "MAIL"),
            new RouteProductBinding("/api/v1/sheets", "SHEETS"),
            new RouteProductBinding("/api/v1/simplelogin", "SIMPLELOGIN"),
            new RouteProductBinding("/api/v1/standard-notes", "STANDARD_NOTES"),
            new RouteProductBinding("/api/v1/vpn", "VPN"),
            new RouteProductBinding("/api/v1/wallet", "WALLET")
    );

    public String resolveApiProductKey(String requestUri) {
        if (requestUri.startsWith("/api/v1/pass/orgs/")) {
            return null;
        }
        if (requestUri.startsWith("/api/v1/pass")) {
            return "PASS";
        }
        for (RouteProductBinding binding : ROUTE_BINDINGS) {
            if (requestUri.startsWith(binding.prefix())) {
                return binding.productKey();
            }
        }
        return null;
    }

    private record RouteProductBinding(String prefix, String productKey) {
    }
}
