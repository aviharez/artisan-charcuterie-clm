package com.project.artisancharcuterie.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Weekly QC inspection log. Immutable once submitted")
public class QualityControlLogRequest {

    @NotNull(message = "pH level is required")
    @DecimalMin(value = "0.0", message = "pH must be between 0.0 and 14.0")
    @DecimalMax(value = "14.0", message = "pH must be between 0.0 and 14.0")
    @Digits(integer = 2, fraction = 2, message = "pH must have at most 2 integer and 2 decimal digits")
    @Schema(description = "Measured pH level of the product surface or interior (safe range: 4.8-6.2)", example = "5.40")
    private BigDecimal phLevel;

    @NotBlank(message = "Aroma profile description is required")
    @Size(max = 500, message = "Aroma profile must not exceed 500 characters")
    @Schema(description = "Free-text aroma profile as assessed by the inspector",
            example = "Nutty, sweet, mild lactic notes. Excellent development")
    private String aromaProfile;

    @NotBlank(message = "Inspector name or ID is required")
    @Size(max = 100, message = "Inspector must not exceed 100 characters")
    @Schema(description = "Name or badge ID of the QC inspector", example = "G. Ferrari (QC Dept.)")
    private String inspector;

    @NotNull(message = "Week number is required")
    @Min(value = 1, message = "Week number must be at least 1")
    @Max(value = 200, message = "Week number cannot exceed 200")
    @Schema(description = "Week number within the curing process (1 = first week post salt-cure)", example = "4")
    private Integer weekNumber;

    @Size(max = 1000, message = "Notes must exceed 1000 characters")
    @Schema(description = "Additional inspector notes or observations",
            example = "Surface mold development is consistent and healty")
    private String notes;

}
