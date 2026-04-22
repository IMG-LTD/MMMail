package com.mmmail.server;

import com.mmmail.foundation.security.PublicShareTokenCodec;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PublicShareTokenHashMigrationIntegrationTest {

    @Test
    void tokenHashMigrationShouldCoverAllPublicShareEntities() throws Exception {
        PublicShareTokenCodec codec = new PublicShareTokenCodec();
        assertThat(codec.hash("demo-token")).isEqualTo(codec.hash("demo-token")).hasSize(64);

        Path moduleRoot = Path.of("").toAbsolutePath().normalize();
        String migration = Files.readString(moduleRoot.resolve("src/main/java/db/migration/V15__public_share_token_hash.java"));
        String mailEntity = Files.readString(moduleRoot.resolve("src/main/java/com/mmmail/server/model/entity/MailExternalSecureLink.java"));
        String passEntity = Files.readString(moduleRoot.resolve("src/main/java/com/mmmail/server/model/entity/PassSecureLink.java"));
        String driveEntity = Files.readString(moduleRoot.resolve("src/main/java/com/mmmail/server/model/entity/DriveShareLink.java"));

        assertThat(migration)
                .contains("mail_external_secure_link")
                .contains("pass_secure_link")
                .contains("drive_share_link")
                .contains("token_hash");
        assertThat(mailEntity).contains("private String tokenHash;");
        assertThat(passEntity).contains("private String tokenHash;");
        assertThat(driveEntity).contains("private String tokenHash;");
    }
}
