package com.msgpipeline.validator.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * =========================================================================
 * CAPA: Infraestructura — DTO de Entrada (Data Transfer Object)
 * ARQUITECTURA: Hexagonal
 * PERFIL: 'local' (solicitudes HTTP para pruebas con Swagger UI)
 * =========================================================================
 *
 * PATRÓN: DTO (Data Transfer Object)
 *   Separa el modelo de la API (HTTP) del modelo del dominio (MessagePayload).
 *   Cambiar el contrato HTTP no afecta al dominio y viceversa.
 *
 * VALIDACIONES Bean Validation (Jakarta Validation):
 *   @NotBlank: campo obligatorio, no puede ser nulo ni solo espacios.
 *   @Min/@Max: rango numérico para el campo priority.
 *   Las validaciones se ejecutan automáticamente en el controlador con @Valid.
 *
 * OPENAPI — Anotaciones @Schema:
 *   Documentan el campo en Swagger UI con descripción, ejemplo y valores permitidos.
 *   Solo tienen efecto en tiempo de generación de documentación.
 *
 * FLUJO LOCAL:
 *   HTTP POST body JSON → (deserialization) → ValidateRequest
 *   → ValidatorController → MessagePayload (dominio) → Use Case
 * =========================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud de validación de un mensaje (simula el request desde API Gateway)")
public class ValidateRequest {

    @NotBlank(message = "El tipo de mensaje es obligatorio")
    @Schema(
        description = "Tipo de mensaje — determina la estrategia de validación aplicada",
        example = "NOTIFICATION",
        allowableValues = {"NOTIFICATION", "RECORD"}
    )
    private String messageType;

    @Schema(
        description = "Canal de entrega (obligatorio para NOTIFICATION)",
        example = "EMAIL",
        allowableValues = {"EMAIL", "SMS", "PUSH"}
    )
    private String channel;

    @Schema(
        description = "Email del destinatario (obligatorio para NOTIFICATION)",
        example = "estudiante@ankuacademy.com"
    )
    private String recipientEmail;

    @NotBlank(message = "El contenido del mensaje es obligatorio")
    @Schema(
        description = "Cuerpo del mensaje. NOTIFICATION: 10-1000 chars. RECORD: hasta 5000 chars.",
        example = "Bienvenido al curso Especialista Spring Boot + AWS Serverless — Sesión 04"
    )
    private String content;

    @Min(value = 1, message = "La prioridad debe ser mínimo 1")
    @Max(value = 5, message = "La prioridad debe ser máximo 5")
    @Schema(
        description = "Prioridad del registro (solo para RECORD). Rango: 1=baja … 5=crítica",
        example = "3",
        minimum = "1",
        maximum = "5"
    )
    private Integer priority;
}
