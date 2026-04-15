package com.project.artisancharcuterie.service;

import com.project.artisancharcuterie.domain.Batch;
import com.project.artisancharcuterie.domain.Chamber;
import com.project.artisancharcuterie.domain.Farm;
import com.project.artisancharcuterie.domain.SensorReading;
import com.project.artisancharcuterie.domain.enums.BatchStatus;
import com.project.artisancharcuterie.domain.enums.ChamberType;
import com.project.artisancharcuterie.domain.enums.ProductType;
import com.project.artisancharcuterie.dto.response.StressAlertResponse;
import com.project.artisancharcuterie.repository.BatchRepository;
import com.project.artisancharcuterie.repository.SensorReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SensorDataService - stress alert detection")
public class SensorDataServiceTest {

    @Mock private SensorReadingRepository sensorReadingRepository;
    @Mock private ChamberService chamberService;
    @Mock private BatchRepository batchRepository;

    @InjectMocks
    private SensorDataService sensorDataService;

    private Chamber agingCellar;
    private Farm farm;
    private Batch activeBatch;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sensorDataService, "temperatureTolerance", 2.0);
        ReflectionTestUtils.setField(sensorDataService, "exposureThresholdHours", 4L);

        agingCellar = Chamber.builder()
                .id(3L).name("Primary Aging Cellar Alpha")
                .chamberType(ChamberType.PRIMARY_AGING_CELLAR)
                .targetTemperatureCelsius(new BigDecimal("14.00"))
                .targetHumidityPercent(new BigDecimal("75.00"))
                .capacity(50).isActive(true).build();

        farm = Farm.builder().id(1L).name("Test Farm").location("Italy").breed("Mixed").build();

        activeBatch = Batch.builder()
                .id(10L).batchCode("PRO-2024-001").farm(farm)
                .productType(ProductType.PROSCIUTTO).animalBreed("Large White")
                .initialWeightKg(new BigDecimal("12.500"))
                .saltCureStartDate(LocalDate.now().minusMonths(18))
                .targetAgingMonths(24).currentStatus(BatchStatus.AGING)
                .currentChamber(agingCellar).build();
    }

    // Alert threshold: strictly MORE THAN 4 hours

    @Nested
    @DisplayName("Threshold enforcement: alert fires ONLY when breach > 4 hours")
    class ThresholdEnforcement {

        @Test
        @DisplayName("Continuous 5-hour breach TRIGGERS a stress alert")
        void fiveHourBreachTriggersAlert() {
            LocalDateTime now = LocalDateTime.now();
            List<SensorReading> readings = outOfRangeReadings(agingCellar, now.minusHours(5), now, 30);

            when(chamberService.getChamber(3L)).thenReturn(agingCellar);
            when(sensorReadingRepository.findByChamberIdAndRecordedAtAfterOrderByRecordedAtAsc(eq(3L), any())).thenReturn(readings);
            when(batchRepository.findActiveBatchesInChamber(3L)).thenReturn(List.of(activeBatch));

            List<StressAlertResponse> alerts = sensorDataService.getStressAlertsForChamber(3L);

            assertThat(alerts).hasSize(1);
            StressAlertResponse alert = alerts.get(0);
            assertThat(alert.getChamberId()).isEqualTo(3L);
            assertThat(alert.getBreachDurationHours()).isGreaterThan(4.0);
        }

        @Test
        @DisplayName("Continuous 4-hour breach NOT trigger a stress alert (strictly > 4h)")
        void exactlyFourBreachDoesNotTrigger() {
            LocalDateTime now = LocalDateTime.now();
            List<SensorReading> readings = outOfRangeReadings(agingCellar, now.minusHours(4), now.minusHours(4).plusMinutes(1), 2);

            when(chamberService.getChamber(3L)).thenReturn(agingCellar);
            when(sensorReadingRepository.findByChamberIdAndRecordedAtAfterOrderByRecordedAtAsc(eq(3L), any())).thenReturn(readings);

            List<StressAlertResponse> alerts = sensorDataService.getStressAlertsForChamber(3L);

            assertThat(alerts).isEmpty();
        }

        @Test
        @DisplayName("3-hour breach does NOT trigger a stress alert")
        void threeHourBreachDoesNotTrigger() {
            LocalDateTime now = LocalDateTime.now();
            List<SensorReading> readings = outOfRangeReadings(agingCellar, now.minusHours(3), now.minusHours(0).minusMinutes(30), 6);

            when(chamberService.getChamber(3L)).thenReturn(agingCellar);
            when(sensorReadingRepository.findByChamberIdAndRecordedAtAfterOrderByRecordedAtAsc(eq(3L), any())).thenReturn(readings);

            List<StressAlertResponse> alerts = sensorDataService.getStressAlertsForChamber(3L);

            assertThat(alerts).isEmpty();
        }

        @Test
        @DisplayName("No sensor readings at all returns no alerts")
        void noReadingsReturnNoAlerts() {
            when(chamberService.getChamber(3L)).thenReturn(agingCellar);
            when(sensorReadingRepository.findByChamberIdAndRecordedAtAfterOrderByRecordedAtAsc(eq(3L), any())).thenReturn(List.of());

            List<StressAlertResponse> alerts = sensorDataService.getStressAlertsForChamber(3L);

            assertThat(alerts).isEmpty();
        }
    }

    // Temperature range tolerance

    @Nested
    @DisplayName("Temperature tolerance: 2C band around target")
    class TemperatureTolerance {

        @Test
        @DisplayName("Temperature at target (14C) does NOT trigger - within range")
        void temperatureAtTargetNoAlert() {
            LocalDateTime now = LocalDateTime.now();
            List<SensorReading> readings = buildReadings(agingCellar, new BigDecimal("14.00"), now.minusHours(6), now, 12);

            when(chamberService.getChamber(3L)).thenReturn(agingCellar);
            when(sensorReadingRepository.findByChamberIdAndRecordedAtAfterOrderByRecordedAtAsc(eq(3L), any())).thenReturn(readings);

            List<StressAlertResponse> alerts = sensorDataService.getStressAlertsForChamber(3L);

            assertThat(alerts).isEmpty();
        }

        @Test
        @DisplayName("Temperature at upper limit (16C) does NOT trigger - within 2C band")
        void temperatureAtUpperLimitNoAlert() {
            LocalDateTime now = LocalDateTime.now();
            List<SensorReading> readings = buildReadings(agingCellar, new BigDecimal("16.00"), now.minusHours(6), now, 12);

            when(chamberService.getChamber(3L)).thenReturn(agingCellar);
            when(sensorReadingRepository.findByChamberIdAndRecordedAtAfterOrderByRecordedAtAsc(eq(3L), any())).thenReturn(readings);

            List<StressAlertResponse> alerts = sensorDataService.getStressAlertsForChamber(3L);

            assertThat(alerts).isEmpty();
        }

        @Test
        @DisplayName("Temperature exceeding upper limit (16.1C for 5h) TRIGGERS alert")
        void temperatureJustAboveUpperLimitTriggersAlert() {
            LocalDateTime now = LocalDateTime.now();
            List<SensorReading> readings = buildReadings(agingCellar, new BigDecimal("16.10"), now.minusHours(5), now, 10);

            when(chamberService.getChamber(3L)).thenReturn(agingCellar);
            when(sensorReadingRepository.findByChamberIdAndRecordedAtAfterOrderByRecordedAtAsc(eq(3L), any())).thenReturn(readings);
            when(batchRepository.findActiveBatchesInChamber(3L)).thenReturn(List.of(activeBatch));

            List<StressAlertResponse> alerts = sensorDataService.getStressAlertsForChamber(3L);

            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getMaxObservedTemperature()).isEqualByComparingTo(new BigDecimal("16.10"));
        }

        @Test
        @DisplayName("Temperature below lower limit (11.9C for 5h) TRIGGERS alert")
        void temperatureBelowLowerLimitTriggersAlert() {
            LocalDateTime now = LocalDateTime.now();
            List<SensorReading> readings = buildReadings(agingCellar, new BigDecimal("11.90"), now.minusHours(5), now, 10);

            when(chamberService.getChamber(3L)).thenReturn(agingCellar);
            when(sensorReadingRepository.findByChamberIdAndRecordedAtAfterOrderByRecordedAtAsc(eq(3L), any())).thenReturn(readings);
            when(batchRepository.findActiveBatchesInChamber(3L)).thenReturn(List.of(activeBatch));

            List<StressAlertResponse> alerts = sensorDataService.getStressAlertsForChamber(3L);

            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getMinObservedTemperature()).isEqualByComparingTo(new BigDecimal("11.90"));
        }
    }

    // Affected batch reporting

    @Nested
    @DisplayName("Affected batch reporting within stress alerts")
    class AffectedBatches {

        @Test
        @DisplayName("Alert response includes all active batches in the affected chamber")
        void alertIncludesAffectedBatches() {
            LocalDateTime now = LocalDateTime.now();
            List<SensorReading> readings = outOfRangeReadings(agingCellar, now.minusHours(5), now, 10);

            Batch batch2 = Batch.builder()
                    .id(11L).batchCode("BRS-2024-001").farm(farm)
                    .productType(ProductType.BRESAOLA).animalBreed("Chianina")
                    .initialWeightKg(new BigDecimal("8.000"))
                    .saltCureStartDate(LocalDate.now().minusMonths(4))
                    .targetAgingMonths(6).currentStatus(BatchStatus.AGING)
                    .currentChamber(agingCellar).build();

            when(chamberService.getChamber(3L)).thenReturn(agingCellar);
            when(sensorReadingRepository.findByChamberIdAndRecordedAtAfterOrderByRecordedAtAsc(eq(3L), any())).thenReturn(readings);
            when(batchRepository.findActiveBatchesInChamber(3L)).thenReturn(List.of(activeBatch, batch2));

            List<StressAlertResponse> alerts = sensorDataService.getStressAlertsForChamber(3L);

            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getAffectedBatches()).hasSize(2);
            assertThat(alerts.get(0).getAffectedBatches())
                    .extracting(StressAlertResponse.AffectedBatch::getBatchCode)
                    .containsExactlyInAnyOrder("PRO-2024-001", "BRS-2024-001");
        }

        @Test
        @DisplayName("Alert with no active batches in chamber returns empty affected list")
        void alertWithNoBatchesReturnsEmptyList() {
            LocalDateTime now = LocalDateTime.now();
            List<SensorReading> readings = outOfRangeReadings(agingCellar, now.minusHours(5), now, 10);

            when(chamberService.getChamber(3L)).thenReturn(agingCellar);
            when(sensorReadingRepository.findByChamberIdAndRecordedAtAfterOrderByRecordedAtAsc(eq(3L), any())).thenReturn(readings);
            when(batchRepository.findActiveBatchesInChamber(3L)).thenReturn(List.of());

            List<StressAlertResponse> alerts = sensorDataService.getStressAlertsForChamber(3L);

            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getAffectedBatches()).isEmpty();
        }
    }

    // Breach interrupted by in-range readings

    @Nested
    @DisplayName("Breach streak interruption by in-range readings")
    class BreachInterruption {

        @Test
        @DisplayName("Breach interrupted by in-range reading resets the streak counter")
        void interruptedBreachDoesNotTrigger() {
            LocalDateTime now = LocalDateTime.now();
            List<SensorReading> readings = Stream.concat(
                    Stream.concat(
                            outOfRangeReadings(agingCellar, now.minusHours(6), now.minusHours(3), 6).stream(),
                            buildReadings(agingCellar, new BigDecimal("14.00"), now.minusHours(3), now.minusHours(2), 2).stream()),
                    outOfRangeReadings(agingCellar, now.minusHours(2), now, 4).stream()
            ).toList();

            when(chamberService.getChamber(3L)).thenReturn(agingCellar);
            when(sensorReadingRepository.findByChamberIdAndRecordedAtAfterOrderByRecordedAtAsc(eq(3L), any())).thenReturn(readings);

            List<StressAlertResponse> alerts = sensorDataService.getStressAlertsForChamber(3L);

            assertThat(alerts).isEmpty();
        }
    }

    // Helpers

    private List<SensorReading> outOfRangeReadings(Chamber chamber, LocalDateTime from, LocalDateTime to, int count) {
        BigDecimal outOfRangeTemp = chamber.getTargetTemperatureCelsius().add(new BigDecimal("3.00"));
        return buildReadings(chamber, outOfRangeTemp, from, to, count);
    }

    private List<SensorReading> buildReadings(Chamber chamber, BigDecimal temp, LocalDateTime from, LocalDateTime to, int count) {
        if (count <= 1) {
            return List.of(buildReading(chamber, temp, from));
        }

        long totalMinutes = ChronoUnit.MINUTES.between(from, to);
        long stepMinutes = Math.max(1, totalMinutes / (count - 1));
        List<SensorReading> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(buildReading(chamber, temp, from.plusMinutes(i * stepMinutes)));
        }
        return list;
    }

    private SensorReading buildReading(Chamber chamber, BigDecimal temp, LocalDateTime at) {
        return SensorReading.builder()
                .id((long) (Math.random() * 10000))
                .chamber(chamber)
                .temperatureCelsius(temp)
                .humidityPercent(new BigDecimal("75.00"))
                .recordedAt(at)
                .sensorId("SENS-TEST-01")
                .build();
    }
}
