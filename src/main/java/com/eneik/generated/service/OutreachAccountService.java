package com.eneik.generated.service;

import com.eneik.generated.model.TGAccount;
import com.eneik.generated.repository.TGAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;

@Service
public class OutreachAccountService {

    private final TGAccountRepository tgAccountRepository;
    private final Clock clock;

    @Autowired
    public OutreachAccountService(TGAccountRepository tgAccountRepository) {
        this.tgAccountRepository = tgAccountRepository;
        this.clock = Clock.systemDefaultZone();
    }

    // Constructor with custom Clock for testing
    public OutreachAccountService(TGAccountRepository tgAccountRepository, Clock clock) {
        this.tgAccountRepository = tgAccountRepository;
        this.clock = clock;
    }

    /**
     * Filters and returns all outreach accounts that are older than 1 month relative to the current clock.
     * Performed at the database-level for high performance (Kano: Performance).
     */
    public List<TGAccount> getEligibleAccounts() {
        OffsetDateTime oneMonthAgo = OffsetDateTime.now(clock).minusMonths(1);
        return tgAccountRepository.findAllByCreatedAtBefore(oneMonthAgo);
    }

    /**
     * Calculates exponential delays for the next action of an account.
     * Uses the formula: BaseDelay * (Factor ^ ConsecutiveActions) + Jitter.
     *
     * @param consecutiveActions Number of consecutive actions/attempts
     * @param baseDelay          The baseline minimum pause between actions in seconds
     * @param factor             The exponential base multiplier
     * @param useJitter          Whether to add a randomized human-emulation jitter
     * @param random             The seedable Random source for deterministic testing
     * @return The calculated delay in seconds
     */
    public long calculateDelay(int consecutiveActions, double baseDelay, double factor, boolean useJitter, Random random) {
        double delay = baseDelay * Math.pow(factor, consecutiveActions);
        if (useJitter && random != null) {
            // Generates a human-behavior emulation jitter of 0 to 30 seconds
            double jitterValue = random.nextDouble() * 30.0;
            delay += jitterValue;
        }
        return Math.round(delay);
    }
}
