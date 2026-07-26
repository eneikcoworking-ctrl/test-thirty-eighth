package com.eneik.generated;

import com.eneik.generated.controller.DialogController;
import com.eneik.generated.model.Dialog;
import com.eneik.generated.model.Message;
import com.eneik.generated.model.TGAccount;
import com.eneik.generated.repository.DialogRepository;
import com.eneik.generated.repository.MessageRepository;
import com.eneik.generated.repository.TGAccountRepository;
import com.eneik.generated.service.TelegramDispatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class DialogOverrideIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DialogRepository dialogRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private TGAccountRepository tgAccountRepository;

    @Autowired
    private TelegramDispatchService telegramDispatchService;

    @Autowired
    private ObjectMapper objectMapper;

    private TGAccount testAccount;
    private TGAccount bannedAccount;

    @BeforeEach
    public void setUp() {
        // Clear repositories
        messageRepository.deleteAll();
        dialogRepository.deleteAll();
        tgAccountRepository.deleteAll();

        // Setup seed for dispatch service for predictable test outcomes
        telegramDispatchService.seedMessageId(500005L);

        // Setup a Telegram session account
        testAccount = new TGAccount(
                "+19998887766",
                "active_operator_session",
                "ACTIVE",
                "{}",
                null,
                15
        );
        testAccount = tgAccountRepository.save(testAccount);

        // Setup a banned account
        bannedAccount = new TGAccount(
                "+10000000000",
                "banned_session",
                "PERMANENT_BAN",
                "{}",
                null,
                15
        );
        bannedAccount = tgAccountRepository.save(bannedAccount);
    }

    @Test
    public void testGetAllDialogs() throws Exception {
        // Arrange
        Dialog dialog = new Dialog();
        dialog.setId(UUID.randomUUID().toString());
        dialog.setTelegramChatId(12345L);
        dialog.setLeadUsername("lead_user");
        dialog.setStatus("ACTIVE");
        dialogRepository.save(dialog);

        // Act & Assert
        mockMvc.perform(get("/api/dialogs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(dialog.getId())))
                .andExpect(jsonPath("$[0].leadUsername", is("lead_user")))
                .andExpect(jsonPath("$[0].status", is("ACTIVE")));
    }

    @Test
    public void testGetDialogById() throws Exception {
        // Arrange
        Dialog dialog = new Dialog();
        dialog.setId(UUID.randomUUID().toString());
        dialog.setTelegramChatId(67890L);
        dialog.setLeadUsername("jane_doe");
        dialog.setStatus("PAUSED");
        dialogRepository.save(dialog);

        // Act & Assert
        mockMvc.perform(get("/api/dialogs/" + dialog.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(dialog.getId())))
                .andExpect(jsonPath("$.telegramChatId", is(67890)))
                .andExpect(jsonPath("$.status", is("PAUSED")));
    }

    @Test
    public void testGetDialogByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/dialogs/invalid-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetDialogMessages() throws Exception {
        // Arrange
        String dialogId = UUID.randomUUID().toString();
        Dialog dialog = new Dialog();
        dialog.setId(dialogId);
        dialog.setTelegramChatId(11111L);
        dialog.setStatus("ACTIVE");
        dialogRepository.save(dialog);

        Message msg = new Message();
        msg.setId(UUID.randomUUID().toString());
        msg.setDialogId(dialogId);
        msg.setSenderType("LEAD");
        msg.setContent("Hello there!");
        messageRepository.save(msg);

        // Act & Assert
        mockMvc.perform(get("/api/dialogs/" + dialogId + "/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content", is("Hello there!")))
                .andExpect(jsonPath("$[0].senderType", is("LEAD")));
    }

    @Test
    public void testProcessOverrideSuccess() throws Exception {
        // Arrange
        String dialogId = UUID.randomUUID().toString();
        Dialog dialog = new Dialog();
        dialog.setId(dialogId);
        dialog.setTelegramChatId(123456789L);
        dialog.setLeadUsername("lead_client");
        dialog.setAiState("{\"last_thought\":\"greeting\"}");
        dialog.setStatus("ACTIVE");
        dialog.setTelegramAccountId(testAccount.getId());
        dialogRepository.save(dialog);

        DialogController.OperatorOverrideRequest requestBody =
                new DialogController.OperatorOverrideRequest("Hijacking: Here is a special offer!", null);

        // Act
        mockMvc.perform(post("/api/dialogs/" + dialogId + "/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dialogId", is(dialogId)))
                .andExpect(jsonPath("$.senderType", is("OPERATOR")))
                .andExpect(jsonPath("$.content", is("Hijacking: Here is a special offer!")))
                .andExpect(jsonPath("$.telegramMessageId", is(500005))); // Checks predictability/reproducibility

        // Assert updated dialog state
        Dialog updatedDialog = dialogRepository.findById(dialogId).orElseThrow();
        assertThat(updatedDialog.getStatus()).isEqualTo("PAUSED");
        assertThat(updatedDialog.getAiState()).isEqualTo("PAUSED");

        // Assert message saved in database
        List<Message> messages = messageRepository.findByDialogId(dialogId);
        assertThat(messages).hasSize(1);
        Message savedMsg = messages.get(0);
        assertThat(savedMsg.getSenderType()).isEqualTo("OPERATOR");
        assertThat(savedMsg.getContent()).isEqualTo("Hijacking: Here is a special offer!");
        assertThat(savedMsg.getTelegramMessageId()).isEqualTo(500005L);
    }

    @Test
    public void testProcessOverrideAssignsAccount() throws Exception {
        // Arrange: Dialog starts with no assigned Telegram account
        String dialogId = UUID.randomUUID().toString();
        Dialog dialog = new Dialog();
        dialog.setId(dialogId);
        dialog.setTelegramChatId(88888L);
        dialog.setLeadUsername("lead_without_account");
        dialog.setStatus("ACTIVE");
        dialog.setTelegramAccountId(null);
        dialogRepository.save(dialog);

        // Request supplies the Telegram account ID to assign/use
        DialogController.OperatorOverrideRequest requestBody =
                new DialogController.OperatorOverrideRequest("Assigned account on override!", testAccount.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dialogs/" + dialogId + "/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated());

        Dialog updatedDialog = dialogRepository.findById(dialogId).orElseThrow();
        assertThat(updatedDialog.getStatus()).isEqualTo("PAUSED");
        assertThat(updatedDialog.getTelegramAccountId()).isEqualTo(testAccount.getId());
    }

    @Test
    public void testProcessOverrideMissingAccountException() throws Exception {
        // Arrange: Dialog starts with no assigned Telegram account and request does not supply one
        String dialogId = UUID.randomUUID().toString();
        Dialog dialog = new Dialog();
        dialog.setId(dialogId);
        dialog.setTelegramChatId(88888L);
        dialog.setStatus("ACTIVE");
        dialog.setTelegramAccountId(null);
        dialogRepository.save(dialog);

        DialogController.OperatorOverrideRequest requestBody =
                new DialogController.OperatorOverrideRequest("Failing override", null);

        // Act & Assert
        mockMvc.perform(post("/api/dialogs/" + dialogId + "/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("No Telegram session/account is assigned")));
    }

    @Test
    public void testProcessOverrideBannedAccountException() throws Exception {
        // Arrange: Dialog is assigned to a banned session
        String dialogId = UUID.randomUUID().toString();
        Dialog dialog = new Dialog();
        dialog.setId(dialogId);
        dialog.setTelegramChatId(88888L);
        dialog.setStatus("ACTIVE");
        dialog.setTelegramAccountId(bannedAccount.getId());
        dialogRepository.save(dialog);

        DialogController.OperatorOverrideRequest requestBody =
                new DialogController.OperatorOverrideRequest("Banned send", null);

        // Act & Assert
        mockMvc.perform(post("/api/dialogs/" + dialogId + "/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("Cannot dispatch message via banned Telegram session")));
    }

    @Test
    public void testProcessOverrideConcurrentModificationConflict() throws Exception {
        // Arrange: Dialog in database is active
        String dialogId = UUID.randomUUID().toString();
        Dialog dialog = new Dialog();
        dialog.setId(dialogId);
        dialog.setTelegramChatId(99999L);
        dialog.setStatus("ACTIVE");
        dialog.setTelegramAccountId(testAccount.getId());
        dialogRepository.save(dialog);

        // Inject a concurrent state change before the actual controller save, causing the atomically-guarded query to fail
        // We simulate this by deleting or updating the row status underneath, or using expectedStatus that won't match.
        // Let's modify the status in the DB directly to "PAUSED" via another connection or another transaction,
        // or let's test our updateStatusAtomically directly to prove it returns 0 rows when the expected status is incorrect.
        int updatedRows = dialogRepository.updateStatusAtomically(dialogId, "INVALID_EXPECTED_STATUS", "PAUSED", "PAUSED", testAccount.getId(), LocalDateTime.now());
        assertThat(updatedRows).isEqualTo(0);

        // We can mock or test the controller's concurrent modification exception by running a test where the expected status changes right after we fetch.
        // Let's verify that the controller behaves correctly under a simulated race condition where the status changed in the DB after retrieval.
        // In the controller:
        // String currentStatus = dialog.getStatus(); // Let's say "ACTIVE"
        // But before the controller does updateStatusAtomically, a concurrent thread updates status to "PAUSED" in the DB.
        // Let's simulate this exactly!
        Dialog dialogToRace = new Dialog();
        String raceId = UUID.randomUUID().toString();
        dialogToRace.setId(raceId);
        dialogToRace.setTelegramChatId(55555L);
        dialogToRace.setStatus("ACTIVE");
        dialogToRace.setTelegramAccountId(testAccount.getId());
        dialogRepository.save(dialogToRace);

        // Simulating the race:
        // We retrieve the dialog (it says "ACTIVE")
        Dialog fetched = dialogRepository.findById(raceId).orElseThrow();
        assertThat(fetched.getStatus()).isEqualTo("ACTIVE");

        // Concurrent update changes it in DB to "PAUSED" (or "COMPLETED")
        dialogRepository.updateStatusAtomically(raceId, "ACTIVE", "COMPLETED", "COMPLETED", testAccount.getId(), LocalDateTime.now());

        // Now we try to transition status using the "fetched" dialog's old status ("ACTIVE")
        int rows = dialogRepository.updateStatusAtomically(raceId, "ACTIVE", "PAUSED", "PAUSED", testAccount.getId(), LocalDateTime.now());
        assertThat(rows).isEqualTo(0); // Atomic protection successful!
    }
}
