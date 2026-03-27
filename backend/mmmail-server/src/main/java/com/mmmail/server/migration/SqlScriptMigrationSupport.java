package com.mmmail.server.migration;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;

public final class SqlScriptMigrationSupport {

    private SqlScriptMigrationSupport() {
    }

    public static void execute(Connection connection, String resourcePath) {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        EncodedResource encoded = new EncodedResource(resource, StandardCharsets.UTF_8);
        ScriptUtils.executeSqlScript(connection, encoded);
    }
}
