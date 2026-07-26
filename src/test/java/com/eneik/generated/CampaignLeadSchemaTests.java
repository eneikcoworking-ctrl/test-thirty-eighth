package com.eneik.generated;

import com.eneik.generated.model.Campaign;
import com.eneik.generated.model.Lead;
import com.eneik.generated.model.TargetList;
import com.eneik.generated.repository.CampaignRepository;
import com.eneik.generated.repository.LeadRepository;
import com.eneik.generated.repository.TargetListRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class CampaignLeadSchemaTests {

    @Autowired
    private TargetListRepository targetListRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Test
    public void testSchemaAndPersistenceOfCampaignsAndLeads() {
        // 1. Create a Target List
        TargetList targetList = new TargetList("Crypto Devs", "List of potential leads who are developers in Crypto space.");
        TargetList savedList = targetListRepository.save(targetList);
        assertThat(savedList.getId()).isNotNull();
        assertThat(savedList.getCreatedAt()).isNotNull();

        // 2. Create a Campaign referencing the Target List
        Campaign campaign = new Campaign("Crypto Outreach V1", "Hello {Dev|Builder}, we love your work!", true, savedList);
        campaign.setStatus("ACTIVE");
        Campaign savedCampaign = campaignRepository.save(campaign);
        assertThat(savedCampaign.getId()).isNotNull();
        assertThat(savedCampaign.getSpintaxTemplate()).isEqualTo("Hello {Dev|Builder}, we love your work!");
        assertThat(savedCampaign.isUseLlmPersonalization()).isTrue();
        assertThat(savedCampaign.getTargetList().getId()).isEqualTo(savedList.getId());
        assertThat(savedCampaign.getStatus()).isEqualTo("ACTIVE");

        // 3. Create Leads under the Target List
        Lead lead1 = new Lead(savedList, "alice_crypto", "+1234567890", "Alice", "Smith", "{\"github\": \"alice-crypto\"}");
        Lead lead2 = new Lead(savedList, "bob_web3", null, "Bob", "Jones", "{\"github\": \"bob-web3\"}");
        leadRepository.save(lead1);
        leadRepository.save(lead2);

        // 4. Verify relations and schema
        List<Lead> leadsInList = leadRepository.findByTargetListId(savedList.getId());
        assertThat(leadsInList).hasSize(2);
        assertThat(leadsInList).extracting(Lead::getUsername).containsExactlyInAnyOrder("alice_crypto", "bob_web3");
        assertThat(leadsInList).extracting(Lead::getPhoneNumber).containsExactlyInAnyOrder("+1234567890", null);
        assertThat(leadsInList).extracting(Lead::getFirstName).containsExactlyInAnyOrder("Alice", "Bob");
        assertThat(leadsInList).extracting(Lead::getMetadata).containsExactlyInAnyOrder("{\"github\": \"alice-crypto\"}", "{\"github\": \"bob-web3\"}");
    }
}
