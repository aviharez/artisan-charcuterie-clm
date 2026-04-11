package com.project.artisancharcuterie.repository;

import com.project.artisancharcuterie.domain.MarketSpotPrice;
import com.project.artisancharcuterie.domain.enums.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MarketSpotPriceRepository extends JpaRepository<MarketSpotPrice, Long> {

    Optional<MarketSpotPrice> findByProductType(ProductType productType);

}
