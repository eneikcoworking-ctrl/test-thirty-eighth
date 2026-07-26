package com.eneik.generated.controller;

import com.eneik.generated.service.SpintaxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/spintax")
public class SpintaxController {

    private final SpintaxService spintaxService;

    @Autowired
    public SpintaxController(SpintaxService spintaxService) {
        this.spintaxService = spintaxService;
    }

    /**
     * Parses the spintax template and returns the randomized or seed-deterministic string.
     */
    @PostMapping("/parse")
    public ResponseEntity<SpintaxResponse> parseSpintax(@RequestBody SpintaxRequest request) {
        if (request == null || request.getTemplate() == null) {
            return ResponseEntity.badRequest().build();
        }

        String parsedText = spintaxService.parse(request.getTemplate(), request.getSeed());
        return ResponseEntity.ok(new SpintaxResponse(parsedText));
    }

    public static class SpintaxRequest {
        private String template;
        private Long seed;

        public SpintaxRequest() {}

        public SpintaxRequest(String template, Long seed) {
            this.template = template;
            this.seed = seed;
        }

        public String getTemplate() {
            return template;
        }

        public void setTemplate(String template) {
            this.template = template;
        }

        public Long getSeed() {
            return seed;
        }

        public void setSeed(Long seed) {
            this.seed = seed;
        }
    }

    public static class SpintaxResponse {
        private String parsedText;

        public SpintaxResponse() {}

        public SpintaxResponse(String parsedText) {
            this.parsedText = parsedText;
        }

        public String getParsedText() {
            return parsedText;
        }

        public void setParsedText(String parsedText) {
            this.parsedText = parsedText;
        }
    }
}
