package com.project.artisancharcuterie.controller;

import com.project.artisancharcuterie.dto.request.ChamberRequest;
import com.project.artisancharcuterie.dto.response.ChamberResponse;
import com.project.artisancharcuterie.service.ChamberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chambers")
@RequiredArgsConstructor
@Tag(name = "Chambers", description = "Manage environmental chambers (Cold Smoke, Fermentation, Aging)")
public class ChamberController {

    private final ChamberService chamberService;

    @GetMapping
    @Operation(summary = "List all chambers with current occupancy")
    public ResponseEntity<List<ChamberResponse>> listAll() {
        return ResponseEntity.ok(chamberService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get chamber details by ID")
    public ResponseEntity<ChamberResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(chamberService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Register a new chamber")
    public ResponseEntity<ChamberResponse> create(@Valid @RequestBody ChamberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chamberService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update chamber configuration (temperature, humidity, capacity)")
    public ResponseEntity<ChamberResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ChamberRequest request) {
        return ResponseEntity.ok(chamberService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a chamber (soft delete, prevents new batch replacement)")
    public ResponseEntity<ChamberResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(chamberService.deactivate(id));
    }

}
