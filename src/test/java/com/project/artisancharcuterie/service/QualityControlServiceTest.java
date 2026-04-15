package com.project.artisancharcuterie.service;

import com.project.artisancharcuterie.domain.Batch;
import com.project.artisancharcuterie.domain.Farm;
import com.project.artisancharcuterie.domain.QualityControlLog;
import com.project.artisancharcuterie.domain.enums.BatchStatus;
import com.project.artisancharcuterie.domain.enums.ProductType;
import com.project.artisancharcuterie.dto.request.QualityControlLogRequest;
import com.project.artisancharcuterie.dto.response.QualityControlLogResponse;
import com.project.artisancharcuterie.exception.ImmutableResourceException;
import com.project.artisancharcuterie.repository.QualityControlLogRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QualityControlService - logging and immutability enforcement")
public class QualityControlServiceTest {

    @Mock private QualityControlLogRepository qcLogRepository;
    @Mock private BatchService batchService;

    @InjectMocks
    private QualityControlService qcService;

    private Farm farm;
    private Batch agingBatch;

    @BeforeEach
    void setUp() {
        farm = Farm.builder().id(1L).name("Test Farm").location("Italy").breed("Mixed").build();

        agingBatch = Batch.builder()
                .id(10L).batchCode("PRO-2024-001").farm(farm)
                .productType(ProductType.PROSCIUTTO).animalBreed("Large White")
                .initialWeightKg(new BigDecimal("12.500"))
                .saltCureStartDate(LocalDate.now().minusMonths(6))
                .targetAgingMonths(24)
                .currentStatus(BatchStatus.AGING)
                .build();
    }

    // Immutability enforcement

    @Nested
    @DisplayName("QC log immutability - regulatory compliance requirement")
    class Immutability {

        @Test
        @DisplayName("rejectUpdate() always throws ImmutableResourceException (maps to HTTP 422)")
        void updateAlwaysThrowsImmutableException() {
            assertThatThrownBy(() -> qcService.rejectUpdate(1L))
                    .isInstanceOf(ImmutableResourceException.class)
                    .hasMessageContaining("immutable")
                    .hasMessageContaining("1");
        }

        @Test
        @DisplayName("rejectDelete() always throws ImmutableResourceException (maps to HTTP 422)")
        void deleteAlwaysThrowsImmutableException() {
            assertThatThrownBy(() -> qcService.rejectDelete(42L))
                    .isInstanceOf(ImmutableResourceException.class)
                    .hasMessageContaining("immutable")
                    .hasMessageContaining("42");
        }

        @Test
        @DisplayName("ImmutableResourceException contains the log ID in the message")
        void exceptionMessageContainsResourceId() {
            Long logId = 99L;
            assertThatThrownBy(() -> qcService.rejectUpdate(logId))
                    .isInstanceOf(ImmutableResourceException.class)
                    .hasMessageContaining(logId.toString());
        }
    }

    // Filling QC logs

    @Nested
    @DisplayName("Filling new QC log entries")
    class LogFilling {

