package com.eneik.generated;

import com.eneik.generated.service.ClientDeliverableReadinessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ClientDeliverableReadinessServiceTest {

    @Autowired
    private ClientDeliverableReadinessService readinessService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testReadinessCalculationWithCappingFixed() {
        // Given an active project pipeline with more than 19 tasks (e.g. 30 completed tasks out of 63 total tasks)
        // When the readiness metric is calculated
        ClientDeliverableReadinessService.ReadinessMetric metric = readinessService.calculateReadiness(30, 63);

        // Then the denominator correctly includes all relevant tasks rather than capping at 19
        assertEquals(63, metric.getDenominator());
        assertEquals(30, metric.getNumerator());

        // And the ratio is calculated proportionally (30 / 63 = ~0.476)
        double expectedRatio = 30.0 / 63.0;
        assertEquals(expectedRatio, metric.getRatio(), 0.001);
    }

    @Test
    public void testReadinessCalculationWithStandardDefaultFallback() {
        // When calling calculateReadiness without parameters, it should dynamically parse `.eneik/records` directory
        ClientDeliverableReadinessService.ReadinessMetric metric = readinessService.calculateReadiness(null, null);

        // Ensure the calculations run without crashing and denominator is non-zero
        assertTrue(metric.getDenominator() > 0);
        assertTrue(metric.getNumerator() >= 0);
        assertEquals((double) metric.getNumerator() / metric.getDenominator(), metric.getRatio(), 0.001);
    }

    @Test
    public void testDashboardEndpointWithDynamicMetrics() throws Exception {
        // When the metric is retrieved via the REST endpoint (overriding done=30, total=63)
        // Then the ratio updates proportionally, and the denominator correctly includes all relevant tasks (63) rather than capping at 19
        mockMvc.perform(get("/api/projects/test-project/dashboard")
                        .param("done", "30")
                        .param("total", "63")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonAndPathMatches());
    }

    private org.springframework.test.web.servlet.ResultMatcher jsonAndPathMatches() {
        return mvcResult -> {
            String content = mvcResult.getResponse().getContentAsString();
            // Use explicit double parsing to satisfy the floating-point comparison boundary constraint:
            // "Any floating-point value that crosses a JSON serialization boundary and is compared in a test
            // must use an explicit, type-safe comparison (parse to a known numeric type, or an approximate/tolerance comparison)"
            com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(content);
            double ratio = root.get("readiness").get("ratio").asDouble();
            int denominator = root.get("readiness").get("denominator").asInt();
            int numerator = root.get("readiness").get("numerator").asInt();

            assertEquals(63, denominator);
            assertEquals(30, numerator);
            assertEquals(30.0 / 63.0, ratio, 0.001);
        };
    }
}
