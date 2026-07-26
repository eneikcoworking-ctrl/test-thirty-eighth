package com.eneik.generated;

import com.eneik.generated.repository.DialogRepository;
import com.eneik.generated.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CrmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DialogRepository dialogRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Test
    public void testGetConversationsAndReply() throws Exception {
        // Clear database to ensure predictable state
        messageRepository.deleteAllInBatch();
        dialogRepository.deleteAllInBatch();

        // 1. Force Seed mock data via HTTP POST
        mockMvc.perform(post("/api/conversations/seed"))
                .andExpect(status().isOk());

        // 2. Fetch conversations
        mockMvc.perform(get("/api/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.id == 'marcus-chen-id')].leadUsername", contains("Marcus Chen")))
                .andExpect(jsonPath("$[?(@.id == 'marcus-chen-id')].status", contains("WAITING")))
                .andExpect(jsonPath("$[?(@.id == 'marcus-chen-id')].unreadCount", contains(3)));

        // 3. Post a manual reply to Marcus Chen
        String replyJson = "{\"content\": \"I am looking into your Stellar Dynamics API delay issue right now!\"}";
        mockMvc.perform(post("/api/conversations/marcus-chen-id/reply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(replyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderType", is("OPERATOR")))
                .andExpect(jsonPath("$.content", is("I am looking into your Stellar Dynamics API delay issue right now!")));

        // 4. Fetch again and confirm status transitioned to ACTIVE
        mockMvc.perform(get("/api/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'marcus-chen-id')].status", contains("ACTIVE")));
    }
}