        @Test
        @DisplayName("Valid QC log for an AGING batch is persisted successfully")
        void validLogForAgingBatch() {
            QualityControlLogRequest request = buildRequest(4, new BigDecimal("5.40"));

            QualityControlLog savedLog = QualityControlLog.builder()
                    .id(1L).batch(agingBatch).phLevel(request.getPhLevel())
                    .aromaProfile(request.getAromaProfile()).inspector(request.getInspector())
                    .weekNumber(request.getWeekNumber()).loggedAt(LocalDateTime.now()).build();

            when(batchService.getBatch(10L)).thenReturn(agingBatch);
            when(qcLogRepository.existsByBatchIdAndWeekNumber(10L, 4)).thenReturn(false);
            when(qcLogRepository.save(any())).thenReturn(savedLog);

            QualityControlLogResponse response = qcService.log(10L, request);

            assertThat(response.getPhLevel()).isEqualByComparingTo(new BigDecimal("5.40"));
            assertThat(response.getWeekNumber()).isEqualTo(4);
            verify(qcLogRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("Duplicate week number for the same batch is rejected")
        void duplicateWeekNumberRejected() {
            QualityControlLogRequest request = buildRequest(4, new BigDecimal("5.40"));

            when(batchService.getBatch(10L)).thenReturn(agingBatch);
            when(qcLogRepository.existsByBatchIdAndWeekNumber(10L, 4)).thenReturn(true);

            assertThatThrownBy(() -> qcService.log(10L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("week 4")
                    .hasMessageContaining("immutable");

            verify(qcLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("GREEN batch cannot receive a QC log")
        void greenBatchCannotBeLogged() {
            Batch greenBatch = Batch.builder()
                    .id(20L).batchCode("PRO-2024-002").farm(farm)
                    .productType(ProductType.PROSCIUTTO).animalBreed("Large White")
                    .initialWeightKg(new BigDecimal("10.000"))
                    .saltCureStartDate(LocalDate.now().minusWeeks(1))
                    .targetAgingMonths(24).currentStatus(BatchStatus.GREEN).build();

            QualityControlLogRequest request = buildRequest(1, new BigDecimal("5.40"));
            when(batchService.getBatch(20L)).thenReturn(greenBatch);

            assertThatThrownBy(() -> qcService.log(20L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("GREEN");

            verify(qcLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("REJECTED batch cannot receive a QC log")
        void rejectedBatchCannotBeLogged() {
            Batch rejectedBatch = Batch.builder()
                    .id(30L).batchCode("PRO-2024-003").farm(farm)
                    .productType(ProductType.PROSCIUTTO).animalBreed("Large White")
                    .initialWeightKg(new BigDecimal("10.000"))
                    .saltCureStartDate(LocalDate.now().minusMonths(3))
                    .targetAgingMonths(24).currentStatus(BatchStatus.REJECTED).build();

            QualityControlLogRequest request = buildRequest(5, new BigDecimal("5.40"));
            when(batchService.getBatch(30L)).thenReturn(rejectedBatch);

            assertThatThrownBy(() -> qcService.log(30L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("REJECTED");

            verify(qcLogRepository, never()).save(any());
        }
    }

    // pH safe-range flag

    @Nested
    @DisplayName("pH safe-range annotation in response")
    class PhSafeRange {

        @Test
        @DisplayName("pH 5.40 (within 4.8-6.2) is flagged as safe")
        void phWithinRangeIsFlaggedSafe() {
            QualityControlLog log = buildLog(new BigDecimal("5.40"));
            QualityControlLogResponse response = QualityControlLogResponse.from(log);
            assertThat(response.isPhWithinSafeRange()).isTrue();
        }

        @Test
        @DisplayName("pH 4.50 (below 4.8) is flagged as unsafe")
        void phBelowRangeIsFlaggedUnsafe() {
            QualityControlLog log = buildLog(new BigDecimal("4.50"));
            QualityControlLogResponse response = QualityControlLogResponse.from(log);
            assertThat(response.isPhWithinSafeRange()).isFalse();
        }

        @Test
        @DisplayName("pH 6.50 (above 6.2) is flagged as unsafe")
        void phAboveRangeIsFlaggedUnsafe() {
            QualityControlLog log = buildLog(new BigDecimal("6.50"));
            QualityControlLogResponse response = QualityControlLogResponse.from(log);
            assertThat(response.isPhWithinSafeRange()).isFalse();
        }

        @Test
        @DisplayName("pH exactly at lower boundary (4.8) is flagged as safe")
        void phAtLowerBoundaryIsSafe() {
            QualityControlLog log = buildLog(new BigDecimal("4.80"));
            QualityControlLogResponse response = QualityControlLogResponse.from(log);
            assertThat(response.isPhWithinSafeRange()).isTrue();
        }

        @Test
        @DisplayName("pH exactly at upper boundary (6.2) is flagged as safe")
        void phAtUpperBoundaryIsSafe() {
            QualityControlLog log = buildLog(new BigDecimal("6.20"));
            QualityControlLogResponse response = QualityControlLogResponse.from(log);
            assertThat(response.isPhWithinSafeRange()).isTrue();
        }
    }

    // Helpers

    private QualityControlLogRequest buildRequest(int weekNumber, BigDecimal ph) {
        QualityControlLogRequest req = new QualityControlLogRequest();
        req.setWeekNumber(weekNumber);
        req.setPhLevel(ph);
        req.setAromaProfile("Nutty, mild lactic - healthy development");
        req.setInspector("G. Ferrari - QC Dept");
        return req;
    }

    private QualityControlLog buildLog(BigDecimal ph) {
        return QualityControlLog.builder()
                .id(1L).batch(agingBatch).phLevel(ph)
                .aromaProfile("Test aroma").inspector("Test Inspector")
                .weekNumber(1).loggedAt(LocalDateTime.now()).build();
    }
}
