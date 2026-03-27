package com.mmmail.server.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class StartupConfigValidator {

    private static final Set<String> FORBIDDEN_VALUES = new HashSet<>(Set.of(
            "change-me",
            "please-change-me-before-production-use",
            "nacos",
            "root"
    ));

    private final Environment environment;

    @Value("${mmmail.jwt-secret}")
    private String jwtSecret;

    @Value("${mmmail.cors-allowed-origins}")
    private String corsAllowedOrigins;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    @Value("${spring.cloud.nacos.username}")
    private String nacosUsername;

    @Value("${spring.cloud.nacos.password}")
    private String nacosPassword;

    @Value("${mmmail.drive.storage-root}")
    private String driveStorageRoot;

    @Value("${mmmail.drive.recycle-bin.retention-days}")
    private Integer driveRecycleBinRetentionDays;

    @Value("${mmmail.drive.preview-text-max-bytes}")
    private Integer drivePreviewTextMaxBytes;

    @Value("${mmmail.drive.public-share-rate-limit.window-seconds}")
    private Integer drivePublicShareRateLimitWindowSeconds;

    @Value("${mmmail.drive.public-share-rate-limit.max-requests}")
    private Integer drivePublicShareRateLimitMaxRequests;

    @Value("${mmmail.drive.public-share-rate-limit.redis-key-prefix}")
    private String drivePublicShareRateLimitRedisKeyPrefix;

    public StartupConfigValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validate() {
        List<String> errors = new ArrayList<>();
        boolean prodProfile = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        validateNotBlank("mmmail.jwt-secret", jwtSecret, errors);
        if (jwtSecret != null && jwtSecret.length() < 32) {
            errors.add("`mmmail.jwt-secret` must be at least 32 characters");
        }

        validateNotBlank("mmmail.cors-allowed-origins", corsAllowedOrigins, errors);
        if (corsAllowedOrigins != null && corsAllowedOrigins.contains("*")) {
            errors.add("`mmmail.cors-allowed-origins` cannot contain wildcard `*`");
        }

        validateNotBlank("spring.datasource.url", datasourceUrl, errors);
        validateNotBlank("spring.datasource.username", datasourceUsername, errors);
        validateNotBlank("spring.datasource.password", datasourcePassword, errors);
        validateNotBlank("spring.cloud.nacos.username", nacosUsername, errors);
        validateNotBlank("spring.cloud.nacos.password", nacosPassword, errors);
        validateNotBlank("mmmail.drive.storage-root", driveStorageRoot, errors);
        validatePositive("mmmail.drive.recycle-bin.retention-days", driveRecycleBinRetentionDays, errors);
        validatePositive("mmmail.drive.preview-text-max-bytes", drivePreviewTextMaxBytes, errors);
        validatePositive("mmmail.drive.public-share-rate-limit.window-seconds", drivePublicShareRateLimitWindowSeconds, errors);
        validatePositive("mmmail.drive.public-share-rate-limit.max-requests", drivePublicShareRateLimitMaxRequests, errors);
        validateNotBlank("mmmail.drive.public-share-rate-limit.redis-key-prefix", drivePublicShareRateLimitRedisKeyPrefix, errors);

        if (prodProfile) {
            validateNotForbidden("spring.datasource.password", datasourcePassword, errors);
            validateNotForbidden("spring.cloud.nacos.username", nacosUsername, errors);
            validateNotForbidden("spring.cloud.nacos.password", nacosPassword, errors);
            validateNotForbidden("mmmail.jwt-secret", jwtSecret, errors);

            if (corsAllowedOrigins != null) {
                String normalized = corsAllowedOrigins.toLowerCase();
                if (normalized.contains("localhost") || normalized.contains("127.0.0.1")) {
                    errors.add("`mmmail.cors-allowed-origins` must not include localhost in prod profile");
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Startup configuration validation failed: " + String.join("; ", errors));
        }
    }

    private static void validateNotBlank(String field, String value, List<String> errors) {
        if (value == null || value.isBlank()) {
            errors.add("`" + field + "` is required");
        }
    }

    private static void validateNotForbidden(String field, String value, List<String> errors) {
        if (value != null && FORBIDDEN_VALUES.contains(value.trim().toLowerCase())) {
            errors.add("`" + field + "` contains an insecure placeholder value");
        }
    }

    private static void validatePositive(String field, Integer value, List<String> errors) {
        if (value == null || value <= 0) {
            errors.add("`" + field + "` must be greater than 0");
        }
    }
}
