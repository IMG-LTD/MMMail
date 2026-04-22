package com.mmmail.server;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DrivePublicShareTokenHashContractTest {

    @Test
    void drivePublicShareServiceShouldLookupByTokenHashInsteadOfRawToken() throws Exception {
        Path moduleRoot = Path.of("").toAbsolutePath().normalize();
        String source = Files.readString(moduleRoot.resolve("src/main/java/com/mmmail/server/service/DriveService.java"));

        assertThat(source).contains("PublicShareTokenCodec");
        assertThat(source).contains("DriveShareLink::getTokenHash");
        assertThat(source).doesNotContain("DriveShareLink::getToken, token");
    }
}
