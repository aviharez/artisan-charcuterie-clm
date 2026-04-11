package com.project.artisancharcuterie.repository;

import com.project.artisancharcuterie.domain.QualityControlLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QualityControlLogRepository extends JpaRepository<QualityControlLog, Long> {

    List<QualityControlLog> findByBatchIdOrderByWeekNumberAsc(Long batchId);

    boolean existsByBatchIdAndWeekNumber(Long batchId, Integer weekNumber);

}
