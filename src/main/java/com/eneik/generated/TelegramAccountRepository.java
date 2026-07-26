package com.eneik.generated;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TelegramAccountRepository extends JpaRepository<TelegramAccount, Long> {

    // Filter account readiness: warmed up, active status, and trust score >= minScore
    List<TelegramAccount> findByIsWarmedUpTrueAndStatusAndCurrentTrustScoreGreaterThanEqual(
            String status, Double minScore);

    // Filter by warm-up status and trust score threshold
    List<TelegramAccount> findByIsWarmedUpAndCurrentTrustScoreGreaterThanEqual(
            Boolean isWarmedUp, Double minScore);

    // Filter accounts by status
    List<TelegramAccount> findByStatus(String status);

    // Atomically-guarded status transition to prevent race conditions during concurrent updates
    // clearAutomatically = true ensures Hibernate's persistence context (first-level cache) is synchronized after update
    @Modifying(clearAutomatically = true)
    @Query("UPDATE TelegramAccount a SET a.status = :newStatus, a.updatedAt = :now WHERE a.id = :id AND a.status = :oldStatus")
    int updateStatusAtomic(
            @Param("id") Long id,
            @Param("oldStatus") String oldStatus,
            @Param("newStatus") String newStatus,
            @Param("now") LocalDateTime now);
}
