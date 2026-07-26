package com.eneik.generated.dto;

import java.time.OffsetDateTime;

public class ProxyDTO {
    private Long id;
    private String host;
    private Integer port;
    private String username;
    private String protocol;
    private OffsetDateTime createdAt;

    public ProxyDTO() {
    }

    public ProxyDTO(Long id, String host, Integer port, String username, String protocol, OffsetDateTime createdAt) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.username = username;
        this.protocol = protocol;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
