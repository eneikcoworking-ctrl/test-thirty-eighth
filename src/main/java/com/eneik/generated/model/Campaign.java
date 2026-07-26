package com.eneik.generated.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "spintax_template", columnDefinition = "TEXT")
    private String spintaxTemplate;

    @Column(name = "use_llm_personalization", nullable = false)
    private boolean useLlmPersonalization = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_list_id")
    private TargetList targetList;

    @Column(name = "status", nullable = false)
    private String status = "DRAFT";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Campaign() {}

    public Campaign(String name, String spintaxTemplate, boolean useLlmPersonalization, TargetList targetList) {
        this.name = name;
        this.spintaxTemplate = spintaxTemplate;
        this.useLlmPersonalization = useLlmPersonalization;
        this.targetList = targetList;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpintaxTemplate() {
        return spintaxTemplate;
    }

    public void setSpintaxTemplate(String spintaxTemplate) {
        this.spintaxTemplate = spintaxTemplate;
    }

    public boolean isUseLlmPersonalization() {
        return useLlmPersonalization;
    }

    public void setUseLlmPersonalization(boolean useLlmPersonalization) {
        this.useLlmPersonalization = useLlmPersonalization;
    }

    public TargetList getTargetList() {
        return targetList;
    }

    public void setTargetList(TargetList targetList) {
        this.targetList = targetList;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
