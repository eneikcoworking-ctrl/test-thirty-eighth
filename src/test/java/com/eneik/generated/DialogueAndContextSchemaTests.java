package com.eneik.generated;

import com.eneik.generated.model.Dialog;
import com.eneik.generated.model.Message;
import com.eneik.generated.repository.DialogRepository;
import com.eneik.generated.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DialogueAndContextSchemaTests {

    @Autowired
    private DialogRepository dialogRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Test
    public void testCreateAndRetrieveDialogAndMessages() {
        // Arrange
        String dialogId = UUID.randomUUID().toString();
        Dialog dialog = new Dialog();
        dialog.setId(dialogId);
        dialog.setTelegramChatId(123456789L);
        dialog.setLeadUsername("test_lead");
        dialog.setLeadPhone("+1234567890");
        dialog.setAiState("{\"current_turn\": 1, \"persona\": \"sales_agent\"}");
        dialog.setStatus("ACTIVE");

        // Act
        dialogRepository.save(dialog);

        String message1Id = UUID.randomUUID().toString();
        Message message1 = new Message();
        message1.setId(message1Id);
        message1.setDialogId(dialogId);
        message1.setSenderType("LEAD");
        message1.setContent("Hello, I am interested in your offer.");
        message1.setTelegramMessageId(101L);

        String message2Id = UUID.randomUUID().toString();
        Message message2 = new Message();
        message2.setId(message2Id);
        message2.setDialogId(dialogId);
        message2.setSenderType("AI");
        message2.setContent("Hi! Great to hear that. How can I help you today?");
        message2.setTelegramMessageId(102L);

        messageRepository.save(message1);
        messageRepository.save(message2);

        // Assert
        Optional<Dialog> retrievedDialogOpt = dialogRepository.findById(dialogId);
        assertTrue(retrievedDialogOpt.isPresent());
        Dialog retrievedDialog = retrievedDialogOpt.get();
        assertEquals(123456789L, retrievedDialog.getTelegramChatId());
        assertEquals("test_lead", retrievedDialog.getLeadUsername());
        assertEquals("+1234567890", retrievedDialog.getLeadPhone());
        assertEquals("{\"current_turn\": 1, \"persona\": \"sales_agent\"}", retrievedDialog.getAiState());
        assertEquals("ACTIVE", retrievedDialog.getStatus());
        assertNotNull(retrievedDialog.getCreatedAt());
        assertNotNull(retrievedDialog.getUpdatedAt());

        List<Message> messages = messageRepository.findByDialogId(dialogId);
        assertEquals(2, messages.size());

        Message m1 = messages.stream().filter(m -> m.getId().equals(message1Id)).findFirst().orElseThrow();
        assertEquals("LEAD", m1.getSenderType());
        assertEquals("Hello, I am interested in your offer.", m1.getContent());
        assertEquals(101L, m1.getTelegramMessageId());
        assertNotNull(m1.getCreatedAt());

        Message m2 = messages.stream().filter(m -> m.getId().equals(message2Id)).findFirst().orElseThrow();
        assertEquals("AI", m2.getSenderType());
        assertEquals("Hi! Great to hear that. How can I help you today?", m2.getContent());
        assertEquals(102L, m2.getTelegramMessageId());
        assertNotNull(m2.getCreatedAt());
    }
}
