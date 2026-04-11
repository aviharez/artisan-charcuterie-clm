package com.project.artisancharcuterie.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A single environmental sensor data point for a chamber.
 * Readings are append-only; the stress-alert engine queries them
 * to detect sustained out-of-range conditions.
 */
@Entity
@Table(
        name = "sensor_readings",
        indexes = {
                @Index(name = "idx_sensor_chamber_recorded", columnList = "chamber_id, recorded_at"),
                @Index(name = "idx_sensor_recorded_at", columnList = "recorded_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chamber_id", nullable = false)
    private Chamber chamber;

    @Column(name = "temperature_celsius", nullable = false, precision = 6, scale = 2)
    private BigDecimal temperatureCelsius;

    @Column(name = "humidity_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal humidityPercent;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    /** Physical or virtual sensor identifier, e.g. "SENS-ALPHA-01". */
    @Column(name = "sensor_id", length = 50)
    private String sensorId;

}
