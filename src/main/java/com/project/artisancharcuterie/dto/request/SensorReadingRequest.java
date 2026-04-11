package com.project.artisancharcuterie.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "Sensor data payload for a chamber reading")
public class SensorReadingRequest {

    @NotNull(message = "Temperature reading is required")
    @DecimalMin(value = "-10.0", message = "Temperature must be above -10 C")
    @DecimalMax(value = "50.0", message = "Temperature must be below 50 C")
    @Digits(integer = 4, fraction = 2, message = "Temperature must have at most 4 integer and 2 decimal digits")
    @Schema(description = "Measured temperature in Celsius", example = "13.50")
    private BigDecimal temperatureCelsius;

    @NotNull(message = "Humidity reading is required")
    @DecimalMin(value = "0.0", message = "Humidity must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "Humidity must be between 0 and 100")
    @Digits(integer = 3, fraction = 2, message = "Humidity must have at most 3 integer and 2 decimal digits")
    @Schema(description = "Measured relative humidity (%)", example = "72.30")
    private BigDecimal humidityPercent;

    @Schema(description = "Timestamp of the reading. Defaults to server time if omitted.", example = "2026-01-10T14:40:00")
    private LocalDateTime recordedAt;

    @Size(max = 50, message = "Sensor ID must not exceed 50 characters")
    @Schema(description = "Identifier of the physical or virtual sensor", example = "SENS-ALPHA-01")
    private String sensorId;

}
