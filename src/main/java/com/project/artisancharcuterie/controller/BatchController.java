package com.project.artisancharcuterie.controller;

import com.project.artisancharcuterie.domain.enums.BatchStatus;
import com.project.artisancharcuterie.domain.enums.ProductType;
import com.project.artisancharcuterie.dto.request.BatchCreateRequest;
import com.project.artisancharcuterie.dto.request.ChamberTransitionRequest;
import com.project.artisancharcuterie.dto.response.BatchResponse;
import com.project.artisancharcuterie.dto.response.BatchTransitionResponse;
import com.project.artisancharcuterie.service.BatchService;
import com.project.artisancharcuterie.service.ChamberTransitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
@Tag(name = "Batches", description = "Core batch lifecycle management")
public class BatchController {

    private final BatchService batchService;
    private final ChamberTransitionService transitionService;

    // Batch CRUD

    @GetMapping
    @Operation(summary = "List all batches",
               description = "Optionally filter by status or product type")
    public ResponseEntity<List<BatchResponse>> listAll(
            @Parameter(description = "Filter by batch lifecycle status")
            @RequestParam(required = false) BatchStatus status,
            @Parameter(description = "Filter by product type")
            @RequestParam(required = false) ProductType productType,
            @Parameter(description = "Filter by farm ID")
            @RequestParam(required = false) Long farmId) {
        if (status != null) {
            return ResponseEntity.ok(batchService.findByStatus(status));
        }
        if (productType != null) {
            return ResponseEntity.ok(batchService.findByProductType(productType));
        }
        if (farmId != null) {
            return ResponseEntity.ok(batchService.findByFarm(farmId));
        }
        return ResponseEntity.ok(batchService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a batch by numeric ID")
    public ResponseEntity<BatchResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(batchService.findById(id));
    }

    @GetMapping("/code/{batchCode}")
    @Operation(summary = "Get a batch by business code")
    public ResponseEntity<BatchResponse> getByCode(@PathVariable String batchCode) {
        return ResponseEntity.ok(batchService.findByCode(batchCode));
    }

    @PostMapping
    @Operation(summary = "Register a new batch",
               description = "Creates a batch in GREEN status. Farm ID is mandatory.")
    public ResponseEntity<BatchResponse> create(@Valid @RequestBody BatchCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(batchService.create(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a batch (only GREEN-status batches may be deleted)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        batchService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Chamber transition workflow

    @GetMapping("/{id}/transitions")
    @Operation(summary = "Get the full transition history for a batch",
               description = "Returns the ordered chain-of-custody audit trail")
    public ResponseEntity<List<BatchTransitionResponse>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(transitionService.getHistory(id));
    }

    @PostMapping("/{id}/transitions")
    @Operation(summary = "Transition a batch to a new chamber or mark as Retail-Ready",
               description = """
                       Enforces the legal workflow:
                       GREEN -> COLD_SMOKE or FERMENTATION_ROOM
                       COLD_SMOKING -> FERMENTATION_ROOM
                       FERMENTING -> PRIMARY_AGING_CELLAR (min. 72h fermentation required)
                       AGING -> RETAIL_READY (targetChamberId = null, target aging duration must be met)
                       """)
    public ResponseEntity<BatchTransitionResponse> transition(
            @PathVariable Long id,
            @Valid @RequestBody ChamberTransitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transitionService.transition(id, request));
    }

}
