package com.eneik.generated;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "telegram_accounts")
public class TelegramAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "username")
    private String username;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "proxy")
    private String proxy;

    @Column(name = "current_trust_score")
    private Double currentTrustScore = 0.0;

    @Column(name = "is_warmed_up")
    private Boolean isWarmedUp = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Default Constructor
    public TelegramAccount() {
    }

    // All-args-like Constructor
    public TelegramAccount(String phoneNumber, String username, String status, String proxy, Double currentTrustScore, Boolean isWarmedUp) {
        this.phoneNumber = phoneNumber;
        this.username = username;
        this.status = status;
        this.proxy = proxy;
        this.currentTrustScore = currentTrustScore;
        this.isWarmedUp = isWarmedUp;
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

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public Double getCurrentTrustScore() {
        return currentTrustScore;
    }

    public void setCurrentTrustScore(Double currentTrustScore) {
        this.currentTrustScore = currentTrustScore;
    }

    public Boolean getWarmedUp() {
        return isWarmedUp;
    }

    public void setWarmedUp(Boolean warmedUp) {
        isWarmedUp = warmedUp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
