package com.project.artisancharcuterie.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Environmental zone types within the salumeria.
 * The ordering reflects the expected curing workflow.
 */
@Getter
@RequiredArgsConstructor
public enum ChamberType {

    COLD_SMOKE(
            "Cold Smoke",
            "Initial smoe application; 10-14 C, 65-75% RH",
            12.0,
            70.0
    ),
    FERMENTATION_ROOM(
            "Fermentation Room",
            "Controlled lacto-fermentation; 16-20 C, 80-90% RH",
            18.0,
            85.0
    ),
    PRIMARY_AGING_CELLAR(
            "Primary Aging Cellar",
            "Long-term aging under stable conditions; 12-16 C, 72-78% RH",
            14.0,
            75.0
    );

    private final String displayName;
    private final String description;

    /** Default target temperature in Celsius. */
    private final double defaultTargetTemperature;

    /** Default target humidity percentage */
    private final double defaultTargetHumidity;

}
