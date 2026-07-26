package com.eneik.generated.service;

import com.eneik.generated.model.Lead;
import com.eneik.generated.model.TGAccount;

public interface TelegramMessageSender {
    void sendMessage(TGAccount account, Lead lead, String message) throws TelegramFloodException;
}
