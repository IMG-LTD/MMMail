package db.migration;

import com.mmmail.server.migration.SqlScriptMigrationSupport;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V2__baseline_seed_data extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        SqlScriptMigrationSupport.execute(context.getConnection(), "data.sql");
    }
}
