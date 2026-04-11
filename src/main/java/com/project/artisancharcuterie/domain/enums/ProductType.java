package com.project.artisancharcuterie.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Supported cured-meat product types with their standard aging targets
 * and weight-loss profiles used in the valuation engine.
 */
@Getter
@RequiredArgsConstructor
public enum ProductType {

    PROSCIUTTO(
            "Prosciutto",
            24,
            30.0,
            "Dry-cured Italian ham, salt-only cure, long aging"
    ),
    BRESAOLA(
            "Bresaola",
            6,
            35.0,
            "Air-dried salted beef eye of round"
    ),
    CULATELLO(
            "Culatello",
            36,
            28.0,
            "Prized cured pork rump, longer fermentation and aging"
    );

    /** Human-readable display name. */
    private final String displayName;

    /** Standard target aging in months for this product type. */
    private final int standardAgingMonths;

    /**
     * Standard total weight-loss percentage over the full aging period.
     * Used as the baseline for the valuation engine's projection formula.
     */
    private final double standardWeightLossPercent;

    /** Short descriptor for documentation/display purpose. */
    private final String description;

}
