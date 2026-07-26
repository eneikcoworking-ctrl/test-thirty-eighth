package com.eneik.generated;

import com.eneik.generated.dto.*;
import com.eneik.generated.model.Proxy;
import com.eneik.generated.model.TGAccount;
import com.eneik.generated.repository.ProxyRepository;
import com.eneik.generated.repository.TGAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AccountManagementApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProxyRepository proxyRepository;

    @Autowired
    private TGAccountRepository tgAccountRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        tgAccountRepository.deleteAll();
        proxyRepository.deleteAll();
    }

    @Test
    public void testProxyCRUD() throws Exception {
        // 1. Create a proxy
        ProxyCreateDTO createDTO = new ProxyCreateDTO(
                "192.168.1.100",
                1080,
                "user",
                "pass",
                "SOCKS5"
        );

        String responseJson = mockMvc.perform(post("/api/proxies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.host").value("192.168.1.100"))
                .andExpect(jsonPath("$.port").value(1080))
                .andExpect(jsonPath("$.username").value("user"))
                .andExpect(jsonPath("$.protocol").value("SOCKS5"))
                .andReturn().getResponse().getContentAsString();

        ProxyDTO createdProxy = objectMapper.readValue(responseJson, ProxyDTO.class);
        Long proxyId = createdProxy.getId();

        // 2. Get proxy details
        mockMvc.perform(get("/api/proxies/" + proxyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.host").value("192.168.1.100"));

        // 3. List all proxies
        mockMvc.perform(get("/api/proxies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].host").value("192.168.1.100"));

        // 4. Delete the proxy
        mockMvc.perform(delete("/api/proxies/" + proxyId))
                .andExpect(status().isNoContent());

        // 5. Assert deleted
        mockMvc.perform(get("/api/proxies/" + proxyId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Proxy not found with ID: " + proxyId));
    }

    @Test
    public void testOtpOnboardingFlowWithProxyBinding() throws Exception {
        // 1. Register a Proxy first
        Proxy proxy = new Proxy("1.2.3.4", 8080, "proxyuser", "proxypass", "HTTP");
        Proxy savedProxy = proxyRepository.save(proxy);

        // 2. Initiate OTP Onboarding
        OtpInitiateRequest initiateReq = new OtpInitiateRequest("+1234567890");

        String initiateResJson = mockMvc.perform(post("/api/accounts/onboard/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initiateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("+1234567890"))
                .andExpect(jsonPath("$.phoneCodeHash").value("hash_1234567890"))
                .andExpect(jsonPath("$.status").value("SENT"))
                .andReturn().getResponse().getContentAsString();

        OtpInitiateResponse initiateRes = objectMapper.readValue(initiateResJson, OtpInitiateResponse.class);

        // 3. Verify OTP and Bind Proxy
        OtpVerifyRequest verifyReq = new OtpVerifyRequest(
                "+1234567890",
                initiateRes.getPhoneCodeHash(),
                "12345", // valid code
                null, // no 2FA
                savedProxy.getId() // bind proxy during onboarding!
        );

        mockMvc.perform(post("/api/accounts/onboard/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("+1234567890"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.proxy.id").value(savedProxy.getId()))
                .andExpect(jsonPath("$.proxy.host").value("1.2.3.4"))
                .andExpect(jsonPath("$.proxy.port").value(8080))
                .andExpect(jsonPath("$.proxy.protocol").value("HTTP"));

        // Verify state is stored in the database correctly
        Optional<TGAccount> dbAccountOpt = tgAccountRepository.findByPhoneNumber("+1234567890");
        assertThat(dbAccountOpt).isPresent();
        TGAccount dbAccount = dbAccountOpt.get();
        assertThat(dbAccount.getStatus()).isEqualTo("ACTIVE");
        assertThat(dbAccount.getProxy()).isNotNull();
        assertThat(dbAccount.getProxy().getId()).isEqualTo(savedProxy.getId());
        assertThat(dbAccount.getSessionData()).contains("verified_via", "OTP", "hash_1234567890");
    }

    @Test
    public void testOnboardingWithInvalidOtpCodeReturnsBadRequest() throws Exception {
        OtpVerifyRequest verifyReq = new OtpVerifyRequest(
                "+1234567890",
                "somehash",
                "00000", // simulated invalid code
                null,
                null
        );

        mockMvc.perform(post("/api/accounts/onboard/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid or expired OTP verification code"));
    }

    @Test
    public void testUploadSessionOnboardingFlowWithProxyBinding() throws Exception {
        // 1. Register a Proxy
        Proxy proxy = new Proxy("9.9.9.9", 3128, null, null, "HTTP");
        Proxy savedProxy = proxyRepository.save(proxy);

        // 2. Prepare file upload
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "session_phone.session",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "mock-session-file-bytes".getBytes()
        );

        // 3. Upload session file and bind proxy
        mockMvc.perform(multipart("/api/accounts/onboard/upload")
                        .file(file)
                        .param("phoneNumber", "+9876543210")
                        .param("format", "SESSION")
                        .param("proxyId", String.valueOf(savedProxy.getId()))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phoneNumber").value("+9876543210"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.proxy.id").value(savedProxy.getId()))
                .andExpect(jsonPath("$.proxy.host").value("9.9.9.9"));

        // Verify persistence
        Optional<TGAccount> dbAccountOpt = tgAccountRepository.findByPhoneNumber("+9876543210");
        assertThat(dbAccountOpt).isPresent();
        TGAccount dbAccount = dbAccountOpt.get();
        assertThat(dbAccount.getStatus()).isEqualTo("ACTIVE");
        assertThat(dbAccount.getProxy().getId()).isEqualTo(savedProxy.getId());
        assertThat(dbAccount.getSessionData()).contains("verified_via", "FILE_UPLOAD", "session_phone.session");
    }

    @Test
    public void testAccountProxyBindingAndConfigurationUpdates() throws Exception {
        // 1. Create account without proxy
        TGAccount account = new TGAccount("+555555", "test_user", "ACTIVE", "{}", null, 15);
        TGAccount savedAccount = tgAccountRepository.save(account);

        // 2. Create Proxy
        Proxy proxy = new Proxy("5.5.5.5", 1080, null, null, "SOCKS5");
        Proxy savedProxy = proxyRepository.save(proxy);

        // 3. Bind proxy to account
        ProxyBindDTO bindDTO = new ProxyBindDTO(savedProxy.getId());
        mockMvc.perform(put("/api/accounts/" + savedAccount.getId() + "/proxy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bindDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proxy.id").value(savedProxy.getId()))
                .andExpect(jsonPath("$.proxy.host").value("5.5.5.5"));

        // Verify in DB
        TGAccount dbAcc = tgAccountRepository.findById(savedAccount.getId()).orElseThrow();
        assertThat(dbAcc.getProxy().getId()).isEqualTo(savedProxy.getId());

        // 4. Unbind proxy (set to null)
        ProxyBindDTO unbindDTO = new ProxyBindDTO(null);
        mockMvc.perform(put("/api/accounts/" + savedAccount.getId() + "/proxy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unbindDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proxy").isEmpty());

        // Verify in DB
        dbAcc = tgAccountRepository.findById(savedAccount.getId()).orElseThrow();
        assertThat(dbAcc.getProxy()).isNull();

        // 5. Update dailyMessagingLimit
        TGAccountUpdateDTO updateDTO = new TGAccountUpdateDTO(25);
        mockMvc.perform(put("/api/accounts/" + savedAccount.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyLimit").value(25));

        // Verify in DB
        dbAcc = tgAccountRepository.findById(savedAccount.getId()).orElseThrow();
        assertThat(dbAcc.getDailyLimit()).isEqualTo(25);

        // 6. Delete account
        mockMvc.perform(delete("/api/accounts/" + savedAccount.getId()))
                .andExpect(status().isNoContent());

        // Verify deleted from DB
        assertThat(tgAccountRepository.findById(savedAccount.getId())).isNotPresent();
    }
}
