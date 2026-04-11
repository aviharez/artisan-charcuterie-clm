package com.project.artisancharcuterie.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request body for moving a batch to a new chamber or marking it as Retail-Ready")
public class ChamberTransitionRequest {

    @Schema(description = """
            Target chamber ID. Omit (or set null) ONLY when advancing the batch to
            RETAIL_READY status after completing the aging phase.
            """,
            example = "2")
    private Long targetChamberId;

    @NotBlank(message = "Performed-by field is required for audit trail")
    @Size(max = 100, message = "Performed-by must not exceed 100 characters")
    @Schema(description = "Name or badge ID of the operator performing this transition", example = "M. Rossi - Head Salumiere")
    private String performedBy;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    @Schema(description = "Optional transition notes", example = "Fermentation complete. Aroma check passed, moving to aging cellar")
    private String notes;

}
