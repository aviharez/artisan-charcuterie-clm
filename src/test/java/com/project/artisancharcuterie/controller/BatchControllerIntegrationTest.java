package com.project.artisancharcuterie.controller;

import com.project.artisancharcuterie.domain.enums.ProductType;
import com.project.artisancharcuterie.dto.request.BatchCreateRequest;
import com.project.artisancharcuterie.dto.request.FarmRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Batch API - integration tests")
class BatchControllerIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    @DisplayName("POST /api/farms + POST /api/batches creates batch with correct code format")
    void createBatchWithFarm() throws Exception {
        FarmRequest farmRequest = new FarmRequest();
        farmRequest.setName("Test Farm IT");
        farmRequest.setLocation("Parma, Italy");
        farmRequest.setBreed("Large White");
        farmRequest.setContactInfo("test@farm.it");

        String farmJson = mvc.perform(post("/api/farms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(farmRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn().getResponse().getContentAsString();

        Long farmId = mapper.readTree(farmJson).get("id").asLong();

        BatchCreateRequest batchRequest = new BatchCreateRequest();
        batchRequest.setFarmId(farmId);
        batchRequest.setProductType(ProductType.PROSCIUTTO);
        batchRequest.setAnimalBreed("Large White x Landrace");
        batchRequest.setInitialWeightKg(new BigDecimal("12.500"));
        batchRequest.setSaltCureStartDate(LocalDate.now().minusMonths(6));

        mvc.perform(post("/api/batches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(batchRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.batchCode", startsWith("PRO-")))
                .andExpect(jsonPath("$.currentStatus").value("GREEN"))
                .andExpect(jsonPath("$.farmId").value(farmId))
                .andExpect(jsonPath("$.targetAgingMonths").value(24));
    }

    @Test
    @DisplayName("POST /api/batches with non-existent farm returns 404")
    void createBatchWithInvalidFarmReturns404() throws Exception {
        BatchCreateRequest batchReq = new BatchCreateRequest();
        batchReq.setFarmId(99999L);
        batchReq.setProductType(ProductType.BRESAOLA);
        batchReq.setAnimalBreed("Chianina");
        batchReq.setInitialWeightKg(new BigDecimal("8.000"));
        batchReq.setSaltCureStartDate(LocalDate.now().minusMonths(1));

        mvc.perform(post("/api/batches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(batchReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("POST /api/batches without farmId returns 400 with field error")
    void createBatchWithoutFarmIdReturns400() throws Exception {
        BatchCreateRequest batchRequest = new BatchCreateRequest();
        batchRequest.setProductType(ProductType.PROSCIUTTO);
        batchRequest.setAnimalBreed("Large White");
        batchRequest.setInitialWeightKg(new BigDecimal("10.000"));
        batchRequest.setSaltCureStartDate(LocalDate.now().minusMonths(3));

        mvc.perform(post("/api/batches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(batchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.farmId").exists());
    }

    @Test
    @DisplayName("GET /api/batches/{id} for non-existent batch returns standardized 404")
    void getNonExistentBatchReturns404() throws Exception {
        mvc.perform(get("/api/batches/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").exists());
    }

    @Test
    @DisplayName("GET /api/batches with status filter returns only matching batches")
    void listBatchesWithStatusFilter() throws Exception {
        mvc.perform(get("/api/batches").param("status", "GREEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Seeded farms are accessible via GET /api/farms")
    void seededFarmsAreAccessible() throws Exception {
        mvc.perform(get("/api/farms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(4))))
                .andExpect(jsonPath("$[0].name").exists());
    }
}
