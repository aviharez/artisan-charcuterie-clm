package com.project.artisancharcuterie.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Request body to update the market spot price for a product type")
public class MarketSpotPriceRequest {

    @NotNull(message = "Price per kg is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 integer and 2 decimal digits")
    @Schema(description = "Current market price per kilogram", example = "92.50")
    private BigDecimal pricePerKg;

    @NotBlank(message = "Currency code is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO 4217 code")
    @Schema(description = "ISO 4217 currency code", example = "USD")
    private String currency;

    @NotNull(message = "Effective date is required")
    @Schema(description = "Date from which this price is effetive (ISO 8601)", example = "2026-01=10")
    private LocalDate effectiveDate;

}
