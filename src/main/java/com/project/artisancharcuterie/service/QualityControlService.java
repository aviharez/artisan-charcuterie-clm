package com.project.artisancharcuterie.service;

import com.project.artisancharcuterie.domain.Batch;
import com.project.artisancharcuterie.domain.QualityControlLog;
import com.project.artisancharcuterie.dto.request.QualityControlLogRequest;
import com.project.artisancharcuterie.dto.response.QualityControlLogResponse;
import com.project.artisancharcuterie.exception.ImmutableResourceException;
import com.project.artisancharcuterie.exception.ResourceNotFoundException;
import com.project.artisancharcuterie.repository.QualityControlLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QualityControlService {

    private final QualityControlLogRepository qcLogRepository;
    private final BatchService batchService;

    public List<QualityControlLogResponse> findByBatch(Long batchId) {
        batchService.getBatch(batchId);
        return qcLogRepository.findByBatchIdOrderByWeekNumberAsc(batchId)
                .stream()
                .map(QualityControlLogResponse::from)
                .toList();
    }

    public QualityControlLogResponse findById(Long logId) {
        return QualityControlLogResponse.from(getLog(logId));
    }

    @Transactional
    public QualityControlLogResponse log(Long batchId, QualityControlLogRequest request) {
        Batch batch = batchService.getBatch(batchId);

        if (!batch.getCurrentStatus().isQcLoggable()) {
            throw new IllegalArgumentException(String.format(
                    "QC logs cannot be filled for a batch with status '%s'. " +
                            "Batches must have progressed past GREEN and not be REJECTED.",
                    batch.getCurrentStatus().getDisplayName()
            ));
        }

        if (qcLogRepository.existsByBatchIdAndWeekNumber(batchId, request.getWeekNumber())) {
            throw new IllegalArgumentException(String.format(
                    "A QC log for batch '%s' at week %d already exists and is immutable. " +
                            "Each batch may only have one QC log per week.",
                    batch.getBatchCode(), request.getWeekNumber()
            ));
        }

        QualityControlLog entry = QualityControlLog.builder()
                .batch(batch)
                .phLevel(request.getPhLevel())
                .aromaProfile(request.getAromaProfile())
                .inspector(request.getInspector())
                .weekNumber(request.getWeekNumber())
                .notes(request.getNotes())
                .loggedAt(LocalDateTime.now())
                .build();

        return QualityControlLogResponse.from(qcLogRepository.save(entry));
    }

    /**
     * QC logs are immutable by regulatory requirement.
     * Any attempt to update or delete a log entry returns 422.
     */
    public void rejectUpdate(Long logId) {
        throw new ImmutableResourceException("QualityControlLog", logId);
    }

    public void rejectDelete(Long logId) {
        throw new ImmutableResourceException("QualityControlLog", logId);
    }

    private QualityControlLog getLog(Long id) {
        return qcLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("QualityControlLog", id));
    }

}
