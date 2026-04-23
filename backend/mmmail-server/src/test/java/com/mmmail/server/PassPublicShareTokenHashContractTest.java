package com.mmmail.server;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PassPublicShareTokenHashContractTest {

    @Test
    void passPublicShareServiceShouldLookupByTokenHashAndStopAuditingRawTokens() throws Exception {
        Path moduleRoot = Path.of("").toAbsolutePath().normalize();
        String source = Files.readString(moduleRoot.resolve("src/main/java/com/mmmail/server/service/PassBusinessService.java"));

        assertThat(source).contains("PublicShareTokenCodec");
        assertThat(source).contains("PassSecureLink::getTokenHash");
        assertThat(source).doesNotContain("PassSecureLink::getToken, token");
        assertThat(source).doesNotContain(",token=");
    }
}
