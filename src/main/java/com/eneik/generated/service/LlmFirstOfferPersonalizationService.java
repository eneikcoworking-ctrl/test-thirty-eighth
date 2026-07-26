package com.eneik.generated.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for LLM-powered First-Offer Personalization (FEAT-CMP-03).
 * Rephrases an outreach template using a lead's public bio/metadata context.
 */
@Service
public class LlmFirstOfferPersonalizationService {

    private final ObjectMapper objectMapper;
    private final LlmEngine llmEngine;

    public LlmFirstOfferPersonalizationService(ObjectMapper objectMapper, LlmEngine llmEngine) {
        this.objectMapper = objectMapper;
        this.llmEngine = llmEngine;
    }

    /**
     * Engine interface representing the LLM prompt execution.
     * Can be implemented using Spring AI, a custom HTTP client, or stubbed for deterministic testing.
     */
    public interface LlmEngine {
        String rephrase(String template, String bio);
    }

    /**
     * Extracts public bio context from a lead's metadata JSON or plain text.
     */
    public String extractBio(String metadata) {
        if (metadata == null || metadata.trim().isEmpty()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(metadata);
            if (root.has("bio")) {
                return root.get("bio").asText();
            }
            if (root.has("public_bio")) {
                return root.get("public_bio").asText();
            }
            if (root.has("about")) {
                return root.get("about").asText();
            }
        } catch (Exception e) {
            // Not a valid JSON, return the raw metadata as bio
            return metadata.trim();
        }
        return null;
    }

    /**
     * Generates a personalized outbound message for a lead.
     * Resolves Spintax/fallbacks if LLM is unavailable or bio is empty.
     */
    public String generatePersonalizedMessage(String spintaxTemplate, String metadata) {
        String bio = extractBio(metadata);
        if (bio == null || bio.trim().isEmpty()) {
            return resolveSpintax(spintaxTemplate);
        }

        try {
            String rephrased = llmEngine.rephrase(spintaxTemplate, bio);
            if (rephrased != null && !rephrased.trim().isEmpty()) {
                return rephrased.trim();
            }
        } catch (Exception e) {
            // Graceful fallback to resolved Spintax template on failure
        }

        return resolveSpintax(spintaxTemplate);
    }

    /**
     * Utility method to resolve spintax (e.g. "{Hi|Hello} there!") deterministically.
     * Selects the first option to avoid non-reproducible randomness.
     */
    public String resolveSpintax(String template) {
        if (template == null) {
            return "";
        }
        Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String optionsStr = matcher.group(1);
            String[] options = optionsStr.split("\\|");
            String selected = options.length > 0 ? options[0] : "";
            matcher.appendReplacement(sb, selected);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
