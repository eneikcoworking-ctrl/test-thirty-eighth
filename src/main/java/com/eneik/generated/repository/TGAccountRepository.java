package com.eneik.generated.repository;

import com.eneik.generated.model.TGAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TGAccountRepository extends JpaRepository<TGAccount, Long> {
    Optional<TGAccount> findByPhoneNumber(String phoneNumber);

    // Database-level query for Kano: Performance to filter accounts older than 1 month
    List<TGAccount> findAllByCreatedAtBefore(OffsetDateTime dateTime);
}
