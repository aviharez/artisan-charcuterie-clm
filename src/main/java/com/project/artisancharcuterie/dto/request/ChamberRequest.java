package com.project.artisancharcuterie.dto.request;

import com.project.artisancharcuterie.domain.enums.ChamberType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Request body for creating or updating a chamber")
public class ChamberRequest {

    @NotBlank(message = "Chamber name is required")
    @Size(max = 150, message = "Chamber name must not exceed 150 characters")
    @Schema(description = "Descriptive name for the chamber", example = "Cold Smoke House A")
    private String name;

    @NotNull(message = "Chamber type is required")
    @Schema(description = "Functional type of the chamber", allowableValues = { "COLD_SMOKE", "FERMENTATION_ROOM", "PRIMARY_AGING_CELLAR" })
    private ChamberType chamberType;

    @NotNull(message = "Target temperature is required")
    @DecimalMin(value = "-10.0", message = "Target temperature must be above -10 C")
    @DecimalMax(value = "40.0", message = "Target temperature must be below 40 C")
    @Schema(description = "Target temperature set-point in Celsius", example = "12.00")
    private BigDecimal targetTemperatureCelsius;

    @NotNull(message = "Target humidity is required")
    @DecimalMin(value = "0.0", message = "Humidity must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "Humidity must be between 0 and 100")
    @Schema(description = "Target relative humidity set-point (%)", example = "70.00")
    private BigDecimal targetHumidityPercent;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Schema(description = "Maximum number of batches this chamber can hold simultaneously", example = "20")
    private Integer capacity;

}
