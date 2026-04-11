package com.project.artisancharcuterie.dto.response;

import com.project.artisancharcuterie.domain.enums.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@Schema(description = "Current market valuation for a single batch based on weight-loss projection")
public class BatchValuationResponse {

    private Long batchId;
    private String batchCode;
    private ProductType productType;
    private LocalDate saltCureStartDate;
    private Integer targetAgingMonths;

    @Schema(description = "Initial weight at intake (kg)")
    private BigDecimal initialWeightKg;

    @Schema(description = "Months elapsed since salt-cure start date")
    private long monthElapsed;

    @Schema(description = "Standard weight-loss percentage for this product type over full aging")
    private double standardWeightLossPercent;

    @Schema(description = "Projected weight-loss percentage applied at current elapsed time (%)")
    private BigDecimal projectedLossPercent;

    @Schema(description = "Estimated current weight after applying projected loss (kg)")
    private BigDecimal estimatedCurrentWeightKg;

    @Schema(description = "Current market spot price per kg")
    private BigDecimal spotPricePerKg;

    @Schema(description = "Currency of the spot price")
    private String currency;

    @Schema(description = "Estimated current market value (estimatedCurrentWeightKg x spotPricePerKg), rounded to 2 decimal places")
    private BigDecimal estimatedMarketValue;

    @Schema(description = "Percentage of the target aging duration that has been completed")
    private BigDecimal agingCompletionPercent;

}
