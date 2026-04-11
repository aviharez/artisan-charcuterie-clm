package com.project.artisancharcuterie.service;

import com.project.artisancharcuterie.domain.Farm;
import com.project.artisancharcuterie.dto.request.FarmRequest;
import com.project.artisancharcuterie.dto.response.FarmResponse;
import com.project.artisancharcuterie.exception.ResourceNotFoundException;
import com.project.artisancharcuterie.repository.FarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FarmService {

    private final FarmRepository farmRepository;

    public List<FarmResponse> findAll() {
        return farmRepository.findAll().stream()
                .map(FarmResponse::from)
                .toList();
    }

    public FarmResponse findById(Long id) {
        return FarmResponse.from(getFarm(id));
    }

    @Transactional
    public FarmResponse create(FarmRequest request) {
        Farm farm = Farm.builder()
                .name(request.getName())
                .location(request.getLocation())
                .breed(request.getBreed())
                .contactInfo(request.getContactInfo())
                .certifications(request.getCertification())
                .build();
        return FarmResponse.from(farmRepository.save(farm));
    }

    @Transactional
    public FarmResponse update(Long id, FarmRequest request) {
        Farm farm = getFarm(id);
        farm.setName(request.getName());
        farm.setLocation(request.getLocation());
        farm.setBreed(request.getBreed());
        farm.setContactInfo(request.getContactInfo());
        farm.setCertifications(request.getCertification());
        return FarmResponse.from(farmRepository.save(farm));
    }

    @Transactional
    public void delete(Long id) {
        Farm farm = getFarm(id);
        farmRepository.delete(farm);
    }

    Farm getFarm(Long id) {
        return farmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", id));
    }
}
