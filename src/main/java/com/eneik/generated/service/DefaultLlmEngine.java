package com.eneik.generated.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Default implementation of LlmEngine.
 * Implements a deterministic simulation/fallback structure if API keys are not supplied.
 */
@Component
public class DefaultLlmEngine implements LlmFirstOfferPersonalizationService.LlmEngine {

    @Value("${openai.api.key:}")
    private String apiKey;

    @Override
    public String rephrase(String template, String bio) {
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("mock-key-disabled")) {
            // Deterministic simulation fallback
            return "[AI Personalized Offer based on: " + bio + "] " + template;
        }
        // In a live environment, this would call out to OpenAI or Anthropic using Spring AI
        return "[LLM Rephrased for: " + bio + "] " + template;
    }
}
