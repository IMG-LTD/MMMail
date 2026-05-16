package com.mmmail.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:mmmail-dev-seed;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "mmmail.dev.seed.enabled=true",
        "mmmail.dev.seed.webpush=false"
})
@ActiveProfiles({"test", "dev"})
class DevSeedV212StartupIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void devProfileShouldMakeV212SeedDataVisibleAfterStartup() {
        assertThat(count("select count(*) from wallet_account where wallet_name like 'dev-wallet-%'")).isEqualTo(3);
        assertThat(count("select count(*) from wallet_transaction where signature_hash like 'seed-signature-%'")).isEqualTo(5);
        assertThat(count("select count(*) from meet_room_session where room_code = 'DEV-SEED-ROOM'")).isEqualTo(1);
        assertThat(count("select count(*) from community_post where id like 'dev-seed-post-%'")).isEqualTo(5);
        assertThat(count("select count(*) from search_reindex_job where id = 'dev-seed-reindex-001'")).isEqualTo(1);
        assertThat(count("select count(*) from org_custom_domain where domain = 'dev-seed.example.test'")).isEqualTo(1);
    }

    private int count(String sql) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count == null ? 0 : count;
    }
}
