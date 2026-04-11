package com.project.artisancharcuterie.service;

import com.project.artisancharcuterie.domain.Batch;
import com.project.artisancharcuterie.domain.Chamber;
import com.project.artisancharcuterie.domain.SensorReading;
import com.project.artisancharcuterie.dto.request.SensorReadingRequest;
import com.project.artisancharcuterie.dto.response.SensorReadingResponse;
import com.project.artisancharcuterie.dto.response.StressAlertResponse;
import com.project.artisancharcuterie.repository.BatchRepository;
import com.project.artisancharcuterie.repository.SensorReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SensorDataService {

    private final SensorReadingRepository sensorReadingRepository;
    private final ChamberService chamberService;
    private final BatchRepository batchRepository;

    @Value("${clm.stress-alert.temperature-tolerance-celsius:2.0}")
    private double temperatureTolerance;

    @Value("${clm.stress-alert.exposure-threshold-hours:4}")
    private long exposureThresholdHours;

    public List<SensorReadingResponse> findByChamber(Long chamberId) {
        chamberService.getChamber(chamberId);
        return sensorReadingRepository.findByChamberIdOrderByRecordedAtDesc(chamberId)
                .stream()
                .map(SensorReadingResponse::from)
                .toList();
    }

    @Transactional
    public SensorReadingResponse ingest(Long chamberId, SensorReadingRequest request) {
        Chamber chamber = chamberService.getChamber(chamberId);

        SensorReading reading = SensorReading.builder()
                .chamber(chamber)
                .temperatureCelsius(request.getTemperatureCelsius())
                .humidityPercent(request.getHumidityPercent())
                .recordedAt(request.getRecordedAt() != null
                        ? request.getRecordedAt()
                        : LocalDateTime.now())
                .sensorId(request.getSensorId())
                .build();

        return SensorReadingResponse.from(sensorReadingRepository.save(reading));
    }

    /**
     * Scans all chambers for sustained out-of-range temperature events.
     */
    public List<StressAlertResponse> getStressAlerts() {
        LocalDateTime scanFrom = LocalDateTime.now().minusHours(exposureThresholdHours + 24);

        List<SensorReading> allReadings = sensorReadingRepository.findAllSince(scanFrom);

        Map<Long, List<SensorReading>> byChamber = allReadings.stream()
                .collect(Collectors.groupingBy(r -> r.getChamber().getId()));

        List<StressAlertResponse> alerts = new ArrayList<>();

        for (Map.Entry<Long, List<SensorReading>> entry : byChamber.entrySet()) {
            Long chamberId = entry.getKey();
            List<SensorReading> readings = entry.getValue();

            if (readings.isEmpty()) continue;

            Chamber chamber = readings.get(0).getChamber();
            BigDecimal target = chamber.getTargetTemperatureCelsius();
            BigDecimal tolerance = BigDecimal.valueOf(temperatureTolerance);
            BigDecimal minSafe = target.subtract(tolerance);
            BigDecimal maxSafe = target.add(tolerance);

            StressAlertResponse alert = detectBreach(chamber, readings, minSafe, maxSafe);
            if (alert != null) {
                alerts.add(alert);
            }
        }
        return alerts;
    }

    /**
     * Scans readings for a specific chamber and returns an alert if a breach
     * exceeding the configured threshold is detected.
     */
    public List<StressAlertResponse> getStressAlertsForChamber(Long chamberId) {
        Chamber chamber = chamberService.getChamber(chamberId);
        LocalDateTime scanFrom = LocalDateTime.now().minusHours(exposureThresholdHours + 24);

        List<SensorReading> readings = sensorReadingRepository
                .findByChamberIdAndRecordedAtAfterOrderByRecordedAtAsc(chamberId, scanFrom);

        BigDecimal target = chamber.getTargetTemperatureCelsius();
        BigDecimal tolerance = BigDecimal.valueOf(temperatureTolerance);

        StressAlertResponse alert = detectBreach(chamber, readings, target.subtract(tolerance), target.add(tolerance));

        return alert != null ? List.of(alert) : List.of();
    }

    /**
     * Identifies the longest continuous out-of-range streak in {@code readings}.
     * If that streak spans more than {@code exposureThresholdHours}, a
     * {@link StressAlertResponse} is constructed and returned.
     */
    private StressAlertResponse detectBreach(Chamber chamber,
                                             List<SensorReading> readings,
                                             BigDecimal minSafe,
                                             BigDecimal maxSafe) {
        LocalDateTime streakStart = null;
        LocalDateTime streakEnd = null;
        BigDecimal streakMin = null;
        BigDecimal streakMax = null;
        double maxBreachHours = 0;

        LocalDateTime currentStreakStart = null;
        LocalDateTime currentStreakEnd = null;
        BigDecimal currentMin = null;
        BigDecimal currentMax = null;

        for (SensorReading reading : readings) {
            boolean outOfRange = reading.getTemperatureCelsius().compareTo(minSafe) < 0
                    || reading.getTemperatureCelsius().compareTo(maxSafe) > 0;

            if (outOfRange) {
                if (currentStreakStart == null) {
                    currentStreakStart = reading.getRecordedAt();
                }
                currentStreakEnd = reading.getRecordedAt();
                currentMin = currentMin == null
                        ? reading.getTemperatureCelsius()
                        : currentMin.min(reading.getTemperatureCelsius());
                currentMax = currentMax == null
                        ? reading.getTemperatureCelsius()
                        : currentMax.max(reading.getTemperatureCelsius());
            } else {
                if (currentStreakStart != null) {
                    double hours = ChronoUnit.MINUTES.between(currentStreakStart, currentStreakEnd) / 60.0;
                    if (hours > maxBreachHours) {
                        maxBreachHours = hours;
                        streakStart = currentStreakStart;
                        streakEnd = currentStreakEnd;
                        streakMin = currentMin;
                        streakMax = currentMax;
                    }
                    currentStreakStart = null;
                    currentStreakEnd = null;
                    currentMin = null;
                    currentMax = null;
                }
            }
        }

        if (currentStreakStart != null) {
            LocalDateTime effectiveEnd = LocalDateTime.now();
            double hours = ChronoUnit.MINUTES.between(currentStreakStart, effectiveEnd) / 60.0;
            if (hours > maxBreachHours) {
                maxBreachHours = hours;
                streakStart = currentStreakStart;
                streakEnd = effectiveEnd;
                streakMin = currentMin;
                streakMax = currentMax;
            }
        }

        if (maxBreachHours <= exposureThresholdHours || streakStart == null) {
            return null;
        }

        List<Batch> affectedBatches = batchRepository.findActiveBatchesInChamber(chamber.getId());
        List<StressAlertResponse.AffectedBatch> batchSummaries = affectedBatches.stream()
                .map(b -> StressAlertResponse.AffectedBatch.builder()
                        .batchId(b.getId())
                        .batchCode(b.getBatchCode())
                        .productType(b.getProductType().getDisplayName())
                        .currentStatus(b.getCurrentStatus().getDisplayName())
                        .build())
                .toList();

        return StressAlertResponse.builder()
                .alertId("ALERT-" + chamber.getId() + "-" + streakStart.toString())
                .chamberId(chamber.getId())
                .chamberName(chamber.getName())
                .chamberType(chamber.getChamberType())
                .targetTemperatureCelsius(chamber.getTargetTemperatureCelsius())
                .minSafeTemperature(minSafe)
                .maxSafeTemperature(maxSafe)
                .minObservedTemperature(streakMin)
                .maxObservedTemperature(streakMax)
                .breachStartTime(streakStart)
                .breachEndTime(streakEnd)
                .breachDurationHours(BigDecimal.valueOf(maxBreachHours).setScale(2, RoundingMode.HALF_UP).doubleValue())
                .affectedBatches(batchSummaries)
                .build();
    }

}
