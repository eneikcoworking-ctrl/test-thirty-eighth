package com.eneik.generated.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "telegram_dispatch_logs")
public class TelegramDispatchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id", nullable = false)
    private TGAccount account;

    @Column(name = "recipient_username")
    private String recipientUsername;

    @Column(name = "recipient_phone", length = 50)
    private String recipientPhone;

    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt = OffsetDateTime.now();

    public TelegramDispatchLog() {
    }

    public TelegramDispatchLog(TGAccount account, String recipientUsername, String recipientPhone) {
        this.account = account;
        this.recipientUsername = recipientUsername;
        this.recipientPhone = recipientPhone;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TGAccount getAccount() {
        return account;
    }

    public void setAccount(TGAccount account) {
        this.account = account;
    }

    public String getRecipientUsername() {
        return recipientUsername;
    }

    public void setRecipientUsername(String recipientUsername) {
        this.recipientUsername = recipientUsername;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(OffsetDateTime sentAt) {
        this.sentAt = sentAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.sentAt == null) {
            this.sentAt = OffsetDateTime.now();
        }
    }
}
