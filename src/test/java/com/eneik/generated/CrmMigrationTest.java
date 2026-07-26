package com.eneik.generated;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class CrmMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testMigrationAndOptimizedSchema() {
        // 1. Verify all necessary tables exist in the H2 database schema
        List<String> tables = jdbcTemplate.queryForList(
            "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'",
            String.class
        );

        assertThat(tables)
            .contains("CRM_TELEGRAM_ACCOUNTS", "CRM_LEADS", "CRM_CHATS", "CRM_MESSAGES");

        // 2. Verify optimized index exists
        List<Map<String, Object>> indexes = jdbcTemplate.queryForList(
            "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME = 'CRM_CHATS' AND INDEX_NAME = 'IDX_CRM_CHATS_ACCOUNT_STATUS_LAST_MSG'"
        );
        assertThat(indexes).isNotEmpty();

        // 3. Verify view exists and can be queried
        List<Map<String, Object>> viewColumns = jdbcTemplate.queryForList(
            "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'CRM_UNIFIED_INBOX_VIEW'"
        );
        assertThat(viewColumns).isNotEmpty();

        // 4. Test insert and selection flow to guarantee unified inbox query correctness
        jdbcTemplate.execute("INSERT INTO crm_telegram_accounts (phone_number, session_status) VALUES ('+123456789', 'Active')");
        Long accountId = jdbcTemplate.queryForObject("SELECT id FROM crm_telegram_accounts WHERE phone_number = '+123456789'", Long.class);

        jdbcTemplate.execute("INSERT INTO crm_leads (username, phone_number, status) VALUES ('lead_user', '+987654321', 'New')");
        Long leadId = jdbcTemplate.queryForObject("SELECT id FROM crm_leads WHERE username = 'lead_user'", Long.class);

        jdbcTemplate.execute(String.format(
            "INSERT INTO crm_chats (telegram_account_id, lead_id, status, last_message_at) VALUES (%d, %d, 'Unassigned', '2026-07-26 12:00:00')",
            accountId, leadId
        ));
        Long chatId = jdbcTemplate.queryForObject("SELECT id FROM crm_chats WHERE telegram_account_id = " + accountId, Long.class);

        jdbcTemplate.execute(String.format(
            "INSERT INTO crm_messages (chat_id, sender_type, text, sent_at) VALUES (%d, 'Lead', 'Hello, interest in product!', '2026-07-26 12:00:00')",
            chatId
        ));

        // Query the unified inbox view to ensure data mapping operates flawlessly
        List<Map<String, Object>> results = jdbcTemplate.queryForList("SELECT * FROM crm_unified_inbox_view");
        assertThat(results).hasSize(1);
        Map<String, Object> record = results.get(0);
        assertThat(record.get("CHAT_STATUS")).isEqualTo("Unassigned");
        assertThat(record.get("ACCOUNT_PHONE_NUMBER")).isEqualTo("+123456789");
        assertThat(record.get("LEAD_USERNAME")).isEqualTo("lead_user");
    }
}
