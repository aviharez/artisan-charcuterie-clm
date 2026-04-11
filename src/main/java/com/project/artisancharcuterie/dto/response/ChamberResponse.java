package com.project.artisancharcuterie.dto.response;

import com.project.artisancharcuterie.domain.Chamber;
import com.project.artisancharcuterie.domain.enums.ChamberType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Chamber details with current occupancy")
public class ChamberResponse {

    private Long id;
    private String name;
    private ChamberType chamberType;
    private String chamberTypeDisplayName;
    private BigDecimal targetTemperatureCelsius;
    private BigDecimal targetHumidityPercent;
    private Integer capacity;
    private boolean active;
    private LocalDateTime createdAt;

    @Schema(description = "Number of active batches currently housed in this chamber")
    private Long activeBatchCount;

    public static ChamberResponse from(Chamber chamber, long activeBatchCount) {
        return ChamberResponse.builder()
                .id(chamber.getId())
                .name(chamber.getName())
                .chamberType(chamber.getChamberType())
                .chamberTypeDisplayName(chamber.getChamberType().getDisplayName())
                .targetTemperatureCelsius(chamber.getTargetTemperatureCelsius())
                .targetHumidityPercent(chamber.getTargetHumidityPercent())
                .capacity(chamber.getCapacity())
                .active(chamber.isActive())
                .createdAt(chamber.getCreatedAt())
                .activeBatchCount(activeBatchCount)
                .build();
    }
}
