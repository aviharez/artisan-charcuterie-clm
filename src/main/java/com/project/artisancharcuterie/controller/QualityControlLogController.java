package com.project.artisancharcuterie.controller;

import com.project.artisancharcuterie.dto.request.QualityControlLogRequest;
import com.project.artisancharcuterie.dto.response.QualityControlLogResponse;
import com.project.artisancharcuterie.service.QualityControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batches/{batchId}/qc-logs")
@RequiredArgsConstructor
@Tag(name = "Quality Control", description = "Immutable weekly QC inspection logging for regulatory compliance")
public class QualityControlLogController {

    private final QualityControlService qcService;

    @GetMapping
    @Operation(summary = "List all QC logs for a batch (ordered by week number)")
    public ResponseEntity<List<QualityControlLogResponse>> listBatchBatch(@PathVariable Long batchId) {
        return ResponseEntity.ok(qcService.findByBatch(batchId));
    }

    @GetMapping("/{logId}")
    @Operation(summary = "Get a specific QC log entry")
    public ResponseEntity<QualityControlLogResponse> getById(
            @PathVariable Long batchId,
            @PathVariable Long logId) {
        return ResponseEntity.ok(qcService.findById(logId));
    }

    @PostMapping
    @Operation(summary = "File a new QC log entry",
               description = """
                       Logs a weekly pH measurement and aroma profile inspection.
                       **QC logs are immutable once submitted**. No updates or deletions are permitted.
                       Only one log per week per batch is allowed.
                       """)
    public ResponseEntity<QualityControlLogResponse> create(
            @PathVariable Long batchId,
            @Valid @RequestBody QualityControlLogRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(qcService.log(batchId, request));
    }

    @PutMapping("/{logId}")
    @Operation(summary = "Update a QC log [REJECTED. Logs are immutable]",
               description = "Always returns 422. QC logs cannot be modified after submission.")
    public ResponseEntity<Void> update(@PathVariable Long batchId, @PathVariable Long logId) {
        qcService.rejectUpdate(logId);
        return ResponseEntity.unprocessableEntity().build();
    }

    @DeleteMapping("/{logId}")
    @Operation(summary = "Delete a QC log [REJECTED. Logs are immutable]",
               description = "Always returns 422. QC logs cannot be deleted after submission.")
    public ResponseEntity<Void> delete(@PathVariable Long batchId, @PathVariable Long logId) {
        qcService.rejectDelete(logId);
        return ResponseEntity.unprocessableEntity().build();
    }
}
