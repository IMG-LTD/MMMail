package com.mmmail.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mmmail.dev.seed")
public class DevSeedProperties {

    private boolean enabled;
    private boolean wallet = true;
    private boolean meet = true;
    private boolean community = true;
    private boolean searchIndex = true;
    private boolean domain = true;
    private boolean webpush;

    public boolean moduleEnabled(String module) {
        return switch (module) {
            case "wallet" -> wallet;
            case "meet" -> meet;
            case "community" -> community;
            case "search-index" -> searchIndex;
            case "domain" -> domain;
            case "webpush" -> webpush;
            default -> false;
        };
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setWallet(boolean wallet) {
        this.wallet = wallet;
    }

    public void setMeet(boolean meet) {
        this.meet = meet;
    }

    public void setCommunity(boolean community) {
        this.community = community;
    }

    public void setSearchIndex(boolean searchIndex) {
        this.searchIndex = searchIndex;
    }

    public void setDomain(boolean domain) {
        this.domain = domain;
    }

    public void setWebpush(boolean webpush) {
        this.webpush = webpush;
    }
}
