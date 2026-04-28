package com.msgpipeline.validator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * =========================================================================
 * CAPA: Infraestructura — Configuración OpenAPI / Swagger UI
 * PERFIL: 'local' (Swagger solo disponible en desarrollo local)
 * =========================================================================
 *
 * SpringDoc genera documentación OpenAPI 3.0 automáticamente a partir de:
 *   1. Las anotaciones @RestController y @RequestMapping (ValidatorController)
 *   2. Las anotaciones @Operation, @ApiResponse, @Schema en el código
 *   3. La configuración de este bean OpenAPI (metadatos generales)
 *
 * URL DE ACCESO (perfil local):
 *   Swagger UI:   http://localhost:8081/swagger-ui.html
 *   JSON OpenAPI: http://localhost:8081/v3/api-docs
 *
 * ¿POR QUÉ NO EN LAMBDA?
 *   En Lambda no hay servidor HTTP (WebApplicationType.NONE).
 *   Swagger UI requiere un servidor HTTP para servir la interfaz web.
 *   En AWS, la documentación se gestiona desde API Gateway (importando el YAML).
 * =========================================================================
 */
@Configuration
@Profile("local")
public class OpenApiConfig {

    /**
     * Configura los metadatos generales de la API para Swagger UI.
     *
     * @return Objeto OpenAPI con la descripción completa de la API
     */
    @Bean
    public OpenAPI validatorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("msg-pipeline-validator API")
                        .description(
                            "**Sesión 04 — SNS + Lambda Validator + SOLID + Patrones de Diseño**\n\n" +
                            "Este microservicio valida los mensajes antes de encolados en SQS.\n\n" +
                            "**Flujo de producción:** API Gateway → **Lambda Validator** → SQS → Lambda Processor → DynamoDB + SNS\n\n" +
                            "**Flujo local (testing):** HTTP POST → ValidatorController → Use Case → InMemoryQueueAdapter\n\n" +
                            "### Patrones de Diseño aplicados:\n" +
                            "- **Strategy**: `NotificationValidator` y `RecordValidator` implementan `ValidationStrategy`\n" +
                            "- **Factory**: `ValidatorFactory.getStrategy(messageType)` selecciona la estrategia\n" +
                            "- **Hexagonal**: puertos y adaptadores desacoplan el dominio de la infraestructura\n\n" +
                            "### Tipos de mensaje soportados:\n" +
                            "- `NOTIFICATION`: valida email, canal (EMAIL/SMS/PUSH), contenido (10-1000 chars)\n" +
                            "- `RECORD`: valida contenido (max 5000 chars), prioridad obligatoria (1-5)\n\n" +
                            "⚠️ Los endpoints aquí solo existen en el perfil `local`. " +
                            "En AWS Lambda, las solicitudes las recibe API Gateway."
                        )
                        .version("1.0.0-sesion-04")
                        .contact(new Contact()
                                .name("Anku Academy")
                                .url("https://ankuacademy.com"))
                        .license(new License()
                                .name("Uso educativo — Anku Academy 2026C2"))
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("Servidor local de desarrollo — perfil 'local'")
                ));
    }
}
