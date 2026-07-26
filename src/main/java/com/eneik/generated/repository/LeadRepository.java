package com.eneik.generated.repository;

import com.eneik.generated.model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {
    List<Lead> findByTargetListId(Long targetListId);

    List<Lead> findByTargetListIdAndStatusOrderByIdAsc(Long targetListId, String status);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Lead l SET l.status = :newStatus WHERE l.id = :id AND l.status = :oldStatus")
    int updateStatusAtomic(
            @Param("id") Long id,
            @Param("oldStatus") String oldStatus,
            @Param("newStatus") String newStatus);

    long countByStatus(String status);

    long countByStatusNot(String status);
}
