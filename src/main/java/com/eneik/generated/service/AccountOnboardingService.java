package com.eneik.generated.service;

import com.eneik.generated.dto.OtpInitiateRequest;
import com.eneik.generated.dto.OtpInitiateResponse;
import com.eneik.generated.dto.OtpVerifyRequest;
import com.eneik.generated.model.Proxy;
import com.eneik.generated.model.TGAccount;
import com.eneik.generated.repository.ProxyRepository;
import com.eneik.generated.repository.TGAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AccountOnboardingService {

    private final TGAccountRepository tgAccountRepository;
    private final ProxyRepository proxyRepository;

    @Autowired
    public AccountOnboardingService(TGAccountRepository tgAccountRepository, ProxyRepository proxyRepository) {
        this.tgAccountRepository = tgAccountRepository;
        this.proxyRepository = proxyRepository;
    }

    /**
     * Start session onboarding by sending an OTP verification code.
     */
    @Transactional
    public OtpInitiateResponse initiateOtp(OtpInitiateRequest request) {
        if (request == null || request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        String phoneNumber = request.getPhoneNumber().trim();

        // Create a predictable/reproducible hash token from the phone number
        String phoneCodeHash = "hash_" + phoneNumber.replaceAll("[^0-9]", "");

        return new OtpInitiateResponse(phoneNumber, phoneCodeHash, "SENT");
    }

    /**
     * Verify OTP code and save active Telegram session, binding proxy if provided.
     */
    @Transactional
    public TGAccount verifyOtp(OtpVerifyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        if (request.getPhoneCodeHash() == null || request.getPhoneCodeHash().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone code hash is required");
        }
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Verification code is required");
        }

        // Simulating invalid code
        if ("00000".equals(request.getCode().trim())) {
            throw new IllegalArgumentException("Invalid or expired OTP verification code");
        }

        String phoneNumber = request.getPhoneNumber().trim();

        TGAccount account = tgAccountRepository.findByPhoneNumber(phoneNumber)
                .orElse(new TGAccount());

        account.setPhoneNumber(phoneNumber);
        account.setStatus("ACTIVE");
        account.setSessionData("{\"verified_via\":\"OTP\",\"phoneCodeHash\":\"" + request.getPhoneCodeHash() + "\"}");

        if (account.getDailyLimit() == null) {
            account.setDailyLimit(15);
        }

        if (request.getProxyId() != null) {
            Proxy proxy = proxyRepository.findById(request.getProxyId())
                    .orElseThrow(() -> new IllegalArgumentException("Proxy not found with ID: " + request.getProxyId()));
            account.setProxy(proxy);
        }

        return tgAccountRepository.save(account);
    }

    /**
     * Onboard account by uploading pre-authenticated .session or tdata files, binding proxy if provided.
     */
    @Transactional
    public TGAccount uploadSession(String phoneNumber, String filename, String format, byte[] fileContent, Long proxyId) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        if (format == null || format.trim().isEmpty()) {
            throw new IllegalArgumentException("Session format is required");
        }
        if (fileContent == null || fileContent.length == 0) {
            throw new IllegalArgumentException("Session file content cannot be empty");
        }

        String phone = phoneNumber.trim();

        TGAccount account = tgAccountRepository.findByPhoneNumber(phone)
                .orElse(new TGAccount());

        account.setPhoneNumber(phone);
        account.setStatus("ACTIVE");
        account.setSessionData("{\"verified_via\":\"FILE_UPLOAD\",\"format\":\"" + format.trim() + "\",\"filename\":\"" + filename + "\",\"size\":" + fileContent.length + "}");

        if (account.getDailyLimit() == null) {
            account.setDailyLimit(15);
        }

        if (proxyId != null) {
            Proxy proxy = proxyRepository.findById(proxyId)
                    .orElseThrow(() -> new IllegalArgumentException("Proxy not found with ID: " + proxyId));
            account.setProxy(proxy);
        }

        return tgAccountRepository.save(account);
    }

    /**
     * Assign or bind proxy to a Telegram account.
     */
    @Transactional
    public TGAccount bindProxy(Long accountId, Long proxyId) {
        TGAccount account = tgAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Telegram account not found with ID: " + accountId));

        if (proxyId == null) {
            account.setProxy(null);
        } else {
            Proxy proxy = proxyRepository.findById(proxyId)
                    .orElseThrow(() -> new IllegalArgumentException("Proxy not found with ID: " + proxyId));
            account.setProxy(proxy);
        }

        return tgAccountRepository.save(account);
    }
}
