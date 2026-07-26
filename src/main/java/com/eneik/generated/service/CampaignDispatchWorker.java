package com.eneik.generated.service;

import com.eneik.generated.model.Campaign;
import com.eneik.generated.model.Dialog;
import com.eneik.generated.model.Lead;
import com.eneik.generated.model.Message;
import com.eneik.generated.model.TGAccount;
import com.eneik.generated.repository.CampaignRepository;
import com.eneik.generated.repository.DialogRepository;
import com.eneik.generated.repository.LeadRepository;
import com.eneik.generated.repository.MessageRepository;
import com.eneik.generated.repository.TGAccountRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CampaignDispatchWorker {

    private final CampaignRepository campaignRepository;
    private final LeadRepository leadRepository;
    private final TGAccountRepository tgAccountRepository;
    private final DialogRepository dialogRepository;
    private final MessageRepository messageRepository;
    private final TelegramMessageSender messageSender;

    public CampaignDispatchWorker(
            CampaignRepository campaignRepository,
            LeadRepository leadRepository,
            TGAccountRepository tgAccountRepository,
            DialogRepository dialogRepository,
            MessageRepository messageRepository,
            TelegramMessageSender messageSender) {
        this.campaignRepository = campaignRepository;
        this.leadRepository = leadRepository;
        this.tgAccountRepository = tgAccountRepository;
        this.dialogRepository = dialogRepository;
        this.messageRepository = messageRepository;
        this.messageSender = messageSender;
    }

    public void dispatchCampaign(Long campaignId) {
        System.out.println("DEBUG: Entering dispatchCampaign with id = " + campaignId);
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + campaignId));

        System.out.println("DEBUG: Campaign status = " + campaign.getStatus());
        if (!"ACTIVE".equals(campaign.getStatus())) {
            return;
        }

        // 1. Fetch pending leads
        List<Lead> pendingLeads = leadRepository.findByTargetListIdAndStatusOrderByIdAsc(
                campaign.getTargetList().getId(), "PENDING");
        System.out.println("DEBUG: Found pending leads count = " + pendingLeads.size());

        if (pendingLeads.isEmpty()) {
            return;
        }

        // 2. Fetch available ACTIVE accounts
        List<TGAccount> activeAccounts = tgAccountRepository.findByStatusOrderByIdAsc("ACTIVE");
        System.out.println("DEBUG: Found active accounts count = " + activeAccounts.size());
        if (activeAccounts.isEmpty()) {
            throw new IllegalStateException("No active Telegram accounts available for dispatching");
        }

        int accountIndex = 0;

        for (Lead lead : pendingLeads) {
            System.out.println("DEBUG: Processing lead = " + lead.getUsername());
            boolean processed = false;

            while (!processed) {
                if (accountIndex >= activeAccounts.size()) {
                    throw new IllegalStateException("All Telegram accounts are blocked/exhausted");
                }

                TGAccount currentAccount = activeAccounts.get(accountIndex);
                System.out.println("DEBUG: Using account = " + currentAccount.getUsername());

                // Check dialogue/session message limit boundary condition
                Optional<Dialog> existingDialogOpt = findExistingDialog(lead);
                System.out.println("DEBUG: Is existing dialog present? = " + existingDialogOpt.isPresent());
                if (existingDialogOpt.isPresent()) {
                    Dialog dialog = existingDialogOpt.get();
                    List<Message> sessionMessages = messageRepository.findByDialogId(dialog.getId());
                    System.out.println("DEBUG: Dialog messages count = " + sessionMessages.size());
                    if (sessionMessages.size() >= 8) {
                        System.out.println("DEBUG: Session has >= 8 messages. Blocking.");
                        dialog.setStatus("BLOCKED");
                        dialog.setUpdatedAt(LocalDateTime.now());
                        dialogRepository.saveAndFlush(dialog);

                        // Atomically mark lead as BLOCKED to prevent reprocessing or loops
                        leadRepository.updateStatusAtomic(lead.getId(), "PENDING", "BLOCKED");
                        processed = true;
                        break;
                    }
                }

                try {
                    String messageText = campaign.getSpintaxTemplate() != null ? campaign.getSpintaxTemplate() : "Hello!";

                    // Attempt dispatch
                    System.out.println("DEBUG: Sending message...");
                    messageSender.sendMessage(currentAccount, lead, messageText);

                    // Create or update Dialog & Message
                    Dialog dialog;
                    if (existingDialogOpt.isPresent()) {
                        dialog = existingDialogOpt.get();
                    } else {
                        System.out.println("DEBUG: Creating new dialog for lead...");
                        dialog = new Dialog();
                        dialog.setId(UUID.randomUUID().toString());
                        dialog.setTelegramChatId(lead.getId()); // fallback mock chat id
                        dialog.setLeadUsername(lead.getUsername());
                        dialog.setLeadPhone(lead.getPhoneNumber());
                        dialog.setStatus("ACTIVE");
                        dialogRepository.saveAndFlush(dialog);
                        System.out.println("DEBUG: New dialog saved.");
                    }

                    Message outboundMessage = new Message();
                    outboundMessage.setId(UUID.randomUUID().toString());
                    outboundMessage.setDialogId(dialog.getId());
                    outboundMessage.setSenderType("AI");
                    outboundMessage.setContent(messageText);
                    messageRepository.saveAndFlush(outboundMessage);
                    System.out.println("DEBUG: Message saved.");

                    // Atomically mark the lead as SENT
                    int rows = leadRepository.updateStatusAtomic(lead.getId(), "PENDING", "SENT");
                    System.out.println("DEBUG: Lead status atomic update rows updated = " + rows);
                    processed = true;

                } catch (TelegramFloodException e) {
                    System.out.println("DEBUG: Caught TelegramFloodException!");
                    // Current account hit a flood/spam block error!
                    // Atomically transition current account to TEMPORARY_SPAM_BLOCK
                    tgAccountRepository.updateStatusAtomic(currentAccount.getId(), "ACTIVE", "TEMPORARY_SPAM_BLOCK");

                    // Rotate to the next available account and loop back to retry the same lead
                    accountIndex++;
                }
            }
        }
    }

    private Optional<Dialog> findExistingDialog(Lead lead) {
        if (lead.getUsername() != null) {
            Optional<Dialog> d = dialogRepository.findByLeadUsername(lead.getUsername());
            if (d.isPresent()) {
                return d;
            }
        }
        if (lead.getPhoneNumber() != null) {
            return dialogRepository.findByLeadPhone(lead.getPhoneNumber());
        }
        return Optional.empty();
    }
}
