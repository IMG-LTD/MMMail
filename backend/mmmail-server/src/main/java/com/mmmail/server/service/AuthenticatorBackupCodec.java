package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AuthenticatorBackupCodec {

    private static final String KEY_DERIVATION = "PBKDF2WithHmacSHA256";
    private static final String ENCRYPTION = "AES-256-GCM";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final int IV_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptedPayload encrypt(String plainText, String passphrase) {
        validatePassphrase(passphrase);
        try {
            byte[] salt = randomBytes(SALT_BYTES);
            byte[] iv = randomBytes(IV_BYTES);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return new EncryptedPayload(
                    Base64.getEncoder().encodeToString(salt),
                    Base64.getEncoder().encodeToString(iv),
                    Base64.getEncoder().encodeToString(cipherBytes),
                    ITERATIONS,
                    KEY_DERIVATION,
                    ENCRYPTION
            );
        } catch (GeneralSecurityException ex) {
            throw new BizException(ErrorCode.AUTHENTICATOR_BACKUP_INVALID, "Failed to encrypt authenticator backup");
        }
    }

    public String decrypt(BackupEnvelope envelope, String passphrase) {
        validatePassphrase(passphrase);
        try {
            byte[] salt = Base64.getDecoder().decode(envelope.salt());
            byte[] iv = Base64.getDecoder().decode(envelope.iv());
            byte[] cipherBytes = Base64.getDecoder().decode(envelope.ciphertext());
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new BizException(ErrorCode.AUTHENTICATOR_BACKUP_INVALID, "Authenticator backup passphrase is invalid");
        }
    }

    private SecretKeySpec deriveKey(String passphrase, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    private byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    private void validatePassphrase(String passphrase) {
        if (!StringUtils.hasText(passphrase) || passphrase.trim().length() < 8) {
            throw new BizException(ErrorCode.AUTHENTICATOR_BACKUP_INVALID, "Authenticator backup passphrase is invalid");
        }
    }

    public record EncryptedPayload(
            String salt,
            String iv,
            String ciphertext,
            int iterations,
            String keyDerivation,
            String encryption
    ) {
    }

    public record BackupEnvelope(
            String salt,
            String iv,
            String ciphertext
    ) {
    }
}
