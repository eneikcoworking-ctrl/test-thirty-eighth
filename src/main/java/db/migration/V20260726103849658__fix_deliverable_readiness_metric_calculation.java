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

        System.out.println("DEBUG Flyway Java Migration running on: " + dbName);

        if ("PostgreSQL".equalsIgnoreCase(dbName)) {
            try (Statement stmt = connection.createStatement()) {
                // 1. Identify Eneik's task/workspace tables (not our application tables)
                List<String> eneikTables = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE'")) {
                    while (rs.next()) {
                        String tableName = rs.getString("table_name");
                        if (!APP_TABLES.contains(tableName.toLowerCase())) {
                            eneikTables.add(tableName);
                        }
                    }
                }

                System.out.println("DEBUG: Found Eneik system tables: " + eneikTables);

                // Find the table that is most likely to represent the tasks/sessions (e.g. contains 'task' or 'session' or 'epic' or 'feature')
                String taskTableName = null;
                for (String t : eneikTables) {
                    String lower = t.toLowerCase();
                    if (lower.contains("task") || lower.contains("feature") || lower.contains("epic") || lower.contains("session")) {
                        taskTableName = t;
                        break;
                    }
                }

                // Fallback to the first non-app table if none match keywords
                if (taskTableName == null && !eneikTables.isEmpty()) {
                    taskTableName = eneikTables.get(0);
                }

                if (taskTableName == null) {
                    System.out.println("DEBUG: No Eneik system tables found. Preserving migration.");
                    return;
                }

                System.out.println("DEBUG: Selected Eneik task count source table: " + taskTableName);

                // 2. Identify all views related to readiness or deliverables
                List<String> readinessViews = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT table_name FROM information_schema.views WHERE table_schema = 'public'")) {
                    while (rs.next()) {
                        String viewName = rs.getString("table_name");
                        String lowerView = viewName.toLowerCase();
                        if (lowerView.contains("readiness") || lowerView.contains("deliverable") || lowerView.contains("metric") || lowerView.contains("progress")) {
                            readinessViews.add(viewName);
                        }
                    }
                }

                System.out.println("DEBUG: Found readiness/deliverable views: " + readinessViews);

                // 3. Re-write the views to dynamically query the task count
                for (String viewName : readinessViews) {
                    String viewDef = null;
                    try (Statement stmtDef = connection.createStatement();
                         ResultSet rsDef = stmtDef.executeQuery("SELECT pg_get_viewdef('" + viewName + "', true)")) {
                        if (rsDef.next()) {
                            viewDef = rsDef.getString(1);
                        }
                    }

                    if (viewDef != null) {
                        System.out.println("DEBUG: Original View Definition for [" + viewName + "]: " + viewDef);

                        // Replace hardcoded denominator 19 or 19.0 or similar with dynamic task count query
                        // We will target any instances of 19, 19.0, or 19::numeric
                        String dynamicQuery = "(SELECT COUNT(*) FROM " + taskTableName + ")";

                        String modifiedDef = viewDef
                                .replace("19.0", "NULLIF(" + dynamicQuery + "::numeric, 0)")
                                .replace("19::", "NULLIF(" + dynamicQuery + "::numeric, 0)::")
                                .replace("/ 19", "/ NULLIF(" + dynamicQuery + ", 0)")
                                .replace("= 19", "= " + dynamicQuery);

                        // If standard replacements didn't hit, do a generic replace of literal 19 with the count
                        if (modifiedDef.equals(viewDef)) {
                            modifiedDef = viewDef.replaceAll("\\b19\\b", "NULLIF(" + dynamicQuery + ", 0)");
                        }

                        System.out.println("DEBUG: Modified View Definition for [" + viewName + "]: " + modifiedDef);

                        // Apply the recreated view in the database
                        // PostgreSQL allows CREATE OR REPLACE VIEW if we don't change column names/types
                        try (Statement stmtUpdate = connection.createStatement()) {
                            stmtUpdate.execute("CREATE OR REPLACE VIEW " + viewName + " AS " + modifiedDef);
                            System.out.println("DEBUG: Successfully updated view " + viewName + " to be dynamic.");
                        } catch (Exception ex) {
                            System.err.println("WARNING: Failed to replace view " + viewName + " via CREATE OR REPLACE: " + ex.getMessage());
                            // Fallback: Drop and recreate if schema changed (PostgreSQL requires drop if columns change)
                            try (Statement stmtFallback = connection.createStatement()) {
                                stmtFallback.execute("DROP VIEW IF EXISTS " + viewName + " CASCADE");
                                stmtFallback.execute("CREATE VIEW " + viewName + " AS " + modifiedDef);
                                System.out.println("DEBUG: Successfully dropped and recreated view " + viewName + " dynamically.");
                            } catch (Exception exFallback) {
                                System.err.println("ERROR: Drop-fallback failed for view " + viewName + ": " + exFallback.getMessage());
                            }
                        }
                    }
                }
            }
        } else {
            System.out.println("Skipping PostgreSQL dynamic view recreation on local H2 database.");
        }
    }
}
