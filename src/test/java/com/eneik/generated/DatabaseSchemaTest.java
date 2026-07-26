package com.eneik.generated;

import com.eneik.generated.model.Proxy;
import com.eneik.generated.model.TGAccount;
import com.eneik.generated.repository.ProxyRepository;
import com.eneik.generated.repository.TGAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class DatabaseSchemaTest {

    @Autowired
    private ProxyRepository proxyRepository;

    @Autowired
    private TGAccountRepository tgAccountRepository;

    @Test
    public void contextLoads() {
        // Confirms that Spring Boot context starts up correctly,
        // running Flyway migrations and validating JPA mapping definitions
    }

    @Test
    public void testProxyAndTGAccountPersistence() {
        // Create proxy
        Proxy proxy = new Proxy("127.0.0.1", 1080, "proxyuser", "proxypass", "SOCKS5");
        Proxy savedProxy = proxyRepository.save(proxy);
        assertThat(savedProxy.getId()).isNotNull();

        // Create TGAccount with reference to the Proxy
        TGAccount tgAccount = new TGAccount(
                "+1234567890",
                "test_user",
                "ACTIVE",
                "{\"session\":\"some_encrypted_json\"}",
                savedProxy,
                15
        );
        TGAccount savedAccount = tgAccountRepository.save(tgAccount);
        assertThat(savedAccount.getId()).isNotNull();
        assertThat(savedAccount.getProxy()).isNotNull();
        assertThat(savedAccount.getProxy().getId()).isEqualTo(savedProxy.getId());

        // Retrieve and assert properties
        Optional<TGAccount> retrieved = tgAccountRepository.findByPhoneNumber("+1234567890");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getUsername()).isEqualTo("test_user");
        assertThat(retrieved.get().getDailyLimit()).isEqualTo(15);
        assertThat(retrieved.get().getStatus()).isEqualTo("ACTIVE");
    }
}
