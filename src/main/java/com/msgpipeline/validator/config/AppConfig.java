package com.msgpipeline.validator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configuracion del Validator Lambda. No requiere variables de entorno AWS. */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private Aws aws = new Aws();

    @Data
    public static class Aws {
        private String region = "us-east-1";
    }
}
