package com.project.artisancharcuterie.controller;

import com.project.artisancharcuterie.dto.request.SensorReadingRequest;
import com.project.artisancharcuterie.dto.response.SensorReadingResponse;
import com.project.artisancharcuterie.dto.response.StressAlertResponse;
import com.project.artisancharcuterie.service.SensorDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Environmental Monitoring", description = "Sensor data ingestion and stress alerting")
public class SensorReadingController {

    private final SensorDataService sensorDataService;

    @GetMapping("/api/chambers/{chamberId}/sensor-readings")
    @Operation(summary = "List all sensor readings for a chamber (most recent first)")
    public ResponseEntity<List<SensorReadingResponse>> listReadings(@PathVariable Long chamberId) {
        return ResponseEntity.ok(sensorDataService.findByChamber(chamberId));
    }

    @PostMapping("/api/chambers/{chamberId}/sensor-readings")
    @Operation(summary = "Ingest a new sensor reading for a chamber",
               description = "Adds a temperature and humidity data point. `recordedAt` defaults to server time if omitted.")
    public ResponseEntity<SensorReadingResponse> ingest(
            @PathVariable Long chamberId,
            @Valid @RequestBody SensorReadingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sensorDataService.ingest(chamberId, request));
    }

    @GetMapping("/api/stress-alerts")
    @Operation(summary = "Get active stress alerts across all chambers",
               description = """
                       Scans all chambers for sustained temperature breaches.
                       A stress alert is raised when a chamber's temperature has been
                       outside the 2 C safety range for more than 4 continuous hours.
                       Returns all batches currently in the affected chamber.
                       """)
    public ResponseEntity<List<StressAlertResponse>> getAllAlerts() {
        return ResponseEntity.ok(sensorDataService.getStressAlerts());
    }

    @GetMapping("/api/chambers/{chamberId}/stress-alerts")
    @Operation(summary = "Get active stress alerts for a specific chamber")
    public ResponseEntity<List<StressAlertResponse>> getAlertsForChamber(@PathVariable Long chamberId) {
        return ResponseEntity.ok(sensorDataService.getStressAlertsForChamber(chamberId));
    }

}
