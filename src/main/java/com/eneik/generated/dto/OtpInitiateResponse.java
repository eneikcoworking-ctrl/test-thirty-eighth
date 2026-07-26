package com.eneik.generated.dto;

public class OtpInitiateResponse {
    private String phoneNumber;
    private String phoneCodeHash;
    private String status;

    public OtpInitiateResponse() {
    }

    public OtpInitiateResponse(String phoneNumber, String phoneCodeHash, String status) {
        this.phoneNumber = phoneNumber;
        this.phoneCodeHash = phoneCodeHash;
        this.status = status;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
