package com.eneik.generated.controller;

import com.eneik.generated.dto.*;
import com.eneik.generated.model.Proxy;
import com.eneik.generated.model.TGAccount;
import com.eneik.generated.repository.TGAccountRepository;
import com.eneik.generated.service.AccountOnboardingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
public class AccountOnboardingController {

    private final AccountOnboardingService accountOnboardingService;
    private final TGAccountRepository tgAccountRepository;

    @Autowired
    public AccountOnboardingController(AccountOnboardingService accountOnboardingService, TGAccountRepository tgAccountRepository) {
        this.accountOnboardingService = accountOnboardingService;
        this.tgAccountRepository = tgAccountRepository;
    }

    /**
     * List all Telegram accounts, optionally filtered by status.
     */
    @GetMapping
    public List<TGAccountDTO> getAllAccounts(@RequestParam(value = "status", required = false) String status) {
        List<TGAccount> accounts = tgAccountRepository.findAll();
        if (status != null && !status.trim().isEmpty()) {
            String statusUpper = status.trim().toUpperCase();
            accounts = accounts.stream()
                    .filter(a -> statusUpper.equals(a.getStatus()))
                    .collect(Collectors.toList());
        }
        return accounts.stream()
                .map(this::mapToTGAccountDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get details of a specific Telegram account.
     */
    @GetMapping("/{id}")
    public TGAccountDTO getAccountById(@PathVariable Long id) {
        TGAccount account = tgAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + id));
        return mapToTGAccountDTO(account);
    }

    /**
     * Update a Telegram account's configuration (daily Messaging limit).
     */
    @PutMapping("/{id}")
    public TGAccountDTO updateAccount(@PathVariable Long id, @RequestBody TGAccountUpdateDTO dto) {
        if (dto == null || dto.getDailyLimit() == null || dto.getDailyLimit() <= 0) {
            throw new IllegalArgumentException("Daily Messaging limit must be a positive integer");
        }

        TGAccount account = tgAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + id));

        account.setDailyLimit(dto.getDailyLimit());
        TGAccount saved = tgAccountRepository.save(account);
        return mapToTGAccountDTO(saved);
    }

    /**
     * Log out and delete Telegram account session.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@PathVariable Long id) {
        TGAccount account = tgAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + id));
        tgAccountRepository.delete(account);
    }

    /**
     * Assign or bind proxy to a Telegram account.
     */
    @PutMapping("/{id}/proxy")
    public TGAccountDTO bindProxy(@PathVariable Long id, @RequestBody ProxyBindDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        TGAccount account = accountOnboardingService.bindProxy(id, dto.getProxyId());
        return mapToTGAccountDTO(account);
    }

    /**
     * Initiate OTP session onboarding.
     */
    @PostMapping("/onboard/initiate")
    public OtpInitiateResponse initiateOtp(@RequestBody OtpInitiateRequest request) {
        return accountOnboardingService.initiateOtp(request);
    }

    /**
     * Verify OTP code and finalize onboarding.
     */
    @PostMapping("/onboard/verify")
    public TGAccountDTO verifyOtp(@RequestBody OtpVerifyRequest request) {
        TGAccount account = accountOnboardingService.verifyOtp(request);
        return mapToTGAccountDTO(account);
    }

    /**
     * Upload pre-authenticated session files.
     */
    @PostMapping(value = "/onboard/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TGAccountDTO> uploadSession(
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam("file") MultipartFile file,
            @RequestParam("format") String format,
            @RequestParam(value = "proxyId", required = false) Long proxyId) throws Exception {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or missing");
        }

        byte[] fileBytes = file.getBytes();
        String filename = file.getOriginalFilename();

        TGAccount account = accountOnboardingService.uploadSession(phoneNumber, filename, format, fileBytes, proxyId);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToTGAccountDTO(account));
    }

    private TGAccountDTO mapToTGAccountDTO(TGAccount account) {
        if (account == null) return null;
        return new TGAccountDTO(
                account.getId(),
                account.getPhoneNumber(),
                account.getUsername(),
                account.getStatus(),
                mapToProxyDTO(account.getProxy()),
                account.getDailyLimit(),
                account.getCreatedAt()
        );
    }

    private ProxyDTO mapToProxyDTO(Proxy proxy) {
        if (proxy == null) return null;
        return new ProxyDTO(
                proxy.getId(),
                proxy.getHost(),
                proxy.getPort(),
                proxy.getUsername(),
                proxy.getProtocol(),
                proxy.getCreatedAt()
        );
    }
}
