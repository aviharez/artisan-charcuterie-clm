package com.project.artisancharcuterie.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents the lifecycle stage of a meat batch.
 * Transitions are strictly governed by the ChamberTransitionService.
 */
@Getter
@RequiredArgsConstructor
public enum BatchStatus {

    GREEN("Green - Raw Intake", "Batch has been registered; salt cure not yet started"),
    COLD_SMOKING("Cold Smoking", "Batch is in the cold smoke phase"),
    FERMENTING("Fermenting", "Batch is undergoing fermentation, Mandatory before aging."),
    AGING("Aging", "Batch is in primary long-term aging"),
    RETAIL_READY("Retail Ready", "Batch has completed aging and is cleared for sale"),
    REJECTED("Rejected", "Batch has failed QC and has been quarantined/discarded");

    private final String displayName;
    private final String description;

    /** Returns true if the batch can still accept new chamber transitions. */
    public boolean isTransitionable() {
        return this != RETAIL_READY && this != REJECTED;
    }

    /** Returns true if a QC log entry may be appended for this status. */
    public boolean isQcLoggable() {
        return this != GREEN && this != REJECTED;
    }

}
