package com.eneik.generated;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TelegramAccountWarmUpWorker {

    private final TelegramAccountRepository accountRepository;
    private final TelegramAccountTrustScoreRepository trustScoreRepository;
    private final TelegramAccountWarmUpLogRepository warmUpLogRepository;
    private final DelayEngine delayEngine;
    private final Sleeper sleeper;

    @Autowired
    public TelegramAccountWarmUpWorker(
            TelegramAccountRepository accountRepository,
            TelegramAccountTrustScoreRepository trustScoreRepository,
            TelegramAccountWarmUpLogRepository warmUpLogRepository,
            DelayEngine delayEngine,
            Sleeper sleeper) {
        this.accountRepository = accountRepository;
        this.trustScoreRepository = trustScoreRepository;
        this.warmUpLogRepository = warmUpLogRepository;
        this.delayEngine = delayEngine;
        this.sleeper = sleeper;
    }

    /**
     * Finds all accounts with status "IDLE" and performs the warm-up logic on them.
     */
    public void triggerWarmUpForAllIdleAccounts() {
        List<TelegramAccount> idleAccounts = accountRepository.findByStatus("IDLE");
        for (TelegramAccount account : idleAccounts) {
            warmUpAccount(account.getId());
        }
    }

    /**
     * Performs warm-up for a single account by ID, if its status is currently "IDLE".
     *
     * IMPORTANT PERFORMANCE & SAFETY CONSIDERATIONS:
     * To prevent database connection starvation (keeping pool connections open during long sleep periods)
     * and long-lived database row-locks, the long-running pause/sleep phase must run OUTSIDE of any
     * active transaction boundary. Therefore, this method itself does NOT have a class-level or method-level
     * @Transactional.
     *
     * Instead, status transitions and persistence updates are performed in localized, short-lived, independent
     * transactions.
     */
    public boolean warmUpAccount(Long accountId) {
        // 1. Atomically transition account status from IDLE to PROCESSING_WARMUP (in a short transaction)
        boolean lockAcquired = transitionStatusToProcessing(accountId);
        if (!lockAcquired) {
            // Already being processed or not in "IDLE" state
            return false;
        }

        String action = delayEngine.getRandomAction();
        int delaySeconds = delayEngine.getRandomDelay(120, 300);

        try {
            // 2. Perform the delay OUTSIDE of any active transaction
            sleeper.sleep(delaySeconds * 1000L);

            // 3. Persist the results, log actions and revert status back to IDLE (in a short transaction)
            persistWarmUpResults(accountId, action, delaySeconds);
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            revertStatusToIdle(accountId);
            throw new RuntimeException("Warm-up execution interrupted", e);
        } catch (Exception e) {
            revertStatusToIdle(accountId);
            throw new RuntimeException("Warm-up execution failed", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean transitionStatusToProcessing(Long accountId) {
        int updatedRows = accountRepository.updateStatusAtomic(
                accountId, "IDLE", "PROCESSING_WARMUP", LocalDateTime.now());
        return updatedRows > 0;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revertStatusToIdle(Long accountId) {
        accountRepository.updateStatusAtomic(
                accountId, "PROCESSING_WARMUP", "IDLE", LocalDateTime.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistWarmUpResults(Long accountId, String action, int delaySeconds) {
        // Retrieve the latest account state
        TelegramAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        // Calculate new trust score
        double currentScore = account.getCurrentTrustScore() != null ? account.getCurrentTrustScore() : 0.0;
        double newScore = Math.min(100.0, currentScore + 1.5);
        account.setCurrentTrustScore(newScore);

        if (newScore >= 80.0) {
            account.setWarmedUp(true);
        }

        // Save new trust score history record
        TelegramAccountTrustScore trustScoreRecord = new TelegramAccountTrustScore(account, newScore);
        trustScoreRepository.save(trustScoreRecord);

        // Log action details
        String details = String.format("Executed randomized %s with delay of %ds.", action, delaySeconds);
        TelegramAccountWarmUpLog log = new TelegramAccountWarmUpLog(
                account, action, "SUCCESS", details);
        warmUpLogRepository.save(log);

        // Save updated account fields (score/warmed up flag)
        accountRepository.save(account);

        // Transition status atomically back to "IDLE"
        accountRepository.updateStatusAtomic(
                accountId, "PROCESSING_WARMUP", "IDLE", LocalDateTime.now());
    }
}
