package com.project.artisancharcuterie.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@Schema(description = "Aggregate inventory valuation across all active aging batches")
public class InventoryValuationResponse {

    @Schema(description = "Timestamp of this valuation snapshot")
    private LocalDateTime calculatedAt;

    @Schema(description = "Total number of active batches included in this valuation")
    private int totalActiveBatches;

    @Schema(description = "Sum of all estimated market values across all active batches")
    private BigDecimal totalEstimatedValue;

    @Schema(description = "Currency of the valuation")
    private String currency;

    @Schema(description = "Estimated total value grouped by product type")
    private Map<String, BigDecimal> valueByProductType;

    @Schema(description = "Estimated total current weight in kilograms, grouped by product type")
    private Map<String, BigDecimal> weightByProductType;

    @Schema(description = "Individual batch valuations")
    private List<BatchValuationResponse> batchValuations;

}
