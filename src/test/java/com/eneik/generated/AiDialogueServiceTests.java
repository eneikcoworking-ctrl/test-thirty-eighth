package com.eneik.generated;

import com.eneik.generated.model.Dialog;
import com.eneik.generated.model.Message;
import com.eneik.generated.repository.DialogRepository;
import com.eneik.generated.repository.MessageRepository;
import com.eneik.generated.service.AiDialogueService;
import com.eneik.generated.service.AiDialogueService.DialogProcessResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AiDialogueServiceTests {

    @Autowired
    private DialogRepository dialogRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private AiDialogueService aiDialogueService;

    private String dialogId;

    @BeforeEach
    public void setUp() {
        dialogId = UUID.randomUUID().toString();
        Dialog dialog = new Dialog();
        dialog.setId(dialogId);
        dialog.setTelegramChatId(999888777L);
        dialog.setLeadUsername("test_user");
        dialog.setStatus("ACTIVE");
        dialog.setAiState("{\"current_turn\": 1, \"persona\": \"sales_agent\"}");
        dialogRepository.save(dialog);
    }

    @Test
    public void testProcessMessage_NoStopTriggerHits() {
        // Arrange
        aiDialogueService.setAiReplyGenerator((context, aiState) -> "Yes, we can absolutely assist you with your requirements!");

        // Act
        DialogProcessResult result = aiDialogueService.processIncomingMessage(dialogId, "Hello, can you help me?", 1L);

        // Assert
        assertNotNull(result);
        assertFalse(result.isStopTriggerHit());
        assertFalse(result.isLimitReached());
        assertEquals("ACTIVE", result.getDialogStatus());
        assertEquals("Yes, we can absolutely assist you with your requirements!", result.getAiReply());

        // Verify Dialog status in DB
        Dialog updatedDialog = dialogRepository.findById(dialogId).orElseThrow();
        assertEquals("ACTIVE", updatedDialog.getStatus());

        // Verify 2 messages exist (1 LEAD, 1 AI)
        List<Message> messages = messageRepository.findByDialogId(dialogId);
        assertEquals(2, messages.size());

        Message m1 = messages.stream().filter(m -> m.getSenderType().equals("LEAD")).findFirst().orElseThrow();
        assertEquals("Hello, can you help me?", m1.getContent());

        Message m2 = messages.stream().filter(m -> m.getSenderType().equals("AI")).findFirst().orElseThrow();
        assertEquals("Yes, we can absolutely assist you with your requirements!", m2.getContent());
    }

    @Test
    public void testProcessMessage_DefaultStopTriggerHit_InIncomingText() {
        // Arrange
        aiDialogueService.setAiReplyGenerator((context, aiState) -> "I'm generating a standard response.");

        // Act - incoming text contains "sales rep" (case-insensitive)
        DialogProcessResult result = aiDialogueService.processIncomingMessage(dialogId, "I want to speak to a SALES REP now.", 1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isStopTriggerHit());
        assertFalse(result.isLimitReached());
        assertEquals("HUMAN_HANDOFF", result.getDialogStatus());
        assertNull(result.getAiReply());

        // Verify Dialog status in DB is HUMAN_HANDOFF
        Dialog updatedDialog = dialogRepository.findById(dialogId).orElseThrow();
        assertEquals("HUMAN_HANDOFF", updatedDialog.getStatus());

        // Verify ONLY 1 message saved in database (LEAD message). AI response generation was stopped!
        List<Message> messages = messageRepository.findByDialogId(dialogId);
        assertEquals(1, messages.size());
        assertEquals("LEAD", messages.get(0).getSenderType());
        assertEquals("I want to speak to a SALES REP now.", messages.get(0).getContent());
    }

    @Test
    public void testProcessMessage_DefaultStopTriggerHit_InGeneratedReply() {
        // Arrange - reply generator returns a trigger word "pricing"
        aiDialogueService.setAiReplyGenerator((context, aiState) -> "Sure! Let me provide you with our pricing details.");

        // Act
        DialogProcessResult result = aiDialogueService.processIncomingMessage(dialogId, "What are your services?", 1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isStopTriggerHit());
        assertFalse(result.isLimitReached());
        assertEquals("HUMAN_HANDOFF", result.getDialogStatus());
        assertNull(result.getAiReply());

        // Verify Dialog status in DB
        Dialog updatedDialog = dialogRepository.findById(dialogId).orElseThrow();
        assertEquals("HUMAN_HANDOFF", updatedDialog.getStatus());

        // Verify AI message was NOT saved
        List<Message> messages = messageRepository.findByDialogId(dialogId);
        assertEquals(1, messages.size());
        assertEquals("LEAD", messages.get(0).getSenderType());
    }

    @Test
    public void testProcessMessage_CustomStopTriggerFromAiStateHit() {
        // Arrange - update dialog to contain a custom stop trigger in aiState JSON
        Dialog d = dialogRepository.findById(dialogId).orElseThrow();
        d.setAiState("{\"stop_triggers\":[\"custom_opt_out\"]}");
        dialogRepository.save(d);

        aiDialogueService.setAiReplyGenerator((context, aiState) -> "Standard answer.");

        // Act - send custom stop trigger word
        DialogProcessResult result = aiDialogueService.processIncomingMessage(dialogId, "Please do custom_opt_out from this list.", 1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isStopTriggerHit());
        assertEquals("HUMAN_HANDOFF", result.getDialogStatus());
        assertNull(result.getAiReply());

        // Verify DB state
        Dialog updatedDialog = dialogRepository.findById(dialogId).orElseThrow();
        assertEquals("HUMAN_HANDOFF", updatedDialog.getStatus());
    }

    @Test
    public void testProcessMessage_MessageLimitReached() {
        // Arrange - pre-populate dialogue with 7 messages (4 LEAD, 3 AI)
        for (int i = 0; i < 7; i++) {
            Message m = new Message();
            m.setId(UUID.randomUUID().toString());
            m.setDialogId(dialogId);
            m.setSenderType(i % 2 == 0 ? "LEAD" : "AI");
            m.setContent("Existing turn " + i);
            messageRepository.save(m);
        }

        aiDialogueService.setAiReplyGenerator((context, aiState) -> "Standard reply.");

        // Act - the 8th message arrives
        DialogProcessResult result = aiDialogueService.processIncomingMessage(dialogId, "The eighth message.", 100L);

        // Assert
        assertNotNull(result);
        assertFalse(result.isStopTriggerHit());
        assertTrue(result.isLimitReached());
        assertEquals("BLOCKED", result.getDialogStatus());
        assertNull(result.getAiReply());

        // Verify Dialog status is BLOCKED
        Dialog updatedDialog = dialogRepository.findById(dialogId).orElseThrow();
        assertEquals("BLOCKED", updatedDialog.getStatus());

        // Total messages in DB must be exactly 8 (7 existing + 1 incoming LEAD, no AI reply)
        List<Message> messages = messageRepository.findByDialogId(dialogId);
        assertEquals(8, messages.size());
    }

    @Test
    public void testProcessMessage_DialogAlreadyNotActive() {
        // Arrange - set status to BLOCKED initially
        Dialog d = dialogRepository.findById(dialogId).orElseThrow();
        d.setStatus("BLOCKED");
        dialogRepository.save(d);

        aiDialogueService.setAiReplyGenerator((context, aiState) -> "Reply.");

        // Act
        DialogProcessResult result = aiDialogueService.processIncomingMessage(dialogId, "Any message", 1L);

        // Assert
        assertNotNull(result);
        assertEquals("BLOCKED", result.getDialogStatus());
        assertNull(result.getAiReply());
        assertTrue(result.getMessage().contains("is not in ACTIVE state"));
    }

    @Test
    public void testProcessMessage_NullInputsHandledGracefully() {
        // Arrange
        aiDialogueService.setAiReplyGenerator((context, aiState) -> null);

        // Act & Assert
        assertDoesNotThrow(() -> {
            DialogProcessResult result = aiDialogueService.processIncomingMessage(dialogId, null, 1L);
            assertNotNull(result);
            assertFalse(result.isStopTriggerHit());
            assertEquals("ACTIVE", result.getDialogStatus());
            assertNull(result.getAiReply());
        });
    }
}
