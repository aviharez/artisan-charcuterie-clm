package com.project.artisancharcuterie.dto.request;

import com.project.artisancharcuterie.domain.enums.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Request body for registering a new production batch")
public class BatchCreateRequest {

    @NotNull(message = "Farm ID is required. Batches cannot exist without a valid farm of origin")
    @Positive(message = "Farm ID must be a positive integer")
    @Schema(description = "ID of the registered farm of origin", example = "1")
    private Long farmId;

    @NotNull(message = "Product type is required")
    @Schema(description = "Cured-meat product type", allowableValues = { "PROSCIUTTO", "BRESAOLA", "CULATELLO" })
    private ProductType productType;

    @NotBlank(message = "Animal breed is required")
    @Size(max = 100, message = "Animal breed must not exceed 100 characters")
    @Schema(description = "Specific breed for this batch (may differ from farm's primary breed)", example = "Large White x Landrace")
    private String animalBreed;

    @NotNull(message = "Initial weight is required")
    @DecimalMin(value = "0.1", message = "Initial weight must be at least 0.1 kg")
    @Digits(integer = 7, fraction = 3, message = "Weight must have a most 7 integer and 3 decimal digits")
    @Schema(description = "Initial weight of the batch in kilograms", example = "12.500")
    private BigDecimal initialWeightKg;

    @NotNull(message = "Salt cure start date is required")
    @Schema(description = "Date when the salt cure was applied (ISO-8601)", example = "2026-01-10")
    private LocalDate saltCureStartDate;

    @Min(value = 1, message = "Target aging must be at least 1 month")
    @Max(value = 60, message = "Target aging cannot exceed 60 months")
    @Schema(description = "Target aging duration in months. If omitted, defaults to the product type standard.", example = "24")
    private Integer targetAgingMonths;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    @Schema(description = "Optional artisan notes", example = "Heritage breed, small farm - priority batch")
    private String notes;

}
