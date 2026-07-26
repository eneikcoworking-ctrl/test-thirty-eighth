package com.eneik.generated.service;

import com.eneik.generated.exception.AccountNotActiveException;
import com.eneik.generated.exception.DailyLimitExceededException;
import com.eneik.generated.model.TGAccount;
import com.eneik.generated.model.TelegramDispatchLog;
import com.eneik.generated.repository.TGAccountRepository;
import com.eneik.generated.repository.TelegramDispatchLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Random;

@Service
public class TelegramInteractionService {

    private final TGAccountRepository tgAccountRepository;
    private final TelegramDispatchLogRepository telegramDispatchLogRepository;
    private final TelegramClient telegramClient;
    private final PauseEnforcer pauseEnforcer;
    private Random random = new Random();

    @Autowired
    public TelegramInteractionService(
            TGAccountRepository tgAccountRepository,
            TelegramDispatchLogRepository telegramDispatchLogRepository,
            TelegramClient telegramClient,
            PauseEnforcer pauseEnforcer) {
        this.tgAccountRepository = tgAccountRepository;
        this.telegramDispatchLogRepository = telegramDispatchLogRepository;
        this.telegramClient = telegramClient;
        this.pauseEnforcer = pauseEnforcer;
    }

    // Setter to allow overriding the Random instance during testing
    public void setRandom(Random random) {
        this.random = random;
    }

    public void dispatchMessage(Long accountId, Long chatId, String recipientUsername, String recipientPhone, String text) throws InterruptedException {
        // 1. Fetch account (runs in its own short-lived transaction)
        TGAccount tgAccount = tgAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + accountId));

        // 2. Verify account is ACTIVE
        if (!"ACTIVE".equalsIgnoreCase(tgAccount.getStatus())) {
            throw new AccountNotActiveException("Account is not active. Status: " + tgAccount.getStatus());
        }

        // 3. Enforce daily limit check (runs in its own short-lived transaction)
        OffsetDateTime twentyFourHoursAgo = OffsetDateTime.now().minusHours(24);
        long sentCount = telegramDispatchLogRepository.countByAccountIdAndSentAtAfter(accountId, twentyFourHoursAgo);
        if (sentCount >= tgAccount.getDailyLimit()) {
            throw new DailyLimitExceededException(
                    "Daily message limit reached for account " + tgAccount.getPhoneNumber() +
                    ". Limit: " + tgAccount.getDailyLimit() + ", Sent in last 24h: " + sentCount
            );
        }

        // 4. Send typing status prior to sending message (external network call, outside transaction)
        telegramClient.sendTypingStatus(accountId, chatId);

        // 5. Enforce randomized pause (120-300 seconds) (outside transaction - releases DB connection!)
        int pauseSeconds = 120 + random.nextInt(181); // range [120, 300]
        pauseEnforcer.pause(pauseSeconds);

        // 6. Send message (external network call, outside transaction)
        telegramClient.sendMessage(accountId, chatId, text);

        // 7. Log dispatch (runs in its own short-lived transaction)
        TelegramDispatchLog log = new TelegramDispatchLog(tgAccount, recipientUsername, recipientPhone);
        telegramDispatchLogRepository.save(log);
    }
}
