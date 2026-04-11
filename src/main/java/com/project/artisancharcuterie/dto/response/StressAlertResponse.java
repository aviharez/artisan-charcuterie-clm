package com.project.artisancharcuterie.dto.response;

import com.project.artisancharcuterie.domain.enums.ChamberType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "Stress alert raised when a chamber's temperature has been outside the 2 C safety range for more than 4 hours")
public class StressAlertResponse {

    @Schema(description = "Unique alert identifier (chamber ID + timestamp)")
    private String alertId;

    private Long chamberId;
    private String chamberName;
    private ChamberType chamberType;

    @Schema(description = "Configured target temperature for this chamber (C)")
    private BigDecimal targetTemperatureCelsius;

    @Schema(description = "Minimum safe temperature (target - 2 C)")
    private BigDecimal minSafeTemperature;

    @Schema(description = "Maximum safe temperature (target + 2 C)")
    private BigDecimal maxSafeTemperature;

    @Schema(description = "Lowest temperature recorded during the breach window")
    private BigDecimal minObservedTemperature;

    @Schema(description = "Highest temperature recorded during the breach window")
    private BigDecimal maxObservedTemperature;

    @Schema(description = "Timestamp of the first out-of-range reading")
    private LocalDateTime breachStartTime;

    @Schema(description = "Timestamp of the last out-of-range reading in the current streak")
    private LocalDateTime breachEndTime;

    @Schema(description = "Continuous hours the temperature has been out of range")
    private double breachDurationHours;

    @Schema(description = "All batches currently housed in this chamber that are potentially affected")
    private List<AffectedBatch> affectedBatches;

    @Data
    @Builder
    @Schema(description = "Summary of an affected batch within a stress alert")
    public static class AffectedBatch {
        private Long batchId;
        private String batchCode;
        private String productType;
        private String currentStatus;
    }
}
