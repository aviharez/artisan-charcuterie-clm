package com.project.artisancharcuterie.repository;

import com.project.artisancharcuterie.domain.Batch;
import com.project.artisancharcuterie.domain.enums.BatchStatus;
import com.project.artisancharcuterie.domain.enums.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {

    Optional<Batch> findByBatchCode(String batchCode);

    boolean existsByBatchCode(String batchCode);

    List<Batch> findByCurrentStatus(BatchStatus status);

    List<Batch> findByProductType(ProductType productType);

    List<Batch> findByFarmId(Long farmId);

    /** All active (non-terminal) batches currently housed in a given chamber. */
    @Query("""
           SELECT b from Batch b
           WHERE b.currentChamber.id = :chamberId
           AND b.currentStatus NOT IN (
                com.project.artisancharcuterie.domain.enums.BatchStatus.RETAIL_READY,
                com.project.artisancharcuterie.domain.enums.BatchStatus.REJECTED
           )
           """)
    List<Batch> findActiveBatchesInChamber(@Param("chamberId") Long chamberId);

    /** All batches eligible for valuation (not yet retail or rejected). */
    @Query("""
            SELECT b FROM Batch b
            WHERE b.currentStatus NOT IN (
                   com.project.artisancharcuterie.domain.enums.BatchStatus.RETAIL_READY,
                   com.project.artisancharcuterie.domain.enums.BatchStatus.REJECTED
            )
           """)
    List<Batch> findAllAgingBatches();

}
