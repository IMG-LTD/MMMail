package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class MailExternalAccountSecretCodec {

    private static final String VERSION = "v1";
    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final String encryptionSecret;
    private final SecureRandom secureRandom = new SecureRandom();

    public MailExternalAccountSecretCodec(
            @Value("${mmmail.mail.external-account-secret:}") String encryptionSecret
    ) {
        this.encryptionSecret = encryptionSecret;
    }

    public String encrypt(String plainText) {
        requireConfigured();
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return VERSION + ":" + encode(iv) + ":" + encode(encrypted);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to encrypt external account secret");
        }
    }

    public String decrypt(String ciphertext) {
        requireConfigured();
        try {
            String[] parts = ciphertext.split(":");
            if (parts.length != 3 || !VERSION.equals(parts[0])) {
                throw new IllegalArgumentException("Unsupported external account secret format");
            }
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, decode(parts[1])));
            return new String(cipher.doFinal(decode(parts[2])), StandardCharsets.UTF_8);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ErrorCode.MAIL_EXTERNAL_ACCOUNT_CONFIG, "External account secret cannot be decrypted");
        }
    }

    private void requireConfigured() {
        if (!StringUtils.hasText(encryptionSecret)) {
            throw new BizException(
                    ErrorCode.MAIL_EXTERNAL_ACCOUNT_CONFIG,
                    "External account credential encryption secret is not configured"
            );
        }
    }

    private SecretKeySpec key() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return new SecretKeySpec(digest.digest(encryptionSecret.getBytes(StandardCharsets.UTF_8)), "AES");
    }

    private String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}
