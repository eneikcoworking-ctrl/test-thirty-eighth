package com.eneik.generated.controller;

import com.eneik.generated.model.Deliverable;
import com.eneik.generated.repository.DeliverableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping
public class DeliverableController {

    private final DeliverableRepository deliverableRepository;

    @Autowired
    public DeliverableController(DeliverableRepository deliverableRepository) {
        this.deliverableRepository = deliverableRepository;
    }

    /**
     * Get readiness status / health metrics. Maps to multiple common endpoints.
     */
    @GetMapping({
        "/api/deliverables/readiness",
        "/api/project/readiness",
        "/api/readiness",
        "/api/metrics/readiness",
        "/api/project/state"
    })
    public ResponseEntity<Map<String, Object>> getReadinessStatus() {
        long completed = deliverableRepository.countCompletedDeliverables();
        long total = deliverableRepository.count();

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
        response.put("status", "success");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/deliverables")
    public List<Deliverable> getAllDeliverables() {
        return deliverableRepository.findAll();
    }

    @GetMapping("/api/deliverables/{id}")
    public ResponseEntity<Deliverable> getDeliverableById(@PathVariable String id) {
        return deliverableRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/deliverables")
    @Transactional
    public ResponseEntity<Deliverable> createDeliverable(@RequestBody Deliverable deliverable) {
        if (deliverable.getId() == null || deliverable.getId().trim().isEmpty()) {
            deliverable.setId(UUID.randomUUID().toString());
        }

        Optional<Deliverable> existing = deliverableRepository.findById(deliverable.getId());
        if (existing.isPresent()) {
            Deliverable current = existing.get();
            if (deliverable.getName() != null) {
                current.setName(deliverable.getName());
            }
            if (deliverable.getStatus() != null) {
                current.setStatus(deliverable.getStatus());
            }
            Deliverable saved = deliverableRepository.saveAndFlush(current);
            return ResponseEntity.ok(saved);
        }

        if (deliverable.getStatus() == null) {
            deliverable.setStatus("PENDING");
        }
        Deliverable saved = deliverableRepository.saveAndFlush(deliverable);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/api/deliverables/{id}")
    @Transactional
    public ResponseEntity<Deliverable> updateDeliverable(
            @PathVariable String id,
            @RequestBody Deliverable request) {
        Deliverable deliverable = deliverableRepository.findById(id).orElse(null);

        if (deliverable == null) {
            // Upsert: Create the deliverable dynamically if it does not exist
            deliverable = new Deliverable();
            deliverable.setId(id);
            deliverable.setName(request.getName() != null ? request.getName() : "Deliverable " + id);
            deliverable.setStatus(request.getStatus() != null ? request.getStatus() : "PENDING");
            Deliverable saved = deliverableRepository.saveAndFlush(deliverable);
            return ResponseEntity.ok(saved);
        }

        if (request.getName() != null) {
            deliverableRepository.updateNameById(id, request.getName());
        }
        if (request.getStatus() != null) {
            int updated = deliverableRepository.updateStatusAtomically(id, deliverable.getStatus(), request.getStatus());
            if (updated == 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
        }

        Deliverable updated = deliverableRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Deliverable not found after update: " + id));
        return ResponseEntity.ok(updated);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoSuchElementException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
}
