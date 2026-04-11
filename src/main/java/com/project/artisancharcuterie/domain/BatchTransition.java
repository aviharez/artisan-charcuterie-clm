package com.project.artisancharcuterie.domain;

import com.project.artisancharcuterie.domain.enums.BatchStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Immutable audit record of every chamber transition a batch undergoes.
 * Provides a complete chain-of-custody history from raw intake to retail.
 */
@Entity
@Table(name = "batch_transition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    /** Null on first placement (batch entering system from GREEN). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_chamber_id")
    private Chamber fromChamber;

    /** Null when batch transitions to RETAIL_READY (leaves the chamber system). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_chamber_id")
    private Chamber toChamber;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 20)
    private BatchStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private BatchStatus toStatus;

    @Column(name = "transition_date", nullable = false)
    private LocalDateTime transitionDate;

    /** Identity of the butcher / operator who performed the move. */
    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    @Column(length = 500)
    private String notes;
}
