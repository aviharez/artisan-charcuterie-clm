package com.project.artisancharcuterie.service;

import com.project.artisancharcuterie.domain.Batch;
import com.project.artisancharcuterie.domain.Farm;
import com.project.artisancharcuterie.domain.MarketSpotPrice;
import com.project.artisancharcuterie.domain.enums.BatchStatus;
import com.project.artisancharcuterie.domain.enums.ProductType;
import com.project.artisancharcuterie.dto.response.BatchValuationResponse;
import com.project.artisancharcuterie.dto.response.InventoryValuationResponse;
import com.project.artisancharcuterie.repository.BatchRepository;
import com.project.artisancharcuterie.repository.MarketSpotPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryValuationService - weight-loss projection formula")
public class InventoryValuationServiceTest {

    @Mock private BatchRepository batchRepository;
    @Mock private BatchService batchService;
    @Mock private MarketSpotPriceRepository spotPriceRepository;

    @InjectMocks
    private InventoryValuationService valuationService;

    private Farm farm;
    private MarketSpotPrice prosciuttoPrice;
    private MarketSpotPrice bresaolaPrice;
    private MarketSpotPrice culatelloPrice;

    @BeforeEach
    void setUp() {
        farm = Farm.builder().id(1L).name("Test Farm").location("Italy").breed("Mixed").build();

        prosciuttoPrice = MarketSpotPrice.builder()
                .id(1L).productType(ProductType.PROSCIUTTO)
                .pricePerKg(new BigDecimal("85.00")).currency("USD")
                .effectiveDate(LocalDate.now()).build();

        bresaolaPrice = MarketSpotPrice.builder()
                .id(2L).productType(ProductType.BRESAOLA)
                .pricePerKg(new BigDecimal("95.00")).currency("USD")
                .effectiveDate(LocalDate.now()).build();

        culatelloPrice = MarketSpotPrice.builder()
                .id(3L).productType(ProductType.CULATELLO)
                .pricePerKg(new BigDecimal("145.00")).currency("USD")
                .effectiveDate(LocalDate.now()).build();
    }

    // Formula accuracy tests

    @Nested
    @DisplayName("Prosciutto weight-loss projection")
    class ProsciuttoVvaluation {

        @Test
        @DisplayName("12 months into 24-month Prosciutto: 15% projected loss, correct market value")
        void halfwayProsciuttoValuation() {
            Batch batch = buildBatch(ProductType.PROSCIUTTO,
                    new BigDecimal("10.000"),
                    LocalDate.now().minusMonths(12),
                    24);

            when(spotPriceRepository.findByProductType(ProductType.PROSCIUTTO))
                    .thenReturn(Optional.of(prosciuttoPrice));

            BatchValuationResponse result = valuationService.valuateBatch(batch);

            assertThat(result.getMonthElapsed()).isEqualTo(12L);
            assertThat(result.getProjectedLossPercent()).isEqualByComparingTo(new BigDecimal("15.00"));
            assertThat(result.getEstimatedCurrentWeightKg()).isEqualByComparingTo(new BigDecimal("8.50"));
            assertThat(result.getEstimatedMarketValue()).isEqualByComparingTo(new BigDecimal("722.50"));
        }

        @Test
        @DisplayName("24 months into 24-month Porsciutto: full 30% loss applied")
        void fullAgingProsciuttoValuation() {
            Batch batch = buildBatch(ProductType.PROSCIUTTO,
                    new BigDecimal("10.000"),
                    LocalDate.now().minusMonths(24),
                    24);

            when(spotPriceRepository.findByProductType(ProductType.PROSCIUTTO)).thenReturn(Optional.of(prosciuttoPrice));

            BatchValuationResponse result = valuationService.valuateBatch(batch);

            assertThat(result.getProjectedLossPercent()).isEqualByComparingTo(new BigDecimal("30.00"));
            assertThat(result.getEstimatedCurrentWeightKg()).isEqualByComparingTo(new BigDecimal("7.00"));
            assertThat(result.getEstimatedMarketValue()).isEqualByComparingTo(new BigDecimal("595.00"));
        }

        @Test
        @DisplayName("36 months into 24-month Proscuitto: lose capped at standard 30%")
        void overAgedProsciuttoCapsAtStandardLoss() {
            Batch batch = buildBatch(ProductType.PROSCIUTTO,
                    new BigDecimal("10.000"),
                    LocalDate.now().minusMonths(36),
                    24);

            when(spotPriceRepository.findByProductType(ProductType.PROSCIUTTO))
                    .thenReturn(Optional.of(prosciuttoPrice));

            BatchValuationResponse result = valuationService.valuateBatch(batch);

            assertThat(result.getProjectedLossPercent()).isEqualByComparingTo(new BigDecimal("30.00"));
            assertThat(result.getEstimatedCurrentWeightKg()).isEqualByComparingTo(new BigDecimal("7.00"));
        }
    }

    @Nested
    @DisplayName("Bresaola weight-loss projection")
    class BresaolaValuation {

