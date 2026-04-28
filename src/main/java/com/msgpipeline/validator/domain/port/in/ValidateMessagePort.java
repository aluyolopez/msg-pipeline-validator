package com.msgpipeline.validator.domain.port.in;

import com.msgpipeline.validator.domain.model.MessagePayload;
import lombok.Builder;
import lombok.Value;

/**
 * =========================================================================
 * CAPA: Dominio — Puerto de Entrada (Input Port / Use Case Interface)
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * Define el contrato del caso de uso de validación.
 * Los adaptadores de entrada (ValidatorHandler, ValidatorController) usan
 * esta interfaz para interactuar con la lógica de negocio.
 *
 * PATRÓN: Use Case Interface (Port In)
 *   En arquitectura hexagonal, los puertos de entrada definen QUÉ puede
 *   hacer el dominio. Los adaptadores (Lambda, REST) son los "driving actors".
 *
 * DIAGRAMA:
 *
 *   [API Gateway]          [HTTP Request (local)]
 *        │                        │
 *   [ValidatorHandler]    [ValidatorController]
 *        │                        │
 *        └───────────┬────────────┘
 *                    │
 *          [ValidateMessagePort]  ← esta interfaz
 *                    │
 *          [ValidateMessageUseCase]
 *                    │
 *          [ValidatorFactory → Strategy]
 *                    │
 *          [MessageQueuePort]
 *                    │
 *        ┌───────────┴──────────────┐
 *  [SqsMessageQueueAdapter]  [InMemoryQueueAdapter]
 *
 * PRINCIPIO: Dependency Inversion (DIP — SOLID)
 *   El adaptador de entrada depende de esta abstracción, no de la implementación.
 *   Permite mockear en tests y cambiar la implementación sin tocar los adaptadores.
 * =========================================================================
 */
public interface ValidateMessagePort {

    /**
     * Valida un mensaje y, si es válido, lo encola en SQS para procesamiento.
     *
     * Responsabilidades del Use Case:
     *   1. Asignar messageId si no viene en el payload (UUID.randomUUID())
     *   2. Asignar createdAt si no viene en el payload (Instant.now())
     *   3. Obtener la estrategia correcta vía ValidatorFactory (Factory Pattern)
     *   4. Ejecutar la validación (Strategy Pattern)
     *   5. Si válido → encolar en SQS vía MessageQueuePort
     *   6. Retornar ValidationResponse con el resultado
     *
     * @param payload Mensaje deserializado del body de la petición
     * @return        Resultado de la validación con messageId y estado
     */
    ValidationResponse validate(MessagePayload payload);

    /**
     * Value Object que encapsula el resultado de la operación.
     *
     * PATRÓN: Value Object (DDD) — inmutable, sin identidad propia.
     * @Immutable (Lombok @Value): todos los campos son final.
     *
     * accepted:      true si la validación fue exitosa y el mensaje fue encolado
     * messageId:     ID del mensaje (generado o el que vino en el payload)
     * errorMessage:  descripción del error si accepted=false, null si accepted=true
     */
    @Value
    @Builder
    class ValidationResponse {
        boolean accepted;
        String  messageId;
        String  errorMessage;

        /** Factory method para resultado exitoso */
        public static ValidationResponse accepted(String messageId) {
            return ValidationResponse.builder()
                    .accepted(true)
                    .messageId(messageId)
                    .errorMessage(null)
                    .build();
        }

        /** Factory method para resultado con error */
        public static ValidationResponse rejected(String errorMessage) {
            return ValidationResponse.builder()
                    .accepted(false)
                    .messageId(null)
                    .errorMessage(errorMessage)
                    .build();
        }
    }
}
