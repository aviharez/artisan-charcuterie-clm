package com.project.artisancharcuterie.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI clmApiConfig() {
        return new OpenAPI()
                .info(new Info()
                        .title("Artisan Charcuterie - Curing Lifecycle Management API")
                        .description("""
                                The Marrow & Salt Collective's CLM system manages the full lifecycle of
                                long-term dry-cured meat batches (Prosciutto, Bresaola, Culatello).
                                
                                Key Capabilities:
                                - Batch lifecycle management from Green (raw) through Retail-Ready
                                - Chamber transition workflow with strict enforcement rules
                                - Real-time environmental sensor data ingestion and stress alerts
                                - Immutable QC logging for regulatory compliance
                                - Dynamic inventory valuation with weight-loss projection engine
                                """)
                        .version("1.0.0")
                )
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development")
                ));
    }

}
