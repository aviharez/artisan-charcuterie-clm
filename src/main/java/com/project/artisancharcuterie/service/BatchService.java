package com.project.artisancharcuterie.service;

import com.project.artisancharcuterie.domain.Batch;
import com.project.artisancharcuterie.domain.Farm;
import com.project.artisancharcuterie.domain.enums.BatchStatus;
import com.project.artisancharcuterie.domain.enums.ProductType;
import com.project.artisancharcuterie.dto.request.BatchCreateRequest;
import com.project.artisancharcuterie.dto.response.BatchResponse;
import com.project.artisancharcuterie.exception.ResourceNotFoundException;
import com.project.artisancharcuterie.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BatchService {

    private final BatchRepository batchRepository;
    private final FarmService farmService;

    /** Simple in-memory counter for batch code generation within a given year. */
    private final AtomicInteger batchSequence = new AtomicInteger(0);

    public List<BatchResponse> findAll() {
        return batchRepository.findAll().stream()
                .map(BatchResponse::from)
                .toList();
    }

    public List<BatchResponse> findByStatus(BatchStatus status) {
        return batchRepository.findByCurrentStatus(status).stream()
                .map(BatchResponse::from)
                .toList();
    }

    public List<BatchResponse> findByProductType(ProductType productType) {
        return batchRepository.findByProductType(productType).stream()
                .map(BatchResponse::from)
                .toList();
    }

    public List<BatchResponse> findByFarm(Long farmId) {
        farmService.getFarm(farmId);
        return batchRepository.findByFarmId(farmId).stream()
                .map(BatchResponse::from)
                .toList();
    }

    public BatchResponse findById(Long id) {
        return BatchResponse.from(getBatch(id));
    }

    public BatchResponse findByCode(String batchCode) {
        Batch batch = batchRepository.findByBatchCode(batchCode)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", batchCode));
        return BatchResponse.from(batch);
    }

    @Transactional
    public BatchResponse create(BatchCreateRequest request) {
        Farm farm = farmService.getFarm(request.getFarmId());

        int agingMonths = request.getTargetAgingMonths() != null
                ? request.getTargetAgingMonths()
                : request.getProductType().getStandardAgingMonths();

        Batch batch = Batch.builder()
                .batchCode(generateBatchCode(request.getProductType()))
                .farm(farm)
                .productType(request.getProductType())
                .animalBreed(request.getAnimalBreed())
                .initialWeightKg(request.getInitialWeightKg())
                .saltCureStartDate(request.getSaltCureStartDate())
                .targetAgingMonths(agingMonths)
                .currentStatus(BatchStatus.GREEN)
                .notes(request.getNotes())
                .build();

        return BatchResponse.from(save(batch));
    }

    @Transactional
    public void delete(Long id) {
        Batch batch = getBatch(id);
        if (batch.getCurrentStatus() != BatchStatus.GREEN) {
            throw new IllegalArgumentException(
                    "Only batches with GREEN status may be deleted. " +
                            "Batches that have entered the curing workflow are permanently retained for audit purposes."
            );
        }
        batchRepository.delete(batch);
    }

    Batch getBatch(Long id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", id));
    }

    Batch save(Batch batch) {
        return batchRepository.save(batch);
    }

    private String generateBatchCode(ProductType productType) {
        String prefix = switch (productType) {
            case PROSCIUTTO -> "PRO";
            case BRESAOLA -> "BRS";
            case CULATELLO -> "CUL";
        };
        int year = Year.now().getValue();
        int seq = batchSequence.incrementAndGet();
        String candidate = String.format("%s-%d-%03d", prefix, year, seq);
        // Ensure uniqueness in edge cases
        while (batchRepository.existsByBatchCode(candidate)) {
            seq = batchSequence.incrementAndGet();
            candidate = String.format("%s-%d-%03d", prefix, year, seq);
        }
        return candidate;
    }

}
