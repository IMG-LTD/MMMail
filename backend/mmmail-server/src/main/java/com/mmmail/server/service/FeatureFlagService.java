package com.mmmail.server.service;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class FeatureFlagService {

    private static final String ENABLED_FLAGS_SQL = """
            select flag_key
            from feature_flag
            where enabled = 1
            order by flag_key
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AtomicReference<List<String>> enabledFlags = new AtomicReference<>(List.of());

    public FeatureFlagService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void loadOnStartup() {
        refresh();
    }

    @Scheduled(fixedDelayString = "${mmmail.feature-flags.watch-interval-ms:5000}")
    public void refreshFromWatch() {
        refresh();
    }

    public void refresh() {
        enabledFlags.set(List.copyOf(jdbcTemplate.queryForList(ENABLED_FLAGS_SQL, String.class)));
    }

    public List<String> enabledFlags() {
        return enabledFlags.get();
    }
}
