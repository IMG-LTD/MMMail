package com.mmmail.server;

import com.mmmail.server.config.DevSeedDataRunner;
import com.mmmail.server.config.DevSeedProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DevSeedV212ContractTest {

    private static final List<String> SEED_SCRIPTS = List.of(
            "wallet.sql",
            "meet.sql",
            "community.sql",
            "search-index.sql",
            "domain.sql",
            "webpush.sql"
    );

    @Test
    void devProfileShouldEnableSeedDataWithIndependentModuleToggles() throws Exception {
        String yaml = readResource("application-dev.yml");

        assertThat(yaml).contains(
                "seed:",
                "enabled: ${MMMAIL_DEV_SEED_ENABLED:true}",
                "wallet: ${MMMAIL_DEV_SEED_WALLET:true}",
                "meet: ${MMMAIL_DEV_SEED_MEET:true}",
                "community: ${MMMAIL_DEV_SEED_COMMUNITY:true}",
                "search-index: ${MMMAIL_DEV_SEED_SEARCH_INDEX:true}",
                "domain: ${MMMAIL_DEV_SEED_DOMAIN:true}",
                "webpush: ${MMMAIL_DEV_SEED_WEBPUSH:false}"
        );
    }

    @Test
    void seedScriptsShouldCoverV212SampleDataRequirements() throws Exception {
        Path seedDirectory = resourceRoot().resolve("data-seed");

        assertThat(seedDirectory)
                .as("v2.1.2 data-seed directory")
                .isDirectory();
        for (String script : SEED_SCRIPTS) {
            assertThat(seedDirectory.resolve(script)).exists();
        }

        assertThat(readSeed("wallet.sql")).contains(
                "insert ignore into wallet_account",
                "insert ignore into wallet_transaction",
                "dev-wallet-primary",
                "dev-wallet-savings",
                "dev-wallet-ops"
        );
        assertThat(readSeed("meet.sql")).contains(
                "insert ignore into meet_room_session",
                "join_code"
        );
        assertThat(readSeed("community.sql")).contains(
                "insert ignore into community_topic",
                "insert ignore into community_post",
                "insert ignore into community_comment"
        );
        assertThat(readSeed("search-index.sql")).contains(
                "insert ignore into search_reindex_job",
                "QUEUED"
        );
        assertThat(readSeed("domain.sql")).contains(
                "insert ignore into org_custom_domain",
                "PENDING",
                "expected DNS"
        );
        assertThat(readSeed("webpush.sql")).contains(
                "-- insert ignore into web_push_subscription",
                "real endpoint"
        );
    }

    @Test
    void devSeedRunnerShouldOnlyRunInDevAndFailOnScriptFailures() throws Exception {
        String runner = readJava("config/DevSeedDataRunner.java");
        String properties = readJava("config/DevSeedProperties.java");

        assertThat(properties).contains(
                "@ConfigurationProperties(prefix = \"mmmail.dev.seed\")",
                "boolean moduleEnabled"
        );
        assertThat(runner).contains(
                "@Profile(\"dev\")",
                "ApplicationRunner",
                "ScriptUtils.executeSqlScript",
                "log.warn",
                "throw new IllegalStateException",
                "classpath:data-seed/wallet.sql",
                "classpath:data-seed/meet.sql",
                "classpath:data-seed/community.sql",
                "classpath:data-seed/search-index.sql",
                "classpath:data-seed/domain.sql",
                "classpath:data-seed/webpush.sql"
        );
    }

    @Test
    void devSeedRunnerShouldNotAbortStartupWhenEnabledScriptIsUnavailable() {
        DevSeedProperties properties = walletOnlySeedProperties();
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        Resource missingResource = mock(Resource.class);

        when(resourceLoader.getResource("classpath:data-seed/wallet.sql")).thenReturn(missingResource);
        when(missingResource.exists()).thenReturn(false);

        DevSeedDataRunner runner = new DevSeedDataRunner(properties, resourceLoader, mock(DataSource.class));

        assertThatCode(() -> runner.run(null)).doesNotThrowAnyException();
    }

    private String readSeed(String filename) throws IOException {
        return Files.readString(resourceRoot().resolve("data-seed").resolve(filename));
    }

    private String readResource(String filename) throws IOException {
        return Files.readString(resourceRoot().resolve(filename));
    }

    private String readJava(String path) throws IOException {
        return Files.readString(moduleRoot().resolve("src/main/java/com/mmmail/server").resolve(path));
    }

    private Path resourceRoot() {
        return moduleRoot().resolve("src/main/resources");
    }

    private Path moduleRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null && !Files.exists(current.resolve("pom.xml"))) {
            current = current.getParent();
        }
        assertThat(current)
                .as("module root containing pom.xml should exist")
                .isNotNull();
        return current;
    }

    private DevSeedProperties walletOnlySeedProperties() {
        DevSeedProperties properties = new DevSeedProperties();
        properties.setEnabled(true);
        properties.setWallet(true);
        properties.setMeet(false);
        properties.setCommunity(false);
        properties.setSearchIndex(false);
        properties.setDomain(false);
        properties.setWebpush(false);
        return properties;
    }
}
