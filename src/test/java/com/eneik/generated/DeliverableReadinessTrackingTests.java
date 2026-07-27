package com.eneik.generated;

import com.eneik.generated.model.Deliverable;
import com.eneik.generated.model.Lead;
import com.eneik.generated.model.TargetList;
import com.eneik.generated.repository.DeliverableRepository;
import com.eneik.generated.repository.LeadRepository;
import com.eneik.generated.repository.TargetListRepository;
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

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private TargetListRepository targetListRepository;

    @Test
    public void testReadinessTransitionsToLeadsAndUpdatesDynamically() throws Exception {
        // Initially, leads table is empty, so it falls back to 5/19 deliverables
        mockMvc.perform(get("/api/project/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed", is(5)))
                .andExpect(jsonPath("$.total", is(19)));

        // Create a TargetList and insert 33 completed/done leads and some pending leads
        TargetList list = new TargetList("Outreach List", "Description");
        targetListRepository.saveAndFlush(list);

        // 33 completed leads (e.g. status "SENT" or "COMPLETED")
        for (int i = 1; i <= 33; i++) {
            Lead lead = new Lead(list, "user" + i, "+123" + i, "First", "Last", "{}");
            lead.setStatus("SENT");
            leadRepository.save(lead);
        }

        // 3 pending leads
        for (int i = 34; i <= 36; i++) {
            Lead lead = new Lead(list, "user" + i, "+123" + i, "First", "Last", "{}");
            lead.setStatus("PENDING");
            leadRepository.save(lead);
        }
        leadRepository.flush();

        // Now leads is NOT empty, so readiness should track completed leads (33 completed, 36 total)
        mockMvc.perform(get("/api/project/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed", is(33)))
                .andExpect(jsonPath("$.total", is(36)))
                .andExpect(jsonPath("$.numerator", is(33)))
                .andExpect(jsonPath("$.denominator", is(36)))
                .andExpect(jsonPath("$.ratio", is(closeTo(33.0 / 36.0, 0.001))));

        // Given a new task/lead is marked as done, the score updates dynamically
        // Add a new lead with "PENDING"
        Lead newLead = new Lead(list, "user37", "+12337", "First", "Last", "{}");
        newLead.setStatus("PENDING");
        leadRepository.saveAndFlush(newLead);

        // Total increases to 37, completed still 33
        mockMvc.perform(get("/api/project/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed", is(33)))
                .andExpect(jsonPath("$.total", is(37)));

        // Now mark it as done (e.g., status is "SENT" which is not "PENDING")
        newLead.setStatus("SENT");
        leadRepository.saveAndFlush(newLead);

        // Total is 37, completed is 34
        mockMvc.perform(get("/api/project/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed", is(34)))
                .andExpect(jsonPath("$.total", is(37)))
                .andExpect(jsonPath("$.ratio", is(closeTo(34.0 / 37.0, 0.001))));
    }

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
                    .andExpect(jsonPath("$.progress", is(closeTo(26.315, 0.001))));
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
}
