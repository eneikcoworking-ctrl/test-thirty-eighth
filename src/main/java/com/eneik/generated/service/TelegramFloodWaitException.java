package com.eneik.generated.service;

public class TelegramFloodWaitException extends TelegramFloodException {
    private final int retryAfterSeconds;

    public TelegramFloodWaitException(int retryAfterSeconds, String message) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
