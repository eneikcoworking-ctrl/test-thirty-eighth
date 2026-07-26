package com.eneik.generated.service;

import com.eneik.generated.model.Lead;
import com.eneik.generated.model.TargetList;
import com.eneik.generated.repository.LeadRepository;
import com.eneik.generated.repository.TargetListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LeadIngestionService {

    private final TargetListRepository targetListRepository;
    private final LeadRepository leadRepository;

    @Autowired
    public LeadIngestionService(TargetListRepository targetListRepository, LeadRepository leadRepository) {
        this.targetListRepository = targetListRepository;
        this.leadRepository = leadRepository;
    }

    /**
     * Parses the CSV string and saves the leads under the given target list.
     * If targetListId is provided, the leads are appended to that list.
     * Otherwise, a new TargetList is created.
     */
    @Transactional
    public List<Lead> ingestCsv(String csvContent, Long targetListId, String newTargetListName) {
        if (csvContent == null || csvContent.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 1. Resolve or Create TargetList
        TargetList targetList = null;
        if (targetListId != null) {
            Optional<TargetList> existing = targetListRepository.findById(targetListId);
            if (existing.isPresent()) {
                targetList = existing.get();
            }
        }

        if (targetList == null) {
            String name = newTargetListName;
            if (name == null || name.trim().isEmpty()) {
                name = "Imported List - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            targetList = new TargetList(name, "Automatically created from CSV Lead Ingestion.");
            targetList = targetListRepository.save(targetList);
        }

        // 2. Parse lines
        String[] lines = csvContent.split("\\r?\\n");
        if (lines.length == 0) {
            return new ArrayList<>();
        }

        // 3. Determine headers
        int usernameIdx = -1;
        int phoneIdx = -1;
        int firstNameIdx = -1;
        int lastNameIdx = -1;
        int metadataIdx = -1;

        List<String> firstLineValues = parseCsvLine(lines[0]);
        boolean hasHeaders = false;

        // Ensure we only treat the first line as headers if it contains username/phone keywords
        boolean hasUsernameOrPhoneHeader = false;
        for (String val : firstLineValues) {
            String cleanVal = val.trim().toLowerCase();
            if (cleanVal.equals("username") || cleanVal.equals("@username") || cleanVal.equals("user") ||
                cleanVal.equals("phone_number") || cleanVal.equals("phone") || cleanVal.equals("number") || cleanVal.equals("tel")) {
                hasUsernameOrPhoneHeader = true;
                break;
            }
        }

        if (hasUsernameOrPhoneHeader) {
            for (int i = 0; i < firstLineValues.size(); i++) {
                String val = firstLineValues.get(i).trim().toLowerCase();
                if (val.equals("username") || val.equals("@username") || val.equals("user")) {
                    usernameIdx = i;
                    hasHeaders = true;
                } else if (val.equals("phone_number") || val.equals("phone") || val.equals("number") || val.equals("tel")) {
                    phoneIdx = i;
                    hasHeaders = true;
                } else if (val.equals("first_name") || val.equals("firstname") || val.equals("first")) {
                    firstNameIdx = i;
                    hasHeaders = true;
                } else if (val.equals("last_name") || val.equals("lastname") || val.equals("last")) {
                    lastNameIdx = i;
                    hasHeaders = true;
                } else if (val.equals("metadata") || val.equals("meta")) {
                    metadataIdx = i;
                    hasHeaders = true;
                }
            }
        }

        // Fallback default column ordering if no headers detected
        if (!hasHeaders) {
            usernameIdx = 0;
            phoneIdx = 1;
            firstNameIdx = 2;
            lastNameIdx = 3;
            metadataIdx = 4;
        }

        int startLine = hasHeaders ? 1 : 0;
        List<Lead> savedLeads = new ArrayList<>();

        for (int i = startLine; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().isEmpty()) {
                continue;
            }

            List<String> rowValues = parseCsvLine(line);

            String username = getValueAt(rowValues, usernameIdx);
            String phoneNumber = getValueAt(rowValues, phoneIdx);
            String firstName = getValueAt(rowValues, firstNameIdx);
            String lastName = getValueAt(rowValues, lastNameIdx);
            String metadata = getValueAt(rowValues, metadataIdx);

            // Skip empty rows
            if (username == null && phoneNumber == null && firstName == null && lastName == null) {
                continue;
            }

            Lead lead = new Lead(targetList, username, phoneNumber, firstName, lastName, metadata);
            lead.setStatus("PENDING");
            Lead saved = leadRepository.save(lead);
            savedLeads.add(saved);
        }

        return savedLeads;
    }

    /**
     * Splits a CSV line into its fields, supporting quoted fields and escaped quotes.
     */
    public static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        if (line == null) {
            return values;
        }

        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        // Escaped quote "" inside a quoted field
                        sb.append('"');
                        i++; // skip next quote
                    } else {
                        // End of quoted field
                        inQuotes = false;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                if (c == '"' && sb.length() == 0) {
                    // Start of a quoted field
                    inQuotes = true;
                } else if (c == ',') {
                    values.add(sb.toString().trim());
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
            }
        }
        values.add(sb.toString().trim());
        return values;
    }

    private String getValueAt(List<String> row, int idx) {
        if (idx >= 0 && idx < row.size()) {
            String val = row.get(idx);
            return val.isEmpty() ? null : val;
        }
        return null;
    }
}
