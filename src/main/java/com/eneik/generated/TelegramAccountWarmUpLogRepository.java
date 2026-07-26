package com.eneik.generated;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TelegramAccountWarmUpLogRepository extends JpaRepository<TelegramAccountWarmUpLog, Long> {
    List<TelegramAccountWarmUpLog> findByAccountId(Long accountId);
}
