package com.msgpipeline.validator.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * =========================================================================
 * CAPA: Dominio — Entidad del Mensaje (Value Object)
 * ARQUITECTURA: Hexagonal + Clean Architecture
 * =========================================================================
 *
 * Representa el payload del mensaje que llega desde API Gateway
 * y que será validado antes de encolarse en SQS.
 *
 * PATRÓN: DTO / Value Object (DDD)
 *   Transfiere datos entre capas sin lógica de negocio propia.
 *   La lógica de validación vive en las estrategias (Strategy Pattern).
 *
 * PRINCIPIO: Open/Closed (OCP — SOLID)
 *   Para agregar nuevos tipos de mensaje: crear una nueva ValidationStrategy
 *   y registrarla en ValidatorFactory. No modificar esta clase.
 *
 * CAMPOS:
 *   messageId     → ID único (generado si no viene en el request)
 *   messageType   → NOTIFICATION | RECORD (determina la estrategia de validación)
 *   channel       → EMAIL | SMS | PUSH (canal de entrega para NOTIFICATION)
 *   recipientEmail → email del destinatario (obligatorio en NOTIFICATION)
 *   content        → cuerpo del mensaje (obligatorio en ambos tipos)
 *   priority       → prioridad 1-5 (obligatorio en RECORD)
 *   createdAt      → timestamp ISO-8601 de creación (generado si no viene)
 *
 * @JsonIgnoreProperties(ignoreUnknown = true):
 *   Tolera campos adicionales en el JSON de entrada sin lanzar excepción.
 *   Garantiza compatibilidad hacia adelante cuando el schema evoluciona.
 * =========================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessagePayload {

    /**
     * Identificador único del mensaje.
     * Si no viene en el request, el Use Case genera uno con UUID.randomUUID().
     * Es la Partition Key en DynamoDB cuando el mensaje sea procesado.
     */
    private String messageId;

    /**
     * Tipo de mensaje — determina la estrategia de validación a aplicar.
     * NOTIFICATION → NotificationValidator (valida email, canal, contenido)
     * RECORD        → RecordValidator (valida contenido, prioridad)
     * PATRÓN Factory: ValidatorFactory selecciona la estrategia según este campo.
     */
    private String messageType;

    /**
     * Canal de entrega del mensaje.
     * Para NOTIFICATION: EMAIL | SMS | PUSH (obligatorio).
     * Para RECORD: no requerido.
     */
    private String channel;

    /**
     * Email del destinatario.
     * Obligatorio para mensajes de tipo NOTIFICATION.
     * Opcional para RECORD (no tiene destinatario externo directo).
     */
    private String recipientEmail;

    /**
     * Contenido o cuerpo del mensaje.
     * Obligatorio para ambos tipos.
     * NOTIFICATION: máximo 1000 caracteres.
     * RECORD: máximo 5000 caracteres.
     */
    private String content;

    /**
     * Prioridad del mensaje (solo para tipo RECORD).
     * Rango válido: 1 (baja) a 5 (crítica).
     * Determina la urgencia del registro de auditoría.
     */
    private Integer priority;

    /**
     * Timestamp ISO-8601 de creación del mensaje.
     * Si no viene en el request, se genera en el Use Case.
     */
    private String createdAt;
}
