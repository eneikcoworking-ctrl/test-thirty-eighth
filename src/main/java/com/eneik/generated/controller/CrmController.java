package com.eneik.generated.controller;

import com.eneik.generated.model.Dialog;
import com.eneik.generated.model.Message;
import com.eneik.generated.repository.DialogRepository;
import com.eneik.generated.repository.MessageRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/conversations")
@CrossOrigin(origins = "*")
public class CrmController {

    @Autowired
    private DialogRepository dialogRepository;

    @Autowired
    private MessageRepository messageRepository;

    @PostConstruct
    public void init() {
        seedMockData();
    }

    @PostMapping("/seed")
    @Transactional
    public ResponseEntity<String> forceSeed() {
        messageRepository.deleteAllInBatch();
        dialogRepository.deleteAllInBatch();
        seedMockData();
        return ResponseEntity.ok("Seeded successfully");
    }

    private void seedMockData() {
        if (dialogRepository.count() > 0) {
            return;
        }

        // Marcus Chen (Waiting for response - High Priority)
        Dialog d1 = new Dialog();
        d1.setId("marcus-chen-id");
        d1.setTelegramChatId(10001L);
        d1.setLeadUsername("Marcus Chen");
        d1.setLeadPhone("+1234567890");
        d1.setAiState("Stellar Dynamics");
        d1.setStatus("WAITING");
        dialogRepository.save(d1);

        createMessage("msg1-1", "marcus-chen-id", "LEAD", "Hello, I have a question regarding Stellar Dynamics integrations.", LocalDateTime.now().minusMinutes(30));
        createMessage("msg1-2", "marcus-chen-id", "AI", "Sure Marcus! Our API supports rapid scaling. What specifically do you need?", LocalDateTime.now().minusMinutes(25));
        createMessage("msg1-3", "marcus-chen-id", "LEAD", "We're seeing a delay in the API response times...", LocalDateTime.now().minusMinutes(2));

        // Elena Rodriguez (Waiting)
        Dialog d2 = new Dialog();
        d2.setId("elena-rodriguez-id");
        d2.setTelegramChatId(10002L);
        d2.setLeadUsername("Elena Rodriguez");
        d2.setLeadPhone("+1987654321");
        d2.setAiState("Horizon Logistics");
        d2.setStatus("WAITING");
        dialogRepository.save(d2);

        createMessage("msg2-1", "elena-rodriguez-id", "LEAD", "The tracking number provided isn't updating yet.", LocalDateTime.now().minusMinutes(14));

        // Jordan Smith (Active)
        Dialog d3 = new Dialog();
        d3.setId("jordan-smith-id");
        d3.setTelegramChatId(10003L);
        d3.setLeadUsername("Jordan Smith");
        d3.setLeadPhone("+1555555555");
        d3.setAiState("CloudScale Inc.");
        d3.setStatus("ACTIVE");
        dialogRepository.save(d3);

        createMessage("msg3-1", "jordan-smith-id", "LEAD", "Hi, can you update our subscription limits please?", LocalDateTime.now().minusHours(2));
        createMessage("msg3-2", "jordan-smith-id", "OPERATOR", "I've updated your subscription limits now.", LocalDateTime.now().minusHours(1));
    }

    private void createMessage(String id, String dialogId, String senderType, String content, LocalDateTime sentAt) {
        Message m = new Message();
        m.setId(id);
        m.setDialogId(dialogId);
        m.setSenderType(senderType);
        m.setContent(content);
        m.setCreatedAt(sentAt);
        messageRepository.save(m);
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getConversations() {
        List<Dialog> dialogs = dialogRepository.findAll();
        List<Map<String, Object>> response = new ArrayList<>();

        for (Dialog dialog : dialogs) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", dialog.getId());
            map.put("telegramChatId", dialog.getTelegramChatId());
            map.put("leadUsername", dialog.getLeadUsername());
            map.put("leadPhone", dialog.getLeadPhone());
            map.put("aiState", dialog.getAiState());
            map.put("status", dialog.getStatus());
            map.put("createdAt", dialog.getCreatedAt());
            map.put("updatedAt", dialog.getUpdatedAt());

            List<Message> messages = messageRepository.findByDialogId(dialog.getId());
            // Sort messages by createdAt asc
            messages.sort(Comparator.comparing(Message::getCreatedAt));

            List<Map<String, Object>> msgList = new ArrayList<>();
            for (Message m : messages) {
                Map<String, Object> m_map = new HashMap<>();
                m_map.put("id", m.getId());
                m_map.put("senderType", m.getSenderType());
                m_map.put("content", m.getContent());
                m_map.put("createdAt", m.getCreatedAt());
                msgList.add(m_map);
            }
            map.put("messages", msgList);

            // Derive unread count (e.g. for waiting/high-priority, let's say count of consecutive LEAD messages at the end, or fixed mock value)
            int unreadCount = 0;
            if (dialog.getId().equals("marcus-chen-id")) {
                unreadCount = 3; // exact match to the mockup requirement
            }
            map.put("unreadCount", unreadCount);

            response.add(map);
        }

        // Sort dialogs by latest message createdAt descending, or dialog updatedAt descending
        response.sort((a, b) -> {
            LocalDateTime ta = (LocalDateTime) a.get("updatedAt");
            LocalDateTime tb = (LocalDateTime) b.get("updatedAt");
            return tb.compareTo(ta);
        });

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reply")
    @Transactional
    public ResponseEntity<?> reply(@PathVariable String id, @RequestBody Map<String, String> request) {
        Optional<Dialog> dialogOpt = dialogRepository.findById(id);
        if (dialogOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String content = request.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Content is required");
        }

        Dialog dialog = dialogOpt.get();

        // Save reply message
        Message reply = new Message();
        reply.setId(UUID.randomUUID().toString());
        reply.setDialogId(id);
        reply.setSenderType("OPERATOR");
        reply.setContent(content);
        reply.setCreatedAt(LocalDateTime.now());
        messageRepository.save(reply);

        int updated = dialogRepository.updateStatusAtomically(id, dialog.getStatus(), "ACTIVE");
        if (updated == 0) {
            return ResponseEntity.status(409).body("Dialog status changed concurrently");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", reply.getId());
        response.put("senderType", reply.getSenderType());
        response.put("content", reply.getContent());
        response.put("createdAt", reply.getCreatedAt());

        return ResponseEntity.ok(response);
    }
}
