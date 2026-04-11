package com.project.artisancharcuterie.dto.response;

import com.project.artisancharcuterie.domain.BatchTransition;
import com.project.artisancharcuterie.domain.enums.BatchStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "A single chamber transition event in a batch's history")
public class BatchTransitionResponse {

    private Long id;
    private Long batchId;
    private String batchCode;

    @Schema(description = "Chamber the batch came from. Null on initial placement.")
    private Long fromChamberId;
    private String fromChamberName;

    @Schema(description = "Chamber the batch moved to. Null when transitioning to RETAIL_READY.")
    private Long toChamberId;
    private String toChamberName;

    private BatchStatus fromStatus;
    private String fromStatusDisplayName;
    private BatchStatus toStatus;
    private String toStatusDisplayName;
    private LocalDateTime transitionDate;
    private String performedBy;
    private String notes;

    public static BatchTransitionResponse from(BatchTransition t) {
        return BatchTransitionResponse.builder()
                .id(t.getId())
                .batchId(t.getBatch().getId())
                .batchCode(t.getBatch().getBatchCode())
                .fromChamberId(t.getFromChamber() != null ? t.getFromChamber().getId() : null)
                .fromChamberName(t.getFromChamber() != null ? t.getFromChamber().getName() : null)
                .toChamberId(t.getToChamber() != null ? t.getToChamber().getId() : null)
                .toChamberName(t.getToChamber() != null ? t.getToChamber().getName() : null)
                .fromStatus(t.getFromStatus())
                .fromStatusDisplayName(t.getFromStatus().getDisplayName())
                .toStatus(t.getToStatus())
                .toStatusDisplayName(t.getToStatus().getDisplayName())
                .transitionDate(t.getTransitionDate())
                .performedBy(t.getPerformedBy())
                .notes(t.getNotes())
                .build();
    }
}