        @Test
        @DisplayName("3 months into 6-months Bresaola: 17.5% projected loss")
        void halfWayBresaolaValuation() {
            Batch batch = buildBatch(ProductType.BRESAOLA,
                    new BigDecimal("8.000"),
                    LocalDate.now().minusMonths(3),
                    6);

            when(spotPriceRepository.findByProductType(ProductType.BRESAOLA)).thenReturn(Optional.of(bresaolaPrice));

            BatchValuationResponse result = valuationService.valuateBatch(batch);

            assertThat(result.getMonthElapsed()).isEqualTo(3L);
            assertThat(result.getProjectedLossPercent()).isEqualByComparingTo(new BigDecimal("17.50"));
            assertThat(result.getEstimatedCurrentWeightKg()).isEqualByComparingTo(new BigDecimal("6.60"));
            assertThat(result.getEstimatedMarketValue()).isEqualByComparingTo(new BigDecimal("627.00"));
        }
    }

    @Nested
    @DisplayName("Culatello weight-loss projection")
    class CulatelloValuation {

        @Test
        @DisplayName("18 months into 36-month Culatello: 14% projected loss")
        void halfwayCulatelloValuation() {
            Batch batch = buildBatch(ProductType.CULATELLO,
                    new BigDecimal("15.000"),
                    LocalDate.now().minusMonths(18),
                    36);

            when(spotPriceRepository.findByProductType(ProductType.CULATELLO)).thenReturn(Optional.of(culatelloPrice));

            BatchValuationResponse result = valuationService.valuateBatch(batch);

            assertThat(result.getMonthElapsed()).isEqualTo(18L);
            assertThat(result.getProjectedLossPercent()).isEqualByComparingTo(new BigDecimal("14.00"));
            assertThat(result.getEstimatedCurrentWeightKg()).isEqualByComparingTo(new BigDecimal("12.90"));
            assertThat(result.getEstimatedMarketValue()).isEqualByComparingTo(new BigDecimal("1870.50"));
        }
    }

    // Aging completion percentage

    @Nested
    @DisplayName("Aging completion percentage calculation")
    class AgingCompletion {

        @Test
        @DisplayName("50% through aging shows 50.00% completion")
        void halfwaysBatchShows50PercentCompletion() {
            Batch batch = buildBatch(ProductType.PROSCIUTTO,
                    new BigDecimal("10.000"),
                    LocalDate.now().minusMonths(12),
                    24);

            when(spotPriceRepository.findByProductType(any()))
                    .thenReturn(Optional.of(prosciuttoPrice));

            BatchValuationResponse result = valuationService.valuateBatch(batch);

            assertThat(result.getAgingCompletionPercent()).isEqualByComparingTo(new BigDecimal("50.00"));
        }

        @Test
        @DisplayName("Batch past target shows 100% completion")
        void overAgedBatchShows100PercentCompletion() {
            Batch batch = buildBatch(ProductType.PROSCIUTTO,
                    new BigDecimal("10.000"),
                    LocalDate.now().minusMonths(30),
                    24);

            when(spotPriceRepository.findByProductType(any())).thenReturn(Optional.of(prosciuttoPrice));

            BatchValuationResponse result = valuationService.valuateBatch(batch);

            assertThat(result.getAgingCompletionPercent()).isEqualByComparingTo(new BigDecimal("100"));
        }
    }

    // Aggregate inventory valuation

    @Nested
    @DisplayName("Aggregate inventory valuation")
    class AggregateValuation {

        @Test
        @DisplayName("Total inventory value sums individual batch values correctly")
        void inventoryTotalSumsAllBatches() {
            Batch b1 = buildBatch(ProductType.PROSCIUTTO, new BigDecimal("10.000"), LocalDate.now().minusMonths(12), 24);
            b1.setId(1L);
            b1.setBatchCode("PRO-2024-001");

            Batch b2 = buildBatch(ProductType.BRESAOLA, new BigDecimal("8.000"), LocalDate.now().minusMonths(3), 6);
            b2.setId(2L);
            b2.setBatchCode("BRS-2024-001");

            when(batchRepository.findAllAgingBatches()).thenReturn(List.of(b1, b2));
            when(spotPriceRepository.findByProductType(ProductType.PROSCIUTTO)).thenReturn(Optional.of(prosciuttoPrice));
            when(spotPriceRepository.findByProductType(ProductType.BRESAOLA)).thenReturn(Optional.of(bresaolaPrice));

            InventoryValuationResponse result = valuationService.valuateAll();

            assertThat(result.getTotalActiveBatches()).isEqualTo(2);
            assertThat(result.getTotalEstimatedValue()).isEqualByComparingTo("1349.50");
            assertThat(result.getValueByProductType()).hasSize(2);
        }

        @Test
        @DisplayName("Empty inventory return zero total value")
        void emptyInventoryReturnsZero() {
            when(batchRepository.findAllAgingBatches()).thenReturn(List.of());

            InventoryValuationResponse result = valuationService.valuateAll();

            assertThat(result.getTotalActiveBatches()).isZero();
            assertThat(result.getTotalEstimatedValue()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // Helper

    private Batch buildBatch(ProductType type, BigDecimal initialWeight, LocalDate cureStart, int targetMonths) {
        return Batch.builder()
                .id(99L).batchCode("TEST-001").farm(farm)
                .productType(type).animalBreed("Test Breed")
                .initialWeightKg(initialWeight)
                .saltCureStartDate(cureStart)
                .targetAgingMonths(targetMonths)
                .currentStatus(BatchStatus.AGING)
                .build();
    }

}
