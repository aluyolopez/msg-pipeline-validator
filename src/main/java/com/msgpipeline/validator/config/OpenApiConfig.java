package com.msgpipeline.validator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("msg-pipeline-validator -- Sesion 07")
                        .description("Validator Lambda: Step Functions Task con Strategy + Factory")
                        .version("1.0.0"));
    }
}
