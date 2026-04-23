package com.mmmail.server;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MailPublicShareTokenHashContractTest {

    @Test
    void mailPublicShareServiceShouldLookupByTokenHashAndStopAuditingRawTokens() throws Exception {
        Path moduleRoot = Path.of("").toAbsolutePath().normalize();
        String source = Files.readString(moduleRoot.resolve("src/main/java/com/mmmail/server/service/MailExternalSecureLinkService.java"));

        assertThat(source).contains("PublicShareTokenCodec");
        assertThat(source).contains("MailExternalSecureLink::getTokenHash");
        assertThat(source).doesNotContain("MailExternalSecureLink::getToken, requireToken(token)");
        assertThat(source).doesNotContain(",token=");
    }
}
