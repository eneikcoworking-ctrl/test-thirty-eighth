package com.eneik.generated;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class WarmUpStateIntegrationTest {

    @Autowired
    private TelegramAccountRepository accountRepository;

    @Autowired
    private TelegramAccountTrustScoreRepository trustScoreRepository;

    @Autowired
    private TelegramAccountWarmUpLogRepository warmUpLogRepository;

    @Test
    public void testContextLoadsAndFlywayApplies() {
        // Just checking that Spring context loads and database migration runs successfully
        assertNotNull(accountRepository);
        assertNotNull(trustScoreRepository);
        assertNotNull(warmUpLogRepository);
    }

    @Test
    public void testCreateAndRetrieveAccountWithLogsAndScores() {
        // Create telegram account
        TelegramAccount account = new TelegramAccount(
                "+1234567890",
                "test_user",
                "ACTIVE",
                "socks5://127.0.0.1:1080",
                85.5,
                true
        );
        TelegramAccount savedAccount = accountRepository.save(account);
        assertNotNull(savedAccount.getId());

        // Add trust score record
        TelegramAccountTrustScore trustScore = new TelegramAccountTrustScore(savedAccount, 85.5);
        trustScoreRepository.save(trustScore);

        // Add warm-up log
        TelegramAccountWarmUpLog log = new TelegramAccountWarmUpLog(
                savedAccount,
                "WARM_UP_STEP",
                "SUCCESS",
                "Completed randomized 120s delay after sending typing status"
        );
        warmUpLogRepository.save(log);

        // Retrieve and assert relationships
        List<TelegramAccountTrustScore> scores = trustScoreRepository.findByAccountId(savedAccount.getId());
        assertEquals(1, scores.size());
        assertEquals(85.5, scores.get(0).getScore());
        assertEquals(savedAccount.getId(), scores.get(0).getAccount().getId());

        List<TelegramAccountWarmUpLog> logs = warmUpLogRepository.findByAccountId(savedAccount.getId());
        assertEquals(1, logs.size());
        assertEquals("WARM_UP_STEP", logs.get(0).getAction());
        assertEquals("SUCCESS", logs.get(0).getStatus());
        assertEquals("Completed randomized 120s delay after sending typing status", logs.get(0).getDetails());
    }

    @Test
    public void testAccountReadinessFiltering() {
        // Create test accounts with varied state and trust scores

        // A: Warmed-up, active, high trust score (Ready)
        TelegramAccount accountA = new TelegramAccount("+1111", "acc_a", "ACTIVE", null, 85.0, true);

        // B: Active, high trust score, but NOT warmed-up (Not ready)
        TelegramAccount accountB = new TelegramAccount("+2222", "acc_b", "ACTIVE", null, 90.0, false);

        // C: Warmed-up, active, but low trust score (Not ready)
        TelegramAccount accountC = new TelegramAccount("+3333", "acc_c", "ACTIVE", null, 40.0, true);

        // D: Warmed-up, banned, high trust score (Not ready)
        TelegramAccount accountD = new TelegramAccount("+4444", "acc_d", "PERMANENT_BAN", null, 95.0, true);

        accountRepository.save(accountA);
        accountRepository.save(accountB);
        accountRepository.save(accountC);
        accountRepository.save(accountD);

        // Filter: Active, warmed-up, and trust score >= 70.0
        List<TelegramAccount> readyAccounts = accountRepository
                .findByIsWarmedUpTrueAndStatusAndCurrentTrustScoreGreaterThanEqual("ACTIVE", 70.0);

        assertEquals(1, readyAccounts.size());
        assertEquals("acc_a", readyAccounts.get(0).getUsername());

        // Filter by warm-up status and trust score threshold only (including non-active/banned ones)
        List<TelegramAccount> allWarmedUpAboveFifty = accountRepository
                .findByIsWarmedUpAndCurrentTrustScoreGreaterThanEqual(true, 50.0);

        assertEquals(2, allWarmedUpAboveFifty.size());
        assertTrue(allWarmedUpAboveFifty.stream().anyMatch(a -> a.getUsername().equals("acc_a")));
        assertTrue(allWarmedUpAboveFifty.stream().anyMatch(a -> a.getUsername().equals("acc_d")));
    }

    @Test
    public void testAtomicallyGuardedStatusTransition() {
        TelegramAccount account = new TelegramAccount("+5555", "acc_e", "ACTIVE", null, 75.0, true);
        TelegramAccount savedAccount = accountRepository.save(account);
        Long id = savedAccount.getId();

        // 1. Success transition: ACTIVE -> TEMPORARY_SPAM_BLOCK
        int rowsUpdated = accountRepository.updateStatusAtomic(id, "ACTIVE", "TEMPORARY_SPAM_BLOCK", LocalDateTime.now());
        assertEquals(1, rowsUpdated);

        // Verify status changed in db
        TelegramAccount updated = accountRepository.findById(id).orElseThrow();
        assertEquals("TEMPORARY_SPAM_BLOCK", updated.getStatus());

        // 2. Failure transition (stale state): try ACTIVE -> PERMANENT_BAN
        int rowsUpdatedStale = accountRepository.updateStatusAtomic(id, "ACTIVE", "PERMANENT_BAN", LocalDateTime.now());
        assertEquals(0, rowsUpdatedStale);

        // Verify status remains TEMPORARY_SPAM_BLOCK
        TelegramAccount unchanged = accountRepository.findById(id).orElseThrow();
        assertEquals("TEMPORARY_SPAM_BLOCK", unchanged.getStatus());
    }
}
