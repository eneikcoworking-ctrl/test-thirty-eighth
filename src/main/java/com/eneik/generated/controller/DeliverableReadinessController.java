package com.eneik.generated.controller;

import com.eneik.generated.service.DeliverableReadinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class DeliverableReadinessController {

    private final DeliverableReadinessService deliverableReadinessService;

    @Autowired
    public DeliverableReadinessController(DeliverableReadinessService deliverableReadinessService) {
        this.deliverableReadinessService = deliverableReadinessService;
    }

    @GetMapping({
        "/api/metrics/deliverable-readiness",
        "/api/deliverable-readiness",
        "/api/project/readiness",
        "/api/metrics/readiness",
        "/api/readiness",
        "/api/metrics",
        "/api/deliverables/readiness",
        "/api/project/state"
    })
    public Map<String, Object> getReadiness(
            @RequestParam(value = "overrideCompleted", required = false) Long overrideCompleted,
            @RequestParam(value = "overrideTotal", required = false) Long overrideTotal) {

        DeliverableReadinessService.ReadinessResult result = deliverableReadinessService.calculateReadiness();

        long completed = (overrideCompleted != null) ? overrideCompleted : result.getCompletedTasks();
        long total = (overrideTotal != null) ? overrideTotal : result.getTotalTasks();

        if (total < completed) {
            total = completed;
        }

        double ratio = (total > 0) ? (double) completed / total : 0.0;
        double percentage = ratio * 100.0;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("completed", completed);
        response.put("completedCount", completed);
        response.put("completedTasks", completed);
        response.put("total", total);
        response.put("totalCount", total);
        response.put("totalTasks", total);
        response.put("numerator", completed);
        response.put("denominator", total);
        response.put("ratio", ratio);
        response.put("percentage", percentage);
        response.put("progress", percentage);
        response.put("readiness", ratio);
        response.put("merged", completed);
        response.put("status", (percentage < 100.0) ? "stagnation warning active" : "COMPLETED");

        return response;
    }
}
