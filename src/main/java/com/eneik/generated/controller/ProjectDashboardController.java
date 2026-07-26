package com.eneik.generated.controller;

import com.eneik.generated.service.ClientDeliverableReadinessService;
import com.eneik.generated.service.ClientDeliverableReadinessService.ReadinessMetric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProjectDashboardController {

    private final ClientDeliverableReadinessService readinessService;

    @Autowired
    public ProjectDashboardController(ClientDeliverableReadinessService readinessService) {
        this.readinessService = readinessService;
    }

    /**
     * Endpoint to retrieve the project pipeline status and deliverable readiness metrics.
     * Supports both optional projectId path variable or query parameter, and manual overrides.
     */
    @GetMapping({"/projects/{projectId}/dashboard", "/projects/dashboard", "/dashboard"})
    public ResponseEntity<Map<String, Object>> getProjectDashboard(
            @PathVariable(value = "projectId", required = false) String projectId,
            @RequestParam(value = "done", required = false) Integer done,
            @RequestParam(value = "total", required = false) Integer total) {

        ReadinessMetric metric = readinessService.calculateReadiness(done, total);

        Map<String, Object> response = new LinkedHashMap<>();

        // 1. Mock standard queue structure expected by the platform observer
        Map<String, Object> queue = new LinkedHashMap<>();
        queue.put("totalQueued", 0);
        queue.put("byTag", new ArrayList<>());
        response.put("queue", queue);

        // 2. Mock standard pipeline structure expected by the platform observer, utilizing our dynamic metric
        Map<String, Object> pipeline = new LinkedHashMap<>();
        pipeline.put("queued", 0);
        pipeline.put("claimed", 0);
        pipeline.put("in_progress", 0);
        pipeline.put("review", 0);
        pipeline.put("done", metric.getNumerator());
        pipeline.put("failed", 0);
        response.put("pipeline", pipeline);

        response.put("openWishlistCount", 0);

        // 3. Structured readiness information
        Map<String, Object> readiness = new LinkedHashMap<>();
        readiness.put("ratio", metric.getRatio());
        readiness.put("numerator", metric.getNumerator());
        readiness.put("denominator", metric.getDenominator());
        response.put("readiness", readiness);

        if (projectId != null) {
            response.put("projectId", projectId);
        }

        return ResponseEntity.ok(response);
    }
}
