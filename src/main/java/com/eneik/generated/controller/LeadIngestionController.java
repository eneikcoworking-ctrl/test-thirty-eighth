package com.eneik.generated.controller;

import com.eneik.generated.model.Lead;
import com.eneik.generated.service.LeadIngestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/leads")
public class LeadIngestionController {

    private final LeadIngestionService leadIngestionService;

    @Autowired
    public LeadIngestionController(LeadIngestionService leadIngestionService) {
        this.leadIngestionService = leadIngestionService;
    }

    /**
     * Import leads from an uploaded CSV multipart file.
     */
    @PostMapping("/import")
    public ResponseEntity<List<Lead>> importLeads(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "targetListId", required = false) Long targetListId,
            @RequestParam(value = "targetListName", required = false) String targetListName) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            String csvContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            List<Lead> leads = leadIngestionService.ingestCsv(csvContent, targetListId, targetListName);
            return ResponseEntity.ok(leads);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Import leads from a raw CSV text body.
     */
    @PostMapping("/import/text")
    public ResponseEntity<List<Lead>> importLeadsFromText(
            @RequestBody String csvContent,
            @RequestParam(value = "targetListId", required = false) Long targetListId,
            @RequestParam(value = "targetListName", required = false) String targetListName) {

        List<Lead> leads = leadIngestionService.ingestCsv(csvContent, targetListId, targetListName);
        return ResponseEntity.ok(leads);
    }
}
