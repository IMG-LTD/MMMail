package com.mmmail.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;

@Component
@Profile("dev")
public class DevSeedDataRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevSeedDataRunner.class);
    private static final List<SeedScript> SEED_SCRIPTS = List.of(
            new SeedScript("wallet", "classpath:data-seed/wallet.sql"),
            new SeedScript("meet", "classpath:data-seed/meet.sql"),
            new SeedScript("community", "classpath:data-seed/community.sql"),
            new SeedScript("search-index", "classpath:data-seed/search-index.sql"),
            new SeedScript("domain", "classpath:data-seed/domain.sql"),
            new SeedScript("webpush", "classpath:data-seed/webpush.sql")
    );

    private final DevSeedProperties properties;
    private final ResourceLoader resourceLoader;
    private final DataSource dataSource;

    public DevSeedDataRunner(DevSeedProperties properties, ResourceLoader resourceLoader, DataSource dataSource) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        SEED_SCRIPTS.stream()
                .filter(script -> properties.moduleEnabled(script.module()))
                .forEach(this::executeSeedScript);
    }

    private void executeSeedScript(SeedScript script) {
        Resource resource = resourceLoader.getResource(script.location());
        if (!resource.exists()) {
            log.warn("Dev seed script missing: {}", script.location());
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new EncodedResource(resource, StandardCharsets.UTF_8));
            log.info("Dev seed script executed: {}", script.location());
        } catch (Exception exception) {
            throw new IllegalStateException("Dev seed script failed: " + script.module() + " (" + script.location() + ")", exception);
        }
    }

    private record SeedScript(String module, String location) {
    }
}
