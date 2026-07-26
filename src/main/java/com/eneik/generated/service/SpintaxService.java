package com.eneik.generated.service;

import org.springframework.stereotype.Service;
import java.util.Random;

@Service
public class SpintaxService {

    private final Random defaultRandom = new Random();

    /**
     * Parses the spintax template using a default Random generator.
     * e.g., "{Hi|Hello|Hey} there!" -> "Hello there!"
     */
    public String parse(String template) {
        return parse(template, defaultRandom);
    }

    /**
     * Parses the spintax template using a seed to guarantee reproducible results.
     */
    public String parse(String template, Long seed) {
        if (seed == null) {
            return parse(template);
        }
        return parse(template, new Random(seed));
    }

    /**
     * Parses the spintax template using the provided Random generator.
     * Supports nested spintax structures like "{Hi|Hello {there|friend}}".
     */
    public String parse(String template, Random random) {
        if (template == null) {
            return null;
        }
        if (random == null) {
            random = defaultRandom;
        }

        String result = template;
        while (true) {
            int endIdx = result.indexOf('}');
            if (endIdx == -1) {
                break;
            }
            int startIdx = result.lastIndexOf('{', endIdx);
            if (startIdx == -1) {
                // If there's an unmatched close brace, just stop parsing or ignore it
                break;
            }

            // Extract the options within the curly braces
            String block = result.substring(startIdx + 1, endIdx);
            String[] options = block.split("\\|", -1);

            // Choose one option at random
            String chosen = options[random.nextInt(options.length)];

            // Replace the template block with the chosen option
            result = result.substring(0, startIdx) + chosen + result.substring(endIdx + 1);
        }
        return result;
    }
}
