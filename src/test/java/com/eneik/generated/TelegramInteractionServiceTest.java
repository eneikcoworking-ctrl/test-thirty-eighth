package com.eneik.generated;

import com.eneik.generated.exception.AccountNotActiveException;
import com.eneik.generated.exception.DailyLimitExceededException;
import com.eneik.generated.model.TGAccount;
import com.eneik.generated.model.TelegramDispatchLog;
import com.eneik.generated.repository.TGAccountRepository;
import com.eneik.generated.repository.TelegramDispatchLogRepository;
import com.eneik.generated.service.PauseEnforcer;
import com.eneik.generated.service.TelegramClient;
import com.eneik.generated.service.TelegramInteractionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
public class TelegramInteractionServiceTest {

    @Autowired
    private TGAccountRepository tgAccountRepository;

    @Autowired
    private TelegramDispatchLogRepository telegramDispatchLogRepository;

    private TelegramClient mockTelegramClient;
    private PauseEnforcer mockPauseEnforcer;
    private TelegramInteractionService service;
    private TGAccount activeAccount;

    @BeforeEach
    public void setUp() {
        mockTelegramClient = mock(TelegramClient.class);
        mockPauseEnforcer = mock(PauseEnforcer.class);

        // Inject seeded Random to satisfy predictability requirement
        Random seededRandom = new Random(42);

        service = new TelegramInteractionService(
                tgAccountRepository,
                telegramDispatchLogRepository,
                mockTelegramClient,
                mockPauseEnforcer
        );
        service.setRandom(seededRandom);

        // Create and save an active account
        activeAccount = new TGAccount();
        activeAccount.setPhoneNumber("+79998887766");
        activeAccount.setUsername("active_outreach");
        activeAccount.setStatus("ACTIVE");
        activeAccount.setDailyLimit(3);
        activeAccount = tgAccountRepository.save(activeAccount);
    }

    @Test
    public void testDispatchSuccessEnforcesSequenceAndHumanEmulation() throws InterruptedException {
        Long accountId = activeAccount.getId();
        Long chatId = 123456L;
        String username = "lead_username";
        String phone = "+15550001111";
        String text = "Hello from AI LeadGen Bot!";

        // Run dispatch
        service.dispatchMessage(accountId, chatId, username, phone, text);

        // 1. Verify sequence of actions: Typing -> Pause -> Message
        InOrder inOrder = inOrder(mockTelegramClient, mockPauseEnforcer);
        inOrder.verify(mockTelegramClient).sendTypingStatus(accountId, chatId);

        // Seed 42 nextInt(181) calculation:
        int expectedPause = 120 + new Random(42).nextInt(181);
        inOrder.verify(mockPauseEnforcer).pause(expectedPause);

        inOrder.verify(mockTelegramClient).sendMessage(accountId, chatId, text);

        // 2. Verify dispatch log persistence
        List<TelegramDispatchLog> logs = telegramDispatchLogRepository.findAll();
        assertThat(logs).hasSize(1);
        TelegramDispatchLog savedLog = logs.get(0);
        assertThat(savedLog.getAccount().getId()).isEqualTo(accountId);
        assertThat(savedLog.getRecipientUsername()).isEqualTo(username);
        assertThat(savedLog.getRecipientPhone()).isEqualTo(phone);
        assertThat(savedLog.getSentAt()).isNotNull();
    }

    @Test
    public void testDispatchThrowsWhenAccountNotActive() {
        // Change status to banned
        activeAccount.setStatus("PERMANENT_BAN");
        tgAccountRepository.save(activeAccount);

        assertThatThrownBy(() -> service.dispatchMessage(activeAccount.getId(), 123L, "user", "phone", "hello"))
                .isInstanceOf(AccountNotActiveException.class)
                .hasMessageContaining("Account is not active");

        // Verify no client actions were invoked
        verifyNoInteractions(mockTelegramClient, mockPauseEnforcer);
    }

    @Test
    public void testDispatchThrowsWhenDailyLimitReached() throws InterruptedException {
        Long accountId = activeAccount.getId();

        // Save mock dispatch logs to consume the daily limit of 3
        TelegramDispatchLog log1 = new TelegramDispatchLog(activeAccount, "user1", "phone1");
        TelegramDispatchLog log2 = new TelegramDispatchLog(activeAccount, "user2", "phone2");
        TelegramDispatchLog log3 = new TelegramDispatchLog(activeAccount, "user3", "phone3");
        telegramDispatchLogRepository.saveAll(List.of(log1, log2, log3));

        // Attempting to dispatch a 4th message should fail
        assertThatThrownBy(() -> service.dispatchMessage(accountId, 123L, "user4", "phone4", "hello"))
                .isInstanceOf(DailyLimitExceededException.class)
                .hasMessageContaining("Daily message limit reached");

        // Verify no interactions with client occurred for the failed attempt
        verifyNoInteractions(mockTelegramClient, mockPauseEnforcer);
    }

    @Test
    public void testDailyLimitScopeLast24HoursOnly() throws InterruptedException {
        Long accountId = activeAccount.getId();

        // Save a log that is older than 24 hours
        TelegramDispatchLog oldLog = new TelegramDispatchLog(activeAccount, "old_user", "old_phone");
        // Force the sent_at to 25 hours ago
        oldLog.setSentAt(OffsetDateTime.now().minusHours(25));
        telegramDispatchLogRepository.save(oldLog);

        // Save two fresh logs
        TelegramDispatchLog freshLog1 = new TelegramDispatchLog(activeAccount, "fresh1", "phone1");
        TelegramDispatchLog freshLog2 = new TelegramDispatchLog(activeAccount, "fresh2", "phone2");
        telegramDispatchLogRepository.saveAll(List.of(freshLog1, freshLog2));

        // The older log should be out of scope, leaving sentCount = 2 (dailyLimit is 3)
        // This dispatch should succeed
        service.dispatchMessage(accountId, 123L, "user3", "phone3", "hello");

        // Verify that the call succeeded and a new log was created
        List<TelegramDispatchLog> logs = telegramDispatchLogRepository.findAll();
        // Old log + 2 fresh logs + 1 newly dispatched log = 4 total logs in DB
        assertThat(logs).hasSize(4);
    }
}
