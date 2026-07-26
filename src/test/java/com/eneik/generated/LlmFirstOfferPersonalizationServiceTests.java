package com.eneik.generated;

import com.eneik.generated.service.LlmFirstOfferPersonalizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class LlmFirstOfferPersonalizationServiceTests {

    @Autowired
    private LlmFirstOfferPersonalizationService personalizationService;

    @Test
    public void testExtractBio_fromValidJsonBio() {
        String metadata = "{\"bio\": \"Crypto builder & Solidity expert\", \"github\": \"sol-dev\"}";
        String bio = personalizationService.extractBio(metadata);
        assertThat(bio).isEqualTo("Crypto builder & Solidity expert");
    }

    @Test
    public void testExtractBio_fromValidJsonPublicBio() {
        String metadata = "{\"public_bio\": \"AI Enthusiast | Svelte Developer\"}";
        String bio = personalizationService.extractBio(metadata);
        assertThat(bio).isEqualTo("AI Enthusiast | Svelte Developer");
    }

    @Test
    public void testExtractBio_fromPlainText() {
        String metadata = "Just a friendly developer looking for Web3 projects.";
        String bio = personalizationService.extractBio(metadata);
        assertThat(bio).isEqualTo("Just a friendly developer looking for Web3 projects.");
    }

    @Test
    public void testGeneratePersonalizedMessage_withLlmRephrased() {
        String template = "Hey {there|builder}, check out our new DeFi tool!";
        String metadata = "{\"bio\": \"Solidity Engineer\"}";

        // Using default LlmEngine with empty/mock key, which provides a deterministic personalized response
        String message = personalizationService.generatePersonalizedMessage(template, metadata);

        assertThat(message).contains("Solidity Engineer");
        assertThat(message).contains("Hey {there|builder}, check out our new DeFi tool!");
    }

    @Test
    public void testGeneratePersonalizedMessage_withFallbackWhenLlmFails() {
        // We can define a local failure LlmEngine
        LlmFirstOfferPersonalizationService customService = new LlmFirstOfferPersonalizationService(
            new com.fasterxml.jackson.databind.ObjectMapper(),
            (template, bio) -> { throw new RuntimeException("LLM Down"); }
        );

        String template = "Hey {there|builder}, check out our new DeFi tool!";
        String metadata = "{\"bio\": \"Solidity Engineer\"}";

        String message = customService.generatePersonalizedMessage(template, metadata);

        // Fallback resolves Spintax selecting the first option "there"
        assertThat(message).isEqualTo("Hey there, check out our new DeFi tool!");
    }

    @Test
    public void testGeneratePersonalizedMessage_withFallbackWhenBioEmpty() {
        String template = "{Hi|Hello} user, try our bot!";
        String metadata = "{}";

        String message = personalizationService.generatePersonalizedMessage(template, metadata);

        // Fallback resolves Spintax selecting the first option "Hi"
        assertThat(message).isEqualTo("Hi user, try our bot!");
    }
}
