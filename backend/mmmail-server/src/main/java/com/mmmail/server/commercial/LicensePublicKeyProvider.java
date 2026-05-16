package com.mmmail.server.commercial;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LicensePublicKeyProvider {

    private static final String PUBLIC_KEY_ENV = "MMMAIL_LICENSE_PUBLIC_KEY";
    private static final String BEGIN_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----";
    private static final String END_PUBLIC_KEY = "-----END PUBLIC KEY-----";

    private final String encodedPublicKey;

    public LicensePublicKeyProvider(@Value("${mmmail.license.public-key:}") String encodedPublicKey) {
        this.encodedPublicKey = encodedPublicKey;
    }

    public PublicKey requirePublicKey() {
        if (!StringUtils.hasText(encodedPublicKey)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, PUBLIC_KEY_ENV + " is required for license upload");
        }
        return parsePublicKey(normalize(encodedPublicKey));
    }

    private PublicKey parsePublicKey(String normalizedPublicKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(normalizedPublicKey);
            return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (IllegalArgumentException | GeneralSecurityException ex) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    PUBLIC_KEY_ENV + " must be an Ed25519 X.509 public key"
            );
        }
    }

    private String normalize(String value) {
        return value
                .replace(BEGIN_PUBLIC_KEY, "")
                .replace(END_PUBLIC_KEY, "")
                .replaceAll("\\s+", "");
    }
}
