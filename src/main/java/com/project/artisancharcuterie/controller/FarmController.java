package com.project.artisancharcuterie.controller;

import com.project.artisancharcuterie.dto.request.FarmRequest;
import com.project.artisancharcuterie.dto.response.FarmResponse;
import com.project.artisancharcuterie.service.FarmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/farms")
@RequiredArgsConstructor
@Tag(name = "Farms", description = "Register and manage farms of origin")
public class FarmController {

    private final FarmService farmService;

    @GetMapping
    @Operation(summary = "List all farms")
    public ResponseEntity<List<FarmResponse>> listAll() {
        return ResponseEntity.ok(farmService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a farm by ID")
    public ResponseEntity<FarmResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(farmService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Register a new farm of origin")
    public ResponseEntity<FarmResponse> create(@Valid @RequestBody FarmRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(farmService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update farm details")
    public ResponseEntity<FarmResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FarmRequest request) {
        return ResponseEntity.ok(farmService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a farm (only if it has no associated batches)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        farmService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
