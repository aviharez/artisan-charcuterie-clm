package com.project.artisancharcuterie.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request body for creating or updating a farm of origin")
public class FarmRequest {

    @NotBlank(message = "Farm name is required")
    @Size(max = 150, message = "Farm name must no exceed 150 characters")
    @Schema(description = "Legal name of the farm", example = "Parma Heritage Farm")
    private String name;

    @NotBlank(message = "Location is required")
    @Size(max = 200, message = "Location must not exceed 200 characters")
    @Schema(description = "Geographic location of the farm", example = "Parma, Emilia-Romagna, Italy")
    private String location;

    @NotBlank(message = "Primary breed is required")
    @Size(max = 100, message = "Breed must exceed 100 characters")
    @Schema(description = "Primary animal breed raised at this farm", example = "Large White x Landrace")
    private String breed;

    @Size(max = 200, message = "Contact info must no exceed 200 characters")
    @Schema(description = "Contact email or phone number", example = "info@parmaheritage.it")
    private String contactInfo;

    @Size(max = 300, message = "Certifications must not exceed 300 characters")
    @Schema(description = "Comma-separated list of certifications", example = "DOR, IGP, Organic EU")
    private String certification;

}
