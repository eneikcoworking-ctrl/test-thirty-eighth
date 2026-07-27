package com.eneik.generated.repository;

import com.eneik.generated.model.Deliverable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliverableRepository extends JpaRepository<Deliverable, String> {

    @Query("SELECT COUNT(d) FROM Deliverable d WHERE UPPER(d.status) IN ('COMPLETED', 'MERGED', 'DONE', 'RESOLVED')")
    long countCompletedDeliverables();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Deliverable d SET d.status = :newStatus WHERE d.id = :id AND d.status = :oldStatus")
    int updateStatusAtomically(
            @Param("id") String id,
            @Param("oldStatus") String oldStatus,
            @Param("newStatus") String newStatus
    );
}
