package com.project.artisancharcuterie.controller;

import com.project.artisancharcuterie.domain.MarketSpotPrice;
import com.project.artisancharcuterie.domain.enums.ProductType;
import com.project.artisancharcuterie.dto.request.MarketSpotPriceRequest;
import com.project.artisancharcuterie.dto.response.BatchValuationResponse;
import com.project.artisancharcuterie.dto.response.InventoryValuationResponse;
import com.project.artisancharcuterie.service.InventoryValuationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Inventory & Valuation", description = "Weight-loss projection engine and market value estimation")
public class InventoryController {

    private final InventoryValuationService valuationService;

    @GetMapping("/api/inventory/valuation")
    @Operation(summary = "Get aggregate inventory valuation",
               description = "Calculates the estimated current market value of all active aging batches.")
    public ResponseEntity<InventoryValuationResponse> valuateAll() {
        return ResponseEntity.ok(valuationService.valuateAll());
    }

    @GetMapping("/api/batches/{id}/valuation")
    @Operation(summary = "Get the current market valuation for a single batch")
    public ResponseEntity<BatchValuationResponse> valuateBatch(@PathVariable Long id) {
        return ResponseEntity.ok(valuationService.valuateById(id));
    }

    @GetMapping("/api/market-prices")
    @Operation(summary = "List all current market spot prices")
    public ResponseEntity<List<MarketSpotPrice>> listPrices() {
        return ResponseEntity.ok(valuationService.findAllSpotPrices());
    }

    @PutMapping("/api/market-prices/{productType}")
    @Operation(summary = "Update the market spot price for a product type",
               description = "Affects all future valuation calculations immediately.")
    public ResponseEntity<MarketSpotPrice> updatePrice(
            @PathVariable ProductType productType,
            @Valid @RequestBody MarketSpotPriceRequest request) {
        return ResponseEntity.ok(valuationService.updateSpotPrice(productType, request));
    }

}
