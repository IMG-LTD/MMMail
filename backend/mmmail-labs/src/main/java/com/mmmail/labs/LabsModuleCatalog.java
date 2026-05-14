package com.mmmail.labs;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class LabsModuleCatalog {

    private static final String PREVIEW = "preview";
    private static final List<LabsModuleDescriptor> MODULES = List.of(
            module("authenticator", "Authenticator", "Preview authenticator module with lightweight device flows.", true),
            module("simplelogin", "SimpleLogin", "Alias and mailbox preview module shell.", true),
            module("standard-notes", "Standard Notes", "Secure notes preview shell and capability statement.", true),
            module("vpn", "VPN", "Network protection preview shell and maturity disclaimer.", false),
            module("meet", "Meet", "Meeting preview shell and current collaboration scope.", false),
            module("wallet", "Wallet", "Wallet preview shell and compliance boundary summary.", false),
            module("lumo", "Lumo", "Lumo preview shell and experiment feedback entry point.", false)
    );

    public List<LabsModuleDescriptor> listModules() {
        return MODULES;
    }

    public Optional<LabsModuleDescriptor> findModule(String key) {
        String normalizedKey = normalizeKey(key);
        return MODULES.stream()
                .filter(module -> module.key().equals(normalizedKey))
                .findFirst();
    }

    private static LabsModuleDescriptor module(String key, String label, String description, boolean enabled) {
        return new LabsModuleDescriptor(key, label, description, enabled, PREVIEW, false, false, null);
    }

    private static String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        return key.trim().toLowerCase(Locale.ROOT);
    }
}
