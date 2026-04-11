package com.project.artisancharcuterie.dto.response;

import com.project.artisancharcuterie.domain.Farm;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Farm of origin details")
public class FarmResponse {

    private Long id;
    private String name;
    private String location;
    private String breed;
    private String contactInfo;
    private String certifications;
    private LocalDateTime createdAt;

    public static FarmResponse from(Farm farm) {
        return FarmResponse.builder()
                .id(farm.getId())
                .name(farm.getName())
                .location(farm.getLocation())
                .breed(farm.getBreed())
                .contactInfo(farm.getContactInfo())
                .certifications(farm.getCertifications())
                .createdAt(farm.getCreatedAt())
                .build();
    }
}
