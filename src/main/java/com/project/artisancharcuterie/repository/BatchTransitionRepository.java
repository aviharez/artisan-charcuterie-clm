package com.project.artisancharcuterie.repository;

import com.project.artisancharcuterie.domain.BatchTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchTransitionRepository extends JpaRepository<BatchTransition, Long> {

    List<BatchTransition> findByBatchIdOrderByTransitionDateAsc(Long batchId);

    long countByBatchId(Long batchId);

}
