package com.eneik.generated;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class MetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetMetricsReadiness() throws Exception {
        // Query the /api/metrics/readiness endpoint
        mockMvc.perform(get("/api/metrics/readiness")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").exists())
                .andExpect(jsonPath("$.completedTasks").exists())
                .andExpect(jsonPath("$.total").exists())
                .andExpect(jsonPath("$.totalTasks").exists())
                .andExpect(jsonPath("$.readiness").exists())
                .andExpect(jsonPath("$.ratio").exists());
    }

    @Test
    public void testGetMetricsAlias() throws Exception {
        // Query the /api/metrics endpoint alias
        mockMvc.perform(get("/api/metrics")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").exists())
                .andExpect(jsonPath("$.total").exists())
                .andExpect(jsonPath("$.readiness").exists());
    }

    @Test
    public void testGetDeliverablesReadiness() throws Exception {
        // Query the /api/deliverables/readiness endpoint
        mockMvc.perform(get("/api/deliverables/readiness")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").exists())
                .andExpect(jsonPath("$.total").exists())
                .andExpect(jsonPath("$.ratio").exists());
    }
}
