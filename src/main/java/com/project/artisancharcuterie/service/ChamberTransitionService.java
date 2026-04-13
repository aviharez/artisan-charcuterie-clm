package com.project.artisancharcuterie.service;

import com.project.artisancharcuterie.domain.Batch;
import com.project.artisancharcuterie.domain.BatchTransition;
import com.project.artisancharcuterie.domain.Chamber;
import com.project.artisancharcuterie.domain.enums.BatchStatus;
import com.project.artisancharcuterie.domain.enums.ChamberType;
import com.project.artisancharcuterie.dto.request.ChamberTransitionRequest;
import com.project.artisancharcuterie.dto.response.BatchTransitionResponse;
import com.project.artisancharcuterie.exception.IllegalTransitionException;
import com.project.artisancharcuterie.repository.BatchTransitionRepository;
import com.project.artisancharcuterie.repository.ChamberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChamberTransitionService {

    static final long MIN_FERMENTATION_HOURS = 72;

    private final BatchService batchService;
    private final ChamberService chamberService;
    private final BatchTransitionRepository transitionRepository;
    private final ChamberRepository chamberRepository;

    public List<BatchTransitionResponse> getHistory(Long batchId) {
        batchService.getBatch(batchId);
        return transitionRepository.findByBatchIdOrderByTransitionDateAsc(batchId)
                .stream()
                .map(BatchTransitionResponse::from)
                .toList();
    }

    @Transactional
    public BatchTransitionResponse transition(Long batchId, ChamberTransitionRequest request) {
        Batch batch = batchService.getBatch(batchId);
        BatchStatus currentStatus = batch.getCurrentStatus();

        if (!currentStatus.isTransitionable()) {
            throw new IllegalTransitionException(currentStatus);
        }

        Chamber toChamber = null;
        BatchStatus nextStatus;

        if (request.getTargetChamberId() == null) {
            nextStatus = resolveRetailReadyTransition(batch);
        } else {
            toChamber = chamberService.getChamber(request.getTargetChamberId());
            assertChamberIsActive(toChamber);
            assertChamberHasCapacity(toChamber);
            nextStatus = resolveStatusForChamber(batch, toChamber.getChamberType());
        }

        BatchTransition record = BatchTransition.builder()
                .batch(batch)
                .fromChamber(batch.getCurrentChamber())
                .toChamber(toChamber)
                .fromStatus(currentStatus)
                .toStatus(nextStatus)
                .transitionDate(LocalDateTime.now())
                .performedBy(request.getPerformedBy())
                .notes(request.getNotes())
                .build();

        transitionRepository.save(record);

        batch.setCurrentStatus(nextStatus);
        batch.setCurrentChamber(toChamber);
        batchService.save(batch);

        return BatchTransitionResponse.from(record);
    }

    /**
     * Determines the new {@link BatchStatus} when entering a specific chamber type.
     * All illegal transitions throw {@link IllegalTransitionException}
     */
    private BatchStatus resolveStatusForChamber(Batch batch, ChamberType targetType) {
        BatchStatus current = batch.getCurrentStatus();

        return switch (targetType) {
            case COLD_SMOKE -> {
                if (current != BatchStatus.GREEN) {
                    throw new IllegalTransitionException(
                            "Cold Smoke is only permitted as the first phase from GREEN status. " +
                                    "Current status: " + current.getDisplayName()
                    );
                }
                yield BatchStatus.COLD_SMOKING;
            }

            case FERMENTATION_ROOM -> {
                if (current != BatchStatus.GREEN && current != BatchStatus.COLD_SMOKING) {
                    throw new IllegalTransitionException(
                            "Fermentation Room can only be entered from GREEN or COLD_SMOKING status. " +
                                    "Current status: " + current.getDisplayName()
                    );
                }
                yield BatchStatus.FERMENTING;
            }

            case PRIMARY_AGING_CELLAR -> {
                if (current != BatchStatus.FERMENTING) {
                    throw new IllegalTransitionException(
                            "Batch must complete the Fermentation phase before entering the Primary Aging Cellar. " +
                                    "Current status: " + current.getDisplayName() +
                                    ". Move the batch to a Fermentation Room first."
                    );
                }
                assertMinimumFermentationDuration(batch);
                yield BatchStatus.AGING;
            }
        };
    }

    /**
     * Validates that a null-target transition to RETAIL_READY is legal:
     * the batch must be AGING and have met its target aging duration.
     */
    private BatchStatus resolveRetailReadyTransition(Batch batch) {
        if (batch.getCurrentStatus() != BatchStatus.AGING) {
            throw new IllegalTransitionException(
                    "A batch can only be marked Retail-ready from AGING status. " +
                            "Current status: " + batch.getCurrentStatus().getDisplayName()
            );
        }

        long monthsAged = ChronoUnit.MONTHS.between(batch.getSaltCureStartDate(), LocalDate.now());

        if (monthsAged < batch.getTargetAgingMonths()) {
            throw new IllegalTransitionException(String.format(
                    "Batch '%s' has only aged %d months of the required %d months. " +
                            "Target aging duration has not been met.",
                    batch.getBatchCode(), monthsAged, batch.getTargetAgingMonths()
            ));
        }
        return BatchStatus.RETAIL_READY;
    }

    /**
     * Looks back at the transition history to find when fermentation started,
     * and enforces the minimum fermentation window before aging can begin.
     */
    private void assertMinimumFermentationDuration(Batch batch) {
        transitionRepository.findByBatchIdOrderByTransitionDateAsc(batch.getId())
                .stream()
                .filter(t -> t.getToStatus() == BatchStatus.FERMENTING)
                .reduce((first, second) -> second)
                .ifPresent(fermentStart -> {
                    long hoursInFermentation = ChronoUnit.HOURS.between(fermentStart.getTransitionDate(), LocalDateTime.now());
                    if (hoursInFermentation < MIN_FERMENTATION_HOURS) {
                        throw new IllegalTransitionException(String.format(
                                "Batch '%s' has only fermented for %d hours. " +
                                        "A minimum of %d hours in fermentation is required before aging.",
                                batch.getBatchCode(), hoursInFermentation, MIN_FERMENTATION_HOURS
                        ));
                    }
                });
    }

    private void assertChamberIsActive(Chamber chamber) {
        if (!chamber.isActive()) {
            throw new IllegalArgumentException("Chamber '" + chamber.getName() + "' is deactivated and cannot accept batches.");
        }
    }

    private void assertChamberHasCapacity(Chamber chamber) {
        long current = chamberRepository.countActiveBatchesInChamber(chamber.getId());
        if (current >= chamber.getCapacity()) {
            throw new IllegalArgumentException(String.format(
                    "Chamber '%s' is at full capacity (%d/%d batches). " +
                            "Move existing batches before placing a new one.",
                    chamber.getName(), current, chamber.getCapacity()
            ));
        }
    }

}
