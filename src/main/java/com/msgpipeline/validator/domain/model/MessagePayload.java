package com.msgpipeline.validator.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * =========================================================================
 * CLASE: MessagePayload -- Entidad del Dominio
 * CAPA: Dominio -- Modelo de Negocio (Value Object)
 * ARQUITECTURA: Hexagonal + Clean Architecture
 * =========================================================================
 *
 * Representa el payload del mensaje recibido de Step Functions Task State.
 *
 * CAMPOS SESION 07:
 *   messageId      --> ID generado por el Orchestrator (UUID)
 *   messageType    --> EMAIL | SMS | PUSH_NOTIFICATION
 *   channel        --> EMAIL | SMS | PUSH
 *   recipientEmail --> email del destinatario (obligatorio si EMAIL)
 *   content        --> contenido del mensaje (obligatorio)
 *   userEmail      --> email del usuario autenticado con Cognito JWT
 *
 * @JsonIgnoreProperties: tolera campos adicionales del input de Step Functions.
 * =========================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessagePayload {

    /** ID del mensaje generado por el Orchestrator (UUID) */
    private String messageId;

    /**
     * Tipo de mensaje -- determina la estrategia de validacion:
     *   EMAIL             --> EmailMessageValidator
     *   SMS               --> SmsMessageValidator
     *   PUSH_NOTIFICATION --> PushMessageValidator
     */
    private String messageType;

    /** Canal: EMAIL | SMS | PUSH */
    private String channel;

    /** Email del destinatario (obligatorio si messageType=EMAIL) */
    private String recipientEmail;

    /** Contenido del mensaje (obligatorio para todos los tipos) */
    private String content;

    /** Email del usuario autenticado con Cognito JWT */
    private String userEmail;

    /** Timestamp de envio del Orchestrator */
    private String submittedAt;

    /** Fuente del mensaje */
    private String source;

    /** ID del request de API Gateway */
    private String requestId;
}
