package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.model.vo.AuthenticatorCodeVo;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Arrays;

@Service
public class TotpCodeService {

    private static final int[] BASE32_LOOKUP = buildBase32Lookup();

    public AuthenticatorCodeVo generateCode(String secretCiphertext, String algorithm, int digits, int periodSeconds) {
        Instant now = Instant.now();
        String code = generateTotpCode(secretCiphertext, algorithm, digits, periodSeconds, now.getEpochSecond());
        int expiresInSeconds = periodSeconds - (int) (now.getEpochSecond() % periodSeconds);
        if (expiresInSeconds <= 0) {
            expiresInSeconds = periodSeconds;
        }
        return new AuthenticatorCodeVo(code, expiresInSeconds, periodSeconds, digits);
    }

    private String generateTotpCode(String secretCiphertext, String algorithm, int digits, int periodSeconds, long epochSeconds) {
        byte[] secret = decodeBase32(secretCiphertext);
        long counter = epochSeconds / periodSeconds;
        byte[] counterBytes = new byte[8];
        for (int index = 7; index >= 0; index--) {
            counterBytes[index] = (byte) (counter & 0xFF);
            counter >>= 8;
        }
        byte[] hash = hmac(secret, algorithm, counterBytes);
        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);
        int otp = binary % pow10(digits);
        return ("%0" + digits + "d").formatted(otp);
    }

    private byte[] hmac(byte[] secret, String algorithm, byte[] counterBytes) {
        String macAlgorithm = switch (algorithm) {
            case "SHA1" -> "HmacSHA1";
            case "SHA256" -> "HmacSHA256";
            case "SHA512" -> "HmacSHA512";
            default -> throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported authenticator algorithm");
        };
        try {
            Mac mac = Mac.getInstance(macAlgorithm);
            mac.init(new SecretKeySpec(secret, macAlgorithm));
            return mac.doFinal(counterBytes);
        } catch (GeneralSecurityException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to generate authenticator code");
        }
    }

    private byte[] decodeBase32(String secretCiphertext) {
        byte[] output = new byte[(secretCiphertext.length() * 5) / 8 + 1];
        int outputIndex = 0;
        int buffer = 0;
        int bitsLeft = 0;
        for (int index = 0; index < secretCiphertext.length(); index++) {
            char current = secretCiphertext.charAt(index);
            if (current == '=') {
                break;
            }
            if (current >= BASE32_LOOKUP.length || BASE32_LOOKUP[current] < 0) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "Authenticator secret must be base32");
            }
            buffer = (buffer << 5) | BASE32_LOOKUP[current];
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                output[outputIndex++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        if (outputIndex == 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Authenticator secret must be base32");
        }
        return Arrays.copyOf(output, outputIndex);
    }

    private int pow10(int exponent) {
        int value = 1;
        for (int index = 0; index < exponent; index++) {
            value *= 10;
        }
        return value;
    }

    private static int[] buildBase32Lookup() {
        int[] lookup = new int[128];
        Arrays.fill(lookup, -1);
        for (int index = 0; index < 26; index++) {
            lookup['A' + index] = index;
        }
        for (int index = 0; index < 6; index++) {
            lookup['2' + index] = 26 + index;
        }
        return lookup;
    }
}
