package com.project.artisancharcuterie.dto.response;

import com.project.artisancharcuterie.domain.Batch;
import com.project.artisancharcuterie.domain.enums.BatchStatus;
import com.project.artisancharcuterie.domain.enums.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Full batch details including current status and chamber placement")
public class BatchResponse {

    private Long id;
    private String batchCode;
    private Long farmId;
    private String farmName;
    private ProductType productType;
    private String productTypeDisplayName;
    private String animalBreed;
    private BigDecimal initialWeightKg;
    private LocalDate saltCureStartDate;
    private Integer targetAgingMonths;
    private BatchStatus currentStatus;
    private String statusDisplayName;

    @Schema(description = "ID of the chamber the batch is currently in. Null when RETAIL_READY")
    private Long currentChamberId;
    private String currentChamberName;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BatchResponse from(Batch batch) {
        return BatchResponse.builder()
                .id(batch.getId())
                .batchCode(batch.getBatchCode())
                .farmId(batch.getFarm().getId())
                .farmName(batch.getFarm().getName())
                .productType(batch.getProductType())
                .productTypeDisplayName(batch.getProductType().getDisplayName())
                .animalBreed(batch.getAnimalBreed())
                .initialWeightKg(batch.getInitialWeightKg())
                .saltCureStartDate(batch.getSaltCureStartDate())
                .targetAgingMonths(batch.getTargetAgingMonths())
                .currentStatus(batch.getCurrentStatus())
                .statusDisplayName(batch.getCurrentStatus().getDisplayName())
                .currentChamberId(batch.getCurrentChamber() != null ? batch.getCurrentChamber().getId() : null)
                .currentChamberName(batch.getCurrentChamber() != null ? batch.getCurrentChamber().getName() : null)
                .notes(batch.getNotes())
                .createdAt(batch.getCreatedAt())
                .updatedAt(batch.getUpdatedAt())
                .build();
    }
}
