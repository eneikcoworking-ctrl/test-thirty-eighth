package com.eneik.generated;

import com.eneik.generated.model.Campaign;
import com.eneik.generated.model.Dialog;
import com.eneik.generated.model.Lead;
import com.eneik.generated.model.Message;
import com.eneik.generated.model.TGAccount;
import com.eneik.generated.model.TargetList;
import com.eneik.generated.repository.CampaignRepository;
import com.eneik.generated.repository.DialogRepository;
import com.eneik.generated.repository.LeadRepository;
import com.eneik.generated.repository.MessageRepository;
import com.eneik.generated.repository.TGAccountRepository;
import com.eneik.generated.repository.TargetListRepository;
import com.eneik.generated.service.CampaignDispatchWorker;
import com.eneik.generated.service.TelegramFloodException;
import com.eneik.generated.service.TelegramFloodWaitException;
import com.eneik.generated.service.TelegramMessageSender;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class CampaignDispatchWorkerTest {

    @Autowired
    private TargetListRepository targetListRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private TGAccountRepository tgAccountRepository;

    @Autowired
    private DialogRepository dialogRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private TestTelegramMessageSender testMessageSender;

    @Autowired
    private CampaignDispatchWorker campaignDispatchWorker;

    @Autowired
    private EntityManager entityManager;

    // Test specific message sender to simulate failures and capture calls
    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public TestTelegramMessageSender testMessageSender() {
            return new TestTelegramMessageSender();
        }
    }

    static class TestTelegramMessageSender implements TelegramMessageSender {
        public List<String> sentLogs = new ArrayList<>();
        public boolean shouldThrowFloodOnceForAccount1 = false;

        public void reset() {
            sentLogs.clear();
            shouldThrowFloodOnceForAccount1 = false;
        }

        @Override
        public void sendMessage(TGAccount account, Lead lead, String message) throws TelegramFloodException {
            if (shouldThrowFloodOnceForAccount1 && "account_1".equals(account.getUsername())) {
                // Throw FLOOD_WAIT error to trigger rotation
                throw new TelegramFloodWaitException(10, "Simulated flood wait limit reached");
            }
            sentLogs.add(account.getUsername() + " -> " + lead.getUsername() + ": " + message);
        }
    }

    @Test
    public void testCampaignSuccessWithoutFlood() {
        testMessageSender.reset();

        // Arrange
        TargetList targetList = targetListRepository.saveAndFlush(new TargetList("List 1", "Desc 1"));
        Campaign campaign = new Campaign("Campaign 1", "Template {Hi}", false, targetList);
        campaign.setStatus("ACTIVE");
        Campaign savedCampaign = campaignRepository.saveAndFlush(campaign);

        tgAccountRepository.saveAndFlush(new TGAccount("+111", "account_1", "ACTIVE", "session", null, 15));

        leadRepository.saveAndFlush(new Lead(targetList, "lead_1", "+9991", "Alice", "A", null));
        leadRepository.saveAndFlush(new Lead(targetList, "lead_2", "+9992", "Bob", "B", null));

        entityManager.flush();
        entityManager.clear();

        // Act
        campaignDispatchWorker.dispatchCampaign(savedCampaign.getId());

        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(testMessageSender.sentLogs).hasSize(2);
        assertThat(testMessageSender.sentLogs).containsExactly(
                "account_1 -> lead_1: Template {Hi}",
                "account_1 -> lead_2: Template {Hi}"
        );

        // Verify lead statuses are SENT
        List<Lead> leads = leadRepository.findByTargetListId(targetList.getId());
        assertThat(leads).hasSize(2);
        assertThat(leads).extracting(Lead::getStatus).containsOnly("SENT");

        // Verify dialog and message are created
        List<Dialog> dialogs = dialogRepository.findAll();
        assertThat(dialogs).isNotEmpty();
        assertThat(dialogs.stream().anyMatch(d -> "lead_1".equals(d.getLeadUsername()))).isTrue();

        List<Message> messages = messageRepository.findAll();
        assertThat(messages).isNotEmpty();
        assertThat(messages.stream().anyMatch(m -> m.getContent().equals("Template {Hi}"))).isTrue();
    }

    @Test
    public void testAccountRotationOnFloodWaitError() {
        testMessageSender.reset();
        testMessageSender.shouldThrowFloodOnceForAccount1 = true;

        // Arrange
        TargetList targetList = targetListRepository.saveAndFlush(new TargetList("List 2", "Desc 2"));
        Campaign campaign = new Campaign("Campaign 2", "Template {Hi}", false, targetList);
        campaign.setStatus("ACTIVE");
        Campaign savedCampaign = campaignRepository.saveAndFlush(campaign);

        // Multiple active TG accounts for rotation
        TGAccount acc1 = tgAccountRepository.saveAndFlush(new TGAccount("+111", "account_1", "ACTIVE", "session", null, 15));
        TGAccount acc2 = tgAccountRepository.saveAndFlush(new TGAccount("+222", "account_2", "ACTIVE", "session", null, 15));

        Lead lead1 = leadRepository.saveAndFlush(new Lead(targetList, "lead_1", "+9991", "Alice", "A", null));
        Lead lead2 = leadRepository.saveAndFlush(new Lead(targetList, "lead_2", "+9992", "Bob", "B", null));

        entityManager.flush();
        entityManager.clear();

        // Act
        campaignDispatchWorker.dispatchCampaign(savedCampaign.getId());

        entityManager.flush();
        entityManager.clear();

        // Assert
        // account_1 should have failed on lead_1, rotating to account_2, which successfully sends to lead_1 and lead_2
        assertThat(testMessageSender.sentLogs).hasSize(2);
        assertThat(testMessageSender.sentLogs).containsExactly(
                "account_2 -> lead_1: Template {Hi}",
                "account_2 -> lead_2: Template {Hi}"
        );

        // Verify account_1 is now TEMPORARY_SPAM_BLOCK
        TGAccount updatedAcc1 = tgAccountRepository.findById(acc1.getId()).orElseThrow();
        assertThat(updatedAcc1.getStatus()).isEqualTo("TEMPORARY_SPAM_BLOCK");

        // Verify account_2 is still ACTIVE
        TGAccount updatedAcc2 = tgAccountRepository.findById(acc2.getId()).orElseThrow();
        assertThat(updatedAcc2.getStatus()).isEqualTo("ACTIVE");

        // Verify leads processed correctly
        List<Lead> leads = leadRepository.findByTargetListId(targetList.getId());
        assertThat(leads).hasSize(2);
        assertThat(leads).extracting(Lead::getStatus).containsOnly("SENT");
    }

    @Test
    public void testBoundaryEightMessagesLimitBlocked() {
        testMessageSender.reset();

        // Arrange
        TargetList targetList = targetListRepository.saveAndFlush(new TargetList("List 3", "Desc 3"));
        Campaign campaign = new Campaign("Campaign 3", "Template {Hi}", false, targetList);
        campaign.setStatus("ACTIVE");
        Campaign savedCampaign = campaignRepository.saveAndFlush(campaign);

        tgAccountRepository.saveAndFlush(new TGAccount("+111", "account_1", "ACTIVE", "session", null, 15));

        Lead lead1 = leadRepository.saveAndFlush(new Lead(targetList, "lead_1", "+9991", "Alice", "A", null));

        // Setup existing dialog with 8 back-and-forth messages for lead_1
        Dialog existingDialog = new Dialog();
        existingDialog.setId(UUID.randomUUID().toString());
        existingDialog.setTelegramChatId(9991L);
        existingDialog.setLeadUsername("lead_1");
        existingDialog.setLeadPhone("+9991");
        existingDialog.setStatus("ACTIVE");
        existingDialog = dialogRepository.saveAndFlush(existingDialog);

        for (int i = 0; i < 8; i++) {
            Message msg = new Message();
            msg.setId(UUID.randomUUID().toString());
            msg.setDialogId(existingDialog.getId());
            msg.setSenderType(i % 2 == 0 ? "LEAD" : "AI");
            msg.setContent("Dummy message " + i);
            messageRepository.saveAndFlush(msg);
        }

        entityManager.flush();
        entityManager.clear();

        // Act
        campaignDispatchWorker.dispatchCampaign(savedCampaign.getId());

        entityManager.flush();
        entityManager.clear();

        // Assert
        // No messages should be sent to lead_1 since it has reached the 8 back-and-forth messages boundary limit
        assertThat(testMessageSender.sentLogs).isEmpty();

        // Dialog status should have been updated to BLOCKED
        Dialog updatedDialog = dialogRepository.findById(existingDialog.getId()).orElseThrow();
        assertThat(updatedDialog.getStatus()).isEqualTo("BLOCKED");

        // Lead status should be BLOCKED
        Lead updatedLead1 = leadRepository.findById(lead1.getId()).orElseThrow();
        assertThat(updatedLead1.getStatus()).isEqualTo("BLOCKED");
    }
}
