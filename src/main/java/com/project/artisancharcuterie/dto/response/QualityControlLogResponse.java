package com.project.artisancharcuterie.dto.response;

import com.project.artisancharcuterie.domain.QualityControlLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Immutable QC inspection log entry")
public class QualityControlLogResponse {

    private Long id;
    private Long batchId;
    private String batchCode;
    private BigDecimal phLevel;
    private String aromaProfile;
    private String inspector;
    private Integer weekNumber;
    private String notes;
    private LocalDateTime loggedAt;

    @Schema(description = "Indicates whether the recorded pH is within the safe sange (4.8-6.2)")
    private boolean phWithinSafeRange;

    public static QualityControlLogResponse from(QualityControlLog log) {
        boolean safe = log.getPhLevel().compareTo(new BigDecimal("4.8")) >= 0 &&
                log.getPhLevel().compareTo(new BigDecimal("6.2")) <= 0;

        return QualityControlLogResponse.builder()
                .id(log.getId())
                .batchId(log.getBatch().getId())
                .batchCode(log.getBatch().getBatchCode())
                .phLevel(log.getPhLevel())
                .aromaProfile(log.getAromaProfile())
                .inspector(log.getInspector())
                .weekNumber(log.getWeekNumber())
                .notes(log.getNotes())
                .loggedAt(log.getLoggedAt())
                .phWithinSafeRange(safe)
                .build();
    }

}
