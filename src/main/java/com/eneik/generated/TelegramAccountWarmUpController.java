package com.eneik.generated;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/warmup")
public class TelegramAccountWarmUpController {

    private final TelegramAccountWarmUpWorker warmUpWorker;

    @Autowired
    public TelegramAccountWarmUpController(TelegramAccountWarmUpWorker warmUpWorker) {
        this.warmUpWorker = warmUpWorker;
    }

    /**
     * Trigger warm-up process for all idle accounts.
     */
    @PostMapping("/trigger")
    public ResponseEntity<String> triggerWarmUpForAll() {
        warmUpWorker.triggerWarmUpForAllIdleAccounts();
        return ResponseEntity.ok("Warm-up trigger initiated for all idle accounts");
    }

    /**
     * Trigger warm-up process for a specific account.
     */
    @PostMapping("/trigger/{accountId}")
    public ResponseEntity<String> triggerWarmUpForAccount(@PathVariable Long accountId) {
        boolean success = warmUpWorker.warmUpAccount(accountId);
        if (success) {
            return ResponseEntity.ok("Warm-up completed successfully for account: " + accountId);
        } else {
            return ResponseEntity.badRequest().body("Account is not idle or cannot be processed right now: " + accountId);
        }
    }
}
