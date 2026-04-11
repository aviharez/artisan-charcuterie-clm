package com.project.artisancharcuterie.repository;

import com.project.artisancharcuterie.domain.Chamber;
import com.project.artisancharcuterie.domain.enums.ChamberType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChamberRepository extends JpaRepository<Chamber, Long> {

    List<Chamber> findByChamberType(ChamberType chamberType);

    List<Chamber> findByIsActiveTrue();

    @Query("""
            SELECT COUNT(b) FROM Batch b
            WHERE b.currentChamber.id = :chamberId
            AND b.currentStatus NOT IN (
                com.project.artisancharcuterie.domain.enums.BatchStatus.RETAIL_READY,
                com.project.artisancharcuterie.domain.enums.BatchStatus.REJECTED
            )
           """)
    long countActiveBatchesInChamber(@Param("chamberId") Long chamberId);

}
