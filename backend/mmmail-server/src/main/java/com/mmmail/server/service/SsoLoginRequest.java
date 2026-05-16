package com.mmmail.server.service;

public record SsoLoginRequest(String email, String ipAddress, String providerSubject) {
}
