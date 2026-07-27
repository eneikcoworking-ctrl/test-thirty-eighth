package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class V20260726103849658__fix_deliverable_readiness_metric_calculation extends BaseJavaMigration {

    private static final Set<String> APP_TABLES = new HashSet<>(Arrays.asList(
            "campaigns", "crm_chats", "crm_leads", "crm_messages", "crm_telegram_accounts",
            "dialogs", "leads", "messages", "proxies", "target_lists", "telegram_accounts",
            "telegram_account_trust_scores", "telegram_account_warm_up_logs", "telegram_dispatch_logs",
            "tg_accounts", "flyway_schema_history"
    ));

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        DatabaseMetaData metaData = connection.getMetaData();
        String dbName = metaData.getDatabaseProductName();

        boolean isPostgres = "PostgreSQL".equalsIgnoreCase(dbName);
        boolean isTestMode = "true".equals(System.getProperty("V20260726103849658.test"));

        System.out.println("DEBUG Flyway Java Migration: dbName=" + dbName + ", isTestMode=" + isTestMode);

        if (isPostgres || isTestMode) {
            executeMigration(connection, isPostgres);
        } else {
            System.out.println("Skipping PostgreSQL migration on H2 database.");
        }
    }

    private void executeMigration(Connection connection, boolean isPostgres) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            // 1. Identify Eneik's task/workspace tables (non-app tables)
            List<String> nonAppTables = new ArrayList<>();
            String queryTables = isPostgres
                ? "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE'"
                : "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'";

            try (ResultSet rs = stmt.executeQuery(queryTables)) {
                while (rs.next()) {
                    String tableName = rs.getString(1);
                    if (!APP_TABLES.contains(tableName.toLowerCase())) {
                        nonAppTables.add(tableName);
                    }
                }
            }

            System.out.println("DEBUG: Found non-app tables: " + nonAppTables);

            // Find the table representing tasks/sessions
            String taskTableName = null;
            for (String t : nonAppTables) {
                String lower = t.toLowerCase();
                if (lower.contains("task") || lower.contains("feature") || lower.contains("epic") || lower.contains("session")) {
                    taskTableName = t;
                    break;
                }
            }

            if (taskTableName == null && !nonAppTables.isEmpty()) {
                taskTableName = nonAppTables.get(0);
            }

            if (taskTableName == null) {
                System.out.println("DEBUG: No task table found. Skipping view patch.");
                return;
            }

            System.out.println("DEBUG: Selected task count source table: " + taskTableName);

            // 2. Identify all views related to readiness, progress, deliverables, or metrics
            List<String> targetViews = new ArrayList<>();
            String queryViews = isPostgres
                ? "SELECT table_name FROM information_schema.views WHERE table_schema = 'public'"
                : "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'VIEW' AND TABLE_SCHEMA = 'PUBLIC'";

            try (ResultSet rs = stmt.executeQuery(queryViews)) {
                while (rs.next()) {
                    String viewName = rs.getString(1);
                    String lowerView = viewName.toLowerCase();
                    if (lowerView.contains("readiness") || lowerView.contains("deliverable") || lowerView.contains("metric") || lowerView.contains("progress")) {
                        targetViews.add(viewName);
                    }
                }
            }

            System.out.println("DEBUG: Found target views: " + targetViews);

            // 3. Re-create the views to dynamically query the task count
            for (String viewName : targetViews) {
                String viewDef = null;
                if (isPostgres) {
                    try (Statement stmtDef = connection.createStatement();
                         ResultSet rsDef = stmtDef.executeQuery("SELECT pg_get_viewdef('" + viewName + "', true)")) {
                        if (rsDef.next()) {
                            viewDef = rsDef.getString(1);
                        }
                    }
                } else {
                    // For H2 test mode, fetch VIEW_DEFINITION from INFORMATION_SCHEMA
                    try (Statement stmtDef = connection.createStatement();
                         ResultSet rsDef = stmtDef.executeQuery("SELECT VIEW_DEFINITION FROM INFORMATION_SCHEMA.VIEWS WHERE TABLE_NAME = '" + viewName.toUpperCase() + "'")) {
                        if (rsDef.next()) {
                            viewDef = rsDef.getString(1);
                        }
                    }
                }

                if (viewDef != null) {
                    System.out.println("DEBUG: Original View Definition for [" + viewName + "]: " + viewDef);

                    String dynamicQuery = "(SELECT COUNT(*) FROM " + taskTableName + ")";

                    // Safe, precise, non-destructive string replacements targeting stagnant denominators (e.g., 19, 19.0, 19::)
                    String modifiedDef = viewDef
                            .replace("19.0", "NULLIF(" + dynamicQuery + "::numeric, 0)")
                            .replace("19::", "NULLIF(" + dynamicQuery + "::numeric, 0)::")
                            .replace("/ 19", "/ NULLIF(" + dynamicQuery + ", 0)")
                            .replace("= 19", "= " + dynamicQuery);

                    // Fallback to word boundaries if exact matches didn't hit
                    if (modifiedDef.equals(viewDef)) {
                        modifiedDef = viewDef.replaceAll("\\b19\\b", "NULLIF(" + dynamicQuery + ", 0)");
                    }

                    System.out.println("DEBUG: Modified View Definition for [" + viewName + "]: " + modifiedDef);

                    // Apply recreated view. NEVER use CASCADE to prevent risk of dependent data loss!
                    try (Statement stmtUpdate = connection.createStatement()) {
                        stmtUpdate.execute("CREATE OR REPLACE VIEW " + viewName + " AS " + modifiedDef);
                        System.out.println("DEBUG: Successfully updated view " + viewName + " to be dynamic.");
                    }
                }
            }
        }
    }
}
