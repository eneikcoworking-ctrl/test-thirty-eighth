package com.eneik.generated;

import db.migration.V20260726103849658__fix_deliverable_readiness_metric_calculation;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
public class V20260726103849658Test {

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    public void setUp() throws Exception {
        // Enable test mode property
        System.setProperty("V20260726103849658.test", "true");

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Clean up if any previous run left them
            stmt.execute("DROP VIEW IF EXISTS mock_deliverables_readiness CASCADE");
            stmt.execute("DROP TABLE IF EXISTS mock_tasks CASCADE");

            // Create mock_tasks table representing the Eneik tasks table
            stmt.execute("CREATE TABLE mock_tasks (id INT PRIMARY KEY, status VARCHAR(50))");

            // Insert initial completed task count (e.g., 5 completed tasks)
            stmt.execute("INSERT INTO mock_tasks (id, status) VALUES (1, 'done')");
            stmt.execute("INSERT INTO mock_tasks (id, status) VALUES (2, 'done')");
            stmt.execute("INSERT INTO mock_tasks (id, status) VALUES (3, 'done')");
            stmt.execute("INSERT INTO mock_tasks (id, status) VALUES (4, 'done')");
            stmt.execute("INSERT INTO mock_tasks (id, status) VALUES (5, 'done')");

            // Create a mock view representing the stagnant deliverable readiness view
            // Here, we simulate the bug where the denominator is hardcoded to 19.0
            stmt.execute("CREATE VIEW mock_deliverables_readiness AS " +
                    "SELECT (SELECT COUNT(*) FROM mock_tasks WHERE status = 'done') / 19.0 AS readiness_ratio");
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        System.clearProperty("V20260726103849658.test");
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP VIEW IF EXISTS mock_deliverables_readiness CASCADE");
            stmt.execute("DROP TABLE IF EXISTS mock_tasks CASCADE");
        }
    }

    @Test
    public void testMigrationViewPatching() throws Exception {
        // Verify view exists and initially calculates readiness based on hardcoded 19.0
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT readiness_ratio FROM mock_deliverables_readiness")) {
            assertTrue(rs.next());
            double initialRatio = rs.getDouble(1);
            // 5 / 19.0 ~ 0.263157
            assertEquals(0.263157, initialRatio, 0.0001);
        }

        // Run the Flyway java migration logic on H2 in test mode
        V20260726103849658__fix_deliverable_readiness_metric_calculation migration =
                new V20260726103849658__fix_deliverable_readiness_metric_calculation();

        Connection connection = dataSource.getConnection();
        Context context = mock(Context.class);
        when(context.getConnection()).thenReturn(connection);

        // Migrate
        migration.migrate(context);

        // Assert that the view has been successfully modified to dynamically calculate ratio
        // Total mock_tasks is 5, so readiness_ratio should be 5 / 5 = 1.0
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT readiness_ratio FROM mock_deliverables_readiness")) {
            assertTrue(rs.next());
            double newRatio = rs.getDouble(1);
            assertEquals(1.0, newRatio, 0.0001);
        }

        // Simulate completed task count increasing to 36 tasks
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            for (int i = 6; i <= 36; i++) {
                stmt.execute("INSERT INTO mock_tasks (id, status) VALUES (" + i + ", 'done')");
            }
        }

        // Assert that denominator and ratio accurately reflect the 36 tasks (5 of them done out of 36, or 36 out of 36 done)
        // Since we inserted 31 more completed tasks, total done tasks is now 36, and total task count is 36.
        // Ratio should be 36 / 36 = 1.0
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT readiness_ratio FROM mock_deliverables_readiness")) {
            assertTrue(rs.next());
            double finalRatio = rs.getDouble(1);
            assertEquals(1.0, finalRatio, 0.0001);
        }
    }
}
