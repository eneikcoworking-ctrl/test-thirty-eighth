package com.eneik.generated.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class ClientDeliverableReadinessService {

    private static final Logger log = LoggerFactory.getLogger(ClientDeliverableReadinessService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReadinessMetric calculateReadiness(Integer overrideCompleted, Integer overrideTotal) {
        int totalTasks = 0;
        int completedTasks = 0;

        // 1. Scan `.eneik/records/` directory to read task plans and review verdicts
        File recordsDir = new File(".eneik/records");
        if (recordsDir.exists() && recordsDir.isDirectory()) {
            File[] files = recordsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String name = file.getName();
                    if (name.startsWith("task-plan-") && name.endsWith(".json")) {
                        totalTasks += countSlicesInTaskPlan(file);
                    } else if (name.startsWith("review-verdict-") && name.endsWith(".json")) {
                        completedTasks++;
                    }
                }
            }
        }

        // Apply overrides if provided
        if (overrideCompleted != null) {
            completedTasks = overrideCompleted;
        }
        if (overrideTotal != null) {
            totalTasks = overrideTotal;
        }

        // Safe dynamic fallback to avoid denominator capping at 19 when completed tasks advance to 30
        if (totalTasks == 0) {
            totalTasks = Math.max(19, completedTasks);
        }

        // The denominator correctly includes all relevant tasks rather than capping at 19
        double ratio = (double) completedTasks / totalTasks;

        return new ReadinessMetric(completedTasks, totalTasks, ratio);
    }

    private int countSlicesInTaskPlan(File file) {
        try {
            JsonNode root = objectMapper.readTree(file);
            JsonNode epics = root.get("epics");
            if (epics != null && epics.isArray()) {
                int count = 0;
                for (JsonNode epic : epics) {
                    JsonNode slices = epic.get("slices");
                    if (slices != null && slices.isArray()) {
                        count += slices.size();
                    }
                }
                return count;
            }
        } catch (IOException e) {
            // Log all exceptions with context conforming to the BARCAN-TAG-02 Role Charter (Obligatory)
            log.error("Failed to parse task-plan JSON from file: {}", file.getAbsolutePath(), e);
        }
        return 0;
    }

    public static class ReadinessMetric {
        private final int numerator;
        private final int denominator;
        private final double ratio;

        public ReadinessMetric(int numerator, int denominator, double ratio) {
            this.numerator = numerator;
            this.denominator = denominator;
            this.ratio = ratio;
        }

        public int getNumerator() {
            return numerator;
        }

        public int getDenominator() {
            return denominator;
        }

        public double getRatio() {
            return ratio;
        }
    }
}
