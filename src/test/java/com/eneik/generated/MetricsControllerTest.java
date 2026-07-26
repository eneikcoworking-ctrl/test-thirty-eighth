package com.eneik.generated;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class MetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetMetricsReadiness() throws Exception {
        mockMvc.perform(get("/api/metrics/readiness")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.completedCount").exists())
                .andExpect(jsonPath("$.totalCount").exists())
                .andExpect(jsonPath("$.ratio").exists())
                .andExpect(jsonPath("$.percentage").exists())
                .andExpect(jsonPath("$.progress").exists());
    }

    @Test
    public void testGetMetricsAliases() throws Exception {
        // Test aliases
        String[] aliases = {"/api/metrics", "/api/project/readiness", "/api/deliverables/readiness", "/api/project/metrics"};
        for (String alias : aliases) {
            mockMvc.perform(get(alias)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completedCount").exists());
        }
    }
}
