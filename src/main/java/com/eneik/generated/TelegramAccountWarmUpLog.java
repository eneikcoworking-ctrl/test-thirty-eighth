package com.eneik.generated;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "telegram_account_warm_up_logs")
public class TelegramAccountWarmUpLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private TelegramAccount account;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public TelegramAccountWarmUpLog() {
    }

    public TelegramAccountWarmUpLog(TelegramAccount account, String action, String status, String details) {
        this.account = account;
        this.action = action;
        this.status = status;
        this.details = details;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TelegramAccount getAccount() {
        return account;
    }

    public void setAccount(TelegramAccount account) {
        this.account = account;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
