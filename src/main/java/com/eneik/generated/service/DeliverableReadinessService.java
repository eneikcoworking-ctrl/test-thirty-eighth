package com.eneik.generated.service;

import com.eneik.generated.repository.DeliverableRepository;
import com.eneik.generated.repository.LeadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DeliverableReadinessService {

    private final DeliverableRepository deliverableRepository;
    private final LeadRepository leadRepository;

    @Autowired
    public DeliverableReadinessService(DeliverableRepository deliverableRepository, LeadRepository leadRepository) {
        this.deliverableRepository = deliverableRepository;
        this.leadRepository = leadRepository;
    }

    public Map<String, Object> getReadinessMetrics() {
        long totalLeads = leadRepository.count();
        long completed;
        long total;

        if (totalLeads == 0) {
            completed = deliverableRepository.countCompletedDeliverables();
            total = deliverableRepository.count();
        } else {
            completed = leadRepository.countCompletedLeads();
            total = totalLeads;
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
        response.put("status", "success");

        return response;
    }
}
