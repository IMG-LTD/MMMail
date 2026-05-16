package com.mmmail.server.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DevSeedDataRunnerTest {

    @Test
    void shouldFailStartupWhenEnabledSeedScriptFails() {
        DevSeedProperties properties = new DevSeedProperties();
        properties.setEnabled(true);
        DevSeedDataRunner runner = new DevSeedDataRunner(
                properties,
                new StaticSqlResourceLoader("insert into missing_seed_table values (1);"),
                devSeedDataSource()
        );

        assertThatThrownBy(() -> runner.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Dev seed script failed")
                .hasMessageContaining("wallet");
    }

    private static DriverManagerDataSource devSeedDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:dev-seed-failure;MODE=MySQL;DB_CLOSE_DELAY=-1");
        return dataSource;
    }

    private record StaticSqlResourceLoader(String sql) implements ResourceLoader {

        @Override
        public Resource getResource(String location) {
            return new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)) {
                @Override
                public String getDescription() {
                    return location;
                }
            };
        }

        @Override
        public ClassLoader getClassLoader() {
            return DevSeedDataRunnerTest.class.getClassLoader();
        }
    }
}
