package com.eneik.generated.repository;

import com.eneik.generated.model.TelegramDispatchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public interface TelegramDispatchLogRepository extends JpaRepository<TelegramDispatchLog, Long> {

    long countByAccountIdAndSentAtAfter(Long accountId, OffsetDateTime timestamp);
}
