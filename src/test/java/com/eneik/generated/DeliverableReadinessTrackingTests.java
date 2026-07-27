package com.eneik.generated;

import com.eneik.generated.model.Deliverable;
import com.eneik.generated.repository.DeliverableRepository;
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

@SpringBootTest
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
                    .andExpect(jsonPath("$.total", is(19)))
                    .andExpect(jsonPath("$.numerator", is(5)))
                    .andExpect(jsonPath("$.denominator", is(19)))
                    .andExpect(jsonPath("$.ratio", is(closeTo(0.26315, 0.001))))
                    .andExpect(jsonPath("$.percentage", is(closeTo(26.315, 0.001))));
        }
    }

    @Test
    public void testReadinessUpdatesDynamicallyOnCompletion() throws Exception {
        // Complete one more task
        Deliverable task6 = deliverableRepository.findById("task-6")
                .orElseThrow(() -> new AssertionError("Initial task-6 should exist"));
        task6.setStatus("MERGED");
        deliverableRepository.saveAndFlush(task6);

        // Verify that completed count is now 6, and ratio is 6/19 (~31.57%)
        mockMvc.perform(get("/api/project/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed", is(6)))
                .andExpect(jsonPath("$.total", is(19)))
                .andExpect(jsonPath("$.numerator", is(6)))
                .andExpect(jsonPath("$.denominator", is(19)))
                .andExpect(jsonPath("$.ratio", is(closeTo(0.31578, 0.001))))
                .andExpect(jsonPath("$.percentage", is(closeTo(31.578, 0.001))));
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
            Deliverable t = deliverableRepository.findById("task-" + taskIdNum)
                    .orElseThrow(() -> new AssertionError("Task should exist: task-" + taskIdNum));
            t.setStatus("COMPLETED");
            deliverableRepository.saveAndFlush(t);
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
}
