package com.eneik.generated.service;

import com.eneik.generated.model.Dialog;
import com.eneik.generated.model.Message;
import com.eneik.generated.repository.DialogRepository;
import com.eneik.generated.repository.MessageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AiDialogueService {

    private final DialogRepository dialogRepository;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;
    private AiReplyGenerator aiReplyGenerator;

    @Autowired
    public AiDialogueService(DialogRepository dialogRepository, MessageRepository messageRepository) {
        this.dialogRepository = dialogRepository;
        this.messageRepository = messageRepository;
        this.objectMapper = new ObjectMapper();
        // Default simple reply generator that echoes or returns a standard response
        this.aiReplyGenerator = (context, aiState) -> "Thank you for your message. How can I assist you further?";
    }

    public void setAiReplyGenerator(AiReplyGenerator aiReplyGenerator) {
        this.aiReplyGenerator = aiReplyGenerator;
    }

    @Transactional
    public DialogProcessResult processIncomingMessage(String dialogId, String incomingText, Long telegramMessageId) {
        // 1. Fetch Dialog
        Dialog dialog = dialogRepository.findById(dialogId)
                .orElseThrow(() -> new IllegalArgumentException("Dialog not found with id: " + dialogId));

        if (!"ACTIVE".equals(dialog.getStatus())) {
            return new DialogProcessResult(false, false, null, dialog.getStatus(), "Dialog is not in ACTIVE state.");
        }

        // 2. Save the incoming LEAD message
        Message leadMsg = new Message();
        leadMsg.setId(UUID.randomUUID().toString());
        leadMsg.setDialogId(dialogId);
        leadMsg.setSenderType("LEAD");
        leadMsg.setContent(incomingText != null ? incomingText : "");
        leadMsg.setTelegramMessageId(telegramMessageId);
        messageRepository.save(leadMsg);

        // Fetch context messages (all messages in the dialogue)
        List<Message> context = messageRepository.findByDialogId(dialogId);

        // 3. Check for 8 back-and-forth messages boundary limit
        if (context.size() >= 8) {
            // "If the session reaches 8 back-and-forth messages, stop with a concrete blocker instead of looping."
            dialogRepository.updateStatusAtomically(dialogId, "ACTIVE", "BLOCKED");
            return new DialogProcessResult(false, true, null, "BLOCKED", "Session blocked: dialogue message limit (8 messages) reached.");
        }

        // 4. Generate AI reply
        String aiReply = aiReplyGenerator.generateReply(context, dialog.getAiState());

        // 5. Gather all stop triggers (default + custom from aiState)
        List<String> stopTriggers = new ArrayList<>();
        // Default keywords implying goal reached, human handoff requested or stop intent
        stopTriggers.add("human");
        stopTriggers.add("representative");
        stopTriggers.add("sales rep");
        stopTriggers.add("stop");
        stopTriggers.add("goal reached");
        stopTriggers.add("buy");
        stopTriggers.add("pricing");
        stopTriggers.add("interested");

        // Custom triggers from aiState
        if (dialog.getAiState() != null && !dialog.getAiState().trim().isEmpty()) {
            try {
                JsonNode root = objectMapper.readTree(dialog.getAiState());
                if (root.has("stop_triggers")) {
                    JsonNode triggersNode = root.get("stop_triggers");
                    if (triggersNode.isArray()) {
                        for (JsonNode node : triggersNode) {
                            stopTriggers.add(node.asText().toLowerCase());
                        }
                    }
                }
            } catch (Exception e) {
                // Silently skip if aiState is not valid JSON
            }
        }

        // 6. Check if any stop trigger is hit in incomingText or generated reply (case-insensitive)
        boolean stopTriggerHit = false;
        String lowercaseIncoming = incomingText != null ? incomingText.toLowerCase() : "";
        String lowercaseReply = aiReply != null ? aiReply.toLowerCase() : "";

        for (String trigger : stopTriggers) {
            if ((!lowercaseIncoming.isEmpty() && lowercaseIncoming.contains(trigger)) ||
                (!lowercaseReply.isEmpty() && lowercaseReply.contains(trigger))) {
                stopTriggerHit = true;
                break;
            }
        }

        if (stopTriggerHit) {
            // Stop generation and mark the dialogue for human handoff using atomically-guarded database update
            dialogRepository.updateStatusAtomically(dialogId, "ACTIVE", "HUMAN_HANDOFF");
            return new DialogProcessResult(true, false, null, "HUMAN_HANDOFF", "Intent flag hit. Dialog marked for human handoff.");
        }

        // 7. Save AI reply if no triggers hit
        Message aiMsg = new Message();
        aiMsg.setId(UUID.randomUUID().toString());
        aiMsg.setDialogId(dialogId);
        aiMsg.setSenderType("AI");
        aiMsg.setContent(aiReply != null ? aiReply : "");
        aiMsg.setTelegramMessageId(telegramMessageId != null ? telegramMessageId + 1 : null);
        messageRepository.save(aiMsg);

        return new DialogProcessResult(false, false, aiReply, "ACTIVE", "Success");
    }

    public static class DialogProcessResult {
        private final boolean stopTriggerHit;
        private final boolean limitReached;
        private final String aiReply;
        private final String dialogStatus;
        private final String message;

        public DialogProcessResult(boolean stopTriggerHit, boolean limitReached, String aiReply, String dialogStatus, String message) {
            this.stopTriggerHit = stopTriggerHit;
            this.limitReached = limitReached;
            this.aiReply = aiReply;
            this.dialogStatus = dialogStatus;
            this.message = message;
        }

        public boolean isStopTriggerHit() {
            return stopTriggerHit;
        }

        public boolean isLimitReached() {
            return limitReached;
        }

        public String getAiReply() {
            return aiReply;
        }

        public String getDialogStatus() {
            return dialogStatus;
        }

        public String getMessage() {
            return message;
        }
    }
}
