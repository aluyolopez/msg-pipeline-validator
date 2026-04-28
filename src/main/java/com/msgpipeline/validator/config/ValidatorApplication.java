package com.msgpipeline.validator.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * =========================================================================
 * CLASE: ValidatorApplication
 * PROPÓSITO: Punto de entrada para ejecución LOCAL y bootstrap de Spring Context
 * =========================================================================
 *
 * ¿POR QUÉ EXISTE ESTA CLASE SI EL HANDLER LAMBDA NO LLAMA A main()?
 *
 *   1. MODO LOCAL: main() arranca el servidor web Tomcat para desarrollo
 *      local con Swagger UI y REST controller (ValidatorController).
 *
 *   2. MODO LAMBDA: ValidatorHandler llama a SpringApplicationBuilder(ValidatorApplication.class)
 *      en su bloque static para inicializar SOLO el contexto IoC (sin servidor web).
 *      Esta clase NO es el handler Lambda — es la raíz de configuración de Spring.
 *
 * DIFERENCIA FUNDAMENTAL:
 *
 *   ┌─────────────────────────────────────────────────────────────────┐
 *   │  MODO LOCAL (perfil 'local')                                    │
 *   │  main() → SpringApplication.run() → Tomcat activo → Swagger UI  │
 *   │  URL: http://localhost:8081/swagger-ui.html                     │
 *   ├─────────────────────────────────────────────────────────────────┤
 *   │  MODO LAMBDA (perfil 'aws')                                     │
 *   │  Lambda → new ValidatorHandler() → static{} → Spring (NONE)    │
 *   │  → handleRequest(APIGatewayProxy...) → Use Case → SQS          │
 *   │  (main() NO se llama en ningún momento en Lambda)               │
 *   └─────────────────────────────────────────────────────────────────┘
 *
 * CÓMO EJECUTAR LOCALMENTE:
 *   ./gradlew bootRun --args='--spring.profiles.active=local'
 *   → Arranca en http://localhost:8081
 *   → Swagger UI en http://localhost:8081/swagger-ui.html
 *
 * @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan
 * scanBasePackages: escanea TODO el paquete 'com.msgpipeline.validator'
 *   → Encuentra ValidateMessageUseCase, adapters, configs, etc.
 * =========================================================================
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "com.msgpipeline.validator")
@EnableConfigurationProperties(AppConfig.class)
public class ValidatorApplication {

    public static void main(String[] args) {
        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║  msg-pipeline-validator — Modo LOCAL (Sesión 04)        ║");
        log.info("║  Swagger UI: http://localhost:8081/swagger-ui.html      ║");
        log.info("║  Patrón: Strategy + Factory para validaciones SOLID     ║");
        log.info("╚══════════════════════════════════════════════════════════╝");

        SpringApplication.run(ValidatorApplication.class, args);
    }
}
