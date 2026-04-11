package com.project.artisancharcuterie.domain;

import com.project.artisancharcuterie.domain.enums.ProductType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Current market spot price per kilogram for each product type.
 * The valuation engine multiplies projected current weight by this price.
 * One record per product type; updated via the Inventory controller.
 */
@Entity
@Table(name = "market_spot_prices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketSpotPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable= false, unique = true, length = 20)
    private ProductType productType;

    /** Price per kilogram in the configured currency. */
    @Column(name = "price_per_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerKg;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
