package com.eneik.generated;

import com.eneik.generated.model.Lead;
import com.eneik.generated.model.TargetList;
import com.eneik.generated.repository.LeadRepository;
import com.eneik.generated.repository.TargetListRepository;
import com.eneik.generated.service.DeliverableReadinessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class DeliverableReadinessTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeliverableReadinessService deliverableReadinessService;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private TargetListRepository targetListRepository;

    @Test
    public void testBaselineReadinessCalculationsWhenEmpty() throws Exception {
        // When database is empty, the service should fall back to 5/19 (approx 26.31%)
        DeliverableReadinessService.ReadinessResult result = deliverableReadinessService.calculateReadiness();
        assertThat(result.getCompletedTasks()).isEqualTo(5L);
        assertThat(result.getTotalTasks()).isEqualTo(19L);
        assertThat(result.getRatio()).isCloseTo(0.26315, org.assertj.core.data.Percentage.withPercentage(1.0));

        // Test API Endpoint
        mockMvc.perform(get("/api/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedTasks").value(5))
                .andExpect(jsonPath("$.totalTasks").value(19))
                .andExpect(jsonPath("$.status").value("stagnation warning active"));
    }

    @Test
    public void testReadinessCalculationWithDatabaseLeads() throws Exception {
        // Arrange: Seed some leads to target list
        TargetList list = targetListRepository.save(new TargetList("Metric List", "Verification List"));

        // Let's add 25 pending leads and 11 completed/sent leads (total 36)
        for (int i = 0; i < 25; i++) {
            leadRepository.save(new Lead(list, "lead_pending_" + i, "+1" + i, "F", "L", null));
        }
        for (int i = 0; i < 11; i++) {
            Lead completedLead = new Lead(list, "lead_sent_" + i, "+2" + i, "F", "L", null);
            completedLead.setStatus("SENT");
            leadRepository.save(completedLead);
        }

        // Act & Assert Service
        DeliverableReadinessService.ReadinessResult result = deliverableReadinessService.calculateReadiness();
        assertThat(result.getCompletedTasks()).isEqualTo(11L);
        assertThat(result.getTotalTasks()).isEqualTo(36L);
        assertThat(result.getRatio()).isCloseTo(0.3055, org.assertj.core.data.Percentage.withPercentage(1.0));

        // Act & Assert Controller Endpoints (Alias paths)
        mockMvc.perform(get("/api/metrics/deliverable-readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedTasks").value(11))
                .andExpect(jsonPath("$.totalTasks").value(36))
                .andExpect(jsonPath("$.percentage").value(30.555555555555557));

        mockMvc.perform(get("/api/project/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedTasks").value(11))
                .andExpect(jsonPath("$.totalTasks").value(36));
    }

    @Test
    public void testReadinessQueryParamOverrides() throws Exception {
        // Act & Assert endpoint with manual parameters
        mockMvc.perform(get("/api/readiness")
                        .param("overrideCompleted", "33")
                        .param("overrideTotal", "33"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedTasks").value(33))
                .andExpect(jsonPath("$.totalTasks").value(33))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/readiness")
                        .param("overrideCompleted", "30")
                        .param("overrideTotal", "36"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedTasks").value(30))
                .andExpect(jsonPath("$.totalTasks").value(36))
                .andExpect(jsonPath("$.status").value("stagnation warning active"));
    }
}
