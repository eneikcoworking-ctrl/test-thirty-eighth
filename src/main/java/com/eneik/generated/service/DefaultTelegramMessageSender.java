package com.eneik.generated.service;

import com.eneik.generated.model.Lead;
import com.eneik.generated.model.TGAccount;
import org.springframework.stereotype.Service;

@Service
public class DefaultTelegramMessageSender implements TelegramMessageSender {
    @Override
    public void sendMessage(TGAccount account, Lead lead, String message) throws TelegramFloodException {
        // Default no-op or console logging mock implementation
        System.out.println("DefaultTelegramMessageSender: Simulating send message from "
                + account.getUsername() + " to " + lead.getUsername() + ": " + message);
    }
}
