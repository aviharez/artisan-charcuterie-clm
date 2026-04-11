package com.project.artisancharcuterie.domain;

import com.project.artisancharcuterie.domain.enums.ChamberType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a physical environmental zone (Cold Smoke, Fermentation Room, Aging Cellar).
 * Each chamber has target temperature and humidity set-points used for stress-alert evaluation.
 */
@Entity
@Table(name = "chambers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chamber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "chamber_type", nullable = false, length = 30)
    private ChamberType chamberType;

    @Column(name = "target_temperature_celsius", nullable = false, precision = 5, scale = 2)
    private BigDecimal targetTemperatureCelsius;

    @Column(name = "target_humidity_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal targetHumidityPercent;

    /** Maximum number of batches this chamber can hold simultaneously. */
    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
