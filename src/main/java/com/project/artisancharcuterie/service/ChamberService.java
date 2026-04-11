package com.project.artisancharcuterie.service;

import com.project.artisancharcuterie.domain.Chamber;
import com.project.artisancharcuterie.dto.request.ChamberRequest;
import com.project.artisancharcuterie.dto.response.ChamberResponse;
import com.project.artisancharcuterie.exception.ResourceNotFoundException;
import com.project.artisancharcuterie.repository.ChamberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChamberService {

    private final ChamberRepository chamberRepository;

    public List<ChamberResponse> findAll() {
        return chamberRepository.findAll().stream()
                .map(c -> ChamberResponse.from(c,
                        chamberRepository.countActiveBatchesInChamber(c.getId())))
                .toList();
    }

    public ChamberResponse findById(Long id) {
        Chamber chamber = getChamber(id);
        return ChamberResponse.from(chamber,
                chamberRepository.countActiveBatchesInChamber(id));
    }

    @Transactional
    public ChamberResponse create(ChamberRequest request) {
        Chamber chamber = Chamber.builder()
                .name(request.getName())
                .chamberType(request.getChamberType())
                .targetTemperatureCelsius(request.getTargetTemperatureCelsius())
                .targetHumidityPercent(request.getTargetHumidityPercent())
                .capacity(request.getCapacity())
                .isActive(true)
                .build();
        return ChamberResponse.from(chamberRepository.save(chamber), 0L);
    }

    @Transactional
    public ChamberResponse update(Long id, ChamberRequest request) {
        Chamber chamber = getChamber(id);
        chamber.setName(request.getName());
        chamber.setChamberType(request.getChamberType());
        chamber.setTargetTemperatureCelsius(request.getTargetTemperatureCelsius());
        chamber.setTargetHumidityPercent(request.getTargetHumidityPercent());
        chamber.setCapacity(request.getCapacity());
        chamberRepository.save(chamber);
        return ChamberResponse.from(chamber, chamberRepository.countActiveBatchesInChamber(id));
    }

    @Transactional
    public ChamberResponse deactivate(Long id) {
        Chamber chamber = getChamber(id);
        chamber.setActive(false);
        chamberRepository.save(chamber);
        return ChamberResponse.from(chamber, chamberRepository.countActiveBatchesInChamber(id));
    }

    Chamber getChamber(Long id) {
        return chamberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chamber", id));
    }

}
