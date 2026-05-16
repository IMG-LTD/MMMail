package com.mmmail.server.commercial.oidc;

import java.security.SecureRandom;
import java.util.Base64;

final class SecureTokenGenerator {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private SecureTokenGenerator() {
    }

    static String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
