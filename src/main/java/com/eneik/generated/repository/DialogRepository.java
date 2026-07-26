package com.eneik.generated.repository;

import com.eneik.generated.model.Dialog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface DialogRepository extends JpaRepository<Dialog, String> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Dialog d SET d.status = :newStatus, d.updatedAt = CURRENT_TIMESTAMP WHERE d.id = :id AND d.status = :expectedStatus")
    int updateStatusAtomically(@Param("id") String id, @Param("expectedStatus") String expectedStatus, @Param("newStatus") String newStatus);
}
