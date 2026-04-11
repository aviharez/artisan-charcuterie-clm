package com.project.artisancharcuterie.dto.response;

import com.project.artisancharcuterie.domain.SensorReading;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Environmental sensor data point for a chamber")
public class SensorReadingResponse {

    private Long id;
    private Long chamberId;
    private String chamberName;
    private BigDecimal temperatureCelsius;
    private BigDecimal humidityPercent;
    private LocalDateTime recordedAt;
    private String sensorId;

    public static SensorReadingResponse from(SensorReading reading) {
        return SensorReadingResponse.builder()
                .id(reading.getId())
                .chamberId(reading.getChamber().getId())
                .chamberName(reading.getChamber().getName())
                .temperatureCelsius(reading.getTemperatureCelsius())
                .humidityPercent(reading.getHumidityPercent())
                .recordedAt(reading.getRecordedAt())
                .sensorId(reading.getSensorId())
                .build();
    }

}
