package com.eneik.generated.repository;

import com.eneik.generated.model.TGAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TGAccountRepository extends JpaRepository<TGAccount, Long> {
    Optional<TGAccount> findByPhoneNumber(String phoneNumber);
}
