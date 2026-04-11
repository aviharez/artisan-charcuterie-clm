package com.project.artisancharcuterie.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * An immutable weekly QC inspection record.
 * Once persisted, no field may be altered, enforced at the service layer
 * to satisfy regulatory compliance requirements.
 *
 * <p>The entity has no setter methods exposed in the service/controller layer;
 * all writes go through the {@code QualityControlService} which rejects
 * any update request with a 422 Unprocessable Entity.</p>
 */
@Entity
@Table(name = "quality_control_log",
       indexes = @Index(name = "idx_qc_batch_week", columnList = "batch_id, week_number"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityControlLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    /**
     * Measured pH level of the product surface/interior.
     * Typical safe range: 4.8 - 6.2
     */
    @Column(name = "ph_level", nullable = false, precision = 4, scale = 2)
    private BigDecimal phLevel;

    /** Free-text aroma profile recorded by the inspector. */
    @Column(name = "aroma_profile", nullable = false, length = 500)
    private String aromaProfile;

    /** Name or badge ID of the inspector who filled this log. */
    @Column(name = "inspector", nullable = false, length = 100)
    private String inspector;

    /** Week number within the curing process (1 = first week post-cure-start). */
    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(length = 1000)
    private String notes;

    @Column(name = "logged_at", nullable = false, updatable = false)
    private LocalDateTime loggedAt;

}
