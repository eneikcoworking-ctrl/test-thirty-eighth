package com.eneik.generated;

import com.eneik.generated.controller.OutreachAccountController;
import com.eneik.generated.model.Proxy;
import com.eneik.generated.model.TGAccount;
import com.eneik.generated.repository.ProxyRepository;
import com.eneik.generated.repository.TGAccountRepository;
import com.eneik.generated.service.OutreachAccountService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class OutreachAccountServiceTests {

    @Autowired
    private TGAccountRepository tgAccountRepository;

    @Autowired
    private ProxyRepository proxyRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OutreachAccountService outreachAccountService;

    private Clock fixedClock;
    private Instant fixedInstant;

    @BeforeEach
    public void setUp() {
        // Clear tables
        tgAccountRepository.deleteAll();
        proxyRepository.deleteAll();

        // Establish a fixed deterministic clock for time-based test queries
        // Set fixed current time to 2026-07-26T12:00:00Z
        fixedInstant = Instant.parse("2026-07-26T12:00:00.00Z");
        fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
    }

    @Test
    public void testExponentialDelayCalculationWithFixedSeeds() {
        // Create service with custom clock
        OutreachAccountService service = new OutreachAccountService(tgAccountRepository, fixedClock);

        // Test with 0 consecutive actions (should be base delay exactly if no jitter)
        long delay0NoJitter = service.calculateDelay(0, 120.0, 1.5, false, null);
        assertThat(delay0NoJitter).isEqualTo(120L);

        // Test with 3 consecutive actions (should be baseDelay * 1.5^3 = 120 * 3.375 = 405)
        long delay3NoJitter = service.calculateDelay(3, 120.0, 1.5, false, null);
        assertThat(delay3NoJitter).isEqualTo(405L);

        // Test with deterministic seed to guarantee exact reproducible jitter
        Random randomWithSeed = new Random(42L);
        long delayWithJitter = service.calculateDelay(2, 100.0, 2.0, true, randomWithSeed);

        // base = 100 * (2^2) = 400
        // random.nextDouble() with seed 42 will generate a fixed sequence. Let's see the exact value:
        Random verifyRandom = new Random(42L);
        double expectedJitter = verifyRandom.nextDouble() * 30.0;
        long expectedCalculated = Math.round(400.0 + expectedJitter);

        assertThat(delayWithJitter).isEqualTo(expectedCalculated);
    }

    @Test
    public void testFilterAgedAccountsWithFixedClock() {
        // Instantiate the service using our fixed clock
        OutreachAccountService service = new OutreachAccountService(tgAccountRepository, fixedClock);

        // Current time is 2026-07-26T12:00:00Z.
        // Account A: Created 2 months ago (eligible) -> 2026-05-26T12:00:00Z
        // Account B: Created 15 days ago (ineligible) -> 2026-07-11T12:00:00Z

        TGAccount accountA = new TGAccount("+1234567890", "aged_acc", "ACTIVE", "sess1", null, 15, 3, null);
        TGAccount accountB = new TGAccount("+9876543210", "young_acc", "ACTIVE", "sess2", null, 15, 0, null);

        tgAccountRepository.save(accountA);
        tgAccountRepository.save(accountB);

        // Flush so they are inserted in database
        entityManager.flush();

        // We manually update the created_at timestamp in the H2 database to bypass
        // the insertable=false updatable=false constraint on database level.
        jdbcTemplate.update("UPDATE tg_accounts SET created_at = ? WHERE phone_number = ?",
                OffsetDateTime.ofInstant(Instant.parse("2026-05-26T12:00:00.00Z"), ZoneId.of("UTC")), "+1234567890");
        jdbcTemplate.update("UPDATE tg_accounts SET created_at = ? WHERE phone_number = ?",
                OffsetDateTime.ofInstant(Instant.parse("2026-07-11T12:00:00.00Z"), ZoneId.of("UTC")), "+9876543210");

        // Clear persistence context so entities are fully reloaded from database
        entityManager.clear();

        // Execute eligibility check
        List<TGAccount> eligible = service.getEligibleAccounts();

        // Assert only account A is returned
        assertThat(eligible).hasSize(1);
        assertThat(eligible.get(0).getUsername()).isEqualTo("aged_acc");
        assertThat(eligible.get(0).getConsecutiveActions()).isEqualTo(3);
    }

    @Test
    public void testEligibleControllerEndpointWithMockMvc() throws Exception {
        // Save outreach accounts in repository
        TGAccount accountA = new TGAccount("+5555555555", "aged_controller_acc", "ACTIVE", "sess_c1", null, 15, 2, null);
        TGAccount accountB = new TGAccount("+6666666666", "young_controller_acc", "ACTIVE", "sess_c2", null, 15, 0, null);

        tgAccountRepository.save(accountA);
        tgAccountRepository.save(accountB);

        // Flush so they are inserted in database
        entityManager.flush();

        // Manually alter creation times in the H2 DB
        // Account A is eligible (Older than 1 month)
        jdbcTemplate.update("UPDATE tg_accounts SET created_at = ? WHERE phone_number = ?",
                OffsetDateTime.now().minusMonths(2), "+5555555555");
        // Account B is ineligible (Created just now)
        jdbcTemplate.update("UPDATE tg_accounts SET created_at = ? WHERE phone_number = ?",
                OffsetDateTime.now(), "+6666666666");

        // Clear persistence context so entities are fully reloaded from database
        entityManager.clear();

        // Perform GET request with parameters, specifying a fixed seed for reproducible calculations
        mockMvc.perform(get("/api/outreach-accounts/eligible")
                        .param("baseDelay", "100.0")
                        .param("factor", "2.0")
                        .param("useJitter", "true")
                        .param("seed", "12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].phoneNumber", is("+5555555555")))
                .andExpect(jsonPath("$[0].username", is("aged_controller_acc")))
                .andExpect(jsonPath("$[0].consecutiveActions", is(2)))
                .andExpect(jsonPath("$[0].calculatedDelaySeconds").isNumber());
    }
}
