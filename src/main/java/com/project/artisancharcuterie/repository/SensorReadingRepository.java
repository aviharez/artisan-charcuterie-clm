package com.project.artisancharcuterie.repository;

import com.project.artisancharcuterie.domain.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    List<SensorReading> findByChamberIdOrderByRecordedAtDesc(Long chamberId);

    /** Readings for a chamber within a time window, ordered ascending for streak analysis. */
    @Query("""
            SELECT s FROM SensorReading s
            WHERE s.chamber.id = :chamberId
            AND s.recordedAt >= :from
            ORDER BY s.recordedAt ASC
           """)
    List<SensorReading> findByChamberIdAndRecordedAtAfterOrderByRecordedAtAsc(
            @Param("chamberId") Long chamberId,
            @Param("from") LocalDateTime from
    );

    /** All readings across all chambers from a given point in time (for bulk stress scan). */
    @Query("""
            SELECT s FROM SensorReading s
            WHERE s.recordedAt >= :from
            ORDER BY s.chamber.id ASC, s.recordedAt ASC
           """)
    List<SensorReading> findAllSince(@Param("from") LocalDateTime from);

}
