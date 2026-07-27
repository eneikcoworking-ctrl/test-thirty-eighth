package com.eneik.generated;

import com.eneik.generated.model.Deliverable;
import com.eneik.generated.repository.DeliverableRepository;
import com.eneik.generated.service.EneikReadinessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "eneik.records.dir=src/test/resources/non_existent_records")
@AutoConfigureMockMvc
@Transactional
public class DeliverableReadinessTrackingTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeliverableRepository deliverableRepository;

    @Test
    public void testInitialReadinessState() throws Exception {
        // Initially, Flyway migration should have inserted 19 deliverables, 5 of which are MERGED
        long total = deliverableRepository.count();
        long completed = deliverableRepository.countCompletedDeliverables();

        assertThat(total).isEqualTo(19);
        assertThat(completed).isEqualTo(5);

        // Test GET API endpoints for readiness
        String[] readinessEndpoints = {
            "/api/deliverables/readiness",
            "/api/project/readiness",
            "/api/readiness",
            "/api/metrics/readiness",
            "/api/project/state"
        };

        for (String url : readinessEndpoints) {
            mockMvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completed", is(5)))
                    .andExpect(jsonPath("$.completedCount", is(5)))
                    .andExpect(jsonPath("$.total", is(19)))
                    .andExpect(jsonPath("$.totalCount", is(19)))
                    .andExpect(jsonPath("$.numerator", is(5)))
                    .andExpect(jsonPath("$.denominator", is(19)))
                    .andExpect(jsonPath("$.ratio", is(closeTo(0.26315, 0.001))))
                    .andExpect(jsonPath("$.percentage", is(closeTo(26.315, 0.001))))
                    .andExpect(jsonPath("$.progress", is(closeTo(26.315, 0.001))))
                    .andExpect(jsonPath("$.stagnationWarning", is(true)))
                    .andExpect(jsonPath("$.stagnated", is(true)));
        }
    }

    @Test
    public void testReadinessUpdatesDynamicallyOnCompletion() throws Exception {
        // Complete one more task
        int updated = deliverableRepository.updateStatusAtomically("task-6", "PENDING", "MERGED");
        assertThat(updated).isEqualTo(1);

        // Verify that completed count is now 6, and ratio is 6/19 (~31.57%)
        mockMvc.perform(get("/api/project/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed", is(6)))
                .andExpect(jsonPath("$.total", is(19)))
                .andExpect(jsonPath("$.numerator", is(6)))
                .andExpect(jsonPath("$.denominator", is(19)))
                .andExpect(jsonPath("$.ratio", is(closeTo(0.31578, 0.001))))
                .andExpect(jsonPath("$.percentage", is(closeTo(31.578, 0.001))))
                .andExpect(jsonPath("$.stagnationWarning", is(false)))
                .andExpect(jsonPath("$.stagnated", is(false)));
    }

    @Test
    public void testAddingAndCompletingNewDeliverablesIncreasesDenominatorAndNumerator() throws Exception {
        // Create 17 new pending tasks to bring total to 36 (19 + 17)
        for (int i = 20; i <= 36; i++) {
            Deliverable newtask = new Deliverable("task-" + i, "New deliverable " + i, "PENDING");
            deliverableRepository.saveAndFlush(newtask);
        }

        // Verify denominator is now 36, completed is still 5
        mockMvc.perform(get("/api/project/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed", is(5)))
                .andExpect(jsonPath("$.total", is(36)))
                .andExpect(jsonPath("$.numerator", is(5)))
                .andExpect(jsonPath("$.denominator", is(36)))
                .andExpect(jsonPath("$.ratio", is(closeTo(0.13888, 0.001))));

        // Complete 25 more deliverables (5 initial + 25 = 30 completed)
        for (int i = 6; i <= 30; i++) {
            final int taskIdNum = i;
            int updated = deliverableRepository.updateStatusAtomically("task-" + taskIdNum, "PENDING", "COMPLETED");
            assertThat(updated).isEqualTo(1);
        }

        // Verify denominator is 36, completed is 30, ratio is 30/36 (~83.33%)
        mockMvc.perform(get("/api/project/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed", is(30)))
                .andExpect(jsonPath("$.total", is(36)))
                .andExpect(jsonPath("$.numerator", is(30)))
                .andExpect(jsonPath("$.denominator", is(36)))
                .andExpect(jsonPath("$.ratio", is(closeTo(0.83333, 0.001))));
    }

    @Test
    public void testControllerCrudOperations() throws Exception {
        // Create deliverable via POST API
        String payload = "{\"id\":\"task-test-post\",\"name\":\"Integration tests for API\",\"status\":\"PENDING\"}";
        mockMvc.perform(post("/api/deliverables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("task-test-post")))
                .andExpect(jsonPath("$.name", is("Integration tests for API")))
                .andExpect(jsonPath("$.status", is("PENDING")));

        // Update deliverable via PUT API
        String updatePayload = "{\"name\":\"Integration tests for API (Updated)\",\"status\":\"MERGED\"}";
        mockMvc.perform(put("/api/deliverables/task-test-post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("task-test-post")))
                .andExpect(jsonPath("$.name", is("Integration tests for API (Updated)")))
                .andExpect(jsonPath("$.status", is("MERGED")));

        // Verify readiness reflects the update (6 completed now, and 20 total)
        mockMvc.perform(get("/api/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed", is(6)))
                .andExpect(jsonPath("$.total", is(20)));
    }

    @SpringBootTest(properties = "eneik.records.dir=target/test_mock_records")
    @AutoConfigureMockMvc
    @Transactional
    public static class DynamicTrackingTests {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private DeliverableRepository deliverableRepository;

        @Autowired
        private EneikReadinessService eneikReadinessService;

        @Test
        public void testDynamicUpdatesAndCaching() throws Exception {
            // Ensure target/test_mock_records exists
            java.io.File dir = new java.io.File("target/test_mock_records");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            // Clean up any old files
            java.io.File[] oldFiles = dir.listFiles();
            if (oldFiles != null) {
                for (java.io.File f : oldFiles) {
                    f.delete();
                }
            }

            eneikReadinessService.resetCache();

            // Initially we have 5 completed deliverables in DB
            long initialCompleted = deliverableRepository.countCompletedDeliverables();
            assertThat(initialCompleted).isEqualTo(5);

            // Write mock verdict approving Epic index 3 (CRM optimization etc.) and 4 (Delay engine etc.)
            // Epic 3 has: task-6, task-17, task-19
            // Epic 4 has: task-10, task-11, task-12
            // Total 6 deliverables should be completed, bringing completed count to 5 + 6 = 11!
            String mockVerdictJson = "{\n" +
                    "  \"verdicts\": [\n" +
                    "    { \"sourceIndex\": 3, \"verdict\": \"approve\" },\n" +
                    "    { \"sourceIndex\": 4, \"verdict\": \"approve\" }\n" +
                    "  ]\n" +
                    "}";

            java.io.File verdictFile = new java.io.File(dir, "review-verdict-mock-test.json");
            java.nio.file.Files.writeString(verdictFile.toPath(), mockVerdictJson);

            try {
                // Call GET API and assert!
                mockMvc.perform(get("/api/project/readiness"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.completed", is(11)))
                        .andExpect(jsonPath("$.total", is(19)))
                        .andExpect(jsonPath("$.numerator", is(11)))
                        .andExpect(jsonPath("$.denominator", is(19)))
                        .andExpect(jsonPath("$.stagnationWarning", is(false)))
                        .andExpect(jsonPath("$.stagnated", is(false)));

                // Check that deliverables in the DB were updated
                long finalCompleted = deliverableRepository.countCompletedDeliverables();
                assertThat(finalCompleted).isEqualTo(11);

                // Write a different mock verdict to test that the caching prevents re-parsing unless file modification time changes
                // If we do not reset the cache or modify files to have a newer modification time, it should ignore the change.
                String mockVerdictJson2 = "{\n" +
                        "  \"verdicts\": [\n" +
                        "    { \"sourceIndex\": 0, \"verdict\": \"approve\" }\n" +
                        "  ]\n" +
                        "}";
                java.nio.file.Files.writeString(verdictFile.toPath(), mockVerdictJson2);

                // Keep same last modified time
                verdictFile.setLastModified(verdictFile.lastModified() - 1000);

                // Request again and check thatcompleted count is still 11 (because file change was ignored due to cached mtime!)
                mockMvc.perform(get("/api/project/readiness"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.completed", is(11)));

                // Now reset cache or force a newer timestamp
                eneikReadinessService.resetCache();
                verdictFile.setLastModified(System.currentTimeMillis() + 10000);

                // Request again and check that completed count is now 7 (since only Epic 0 is approved in verdict 2)
                // Wait! Since the first update already set Epic 3 and 4 deliverables to MERGED in the DB,
                // and those rows are committed, those are already MERGED.
                // Now Epic 0 has been approved, which has task-9 and task-16.
                // So now Epic 0, 3, and 4 deliverables are all MERGED, making 5 + 6 + 2 = 13!
                mockMvc.perform(get("/api/project/readiness"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.completed", is(13)));

            } finally {
                // Clean up
                verdictFile.delete();
            }
        }
    }
}
