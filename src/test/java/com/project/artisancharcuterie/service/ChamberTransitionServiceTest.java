package com.project.artisancharcuterie.service;

import com.project.artisancharcuterie.domain.Batch;
import com.project.artisancharcuterie.domain.BatchTransition;
import com.project.artisancharcuterie.domain.Chamber;
import com.project.artisancharcuterie.domain.Farm;
import com.project.artisancharcuterie.domain.enums.BatchStatus;
import com.project.artisancharcuterie.domain.enums.ChamberType;
import com.project.artisancharcuterie.domain.enums.ProductType;
import com.project.artisancharcuterie.dto.request.ChamberTransitionRequest;
import com.project.artisancharcuterie.dto.response.BatchTransitionResponse;
import com.project.artisancharcuterie.exception.IllegalTransitionException;
import com.project.artisancharcuterie.repository.BatchTransitionRepository;
import com.project.artisancharcuterie.repository.ChamberRepository;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChamberTransitionService - workflow enforcement rules")
class ChamberTransitionServiceTest {

    @Mock private BatchService batchService;
    @Mock private ChamberService chamberService;
    @Mock private BatchTransitionRepository transitionRepository;
    @Mock private ChamberRepository chamberRepository;

    @InjectMocks
    private ChamberTransitionService transitionService;

    private Farm farm;
    private Batch greenBatch;
    private Chamber coldSmokeChamber;
    private Chamber fermentationChamber;
    private Chamber agingCellar;

    @BeforeEach
    void setUp() {
        farm = Farm.builder().id(1L).name("Parma Heritage Farm")
                .location("Italy").breed("Large White").build();

        greenBatch = Batch.builder()
                .id(10L).batchCode("PRO-2024-001").farm(farm)
                .productType(ProductType.PROSCIUTTO).animalBreed("Large White")
                .initialWeightKg(new BigDecimal("12.500"))
                .saltCureStartDate(LocalDate.now().minusMonths(30))
                .targetAgingMonths(24).currentStatus(BatchStatus.GREEN)
                .build();

        coldSmokeChamber = buildChamber(1L, "Cold Smoke A", ChamberType.COLD_SMOKE);
        fermentationChamber = buildChamber(2L, "Fermentation 1", ChamberType.FERMENTATION_ROOM);
        agingCellar = buildChamber(3L, "Aging Cellar Alpha", ChamberType.PRIMARY_AGING_CELLAR);
    }

    // GREEN -> COLD SMOKE

    @Nested
    @DisplayName("GREEN -> COLD_SMOKE transitions")
    class GreenToColdSmoke {

