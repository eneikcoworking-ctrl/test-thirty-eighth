package com.eneik.generated;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class DeliverableReadinessTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetProjectReadinessEndpoints() throws Exception {
        String[] endpoints = {
                "/api/metrics/readiness",
                "/api/readiness",
                "/api/project/readiness",
                "/api/deliverables/readiness",
                "/api/deliverable-readiness",
                "/api/project-progress"
        };

        for (String endpoint : endpoints) {
            mockMvc.perform(get(endpoint))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completed", is(notNullValue())))
                    .andExpect(jsonPath("$.total", is(notNullValue())))
                    .andExpect(jsonPath("$.ratio", is(notNullValue())))
                    .andExpect(jsonPath("$.readiness", is(notNullValue())))
                    .andExpect(jsonPath("$.percentage", is(notNullValue())));
        }
    }
}
