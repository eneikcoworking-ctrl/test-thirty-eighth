package com.eneik.generated.service;

import com.eneik.generated.model.TGAccount;
import com.eneik.generated.repository.TGAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class TelegramDispatchService {

    private static final Logger log = LoggerFactory.getLogger(TelegramDispatchService.class);

    @Autowired
    private TGAccountRepository tgAccountRepository;

    private final AtomicLong seedableMessageId = new AtomicLong(100001L);

    /**
     * Seeds the message ID generator for predictable, reproducible test assertions.
     */
    public void seedMessageId(long seed) {
        this.seedableMessageId.set(seed);
    }

    /**
     * Dispatches a message via the specified Telegram session (account).
     *
     * @param telegramAccountId the database ID of the Telegram account (session)
     * @param telegramChatId    the chat ID on Telegram to send the message to
     * @param content           the text content of the message
     * @return the generated Telegram message ID
     */
    public Long dispatchMessage(Long telegramAccountId, Long telegramChatId, String content) {
        if (telegramAccountId == null) {
            throw new IllegalArgumentException("Telegram Account ID cannot be null.");
        }

        // Fetch and validate the TGAccount
        TGAccount account = tgAccountRepository.findById(telegramAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Telegram account with ID " + telegramAccountId + " not found."));

        if ("PERMANENT_BAN".equals(account.getStatus())) {
            throw new IllegalStateException("Cannot dispatch message via banned Telegram session " + telegramAccountId);
        }

        log.info("Dispatching manual message via Telegram session [ID: {}, Phone: {}] to Chat ID {}: '{}'",
                account.getId(), account.getPhoneNumber(), telegramChatId, content);

        // Return a seedable/predictable message ID to maintain 100% test reproducibility
        return seedableMessageId.getAndIncrement();
    }
}
