package com.eneik.generated;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TelegramAccountTrustScoreRepository extends JpaRepository<TelegramAccountTrustScore, Long> {
    List<TelegramAccountTrustScore> findByAccountId(Long accountId);
}
