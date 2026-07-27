package com.eneik.generated.service;

import com.eneik.generated.repository.DeliverableRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.*;

@Service
public class EneikReadinessService {

    private final DeliverableRepository deliverableRepository;
    private final String recordsDirPath;

    private long lastMaxMtime = -1;

    // Mapping of epic indices to database deliverable IDs
    private static final Map<Integer, List<String>> EPIC_TO_DELIVERABLES = Map.of(
        0, List.of("task-9", "task-16"),
        1, List.of("task-7", "task-14", "task-18"),
        2, List.of("task-8", "task-13", "task-15"),
        3, List.of("task-6", "task-17", "task-19"),
        4, List.of("task-10", "task-11", "task-12")
    );

    @Autowired
    public EneikReadinessService(
            DeliverableRepository deliverableRepository,
            @Value("${eneik.records.dir:.eneik/records}") String recordsDirPath) {
        this.deliverableRepository = deliverableRepository;
        this.recordsDirPath = recordsDirPath;
    }

    /**
     * Parse approved review verdicts from Eneik records and update corresponding
     * database deliverables. Employs caching based on file modification times
     * to eliminate performance bottlenecks.
     */
    @Transactional
    public synchronized void updateDeliverablesFromRecords() {
        try {
            if (recordsDirPath == null || recordsDirPath.trim().isEmpty()) {
                return;
            }

            File recordsDir = new File(recordsDirPath);
            if (!recordsDir.exists() || !recordsDir.isDirectory()) {
                return;
            }

            File[] files = recordsDir.listFiles((dir, name) -> name.startsWith("review-verdict-") && name.endsWith(".json"));
            if (files == null || files.length == 0) {
                return;
            }

            // Find maximum modification time to check if files have changed
            long maxMtime = 0;
            for (File file : files) {
                maxMtime = Math.max(maxMtime, file.lastModified());
            }

            // If no file has been modified since the last check, skip parsing entirely
            if (maxMtime <= lastMaxMtime) {
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            Set<Integer> approvedEpics = new HashSet<>();

            for (File file : files) {
                try {
                    JsonNode root = mapper.readTree(file);
                    JsonNode verdictsNode = root.get("verdicts");
                    if (verdictsNode != null && verdictsNode.isArray()) {
                        for (JsonNode verdictNode : verdictsNode) {
                            JsonNode sourceIndexNode = verdictNode.get("sourceIndex");
                            JsonNode verdictValNode = verdictNode.get("verdict");
                            if (sourceIndexNode != null && verdictValNode != null) {
                                int sourceIndex = sourceIndexNode.asInt();
                                String verdict = verdictValNode.asText();
                                if ("approve".equalsIgnoreCase(verdict)) {
                                    approvedEpics.add(sourceIndex);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Obligatory: Log all exceptions with context
                    System.err.println("Error reading review verdict file " + file.getName() + ": " + e.getMessage());
                }
            }

            // Perform atomic database updates for all approved deliverables
            for (int epicIndex : approvedEpics) {
                List<String> taskIds = EPIC_TO_DELIVERABLES.get(epicIndex);
                if (taskIds != null) {
                    for (String taskId : taskIds) {
                        deliverableRepository.updateStatusAtomically(taskId, "PENDING", "MERGED");
                    }
                }
            }

            // Cache the maximum modification time
            lastMaxMtime = maxMtime;

        } catch (Exception e) {
            System.err.println("Error in EneikReadinessService: " + e.getMessage());
        }
    }

    /**
     * Resets the cache of last modified timestamps. Primarily used for testing purposes.
     */
    public synchronized void resetCache() {
        this.lastMaxMtime = -1;
    }
}
