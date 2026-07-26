package com.eneik.generated.service;

public interface TelegramClient {
    void sendTypingStatus(Long accountId, Long chatId);
    void sendMessage(Long accountId, Long chatId, String text);
}
