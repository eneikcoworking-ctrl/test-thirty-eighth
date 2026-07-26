package com.eneik.generated.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MockTelegramClient implements TelegramClient {
    private static final Logger log = LoggerFactory.getLogger(MockTelegramClient.class);

    @Override
    public void sendTypingStatus(Long accountId, Long chatId) {
        log.info("Sending typing status for accountId: {}, chatId: {}", accountId, chatId);
    }

    @Override
    public void sendMessage(Long accountId, Long chatId, String text) {
        log.info("Sending message for accountId: {}, chatId: {}, text: {}", accountId, chatId, text);
    }
}
