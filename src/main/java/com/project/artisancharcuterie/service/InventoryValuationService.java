package com.project.artisancharcuterie.service;

import com.project.artisancharcuterie.domain.Batch;
import com.project.artisancharcuterie.domain.MarketSpotPrice;
import com.project.artisancharcuterie.domain.enums.ProductType;
import com.project.artisancharcuterie.dto.request.MarketSpotPriceRequest;
import com.project.artisancharcuterie.dto.response.BatchValuationResponse;
import com.project.artisancharcuterie.dto.response.InventoryValuationResponse;
import com.project.artisancharcuterie.exception.ResourceNotFoundException;
import com.project.artisancharcuterie.repository.BatchRepository;
import com.project.artisancharcuterie.repository.MarketSpotPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Calculates the estimated current market value of aging inventory.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryValuationService {

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);
    private static final int VALUE_SCALE = 2;

    private final BatchRepository batchRepository;
    private final BatchService batchService;
    private final MarketSpotPriceRepository spotPriceRepository;

    // Inventory-wide valuation
    public InventoryValuationResponse valuateAll() {
        List<Batch> batches = batchRepository.findAllAgingBatches();
        List<BatchValuationResponse> valuations = batches.stream()
                .map(this::valuateBatch)
                .toList();

        BigDecimal total = valuations.stream()
                .map(BatchValuationResponse::getEstimatedMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> valueByType = valuations.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getProductType().getDisplayName(),
                        Collectors.reducing(BigDecimal.ZERO,
                                BatchValuationResponse::getEstimatedMarketValue,
                                BigDecimal::add)
                ));

        Map<String, BigDecimal> weightByType = valuations.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getProductType().getDisplayName(),
                        Collectors.reducing(BigDecimal.ZERO,
                                BatchValuationResponse::getEstimatedCurrentWeightKg,
                                BigDecimal::add)
                ));

        String currency = valuations.isEmpty() ? "USD" : valuations.get(0).getCurrency();

        return InventoryValuationResponse.builder()
                .calculatedAt(LocalDateTime.now())
                .totalActiveBatches(valuations.size())
                .totalEstimatedValue(total.setScale(VALUE_SCALE, RoundingMode.HALF_UP))
                .currency(currency)
                .valueByProductType(valueByType)
                .weightByProductType(weightByType)
                .batchValuations(valuations)
                .build();
    }

    // Single batch valuation
    public BatchValuationResponse valuateById(Long batchId) {
        return valuateBatch(batchService.getBatch(batchId));
    }

    // Market spot price management
    public List<MarketSpotPrice> findAllSpotPrices() {
        return spotPriceRepository.findAll();
    }

    @Transactional
    public MarketSpotPrice updateSpotPrice(ProductType productType, MarketSpotPriceRequest request) {
        MarketSpotPrice price = spotPriceRepository.findByProductType(productType)
                .orElseThrow(() -> new ResourceNotFoundException("MarketSpotPrice", productType.name()));
        price.setPricePerKg(request.getPricePerKg());
        price.setCurrency(request.getCurrency());
        price.setEffectiveDate(request.getEffectiveDate());
        return spotPriceRepository.save(price);
    }

    /**
     * Applies the standardized weight-loss projection formula to a single batch.
     * Accurate to 2 decimal places per the acceptance criteria.
     */
    BatchValuationResponse valuateBatch(Batch batch) {
        ProductType productType = batch.getProductType();
        LocalDate today = LocalDate.now();

        long monthsElapsed = ChronoUnit.MONTHS.between(batch.getSaltCureStartDate(), today);

        double standardLossPercent = productType.getStandardWeightLossPercent();
        int targetMonths = batch.getTargetAgingMonths();

        double monthlyLossRate = standardLossPercent / targetMonths;

        double rawProjectLoss = monthsElapsed * monthlyLossRate;
        double projectedLossPercent = Math.min(rawProjectLoss, standardLossPercent);

        BigDecimal lossPercent = BigDecimal.valueOf(projectedLossPercent)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal retentionFactor = BigDecimal.ONE
                .subtract(lossPercent.divide(BigDecimal.valueOf(100), MC));
        BigDecimal estimatedWeight = batch.getInitialWeightKg()
                .multiply(retentionFactor, MC)
                .setScale(VALUE_SCALE, RoundingMode.HALF_UP);

        MarketSpotPrice spot = spotPriceRepository.findByProductType(productType)
                .orElseThrow(() -> new ResourceNotFoundException("MarketSpotPrice", productType.name()));

        BigDecimal marketValue = estimatedWeight
                .multiply(spot.getPricePerKg(), MC)
                .setScale(VALUE_SCALE, RoundingMode.HALF_UP);

        BigDecimal agingCompletion = monthsElapsed >= targetMonths
                ? BigDecimal.valueOf(100)
                : BigDecimal.valueOf((double) monthsElapsed / targetMonths * 100).setScale(VALUE_SCALE, RoundingMode.HALF_UP);

        return BatchValuationResponse.builder()
                .batchId(batch.getId())
                .batchCode(batch.getBatchCode())
                .productType(productType)
                .saltCureStartDate(batch.getSaltCureStartDate())
                .targetAgingMonths(targetMonths)
                .initialWeightKg(batch.getInitialWeightKg())
                .monthElapsed(monthsElapsed)
                .standardWeightLossPercent(standardLossPercent)
                .projectedLossPercent(lossPercent.setScale(VALUE_SCALE, RoundingMode.HALF_UP))
                .estimatedCurrentWeightKg(estimatedWeight)
                .spotPricePerKg(spot.getPricePerKg())
                .currency(spot.getCurrency())
                .estimatedMarketValue(marketValue)
                .agingCompletionPercent(agingCompletion)
                .build();
    }

}
