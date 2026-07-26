package com.eneik.generated.controller;

import com.eneik.generated.model.TGAccount;
import com.eneik.generated.service.OutreachAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/outreach-accounts")
public class OutreachAccountController {

    private final OutreachAccountService outreachAccountService;

    @Autowired
    public OutreachAccountController(OutreachAccountService outreachAccountService) {
        this.outreachAccountService = outreachAccountService;
    }

    /**
     * Endpoint to retrieve eligible accounts (older than 1 month) and calculate their exponential delays.
     */
    @GetMapping("/eligible")
    public List<OutreachAccountResponse> getEligibleAccounts(
            @RequestParam(value = "baseDelay", defaultValue = "120.0") double baseDelay,
            @RequestParam(value = "factor", defaultValue = "1.5") double factor,
            @RequestParam(value = "useJitter", defaultValue = "true") boolean useJitter,
            @RequestParam(value = "seed", required = false) Long seed) {

        List<TGAccount> eligible = outreachAccountService.getEligibleAccounts();

        // Instantiate Random source (reproducible with fixed seed if requested)
        final Random random = (seed != null) ? new Random(seed) : new Random();

        return eligible.stream().map(account -> {
            long calculatedDelay = outreachAccountService.calculateDelay(
                    account.getConsecutiveActions(),
                    baseDelay,
                    factor,
                    useJitter,
                    random
            );

            return new OutreachAccountResponse(
                    account.getId(),
                    account.getPhoneNumber(),
                    account.getUsername(),
                    account.getStatus(),
                    account.getCreatedAt(),
                    account.getConsecutiveActions(),
                    calculatedDelay
            );
        }).collect(Collectors.toList());
    }

    /**
     * Response DTO for eligible outreach accounts.
     */
    public static class OutreachAccountResponse {
        private Long id;
        private String phoneNumber;
        private String username;
        private String status;
        private OffsetDateTime createdAt;
        private Integer consecutiveActions;
        private long calculatedDelaySeconds;

        public OutreachAccountResponse() {
        }

        public OutreachAccountResponse(Long id, String phoneNumber, String username, String status,
                                       OffsetDateTime createdAt, Integer consecutiveActions, long calculatedDelaySeconds) {
            this.id = id;
            this.phoneNumber = phoneNumber;
            this.username = username;
            this.status = status;
            this.createdAt = createdAt;
            this.consecutiveActions = consecutiveActions;
            this.calculatedDelaySeconds = calculatedDelaySeconds;
        }

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public OffsetDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(OffsetDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public Integer getConsecutiveActions() {
            return consecutiveActions;
        }

        public void setConsecutiveActions(Integer consecutiveActions) {
            this.consecutiveActions = consecutiveActions;
        }

        public long getCalculatedDelaySeconds() {
            return calculatedDelaySeconds;
        }

        public void setCalculatedDelaySeconds(long calculatedDelaySeconds) {
            this.calculatedDelaySeconds = calculatedDelaySeconds;
        }
    }
}
