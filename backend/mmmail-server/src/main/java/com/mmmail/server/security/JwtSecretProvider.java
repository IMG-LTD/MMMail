package com.mmmail.server.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class JwtSecretProvider {

    private final String inlineSecret;
    private final String secretFile;

    public JwtSecretProvider(
            @Value("${mmmail.jwt-secret:}") String inlineSecret,
            @Value("${mmmail.jwt-secret-file:}") String secretFile
    ) {
        this.inlineSecret = inlineSecret;
        this.secretFile = secretFile;
    }

    public String resolve() {
        return resolve(inlineSecret, secretFile);
    }

    public static String resolve(String inlineSecret, String secretFile) {
        if (StringUtils.hasText(secretFile)) {
            return readSecretFile(secretFile.trim());
        }
        if (StringUtils.hasText(inlineSecret)) {
            return inlineSecret.trim();
        }
        throw new IllegalStateException("`mmmail.jwt-secret` or `mmmail.jwt-secret-file` is required");
    }

    private static String readSecretFile(String secretFile) {
        try {
            String secret = Files.readString(Path.of(secretFile), StandardCharsets.UTF_8).trim();
            if (!StringUtils.hasText(secret)) {
                throw new IllegalStateException("`mmmail.jwt-secret-file` must not be blank");
            }
            return secret;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read `mmmail.jwt-secret-file`: " + secretFile, exception);
        }
    }
}
