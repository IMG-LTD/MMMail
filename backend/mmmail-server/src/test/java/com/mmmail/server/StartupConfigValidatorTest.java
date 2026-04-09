package com.mmmail.server;

import com.mmmail.server.config.StartupConfigValidator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StartupConfigValidatorTest {

    @Test
    void allowsMissingNacosCredentialsWhenMinimalModeDisablesNacos() {
        StartupConfigValidator validator = newValidator(false, "", "");

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void requiresNacosCredentialsWhenNacosModeRemainsEnabled() {
        StartupConfigValidator validator = newValidator(true, "", "");

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.cloud.nacos.username")
                .hasMessageContaining("spring.cloud.nacos.password");
    }

    private StartupConfigValidator newValidator(boolean nacosEnabled, String username, String password) {
        MockEnvironment environment = new MockEnvironment();
        StartupConfigValidator validator = new StartupConfigValidator(environment);
        setField(validator, "jwtSecret", "0123456789abcdef0123456789abcdef");
        setField(validator, "corsAllowedOrigins", "http://127.0.0.1:3001");
        setField(validator, "datasourceUrl", "jdbc:mysql://127.0.0.1:3306/mmmail");
        setField(validator, "datasourceUsername", "mmmail_app");
        setField(validator, "datasourcePassword", "DbPassword123!");
        setField(validator, "nacosEnabled", nacosEnabled);
        setField(validator, "nacosUsername", username);
        setField(validator, "nacosPassword", password);
        setField(validator, "driveStorageRoot", "/tmp/mmmail-drive-test");
        setField(validator, "driveRecycleBinRetentionDays", 30);
        setField(validator, "drivePreviewTextMaxBytes", 262144);
        setField(validator, "drivePublicShareRateLimitWindowSeconds", 60);
        setField(validator, "drivePublicShareRateLimitMaxRequests", 30);
        setField(validator, "drivePublicShareRateLimitRedisKeyPrefix", "mmmail:drive:test");
        return validator;
    }

    private void setField(StartupConfigValidator validator, String name, Object value) {
        ReflectionTestUtils.setField(validator, name, value);
    }
}
