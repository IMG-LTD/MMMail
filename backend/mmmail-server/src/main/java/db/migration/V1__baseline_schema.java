package db.migration;

import com.mmmail.server.migration.SqlScriptMigrationSupport;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V1__baseline_schema extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        SqlScriptMigrationSupport.execute(context.getConnection(), "db/baseline/community-v1-schema.sql");
    }
}
