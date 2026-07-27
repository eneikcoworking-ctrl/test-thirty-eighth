package com.eneik.generated.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "tg_accounts")
public class TGAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    private String username;

    @Column(nullable = false)
    private String status; // e.g. ACTIVE, TEMPORARY_SPAM_BLOCK, PERMANENT_BAN, RE_AUTHORIZATION_REQUIRED

    @Column(name = "session_data", columnDefinition = "TEXT")
    private String sessionData;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "proxy_id")
    private Proxy proxy;

    @Column(name = "daily_limit", nullable = false)
    private Integer dailyLimit = 15;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public TGAccount() {
    }

    public TGAccount(String phoneNumber, String username, String status, String sessionData, Proxy proxy, Integer dailyLimit) {
        this.phoneNumber = phoneNumber;
        this.username = username;
        this.status = status;
        this.sessionData = sessionData;
        this.proxy = proxy;
        this.dailyLimit = dailyLimit;
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

    public String getSessionData() {
        return sessionData;
    }

    public void setSessionData(String sessionData) {
        this.sessionData = sessionData;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public void setProxy(Proxy proxy) {
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
