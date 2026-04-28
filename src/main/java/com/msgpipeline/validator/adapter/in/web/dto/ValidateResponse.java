package com.msgpipeline.validator.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * =========================================================================
 * CAPA: Infraestructura — DTO de Salida (Data Transfer Object)
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * PATRÓN: DTO (Data Transfer Object)
 *   Respuesta estandarizada del endpoint de validación.
 *   Oculta los detalles internos del dominio exponiendo solo
 *   lo relevante para el cliente HTTP (Postman, Swagger, API Gateway).
 *
 * CAMPOS:
 *   success    → true si el mensaje fue validado y encolado exitosamente
 *   messageId  → ID único del mensaje (presente si success=true)
 *   messageType → tipo de mensaje validado
 *   message    → descripción del resultado (éxito o error)
 *
 * ESTADOS HTTP:
 *   202 Accepted  → success=true  (válido y encolado en SQS)
 *   400 Bad Request → success=false (error de validación)
 *   500 Internal Server Error → error inesperado
 * =========================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta del proceso de validación del mensaje")
public class ValidateResponse {

    @Schema(description = "true si el mensaje fue validado y encolado, false si hubo error de validación",
            example = "true")
    private boolean success;

    @Schema(description = "ID único del mensaje (presente si success=true)",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String messageId;

    @Schema(description = "Tipo de mensaje procesado",
            example = "NOTIFICATION")
    private String messageType;

    @Schema(description = "Mensaje descriptivo del resultado de la validación",
            example = "Mensaje aceptado para procesamiento asíncrono")
    private String message;
}
