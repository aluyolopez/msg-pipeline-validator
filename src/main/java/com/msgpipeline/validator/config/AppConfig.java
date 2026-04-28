package com.msgpipeline.validator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * =========================================================================
 * CAPA: Infraestructura — Configuración Centralizada de la Aplicación
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * PATRÓN: Configuration Object
 *   Centraliza todas las propiedades de la aplicación en un objeto tipado.
 *
 * @ConfigurationProperties(prefix = "app"):
 *   Lee propiedades que empiezan con "app." desde application.yml
 *   y las mapea a los campos de esta clase automáticamente.
 *
 * EJEMPLO — application.yml:
 *   app:
 *     aws:
 *       region: us-east-1         → aws.region = "us-east-1"
 *       sqs-queue-url: https://... → aws.sqsQueueUrl = "https://..."
 *
 * VENTAJA sobre @Value:
 *   Agrupa propiedades relacionadas. Más legible y mantenible que
 *   múltiples @Value("${app.aws.region}") dispersos por el código.
 *
 * BUENAS PRÁCTICAS de Variables de Entorno:
 *   1. NUNCA hardcodear valores de producción en código fuente
 *   2. Los defaults en application.yml son para desarrollo local
 *   3. En Lambda: configurar Variables de Entorno en la consola AWS
 *   4. Formato en application.yml: ${VARIABLE_ENTORNO:valor_default}
 * =========================================================================
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    /** Configuración específica de AWS */
    private Aws aws = new Aws();

    /**
     * Propiedades AWS para el validador.
     *
     * VARIABLES DE ENTORNO REQUERIDAS EN LAMBDA:
     *   SQS_QUEUE_URL = https://sqs.us-east-1.amazonaws.com/{accountId}/msg-pipeline-queue
     *   AWS_REGION    = us-east-1 (Lambda la inyecta automáticamente)
     */
    @Data
    public static class Aws {

        /**
         * Región AWS donde están desplegados los servicios.
         * En Lambda: la variable AWS_REGION_NAME la inyecta el runtime.
         */
        private String region = "us-east-1";

        /**
         * URL completa de la cola SQS a la que se envían los mensajes validados.
         * Formato: https://sqs.{region}.amazonaws.com/{accountId}/{queue-name}
         * En Lambda: configurar como variable de entorno SQS_QUEUE_URL.
         */
        private String sqsQueueUrl = "";
    }
}
