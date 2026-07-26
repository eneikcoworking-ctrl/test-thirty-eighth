package com.eneik.generated.dto;

import java.time.OffsetDateTime;

public class TGAccountDTO {
    private Long id;
    private String phoneNumber;
    private String username;
    private String status;
    private ProxyDTO proxy;
    private Integer dailyLimit;
    private OffsetDateTime createdAt;

    public TGAccountDTO() {
    }

    public TGAccountDTO(Long id, String phoneNumber, String username, String status, ProxyDTO proxy, Integer dailyLimit, OffsetDateTime createdAt) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.username = username;
        this.status = status;
        this.proxy = proxy;
        this.dailyLimit = dailyLimit;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ProxyDTO getProxy() {
        return proxy;
    }

    public void setProxy(ProxyDTO proxy) {
        this.proxy = proxy;
    }

    public Integer getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(Integer dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