        @Test
        @DisplayName("GREEN batch can enter Cold Smoke chamber")
        void greenBatchCanEnterColdSmoke() {
            setupChamberMocks(1L, coldSmokeChamber, 0L);
            when(batchService.getBatch(10L)).thenReturn(greenBatch);
            when(transitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(batchService.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BatchTransitionResponse result = transitionService.transition(10L, buildRequest(1L, "Operator A"));

            assertThat(result.getToStatus()).isEqualTo(BatchStatus.COLD_SMOKING);
            assertThat(greenBatch.getCurrentStatus()).isEqualTo(BatchStatus.COLD_SMOKING);
        }
    }

    // GREEN -> FERMENTATION (skip cold smoke)

    @Nested
    @DisplayName("GREEN -> FERMENTATION_ROOM (direct, for products skipping cold smoke)")
    class GreenToFermentation {

        @Test
        @DisplayName("GREEN batch can skip cold smoke and enter Fermentation directly")
        void greenBatchCanEnterFermentationDirectly() {
            setupChamberMocks(2L, fermentationChamber, 0L);
            when(batchService.getBatch(10L)).thenReturn(greenBatch);
            when(transitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(batchService.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BatchTransitionResponse result = transitionService.transition(10L, buildRequest(2L, "Operator A"));

            assertThat(result.getToStatus()).isEqualTo(BatchStatus.FERMENTING);
        }
    }

    // Illegal: GREEN -> AGING CELLAR

    @Nested
    @DisplayName("Illegal transitions - must throw IllegalTransitionException")
    class IllegalTransitions {

        @Test
        @DisplayName("GREEN batch CANNOT jump directly to Aging Cellar")
        void greenBatchCannotJumpToAgingCellar() {
            setupChamberMocks(3L, agingCellar, 0L);
            when(batchService.getBatch(10L)).thenReturn(greenBatch);

            assertThatThrownBy(() ->
                    transitionService.transition(10L, buildRequest(3L, "Operator A")))
                    .isInstanceOf(IllegalTransitionException.class)
                    .hasMessageContaining("Fermentation");
        }

        @Test
        @DisplayName("COLD_SMOKING batch CANNOT jump directly to Aging Cellar")
        void coldSmokingBatchCannotJumpToAginCellar() {
            Batch smokingBatch = withStatus(greenBatch, BatchStatus.COLD_SMOKING, coldSmokeChamber);
            setupChamberMocks(3L, agingCellar, 0L);
            when(batchService.getBatch(10L)).thenReturn(smokingBatch);

            assertThatThrownBy(() ->
                    transitionService.transition(10L, buildRequest(3L, "Operator A")))
                    .isInstanceOf(IllegalTransitionException.class)
                    .hasMessageContaining("Fermentation");
        }

        @Test
        @DisplayName("RETAIL_READY batch CANNOT be transitioned (terminal state)")
        void retailReadyBatchCannotBeTransitioned() {
            Batch retailBatch = withStatus(greenBatch, BatchStatus.RETAIL_READY, null);
            when(batchService.getBatch(10L)).thenReturn(retailBatch);

            assertThatThrownBy(() ->
                    transitionService.transition(10L, buildRequest(3L, "Operator A")))
                    .isInstanceOf(IllegalTransitionException.class);
        }

        @Test
        @DisplayName("REJECTED batch CANNOT be transitioned (terminal state)")
        void rejectedBatchCannotBeTransitioned() {
            Batch rejectedBatch = withStatus(greenBatch, BatchStatus.REJECTED, null);
            when(batchService.getBatch(10L)).thenReturn(rejectedBatch);

            assertThatThrownBy(() ->
                    transitionService.transition(10L, buildRequest(3L, "Operator A")))
                    .isInstanceOf(IllegalTransitionException.class);
        }

        @Test
        @DisplayName("GREEN batch CANNOT enter a second Cold Smoke after any phase change")
        void cannotReEnterColdSmokeAfterFermentation() {
            Batch fermentingBatch = withStatus(greenBatch, BatchStatus.FERMENTING, fermentationChamber);
            setupChamberMocks(1L, coldSmokeChamber, 0L);
            when(batchService.getBatch(10L)).thenReturn(fermentingBatch);

            assertThatThrownBy(() ->
                    transitionService.transition(10L, buildRequest(1L, "Operator A")))
                    .isInstanceOf(IllegalTransitionException.class)
                    .hasMessageContaining("GREEN");
        }
    }

    // FERMENTING -> AGING - minimum fermentation duration

    @Nested
    @DisplayName("FERMENTING -> AGING - fermentation duration enforcement")
    class FermentationDuration {

        @Test
        @DisplayName("Batch that has fermented for 80h CAN move to Aging Cellar")
        void sufficientFermentationAllowsAgingTransition() {
            Batch fermentingBatch = withStatus(greenBatch, BatchStatus.FERMENTING, fermentationChamber);
            setupChamberMocks(3L, agingCellar, 0L);
            when(batchService.getBatch(10L)).thenReturn(fermentingBatch);

            BatchTransition fermentStart = BatchTransition.builder()
                    .batch(fermentingBatch)
                    .toStatus(BatchStatus.FERMENTING)
                    .transitionDate(LocalDateTime.now().minusHours(80))
                    .fromStatus(BatchStatus.GREEN)
                    .performedBy("Operator")
                    .build();
            when(transitionRepository.findByBatchIdOrderByTransitionDateAsc(10L))
                    .thenReturn(List.of(fermentStart));
            when(transitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(batchService.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BatchTransitionResponse result = transitionService.transition(10L, buildRequest(3L, "Operator A"));

            assertThat(result.getToStatus()).isEqualTo(BatchStatus.AGING);
        }

        @Test
        @DisplayName("Batch that has only fermented for 48h CANNOT move to Aging Cellar")
        void insufficientFermentationBlocksAgingTransition() {
            Batch fermentingBatch = withStatus(greenBatch, BatchStatus.FERMENTING, fermentationChamber);
            setupChamberMocks(3L, agingCellar, 0L);
            when(batchService.getBatch(10L)).thenReturn(fermentingBatch);

            BatchTransition fermentStart = BatchTransition.builder()
                    .batch(fermentingBatch)
                    .toStatus(BatchStatus.FERMENTING)
                    .transitionDate(LocalDateTime.now().minusHours(48))
                    .fromStatus(BatchStatus.GREEN)
                    .performedBy("Operator")
                    .build();
            when(transitionRepository.findByBatchIdOrderByTransitionDateAsc(10L))
                    .thenReturn(List.of(fermentStart));

            assertThatThrownBy(() ->
                    transitionService.transition(10L, buildRequest(3L, "Operator A")))
                    .isInstanceOf(IllegalTransitionException.class)
                    .hasMessageContaining("72");
        }
    }

    // AGING -> RETAIL_READY

    @Nested
    @DisplayName("AGING -> RETAIL_READY transitions")
    class AgingToRetailReady {

        @Test
        @DisplayName("Batch that has met its target aging duration CAN be marked RETAIL_READY")
        void batchThatMetTargetCanBeMarkedRetailReady() {
            Batch agingBatch = Batch.builder()
                    .id(10L).batchCode("PRO-2024-001").farm(farm)
                    .productType(ProductType.PROSCIUTTO).animalBreed("Large White")
                    .initialWeightKg(new BigDecimal("12.500"))
                    .saltCureStartDate(LocalDate.now().minusMonths(30))
                    .targetAgingMonths(24)
                    .currentStatus(BatchStatus.AGING)
                    .currentChamber(agingCellar)
                    .build();

            when(batchService.getBatch(10L)).thenReturn(agingBatch);
            when(transitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(batchService.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ChamberTransitionRequest req = new ChamberTransitionRequest();
            req.setTargetChamberId(null);
            req.setPerformedBy("Head Salumiere");

            BatchTransitionResponse result = transitionService.transition(10L, req);

            assertThat(result.getToStatus()).isEqualTo(BatchStatus.RETAIL_READY);
            assertThat(agingBatch.getCurrentStatus()).isEqualTo(BatchStatus.RETAIL_READY);
            assertThat(agingBatch.getCurrentChamber()).isNull();
        }

        @Test
        @DisplayName("Batch that has NOT met its target aging duration CANNOT be market RETAIL_READY")
        void batchThatHasNotMetTargetCannotBeMarkedRetailReady() {
            Batch agingBatch = Batch.builder()
                    .id(10L).batchCode("PRO-2024-001").farm(farm)
                    .productType(ProductType.PROSCIUTTO).animalBreed("Large White")
                    .initialWeightKg(new BigDecimal("12.500"))
                    .saltCureStartDate(LocalDate.now().minusMonths(12))
                    .targetAgingMonths(24)
                    .currentStatus(BatchStatus.AGING)
                    .currentChamber(agingCellar)
                    .build();

            when(batchService.getBatch(10L)).thenReturn(agingBatch);

            ChamberTransitionRequest req = new ChamberTransitionRequest();
            req.setTargetChamberId(null);
            req.setPerformedBy("Head Salumiere");

            assertThatThrownBy(() -> transitionService.transition(10L, req))
                    .isInstanceOf(IllegalTransitionException.class)
                    .hasMessageContaining("12 months of the required 24 months");
        }

        @Test
        @DisplayName("FERMENTING batch CANNOT be marked RETAIL_READY")
        void fermentingBatchCannotBeMarkedRetailReady() {
            Batch fermentingBatch = withStatus(greenBatch, BatchStatus.FERMENTING, fermentationChamber);
            when(batchService.getBatch(10L)).thenReturn(fermentingBatch);

            ChamberTransitionRequest req = new ChamberTransitionRequest();
            req.setTargetChamberId(null);
            req.setPerformedBy("Head Salumiere");

            assertThatThrownBy(() -> transitionService.transition(10L, req))
                    .isNotSameAs(IllegalTransitionException.class)
                    .hasMessageContaining("AGING status");
        }
    }

    // Capacity enforcement

    @Nested
    @DisplayName("Chamber capacity enforcement")
    class CapacityEnforcement {

        @Test
        @DisplayName("Cannot place a batch into a chamber at full capacity")
        void cannotPlaceBatchInFullChamber() {
            Chamber fullChamber = Chamber.builder()
                    .id(1L).name("Cold Smoke A").chamberType(ChamberType.COLD_SMOKE)
                    .targetTemperatureCelsius(new BigDecimal("12.00"))
                    .targetHumidityPercent(new BigDecimal("70.00"))
                    .capacity(1).isActive(true).build();

            when(batchService.getBatch(10L)).thenReturn(greenBatch);
            when(chamberService.getChamber(1L)).thenReturn(fullChamber);
            when(chamberRepository.countActiveBatchesInChamber(1L)).thenReturn(1L);

            assertThatThrownBy(() ->
                    transitionService.transition(10L, buildRequest(1L, "Operator A")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("full capacity");
        }
    }

    // Helpers

    private void setupChamberMocks(Long chamberId, Chamber chamber, long activeBatchCount) {
        when(chamberService.getChamber(chamberId)).thenReturn(chamber);
        when(chamberRepository.countActiveBatchesInChamber(chamberId))
                .thenReturn(activeBatchCount);
    }

    private ChamberTransitionRequest buildRequest(Long chamberId, String operator) {
        ChamberTransitionRequest req = new ChamberTransitionRequest();
        req.setTargetChamberId(chamberId);
        req.setPerformedBy(operator);
        return req;
    }

    private Chamber buildChamber(Long id, String name, ChamberType type) {
        return Chamber.builder()
                .id(id).name(name).chamberType(type)
                .targetTemperatureCelsius(new BigDecimal("12.00"))
                .targetHumidityPercent(new BigDecimal("70.00"))
                .capacity(20).isActive(true).build();
    }

    private Batch withStatus(Batch source, BatchStatus status, Chamber chamber) {
        return Batch.builder()
                .id(source.getId()).batchCode(source.getBatchCode())
                .farm(source.getFarm()).productType(source.getProductType())
                .animalBreed(source.getAnimalBreed())
                .initialWeightKg(source.getInitialWeightKg())
                .saltCureStartDate(source.getSaltCureStartDate())
                .targetAgingMonths(source.getTargetAgingMonths())
                .currentStatus(status).currentChamber(chamber).build();
    }

}
