package com.eneik.generated.repository;

import com.eneik.generated.model.Dialog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DialogRepository extends JpaRepository<Dialog, String> {
    Optional<Dialog> findByLeadUsername(String leadUsername);
    Optional<Dialog> findByLeadPhone(String leadPhone);
}
