package com.eneik.generated.dto;

public class OtpVerifyRequest {
    private String phoneNumber;
    private String phoneCodeHash;
    private String code;
    private String twoFactorPassword;
    private Long proxyId;

    public OtpVerifyRequest() {
    }

    public OtpVerifyRequest(String phoneNumber, String phoneCodeHash, String code, String twoFactorPassword, Long proxyId) {
        this.phoneNumber = phoneNumber;
        this.phoneCodeHash = phoneCodeHash;
        this.code = code;
        this.twoFactorPassword = twoFactorPassword;
        this.proxyId = proxyId;
    }

    // Getters and Setters
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneCodeHash() {
        return phoneCodeHash;
    }

    public void setPhoneCodeHash(String phoneCodeHash) {
        this.phoneCodeHash = phoneCodeHash;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTwoFactorPassword() {
        return twoFactorPassword;
    }

    public void setTwoFactorPassword(String twoFactorPassword) {
        this.twoFactorPassword = twoFactorPassword;
    }

    public Long getProxyId() {
        return proxyId;
    }

    public void setProxyId(Long proxyId) {
        this.proxyId = proxyId;
    }
}
