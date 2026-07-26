package com.eneik.generated;

import com.eneik.generated.controller.SpintaxController;
import com.eneik.generated.model.Lead;
import com.eneik.generated.model.TargetList;
import com.eneik.generated.repository.LeadRepository;
import com.eneik.generated.repository.TargetListRepository;
import com.eneik.generated.service.LeadIngestionService;
import com.eneik.generated.service.SpintaxService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class LeadAndSpintaxIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpintaxService spintaxService;

    @Autowired
    private LeadIngestionService leadIngestionService;

    @Autowired
    private TargetListRepository targetListRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Test
    public void testSpintaxParsingSimpleAndNested() {
        // Test simple spintax
        String simpleTemplate = "{Hi|Hello} world!";

        // Deterministic tests using seed
        // With seed 12345, let's verify reproducibility
        String result1 = spintaxService.parse(simpleTemplate, 12345L);
        String result2 = spintaxService.parse(simpleTemplate, 12345L);
        assertThat(result1).isEqualTo(result2);

        // Test nested spintax template
        String nestedTemplate = "{Hello|Hi {there|friend}} from the {AI|automation} engine!";
        String nestedResult1 = spintaxService.parse(nestedTemplate, 42L);
        String nestedResult2 = spintaxService.parse(nestedTemplate, 42L);
        assertThat(nestedResult1).isEqualTo(nestedResult2);

        // Ensure outcome matches options and does not contain raw spintax symbols
        assertThat(nestedResult1)
            .doesNotContain("{")
            .doesNotContain("}")
            .doesNotContain("|");
    }

    @Test
    public void testCsvParsingAndIngestionService() {
        String csvData = "username,phone_number,first_name,last_name,metadata\n" +
                "alice_dev,+111222,Alice,Smith,{\"role\": \"developer\"}\n" +
                "\"bob,the,builder\",+333444,Bob,Jones,\"{\"\"project\"\": \"\"crypto\"\"}\"\n" +
                "charlie_reach,,Charlie,Brown,\n";

        List<Lead> leads = leadIngestionService.ingestCsv(csvData, null, "Beta Campaign List");
        assertThat(leads).hasSize(3);

        // Verify Target List was created
        TargetList targetList = leads.get(0).getTargetList();
        assertThat(targetList).isNotNull();
        assertThat(targetList.getName()).isEqualTo("Beta Campaign List");

        // Verify lead content
        Lead lead1 = leads.stream().filter(l -> l.getUsername().equals("alice_dev")).findFirst().orElseThrow();
        assertThat(lead1.getPhoneNumber()).isEqualTo("+111222");
        assertThat(lead1.getFirstName()).isEqualTo("Alice");
        assertThat(lead1.getLastName()).isEqualTo("Smith");
        assertThat(lead1.getMetadata()).isEqualTo("{\"role\": \"developer\"}");
        assertThat(lead1.getStatus()).isEqualTo("PENDING");

        // Verify row with quotes and commas
        Lead lead2 = leads.stream().filter(l -> l.getUsername().equals("bob,the,builder")).findFirst().orElseThrow();
        assertThat(lead2.getPhoneNumber()).isEqualTo("+333444");
        assertThat(lead2.getFirstName()).isEqualTo("Bob");
        assertThat(lead2.getLastName()).isEqualTo("Jones");
        assertThat(lead2.getMetadata()).isEqualTo("{\"project\": \"crypto\"}");

        // Verify row with empty metadata/phone number
        Lead lead3 = leads.stream().filter(l -> l.getUsername().equals("charlie_reach")).findFirst().orElseThrow();
        assertThat(lead3.getPhoneNumber()).isNull();
        assertThat(lead3.getMetadata()).isNull();
    }

    @Test
    public void testCsvIngestionServiceWithNoHeaders() {
        // Without headers, fallback defaults to: username, phone_number, first_name, last_name, metadata
        String csvDataNoHeaders = "direct_user,+1234,Direct,First,{\"custom\": \"yes\"}\n" +
                "direct_user2,+5678,Direct2,Second,\n";

        List<Lead> leads = leadIngestionService.ingestCsv(csvDataNoHeaders, null, "No Header List");
        assertThat(leads).hasSize(2);

        Lead lead1 = leads.get(0);
        assertThat(lead1.getUsername()).isEqualTo("direct_user");
        assertThat(lead1.getPhoneNumber()).isEqualTo("+1234");
        assertThat(lead1.getFirstName()).isEqualTo("Direct");
        assertThat(lead1.getLastName()).isEqualTo("First");
        assertThat(lead1.getMetadata()).isEqualTo("{\"custom\": \"yes\"}");
    }

    @Test
    public void testControllerSpintaxParseApi() throws Exception {
        String requestJson = "{\"template\": \"{Good morning|Welcome} to {Java|Spring}!\", \"seed\": 999}";

        mockMvc.perform(post("/api/spintax/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parsedText").exists())
                .andExpect(jsonPath("$.parsedText").value("Welcome to Spring!"));
    }

    @Test
    public void testControllerLeadImportTextApi() throws Exception {
        String csvContent = "username,phone_number,first_name,last_name\n" +
                "api_user1,+999111,API_First,API_Last\n";

        mockMvc.perform(post("/api/leads/import/text?targetListName=API Ingest List")
                .contentType(MediaType.TEXT_PLAIN)
                .content(csvContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("api_user1"))
                .andExpect(jsonPath("$[0].phoneNumber").value("+999111"))
                .andExpect(jsonPath("$[0].firstName").value("API_First"))
                .andExpect(jsonPath("$[0].lastName").value("API_Last"))
                .andExpect(jsonPath("$[0].targetList.name").value("API Ingest List"));
    }

    @Test
    public void testControllerLeadImportMultipartFileApi() throws Exception {
        String csvContent = "username,phone_number,first_name,last_name\n" +
                "multipart_user1,+444555,Multi_First,Multi_Last\n";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "leads.csv",
                MediaType.TEXT_PLAIN_VALUE,
                csvContent.getBytes()
        );

        mockMvc.perform(multipart("/api/leads/import")
                .file(file)
                .param("targetListName", "Multipart Ingest List"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("multipart_user1"))
                .andExpect(jsonPath("$[0].phoneNumber").value("+444555"))
                .andExpect(jsonPath("$[0].firstName").value("Multi_First"))
                .andExpect(jsonPath("$[0].lastName").value("Multi_Last"))
                .andExpect(jsonPath("$[0].targetList.name").value("Multipart Ingest List"));
    }
}
