package com.eneik.generated.controller;

import com.eneik.generated.dto.ProxyCreateDTO;
import com.eneik.generated.dto.ProxyDTO;
import com.eneik.generated.model.Proxy;
import com.eneik.generated.repository.ProxyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/proxies")
public class ProxyController {

    private final ProxyRepository proxyRepository;

    @Autowired
    public ProxyController(ProxyRepository proxyRepository) {
        this.proxyRepository = proxyRepository;
    }

    /**
     * List all configured proxies.
     */
    @GetMapping
    public List<ProxyDTO> getAllProxies() {
        return proxyRepository.findAll().stream()
                .map(this::mapToProxyDTO)
                .collect(Collectors.toList());
    }

    /**
     * Create a new proxy configuration.
     */
    @PostMapping
    public ResponseEntity<ProxyDTO> createProxy(@RequestBody ProxyCreateDTO dto) {
        if (dto == null || dto.getHost() == null || dto.getHost().trim().isEmpty()) {
            throw new IllegalArgumentException("Host is required");
        }
        if (dto.getPort() == null || dto.getPort() <= 0 || dto.getPort() > 65535) {
            throw new IllegalArgumentException("Valid port (1-65535) is required");
        }
        if (dto.getProtocol() == null || (!"SOCKS5".equalsIgnoreCase(dto.getProtocol()) && !"HTTP".equalsIgnoreCase(dto.getProtocol()))) {
            throw new IllegalArgumentException("Protocol must be either SOCKS5 or HTTP");
        }

        Proxy proxy = new Proxy(
                dto.getHost().trim(),
                dto.getPort(),
                dto.getUsername(),
                dto.getPassword(),
                dto.getProtocol().toUpperCase()
        );

        Proxy saved = proxyRepository.save(proxy);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToProxyDTO(saved));
    }

    /**
     * Get details of a proxy.
     */
    @GetMapping("/{id}")
    public ProxyDTO getProxyById(@PathVariable Long id) {
        Proxy proxy = proxyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proxy not found with ID: " + id));
        return mapToProxyDTO(proxy);
    }

    /**
     * Delete a proxy configuration.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProxy(@PathVariable Long id) {
        Proxy proxy = proxyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proxy not found with ID: " + id));
        proxyRepository.delete(proxy);
    }

    private ProxyDTO mapToProxyDTO(Proxy proxy) {
        if (proxy == null) return null;
        return new ProxyDTO(
                proxy.getId(),
                proxy.getHost(),
                proxy.getPort(),
                proxy.getUsername(),
                proxy.getProtocol(),
                proxy.getCreatedAt()
        );
    }
}
