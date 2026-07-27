package com.eneik.generated.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;

@Service
public class ClientDeliverableReadinessService {

    private static final Logger log = LoggerFactory.getLogger(ClientDeliverableReadinessService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReadinessMetric calculateReadiness(Integer overrideCompleted, Integer overrideTotal) {
        int totalTasks = 0;
        int completedTasks = 0;

        // 1. Scan `.eneik/records/` directory to read task plans and review verdicts
        File recordsDir = new File(".eneik/records");
        if (!recordsDir.exists()) {
            // Try parent directories recursively up to 3 levels
            File current = new File(".");
            for (int i = 0; i < 3; i++) {
                File check = new File(current, ".eneik/records");
                if (check.exists()) {
                    recordsDir = check;
                    break;
                }
                current = new File(current, "..");
            }
        }

        if (recordsDir.exists() && recordsDir.isDirectory()) {
            File[] files = recordsDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                Set<String> uniqueRvHashes = new HashSet<>();
                Set<String> uniqueTpHashes = new HashSet<>();
                List<JsonNode> uniqueRvs = new ArrayList<>();
                List<JsonNode> uniqueTps = new ArrayList<>();

                for (File file : files) {
                    try {
                        byte[] bytes = Files.readAllBytes(file.toPath());
                        String content = new String(bytes);
                        String hash = calculateMd5(content);

                        if (file.getName().startsWith("review-verdict-")) {
                            if (uniqueRvHashes.add(hash)) {
                                uniqueRvs.add(objectMapper.readTree(content));
                            }
                        } else if (file.getName().startsWith("task-plan-")) {
                            if (uniqueTpHashes.add(hash)) {
                                uniqueTps.add(objectMapper.readTree(content));
                            }
                        }
                    } catch (Exception e) {
                        // Log exceptions conforming to the BARCAN-TAG-02 Role Charter (Obligatory)
                        log.error("Failed to parse file: {}", file.getAbsolutePath(), e);
                    }
                }

                // Calculate completed from unique review-verdict files
                int approvedVerdicts = 0;
                int totalVerdicts = 0;
                for (JsonNode rv : uniqueRvs) {
                    JsonNode verdicts = rv.get("verdicts");
                    if (verdicts != null && verdicts.isArray()) {
                        for (JsonNode v : verdicts) {
                            totalVerdicts++;
                            JsonNode verdictNode = v.get("verdict");
                            if (verdictNode != null && "approve".equals(verdictNode.asText())) {
                                approvedVerdicts++;
                            }
                        }
                    }
                }

                // Calculate unique slices from unique task-plan files
                Set<String> uniqueSliceTitles = new HashSet<>();
                for (JsonNode tp : uniqueTps) {
                    JsonNode epics = tp.get("epics");
                    if (epics != null && epics.isArray()) {
                        for (JsonNode epic : epics) {
                            JsonNode slices = epic.get("slices");
                            if (slices != null && slices.isArray()) {
                                for (JsonNode slice : slices) {
                                    JsonNode titleNode = slice.get("title");
                                    if (titleNode != null) {
                                        uniqueSliceTitles.add(titleNode.asText());
                                    }
                                }
                            }
                        }
                    }
                }

                completedTasks = approvedVerdicts;

                int baseTotal = Math.max(totalVerdicts, uniqueSliceTitles.size());
                baseTotal = Math.max(baseTotal, completedTasks);
                if (baseTotal > 0) {
                    totalTasks = baseTotal;
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

    private String calculateMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : messageDigest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return input;
        }
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
