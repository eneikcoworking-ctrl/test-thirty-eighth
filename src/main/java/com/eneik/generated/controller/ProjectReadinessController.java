package com.eneik.generated.controller;

import com.eneik.generated.service.DeliverableReadinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping
public class ProjectReadinessController {

    private final DeliverableReadinessService readinessService;

    @Autowired
    public ProjectReadinessController(DeliverableReadinessService readinessService) {
        this.readinessService = readinessService;
    }

    @GetMapping({
            "/api/metrics/readiness",
            "/api/readiness",
            "/api/project/readiness",
            "/api/deliverables/readiness",
            "/api/deliverable-readiness",
            "/api/project-progress"
    })
    public ResponseEntity<Map<String, Object>> getProjectReadiness() {
        Map<String, Object> readiness = readinessService.calculateReadiness();
        return ResponseEntity.ok(readiness);
    }
}
